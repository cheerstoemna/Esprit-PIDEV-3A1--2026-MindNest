package services;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PerspectiveModerationService {
    public static final String PERSPECTIVE_API_KEY = "AIzaSyBbZIDMYLyebtL0OrjONjeZQDenG7RnOPQ";
    private final HttpClient http = HttpClient.newHttpClient();

    // Tune this threshold (0.70 is a solid start for demos)
    private static final double TOXICITY_THRESHOLD = 0.70;

    // Extracts: "TOXICITY": { "summaryScore": { "value": 0.123 ... } }
    private static final Pattern TOXICITY_VALUE =
            Pattern.compile("\"TOXICITY\"\\s*:\\s*\\{.*?\"summaryScore\"\\s*:\\s*\\{.*?\"value\"\\s*:\\s*([0-9.]+)",
                    Pattern.DOTALL);

    public boolean isToxic(String text) {
        String key = PERSPECTIVE_API_KEY;
        if (key == null || key.isBlank() || key.contains("xxxx")) {
            throw new IllegalStateException("Perspective API key is missing. Set it in utils.AppConfig.PERSPECTIVE_API_KEY");
        }

        String input = (text == null) ? "" : text.trim();
        if (input.isBlank()) return false;

        try {
            String body = """
            {
              "comment": { "text": "%s" },
              "languages": ["en","fr","ar"],
              "requestedAttributes": { "TOXICITY": {} }
            }
            """.formatted(jsonEscape(input));

            String url = "https://commentanalyzer.googleapis.com/v1alpha1/comments:analyze?key=" + key;

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json; charset=utf-8")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (res.statusCode() / 100 != 2) {
                // For now: if API is down / blocked, don't break posting (your choice).
                // If you want "fail closed" (block), change return false -> throw.
                System.out.println("Perspective API error " + res.statusCode() + ": " + res.body());
                return false;
            }

            double score = extractToxicityScore(res.body());
            System.out.println("Perspective TOXICITY score = " + score);

            return score >= TOXICITY_THRESHOLD;

        } catch (Exception e) {
            // Same behavior: allow comment if moderation fails (demo-friendly).
            System.out.println("Perspective moderation failed: " + e.getMessage());
            return false;
        }
    }

    private double extractToxicityScore(String json) {
        Matcher m = TOXICITY_VALUE.matcher(json);
        if (m.find()) {
            try { return Double.parseDouble(m.group(1)); }
            catch (Exception ignored) {}
        }
        return 0.0;
    }

    private String jsonEscape(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}