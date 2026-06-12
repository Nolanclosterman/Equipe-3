package com.equipe3.backend.service;

import com.equipe3.backend.model.ChatMessage;
import com.equipe3.backend.model.Company;
import com.equipe3.backend.model.DecisionRecord;
import com.equipe3.backend.model.EventOutcome;
import com.equipe3.backend.model.GameEvent;
import com.equipe3.backend.model.Profile;
import com.equipe3.backend.model.Scores;
import com.equipe3.backend.repository.ProfileRepository;
import com.equipe3.backend.web.ApiExceptions.ConflictException;
import com.equipe3.backend.web.ApiExceptions.NotFoundException;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Business rules around the single company a player can own: company lifecycle
 * (create / start / delete) and the game loop (event generation, decision
 * scoring, help chat).
 */
@Service
public class ProfileService {

    private static final Logger log = LoggerFactory.getLogger(ProfileService.class);

    private final ProfileRepository repository;
    private final AnthropicGameService ai;
    private final IllustrationService illustrations;
    private final NameModerationService moderation;

    /** Runs the background prefetch of the next event (text + images). */
    private final ExecutorService prefetchPool = Executors.newFixedThreadPool(2);

    public ProfileService(ProfileRepository repository, AnthropicGameService ai,
                          IllustrationService illustrations, NameModerationService moderation) {
        this.repository = repository;
        this.ai = ai;
        this.illustrations = illustrations;
        this.moderation = moderation;
    }

    /** GET /profiles/{name} — returns (and lazily creates) the connected profile. */
    public Profile getProfile(String name) {
        return repository.findOrCreate(name);
    }

    /** POST /profiles/{name}/company — create a company only if none exists. */
    public Company createCompany(String name, String companyName, String type,
                                 String character, String avatar) {
        Profile profile = repository.findOrCreate(name);
        if (profile.hasCompany()) {
            throw new ConflictException("Profile '" + name + "' already owns a company.");
        }
        // Guardrail on everything the player typed (kids' game).
        moderation.validate("le nom de l'entreprise", companyName);
        moderation.validate("l'activité", type);
        moderation.validate("le nom de ton avatar", character);

        Company company = new Company(companyName);
        company.setType(type);
        company.setCharacter(character);
        company.setAvatar(avatar);
        profile.setCompany(company);
        repository.save(profile);
        return company;
    }

    /** POST /profiles/{name}/company/start — activate the existing company. */
    public Company startCompany(String name) {
        Company company = requireCompany(name);
        company.setActive(true);
        // The logo appears at the top of the game frame: generate it in the
        // background as soon as the adventure starts, no player action needed.
        if (illustrations.isEnabled()) {
            illustrations.companyIconAsync(company);
        } else if (company.getIconUrl() == null || company.getIconUrl().isBlank()) {
            // No OpenAI key: deterministic placeholder so the brand still shows.
            String seed = company.getName() == null ? "company" : company.getName().trim();
            company.setIconUrl("https://api.dicebear.com/9.x/shapes/svg?seed="
                    + java.net.URLEncoder.encode(seed, java.nio.charset.StandardCharsets.UTF_8));
        }
        if (company.getChat().isEmpty()) {
            company.getChat().add(ChatMessage.assistant(
                    "Salut ! Bienvenue à la tête de « " + company.getName()
                            + " ». Je suis ton assistant. Pose-moi des questions pour t'aider à décider ! 🚀"));
        }
        return company;
    }

    /** DELETE /profiles/{name}/company — remove the existing company. */
    public void deleteCompany(String name) {
        Profile profile = requireProfile(name);
        if (!profile.hasCompany()) {
            throw new NotFoundException("Profile '" + name + "' has no company to delete.");
        }
        profile.setCompany(null);
        repository.save(profile);
    }

    /** POST /profiles/{name}/company/chat — append a player message and the AI reply. */
    public Company addChatMessage(String name, String message) {
        Company company = requireCompany(name);
        company.getChat().add(ChatMessage.user(message));
        String reply = ai.chat(company, message);
        company.getChat().add(ChatMessage.assistant(reply));
        return company;
    }

    /** POST /profiles/{name}/company/icon — (re)generate the company icon. */
    public Company generateIcon(String name) {
        Company company = requireCompany(name);
        if (illustrations.isEnabled()) {
            company.setIconUrl(illustrations.companyIcon(company));
        } else {
            // No OpenAI key: deterministic placeholder icon from the name.
            String seed = company.getName() == null ? "company" : company.getName().trim();
            company.setIconUrl("https://api.dicebear.com/9.x/shapes/svg?seed="
                    + java.net.URLEncoder.encode(seed, java.nio.charset.StandardCharsets.UTF_8));
        }
        return company;
    }

