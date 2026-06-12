package com.equipe3.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Opt-in integration timing test for the live OpenAI image API.
 *
 * Run with:
 * ./mvnw -Dtest=OpenAiImageLatencyIT -DrunOpenAiLatency=true test
 */
class OpenAiImageLatencyIT {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String PROMPT = """
            Generate a kid-friendly Franco-Belgian comic illustration for a small
            entrepreneurship game. A young founder discovers that a supplier is late,
            boxes are missing, and customers are waiting. No text, no letters.
            """;

    @Test
    void compareMoodboardLatency() throws Exception {
        Assumptions.assumeTrue(Boolean.getBoolean("runOpenAiLatency"),
                "Live OpenAI latency test is disabled. Pass -DrunOpenAiLatency=true to run it.");

        String apiKey = System.getenv("OPENAI_API_KEY");
        Assumptions.assumeTrue(apiKey != null && !apiKey.isBlank(), "OPENAI_API_KEY is required.");

        int samples = Integer.getInteger("openAiLatencySamples", 2);
        String size = System.getProperty("openAiLatencySize", "1024x768");
        String model = System.getProperty("openAiLatencyModel", "gpt-image-2");

        List<byte[]> fullMoodboard = loadFullMoodboard();
        List<byte[]> compactMoodboard = List.of(resizeToPng(fullMoodboard.get(0), 512, 512));

        List<Result> results = new ArrayList<>();
        runCase(results, "full moodboard edits", apiKey, model, fullMoodboard, size, samples);
        runCase(results, "no moodboard generations", apiKey, model, List.of(), size, samples);
        runCase(results, "512x512 compact moodboard edits", apiKey, model, compactMoodboard, size, samples);

        System.out.println();
        System.out.println("OpenAI image latency summary (" + model + ", size=" + size
                + ", quality=low, samples=" + samples + ")");
        results.stream()
                .collect(java.util.stream.Collectors.groupingBy(Result::name))
                .forEach((name, group) -> {
                    List<Long> millis = group.stream().map(Result::millis).sorted().toList();
                    long avg = Math.round(millis.stream().mapToLong(Long::longValue).average().orElse(0));
                    System.out.printf("%-32s avg=%6d ms min=%6d ms max=%6d ms values=%s%n",
                            name, avg, millis.get(0), millis.get(millis.size() - 1), millis);
                });
    }

    @Test
    void compareSmallFastOptionsAndSaveImages() throws Exception {
        Assumptions.assumeTrue(Boolean.getBoolean("runOpenAiSmallLatency"),
                "Live OpenAI small-image latency test is disabled. Pass -DrunOpenAiSmallLatency=true to run it.");

        String apiKey = System.getenv("OPENAI_API_KEY");
        Assumptions.assumeTrue(apiKey != null && !apiKey.isBlank(), "OPENAI_API_KEY is required.");

        Path outputDir = Path.of(System.getProperty("openAiLatencyOutputDir",
                "target/openai-latency-images")).toAbsolutePath().normalize();
        Files.createDirectories(outputDir);

        List<DirectCase> cases = List.of(
                new DirectCase("gpt-image-2-896x736-low-jpeg", "gpt-image-2", "896x736", "low", "jpeg"),
                new DirectCase("gpt-image-1-mini-1024x1024-low-png", "gpt-image-1-mini", "1024x1024", "low", null)
        );

        System.out.println();
        System.out.println("OpenAI small image latency summary");
        for (DirectCase directCase : cases) {
            long start = System.nanoTime();
            byte[] image = generateDirect(apiKey, directCase);
            long elapsed = Duration.ofNanos(System.nanoTime() - start).toMillis();
            String extension = directCase.outputFormat() == null ? "png" : directCase.outputFormat();
            Path file = outputDir.resolve(directCase.name() + "." + extension);
            Files.write(file, image);
            System.out.printf("%-38s time=%6d ms bytes=%7d file=%s%n",
                    directCase.name(), elapsed, image.length, file);
        }
    }

    private static void runCase(List<Result> results, String name, String apiKey, String model,
                                List<byte[]> moodboard, String size, int samples) throws Exception {
        OpenAiImageService service = new OpenAiImageService(apiKey, model);
        replaceMoodboard(service, moodboard);

        for (int i = 1; i <= samples; i++) {
            long start = System.nanoTime();
            byte[] png = service.generate(PROMPT, size, "low", false, !moodboard.isEmpty());
            long elapsed = Duration.ofNanos(System.nanoTime() - start).toMillis();
            results.add(new Result(name, elapsed));
            System.out.printf("%-32s sample=%d time=%d ms bytes=%d%n", name, i, elapsed, png.length);
        }
    }

    @SuppressWarnings("unchecked")
    private static void replaceMoodboard(OpenAiImageService service, List<byte[]> moodboard) throws Exception {
        Field field = OpenAiImageService.class.getDeclaredField("moodboard");
        field.setAccessible(true);
        List<byte[]> current = (List<byte[]>) field.get(service);
        current.clear();
        current.addAll(moodboard);
    }

    private static List<byte[]> loadFullMoodboard() throws Exception {
        return List.of(
                new ClassPathResource("moodboard/moodboard-creation.png").getContentAsByteArray(),
                new ClassPathResource("moodboard/moodboard-game.png").getContentAsByteArray()
        );
    }

    private static byte[] resizeToPng(byte[] image, int width, int height) throws Exception {
        BufferedImage src = ImageIO.read(new ByteArrayInputStream(image));
        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = out.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.drawImage(src, 0, 0, width, height, null);
        graphics.dispose();

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(out, "png", bytes);
        return bytes.toByteArray();
    }

    private record Result(String name, long millis) {
    }

    private static byte[] generateDirect(String apiKey, DirectCase directCase) throws Exception {
        StringBuilder payload = new StringBuilder()
                .append("{")
                .append("\"model\":\"").append(directCase.model()).append("\",")
                .append("\"prompt\":").append(json(PROMPT + "\n" + OpenAiImageService.STYLE)).append(",")
                .append("\"n\":1,")
                .append("\"size\":\"").append(directCase.size()).append("\",")
                .append("\"quality\":\"").append(directCase.quality()).append("\"");
        if (directCase.outputFormat() != null) {
            payload.append(",\"output_format\":\"").append(directCase.outputFormat()).append("\"");
        }
        payload.append("}");

        HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.openai.com/v1/images/generations"))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofMinutes(4))
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) {
            throw new RuntimeException("OpenAI image API returned " + response.statusCode() + ": " + response.body());
        }
        JsonNode b64 = MAPPER.readTree(response.body()).path("data").path(0).path("b64_json");
        if (b64.isMissingNode() || b64.asText().isBlank()) {
            throw new RuntimeException("OpenAI image API returned no b64_json payload: " + response.body());
        }
        return Base64.getDecoder().decode(b64.asText());
    }

    private static String json(String text) {
        return "\"" + text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r") + "\"";
    }

    private record DirectCase(String name, String model, String size, String quality, String outputFormat) {
    }
}
