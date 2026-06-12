package com.equipe3.backend.service;

import com.equipe3.backend.model.Company;
import com.equipe3.backend.model.GameEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.UnaryOperator;

/**
 * Illustrates the game at runtime with the OpenAI image pipeline:
 * <ul>
 *   <li>a landscape illustration of each event ({@code prompts/event_illustration.md}),</li>
 *   <li>a small comic vignette for each proposed solution,</li>
 *   <li>a portrait of the character presenting the problem (white background
 *       removed, cached per company so recurring characters keep their face),</li>
 *   <li>the company logo/icon.</li>
 * </ul>
 *
 * Generation runs asynchronously: the event is returned to the player right
 * away with empty image fields and the frontend polls the profile until the
 * URLs appear. When the OpenAI key is missing this service is a no-op.
 */
@Service
public class IllustrationService {

    private static final Logger log = LoggerFactory.getLogger(IllustrationService.class);

    private final OpenAiImageService images;
    private final AssetStorageService assets;
    private final ExecutorService pool = Executors.newFixedThreadPool(5);

    public IllustrationService(OpenAiImageService images, AssetStorageService assets) {
        this.images = images;
        this.assets = assets;
    }

    public boolean isEnabled() {
        return images.isEnabled();
    }

    /** Kicks off the async generation of all images for the given event. */
    public void illustrateEvent(Company company, GameEvent event) {
        if (!images.isEnabled() || event == null || event.problem() == null) {
            return;
        }
        String context = "Company: " + company.getName()
                + " (" + (company.getType() == null ? "small startup" : company.getType()) + ")";

        // Landscape scene of the problem, driven by prompts/event_illustration.md.
        submit(company, event, () -> {
            String prompt = loadTemplate()
                    .replace("<issue>", event.problem())
                    .replace("<company-context>", context);
            byte[] png = images.generate(prompt, OpenAiImageService.LANDSCAPE, "low", false);
            String url = assets.save("event-" + UUID.randomUUID() + ".png", png);
            return current -> current.withIllustration(url);
        });

        // One comic vignette per proposed solution.
        List<String> solutions = event.solutions();
        for (int i = 0; i < solutions.size(); i++) {
            final int index = i;
            submit(company, event, () -> {
                String prompt = "A single small comic vignette showing this action taken by a kid "
                        + "entrepreneur, easy to read at a glance: \"" + solutions.get(index) + "\". "
                        + context;
                byte[] png = images.generate(prompt, OpenAiImageService.SQUARE, "low", false);
                String url = assets.save("solution-" + UUID.randomUUID() + ".png", png);
                return current -> current.withSolutionIllustration(index, url);
            });
        }

        // Portrait of the presenting character (cached per company).
        if (event.character() != null && !event.character().isBlank()) {
            submit(company, event, () -> {
                String url = characterImage(company, event.character());
                return current -> current.withCharacterImage(url);
            });
        }
    }

    /** Returns (generating if needed) the portrait of a story character. */
    public String characterImage(Company company, String character) {
        String key = slug(character);
        return company.getCharacterImages().computeIfAbsent(key, k -> {
            String prompt = "Portrait (head and shoulders) of this character from a kids' "
                    + "entrepreneurship story: \"" + character + "\". Friendly and expressive.";
            byte[] png = images.generate(prompt, OpenAiImageService.SQUARE, "low", true);
            return assets.save("character-" + k + "-" + shortId() + ".png", png);
        });
    }

    /** Generates the company logo (transparent background). Blocking call. */
    public String companyIcon(Company company) {
        String prompt = "A round logo/badge for a company named \"" + company.getName() + "\""
                + (company.getType() == null ? "" : " which is about: " + company.getType())
                + ". Simple, bold, memorable, sticker-like.";
        byte[] png = images.generate(prompt, OpenAiImageService.SQUARE, "medium", true);
        return assets.save("icon-" + slug(company.getName()) + "-" + shortId() + ".png", png);
    }

    // --------------------------------------------------------------- helpers

    /**
     * Runs one image generation off-thread, then applies the update to the
     * company's current event — unless the player already moved to another
     * event in the meantime.
     */
    private void submit(Company company, GameEvent event, ImageJob job) {
        pool.submit(() -> {
            try {
                UnaryOperator<GameEvent> update = job.run();
                synchronized (company) {
                    GameEvent current = company.getCurrentEvent();
                    if (current != null && current.problem().equals(event.problem())) {
                        company.setCurrentEvent(update.apply(current));
                    }
                }
            } catch (Exception e) {
                log.warn("Event illustration failed (the game continues without it): {}", e.getMessage());
            }
        });
    }

    private String loadTemplate() {
        try {
            return new ClassPathResource("prompts/event_illustration.md")
                    .getContentAsString(StandardCharsets.UTF_8).trim();
        } catch (Exception e) {
            return "Generate an image (landscape) that will illustrate the issue <issue> "
                    + "in this company context <company-context>";
        }
    }

    private static String slug(String text) {
        String normalized = Normalizer.normalize(text == null ? "x" : text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        return normalized.isBlank() ? "x" : normalized.length() > 40 ? normalized.substring(0, 40) : normalized;
    }

    private static String shortId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    @PreDestroy
    void shutdown() {
        pool.shutdownNow();
    }

    @FunctionalInterface
    private interface ImageJob {
        UnaryOperator<GameEvent> run() throws Exception;
    }
}
