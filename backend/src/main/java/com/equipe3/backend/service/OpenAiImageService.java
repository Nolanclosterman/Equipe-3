package com.equipe3.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Generates the game's visual assets with the OpenAI image API (gpt-image-2).
 *
 * Every call sends the project's moodboard (the 2000s bande dessinée mockups in
 * {@code resources/moodboard/}) as base64-encoded reference images through the
 * {@code /v1/images/edits} endpoint so generated assets match the app's style.
 *
 * Icons and character portraits are requested on a plain white background and
 * the white is then removed (flood fill from the borders) to obtain transparent
 * PNGs, as the UI displays them over colored panels.
 *
 * When {@code openai.api-key} is blank the service is disabled and callers fall
 * back to emoji / placeholder assets so the game keeps working without a key.
 */
@Service
public class OpenAiImageService {

    private static final Logger log = LoggerFactory.getLogger(OpenAiImageService.class);

    private static final String EDITS_URL = "https://api.openai.com/v1/images/edits";
    private static final String GENERATIONS_URL = "https://api.openai.com/v1/images/generations";

    /** Common style suffix so every asset stays in the 2000s BD universe. */
    public static final String STYLE = "Style: early-2000s Franco-Belgian comic strip (bande dessinée), "
            + "bold black outlines, flat vivid colors, slight halftone texture, fun and kid-friendly, "
            + "matching the reference moodboard images. No text, no letters, no watermark.";

    public static final String SQUARE = "1024x1024";
    public static final String LANDSCAPE = "1536x1024";

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    private final String apiKey;
    private final String model;
    private final List<byte[]> moodboard = new ArrayList<>();

