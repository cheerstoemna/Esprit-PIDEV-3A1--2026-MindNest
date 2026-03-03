package services;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SentimentService {

    // Keep your token here
    private static final String HF_TOKEN = "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";

    // ✅ model id only (not a full URL)
    private static final String MODEL_ID = "cardiffnlp/twitter-roberta-base-sentiment-latest";

    /**
     * ✅ NEW endpoint (router). Legacy api-inference returns 410 now.
     * Docs/community threads show migration to router.huggingface.co/hf-inference. :contentReference[oaicite:1]{index=1}
     */
    private static final String ROUTER_BASE = "https://router.huggingface.co/hf-inference/models/";

    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(25);
    private static final boolean DEBUG = true;

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public enum Label { POSITIVE, NEUTRAL, NEGATIVE, UNKNOWN }

    public static class Result {
        private final Label label;
        private final double confidence;
        private final String rawLabelOrError;

        public Result(Label label, double confidence, String rawLabelOrError) {
            this.label = label;
            this.confidence = confidence;
            this.rawLabelOrError = rawLabelOrError;
        }

        public Label getLabel() { return label; }
        public double getConfidence() { return confidence; }
        public String getRawLabelOrError() { return rawLabelOrError; }

        public static Result unknown(String reason) {
            return new Result(Label.UNKNOWN, 0.0, reason == null ? "" : reason);
        }
    }

    public Result analyze(String text) {
        try {
            String clean = normalize(text);
            if (clean.isEmpty()) return Result.unknown("Empty text.");
            if (clean.length() > 700) clean = clean.substring(0, 700);

            // Build router URL safely
            String url = ROUTER_BASE + URLEncoder.encode(MODEL_ID, StandardCharsets.UTF_8);

            // wait_for_model prevents cold-start failures
            String payload = "{"
                    + "\"inputs\":" + jsonString(clean) + ","
                    + "\"options\":{\"wait_for_model\":true}"
                    + "}";

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(HTTP_TIMEOUT)
                    .header("Authorization", "Bearer " + HF_TOKEN)
                    .header("Content-Type", "application/json; charset=utf-8")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            int code = res.statusCode();
            String body = res.body() == null ? "" : res.body();

            if (DEBUG) {
                System.out.println("[HF Sentiment] URL: " + url);
                System.out.println("[HF Sentiment] HTTP " + code);
                System.out.println("[HF Sentiment] Body: " + shorten(body, 700));
            }

            if (code == 401 || code == 403) {
                return Result.unknown("Auth error HTTP " + code + " (token permissions / invalid token).");
            }

            if (code < 200 || code >= 300) {
                String err = extractJsonStringField(body, "error");
                String est = extractJsonNumberField(body, "estimated_time");
                if (err != null && !err.isBlank()) {
                    if (est != null) return Result.unknown(err + " (estimated_time=" + est + "s)");
                    return Result.unknown(err);
                }
                return Result.unknown("HTTP " + code + " - " + shorten(body, 260));
            }

            return parseHfResponse(body);

        } catch (Exception e) {
            if (DEBUG) e.printStackTrace();
            return Result.unknown("Exception: " + e.getMessage());
        }
    }

    private Result parseHfResponse(String body) {
        String b = body == null ? "" : body.trim();
        if (b.isEmpty()) return Result.unknown("Empty response.");

        String err = extractJsonStringField(b, "error");
        if (err != null && !err.isBlank()) {
            String est = extractJsonNumberField(b, "estimated_time");
            if (est != null) return Result.unknown(err + " (estimated_time=" + est + "s)");
            return Result.unknown(err);
        }

        // Typical:
        // [[{"label":"POSITIVE","score":0.9}, ...]]
        // or [{"label":"LABEL_2","score":...}, ...]
        Pattern p = Pattern.compile(
                "\\{[^\\}]*\"label\"\\s*:\\s*\"(.*?)\"[^\\}]*\"score\"\\s*:\\s*([0-9eE+\\-\\.]+)[^\\}]*\\}"
        );
        Matcher m = p.matcher(b);

        String bestLabel = null;
        double bestScore = -1;

        while (m.find()) {
            String rawLabel = unescapeJson(m.group(1));
            double score = parseDoubleSafe(m.group(2));
            if (rawLabel == null) continue;

            if (score > bestScore) {
                bestScore = score;
                bestLabel = rawLabel;
            }
        }

        if (bestLabel == null) {
            return Result.unknown("Unexpected response: " + shorten(b, 260));
        }

        Label mapped = mapLabel(bestLabel);
        return new Result(mapped, bestScore, bestLabel);
    }

    private Label mapLabel(String raw) {
        String l = raw == null ? "" : raw.trim().toUpperCase();
        if (l.contains("POSITIVE")) return Label.POSITIVE;
        if (l.contains("NEGATIVE")) return Label.NEGATIVE;
        if (l.contains("NEUTRAL")) return Label.NEUTRAL;

        // Many models use LABEL_0/1/2
        if (l.contains("LABEL_0")) return Label.NEGATIVE;
        if (l.contains("LABEL_1")) return Label.NEUTRAL;
        if (l.contains("LABEL_2")) return Label.POSITIVE;

        return Label.UNKNOWN;
    }

    // ---- helpers ----

    private String normalize(String s) {
        if (s == null) return "";
        return s.replaceAll("\\s+", " ").trim();
    }

    private String jsonString(String s) {
        if (s == null) return "\"\"";
        String out = s
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
        return "\"" + out + "\"";
    }

    private String extractJsonStringField(String json, String fieldName) {
        if (json == null) return null;
        Pattern p = Pattern.compile("\"" + Pattern.quote(fieldName) + "\"\\s*:\\s*\"(.*?)\"");
        Matcher m = p.matcher(json);
        if (m.find()) return unescapeJson(m.group(1));
        return null;
    }

    private String extractJsonNumberField(String json, String fieldName) {
        if (json == null) return null;
        Pattern p = Pattern.compile("\"" + Pattern.quote(fieldName) + "\"\\s*:\\s*([0-9eE+\\-\\.]+)");
        Matcher m = p.matcher(json);
        if (m.find()) return m.group(1);
        return null;
    }

    private String unescapeJson(String s) {
        if (s == null) return null;
        return s.replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }

    private double parseDoubleSafe(String s) {
        try { return Double.parseDouble(s); }
        catch (Exception e) { return -1; }
    }

    private String shorten(String s, int max) {
        if (s == null) return "";
        String t = s.trim();
        if (t.length() <= max) return t;
        return t.substring(0, max) + "...";
    }
}