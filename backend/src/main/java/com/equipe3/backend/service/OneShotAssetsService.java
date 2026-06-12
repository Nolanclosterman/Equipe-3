package com.equipe3.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One-shot generation of the static assets the UI needs: the player profile
 * pictures (2000s bande dessinée kid avatars) and the icons for every UI
 * element (loading screen, assistant, company activity types).
 *
 * Generation runs once in the background after startup and is idempotent: a
 * file that already exists in the assets directory is never re-generated, so
 * the OpenAI API is only hit the first time the backend boots with a key.
 * The frontend fetches {@code GET /assets/manifest} and falls back to emojis
 * for any asset that is missing (no key yet, or generation still running).
 */
@Service
public class OneShotAssetsService {

    private static final Logger log = LoggerFactory.getLogger(OneShotAssetsService.class);

    /** key (used by the frontend) -> prompt for the image. All transparent PNGs. */
    private static final Map<String, String> CATALOG = new LinkedHashMap<>();

    static {
        // --- profile pictures: a diverse series of kid entrepreneur avatars ---
        avatar("avatar-1", "a cheerful 12-year-old boy with short brown hair and a yellow t-shirt");
        avatar("avatar-2", "a smiling 12-year-old girl with red curly hair, freckles and a green hoodie");
        avatar("avatar-3", "a 12-year-old black boy with glasses and a blue cap worn backwards");
        avatar("avatar-4", "a 12-year-old asian girl with a black ponytail and an orange sweater");
        avatar("avatar-5", "a 12-year-old boy with blond spiky hair and headphones around his neck");
        avatar("avatar-6", "a 12-year-old girl with brown skin, braided hair and a purple jacket");
        avatar("avatar-7", "a mischievous 12-year-old boy with messy black hair and a red scarf");
        avatar("avatar-8", "a confident 12-year-old girl with short blond hair and a denim overall");

        // --- icons for the company activity types of the creation screen ---
        icon("type-resto", "a tasty burger with a chef hat");
        icon("type-jeux-video", "a video game controller with colorful buttons");
        icon("type-mode", "a stylish t-shirt on a hanger");
        icon("type-robots", "a cute friendly robot with antenna");
        icon("type-eco", "a small green plant sprouting from planet earth");
        icon("type-techno", "a flying rocket with a laptop");
        icon("type-custom", "a magic pencil drawing a glowing light bulb (a custom idea)");

        // --- icons for the other UI elements ---
        icon("ui-assistant", "the friendly face of a small round helper robot, smiling");
        icon("ui-loader", "a small round robot thinking very hard, gears and question marks over its head");
        icon("ui-money", "a bag of golden coins");
        icon("ui-ecology", "a green leaf with a small heart");
        icon("ui-reputation", "two hands shaking in front of a small golden star");
        icon("ui-lexicon", "an open book with a small light bulb above it");
        icon("ui-problem", "a comic-style warning sign with an exclamation mark");
    }

    private static void avatar(String key, String who) {
        CATALOG.put(key, "Profile picture (head and shoulders portrait) of " + who
                + ", hero of a 2000s comic strip for kids, looking at the viewer.");
    }

    private static void icon(String key, String what) {
        CATALOG.put(key, "A simple bold cartoon icon of " + what
                + ". Sticker-like, thick outline, easy to read when small.");
    }

    private final OpenAiImageService images;
    private final AssetStorageService assets;

    public OneShotAssetsService(OpenAiImageService images, AssetStorageService assets) {
        this.images = images;
        this.assets = assets;
    }

    /**
     * Manifest of the one-shot assets that exist on disk right now, so the UI
     * can use them and keep emoji fallbacks for the rest.
     */
    public Map<String, String> manifest() {
        Map<String, String> result = new LinkedHashMap<>();
        for (String key : CATALOG.keySet()) {
            String file = key + ".png";
            if (assets.exists(file)) {
                result.put(key, assets.url(file));
            }
        }
        return result;
    }

    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void generateMissingAssets() {
        if (!images.isEnabled()) {
            return; // no key: the UI keeps its emoji fallbacks
        }
        int generated = 0;
        for (Map.Entry<String, String> entry : CATALOG.entrySet()) {
            String file = entry.getKey() + ".png";
            if (assets.exists(file)) {
                continue;
            }
            try {
                byte[] png = images.generate(entry.getValue(), OpenAiImageService.SQUARE, "medium", true);
                assets.save(file, png);
                generated++;
                log.info("One-shot asset generated: {}", file);
            } catch (Exception e) {
                log.warn("Could not generate one-shot asset {}: {}", file, e.getMessage());
            }
        }
        if (generated > 0) {
            log.info("One-shot asset generation done ({} new images).", generated);
        }
    }
}
