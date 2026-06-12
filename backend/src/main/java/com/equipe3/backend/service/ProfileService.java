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
import org.springframework.stereotype.Service;

/**
 * Business rules around the single company a player can own: company lifecycle
 * (create / start / delete) and the game loop (event generation, decision
 * scoring, help chat).
 */
@Service
public class ProfileService {

    private final ProfileRepository repository;
    private final AnthropicGameService ai;

    public ProfileService(ProfileRepository repository, AnthropicGameService ai) {
        this.repository = repository;
        this.ai = ai;
    }

    /** GET /profiles/{name} — returns (and lazily creates) the connected profile. */
    public Profile getProfile(String name) {
        return repository.findOrCreate(name);
    }

    /** POST /profiles/{name}/company — create a company only if none exists. */
    public Company createCompany(String name, String companyName, String type, String character) {
        Profile profile = repository.findOrCreate(name);
        if (profile.hasCompany()) {
            throw new ConflictException("Profile '" + name + "' already owns a company.");
        }
        Company company = new Company(companyName);
        company.setType(type);
        company.setCharacter(character);
        profile.setCompany(company);
        repository.save(profile);
        return company;
    }

    /** POST /profiles/{name}/company/start — activate the existing company. */
    public Company startCompany(String name) {
        Company company = requireCompany(name);
        company.setActive(true);
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
        // Placeholder deterministic icon based on the company name; swap for the
        // real generative asset service later.
        String seed = company.getName() == null ? "company" : company.getName().trim();
        company.setIconUrl("https://api.dicebear.com/9.x/shapes/svg?seed="
                + java.net.URLEncoder.encode(seed, java.nio.charset.StandardCharsets.UTF_8));
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
        GameEvent event = ai.generateEvent(company);
        company.setCurrentEvent(event);
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

        EventOutcome outcome = ai.scoreDecision(company, event.problem(), solution);
        Scores scores = company.getScores();
        scores.setMoney(scores.getMoney() + outcome.moneyDelta());
        scores.setEcology(scores.getEcology() + outcome.ecologyDelta());
        scores.setEthics(scores.getEthics() + outcome.ethicsDelta());

        company.setLastOutcome(outcome);
        company.getHistory().add(new DecisionRecord(event.problem(), solution, outcome.narrative()));

        // End conditions (Design §8): any indicator reaches 0, or the 15th event is done.
        if (scores.getMoney() <= 0 || scores.getEcology() <= 0 || scores.getEthics() <= 0
                || company.getEventNumber() >= Company.MAX_EVENTS) {
            company.setGameOver(true);
        }
        return company;
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