    /** POST /profiles/{name}/company/event — generate the next event of the loop. */
    public Company generateEvent(String name) {
        Company company = requireCompany(name);
        if (company.isGameOver()) {
            throw new ConflictException("The game is over for '" + name + "'.");
        }
        if (company.getEventNumber() >= Company.MAX_EVENTS) {
            company.setGameOver(true);
            return company;
        }
        // Use the event prefetched right after the previous decision when
        // available — it may already carry some illustrations — and only fall
        // back to a blocking AI call when there is none (first event, errors).
        GameEvent prefetched = awaitPrefetched(company);
        if (prefetched != null) {
            synchronized (company) {
                // Take the freshest copy: images may have landed since the
                // future completed. Later illustration updates now target
                // currentEvent (same problem text).
                GameEvent next = company.getNextEvent() != null ? company.getNextEvent() : prefetched;
                company.setCurrentEvent(next);
                company.setNextEvent(null);
                company.setNextEventFuture(null);
            }
        } else {
            GameEvent event = ai.generateEvent(company);
            company.setCurrentEvent(event);
            // Illustrate the event, the solutions and the presenting character
            // in the background; the frontend polls until the URLs appear.
            illustrations.illustrateEvent(company, event);
        }
        company.setLastOutcome(null);
        company.setEventNumber(company.getEventNumber() + 1);
        return company;
    }

    /** POST /profiles/{name}/company/decision — score the chosen solution. */
    public Company decide(String name, String solution) {
        Company company = requireCompany(name);
        GameEvent event = company.getCurrentEvent();
        if (event == null) {
            throw new ConflictException("No current event to decide on for '" + name + "'.");
        }
        if (company.getLastOutcome() != null) {
            throw new ConflictException("This event has already been decided for '" + name + "'.");
        }

        // Record the decision and start prefetching the next event (text +
        // images) IMMEDIATELY, in parallel with the scoring call below — the
        // event generation prompt only uses the problem/solution history and
        // the current indicators, not the narrative outcome. The indicators it
        // sees are one decision behind, an acceptable tradeoff for starting
        // the image pipeline several seconds earlier.
        company.getHistory().add(new DecisionRecord(event.problem(), solution, ""));
        prefetchNextEvent(company);

        EventOutcome outcome = ai.scoreDecision(company, event.problem(), solution);
        Scores scores = company.getScores();
        scores.setMoney(scores.getMoney() + outcome.moneyDelta());
        scores.setEcology(scores.getEcology() + outcome.ecologyDelta());
        scores.setEthics(scores.getEthics() + outcome.ethicsDelta());

        company.setLastOutcome(outcome);
        // Fill in the narrative on the record added before scoring.
        List<DecisionRecord> history = company.getHistory();
        history.set(history.size() - 1,
                new DecisionRecord(event.problem(), solution, outcome.narrative()));

        // End conditions (Design §8): any indicator reaches 0, or the 15th event is done.
        if (scores.getMoney() <= 0 || scores.getEcology() <= 0 || scores.getEthics() <= 0
                || company.getEventNumber() >= Company.MAX_EVENTS) {
            company.setGameOver(true);
            // The optimistically prefetched next event is now useless.
            cancelPrefetch(company);
        }
        return company;
    }

    /** Drops a pending prefetch (the game ended before it was needed). */
    private void cancelPrefetch(Company company) {
        synchronized (company) {
            CompletableFuture<GameEvent> future = company.getNextEventFuture();
            if (future != null) {
                future.cancel(true);
            }
            company.setNextEvent(null);
            company.setNextEventFuture(null);
        }
    }

    /** Generates the next event in the background and stores it on the company. */
    private void prefetchNextEvent(Company company) {
        CompletableFuture<GameEvent> future = CompletableFuture.supplyAsync(() -> {
            GameEvent event = ai.generateEvent(company);
            synchronized (company) {
                company.setNextEvent(event);
            }
            // Illustrations attach to nextEvent while it waits, then follow it
            // to currentEvent once promoted (matched by problem text).
            illustrations.illustrateEvent(company, event);
            return event;
        }, prefetchPool);
        company.setNextEventFuture(future);
    }

    /**
     * Returns the prefetched next event, waiting briefly if its generation is
     * still in flight; null when there is nothing prefetched (or it failed),
     * in which case the caller generates synchronously.
     */
    private GameEvent awaitPrefetched(Company company) {
        CompletableFuture<GameEvent> future = company.getNextEventFuture();
        if (future == null) {
            return null;
        }
        try {
            return future.get(90, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Next-event prefetch failed, generating synchronously: {}", e.getMessage());
            synchronized (company) {
                company.setNextEvent(null);
                company.setNextEventFuture(null);
            }
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return null;
        }
    }

    @PreDestroy
    void shutdown() {
        prefetchPool.shutdownNow();
    }

    private Profile requireProfile(String name) {
        return repository.findByName(name)
                .orElseThrow(() -> new NotFoundException("Profile '" + name + "' not found."));
    }

    private Company requireCompany(String name) {
        Profile profile = requireProfile(name);
        if (!profile.hasCompany()) {
            throw new NotFoundException("Profile '" + name + "' has no company.");
        }
        return profile.getCompany();
    }
}