    public OpenAiImageService(
            @Value("${openai.api-key:}") String apiKey,
            @Value("${openai.image-model:gpt-image-2}") String model) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model;
        if (isEnabled()) {
            loadMoodboard();
            log.info("OpenAI image generation enabled (model={}, {} moodboard images).",
                    model, moodboard.size());
        } else {
            log.warn("OPENAI_API_KEY is not set — asset generation is disabled, "
                    + "the game will use emoji placeholders.");
        }
    }

    public boolean isEnabled() {
        return !apiKey.isBlank();
    }

    /**
     * Generate one PNG image.
     *
     * @param prompt        what to draw (the BD style suffix is appended)
     * @param size          {@link #SQUARE} or {@link #LANDSCAPE}
     * @param quality       "low" for fast in-game assets, "medium"/"high" for one-shot assets
     * @param transparent   ask for a plain white background then strip it to transparency
     */
    public byte[] generate(String prompt, String size, String quality, boolean transparent) {
        if (!isEnabled()) {
            throw new IllegalStateException("OpenAI image generation is disabled (no API key).");
        }
        String fullPrompt = prompt.trim() + "\n" + STYLE;
        if (transparent) {
            fullPrompt += "\nThe subject is centered on a plain solid pure white background "
                    + "(#FFFFFF), with nothing else around it.";
        }
        byte[] png = request(fullPrompt, size, quality);
        return transparent ? removeWhiteBackground(png) : png;
    }

    // ----------------------------------------------------------------- HTTP

    /** Calls the API, falling back from the configured model to gpt-image-1 if rejected. */
    private byte[] request(String prompt, String size, String quality) {
        Set<String> models = new LinkedHashSet<>(List.of(model, "gpt-image-1"));
        RuntimeException last = null;
        for (String m : models) {
            try {
                return moodboard.isEmpty()
                        ? requestGeneration(m, prompt, size, quality)
                        : requestEdit(m, prompt, size, quality);
            } catch (RuntimeException e) {
                log.warn("Image request with model '{}' failed: {}", m, e.getMessage());
                last = e;
            }
        }
        throw last;
    }

    /** images/edits with the moodboard as reference images (multipart upload). */
    private byte[] requestEdit(String model, String prompt, String size, String quality) {
        String boundary = "----equipe3-" + UUID.randomUUID();
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        writeField(body, boundary, "model", model);
        writeField(body, boundary, "prompt", prompt);
        writeField(body, boundary, "n", "1");
        writeField(body, boundary, "size", size);
        if (quality != null) {
            writeField(body, boundary, "quality", quality);
        }
        int i = 0;
        for (byte[] image : moodboard) {
            writeFile(body, boundary, "image[]", "moodboard-" + (++i) + ".png", image);
        }
        write(body, "--" + boundary + "--\r\n");

        HttpRequest request = HttpRequest.newBuilder(URI.create(EDITS_URL))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .timeout(Duration.ofMinutes(4))
                .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()))
                .build();
        return send(request);
    }

    /** images/generations, used only when no moodboard is bundled. */
    private byte[] requestGeneration(String model, String prompt, String size, String quality) {
        ObjectNode payload = mapper.createObjectNode()
                .put("model", model)
                .put("prompt", prompt)
                .put("n", 1)
                .put("size", size);
        if (quality != null) {
            payload.put("quality", quality);
        }
        HttpRequest request = HttpRequest.newBuilder(URI.create(GENERATIONS_URL))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofMinutes(4))
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();
        return send(request);
    }

    private byte[] send(HttpRequest request) {
        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw new RuntimeException("OpenAI image API returned " + response.statusCode()
                        + ": " + abbreviate(response.body()));
            }
            JsonNode b64 = mapper.readTree(response.body()).path("data").path(0).path("b64_json");
            if (b64.isMissingNode() || b64.asText().isBlank()) {
                throw new RuntimeException("OpenAI image API returned no b64_json payload.");
            }
            return Base64.getDecoder().decode(b64.asText());
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("OpenAI image API call failed: " + e.getMessage(), e);
        }
    }

    private static void writeField(ByteArrayOutputStream out, String boundary, String name, String value) {
        write(out, "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n"
                + value + "\r\n");
    }

    private static void writeFile(ByteArrayOutputStream out, String boundary, String name,
                                  String filename, byte[] content) {
        write(out, "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"" + name + "\"; filename=\"" + filename + "\"\r\n"
                + "Content-Type: image/png\r\n\r\n");
        out.writeBytes(content);
        write(out, "\r\n");
    }

    private static void write(ByteArrayOutputStream out, String text) {
        out.writeBytes(text.getBytes(StandardCharsets.UTF_8));
    }

    private static String abbreviate(String text) {
        if (text == null) return "";
        return text.length() > 400 ? text.substring(0, 400) + "…" : text;
    }

    // -------------------------------------------------- background removal

    /**
     * Turns the white background of a generated image into transparency by
     * flood-filling near-white pixels connected to the image borders. Interior
     * white details (eyes, teeth…) are preserved because they are not connected
     * to the border.
     */
    static byte[] removeWhiteBackground(byte[] png) {
        try {
            BufferedImage src = ImageIO.read(new ByteArrayInputStream(png));
            int w = src.getWidth(), h = src.getHeight();
            BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            out.getGraphics().drawImage(src, 0, 0, null);

            boolean[] background = new boolean[w * h];
            Deque<Integer> queue = new ArrayDeque<>();
            for (int x = 0; x < w; x++) {
                enqueueIfWhite(out, x, 0, w, background, queue);
                enqueueIfWhite(out, x, h - 1, w, background, queue);
            }
            for (int y = 0; y < h; y++) {
                enqueueIfWhite(out, 0, y, w, background, queue);
                enqueueIfWhite(out, w - 1, y, w, background, queue);
            }
            while (!queue.isEmpty()) {
                int index = queue.poll();
                int x = index % w, y = index / w;
                if (x > 0) enqueueIfWhite(out, x - 1, y, w, background, queue);
                if (x < w - 1) enqueueIfWhite(out, x + 1, y, w, background, queue);
                if (y > 0) enqueueIfWhite(out, x, y - 1, w, background, queue);
                if (y < h - 1) enqueueIfWhite(out, x, y + 1, w, background, queue);
            }
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    if (background[y * w + x]) {
                        out.setRGB(x, y, 0x00000000);
                    }
                }
            }
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            ImageIO.write(out, "png", bytes);
            return bytes.toByteArray();
        } catch (Exception e) {
            log.warn("Background removal failed, keeping the white background.", e);
            return png;
        }
    }

    private static void enqueueIfWhite(BufferedImage img, int x, int y, int w,
                                       boolean[] background, Deque<Integer> queue) {
        int index = y * w + x;
        if (background[index]) {
            return;
        }
        int rgb = img.getRGB(x, y);
        int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
        if (r > 235 && g > 235 && b > 235) {
            background[index] = true;
            queue.add(index);
        }
    }

    private void loadMoodboard() {
        for (String name : List.of("moodboard/moodboard-creation.png", "moodboard/moodboard-game.png")) {
            try {
                moodboard.add(new ClassPathResource(name).getContentAsByteArray());
            } catch (Exception e) {
                log.warn("Could not load moodboard image {}.", name, e);
            }
        }
    }
}
