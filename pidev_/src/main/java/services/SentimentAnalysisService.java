package services;

// ═══════════════════════════════════════════════════════════════════════════════
//  FILE: src/main/java/services/SentimentAnalysisService.java
//  APILayer Sentiment Analysis API
//  Endpoint: POST https://api.apilayer.com/sentiment/analysis
//  Body: raw text (NOT JSON)
//  Response: {"sentiment":"positive","language":"en","content_type":"text"}
// ═══════════════════════════════════════════════════════════════════════════════

import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class SentimentAnalysisService {

    private static final String API_KEY = "LiC71OIWbaqeHVVl4ZshEzRMaoOsFbJi";
    private static final String API_URL = "https://api.apilayer.com/sentiment/analysis";

    public enum Sentiment { POSITIVE, NEGATIVE, NEUTRAL, UNKNOWN }

    public static class SentimentResult {
        public final Sentiment sentiment;
        public final String    label;
        public final String    color;
        public final String    bgColor;

        private SentimentResult(Sentiment s) {
            this.sentiment = s;
            switch (s) {
                case POSITIVE -> { label = "😊 Positive"; color = "#166534"; bgColor = "#dcfce7"; }
                case NEGATIVE -> { label = "😟 Negative"; color = "#991b1b"; bgColor = "#fee2e2"; }
                case NEUTRAL  -> { label = "😐 Neutral";  color = "#92400e"; bgColor = "#fef3c7"; }
                default       -> { label = "— Unknown";   color = "#6b7280"; bgColor = "#f3f4f6"; }
            }
        }
        public static SentimentResult of(Sentiment s) { return new SentimentResult(s); }
    }

    /**
     * Analyze sentiment using API, with local fallback.
     * @param text   the feedback comment
     * @param rating star rating 1-5 — used as tiebreaker when words don't match
     */
    public SentimentResult analyze(String text, int rating) {
        SentimentResult result = analyze(text);
        // If local fallback returned Neutral due to no word matches,
        // use the star rating as the ground truth
        if (result.sentiment == Sentiment.NEUTRAL && rating > 0) {
            if (rating >= 4) {
                System.out.println("[Sentiment] no word match — using rating " + rating + " → Positive");
                return SentimentResult.of(Sentiment.POSITIVE);
            } else if (rating <= 2) {
                System.out.println("[Sentiment] no word match — using rating " + rating + " → Negative");
                return SentimentResult.of(Sentiment.NEGATIVE);
            }
        }
        return result;
    }

    public SentimentResult analyze(String text) {
        if (text == null || text.isBlank()) return SentimentResult.of(Sentiment.UNKNOWN);

        try {
            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);  // 10 seconds
            conn.setReadTimeout(10000);
            // ── KEY FIX: send as plain text, not JSON ─────────────────────────
            conn.setRequestProperty("Content-Type", "text/plain");
            conn.setRequestProperty("apikey", API_KEY);
            conn.getOutputStream().write(text.getBytes(StandardCharsets.UTF_8));

            int status = conn.getResponseCode();

            // ── If quota exceeded or any non-200 → use local fallback ─────────
            if (status != 200) {
                System.err.println("[Sentiment] API returned status=" + status + " (quota exceeded or error) — using local fallback");
                return analyzeLocally(text);
            }

            String json = new Scanner(conn.getInputStream(), StandardCharsets.UTF_8).useDelimiter("\\A").next();

            System.out.println("[Sentiment] status=200 | response=" + json);

            // Response: {"sentiment":"positive","language":"en","content_type":"text"}
            String sentiment = extractField(json, "sentiment").toLowerCase();

            return switch (sentiment) {
                case "positive" -> SentimentResult.of(Sentiment.POSITIVE);
                case "negative" -> SentimentResult.of(Sentiment.NEGATIVE);
                case "neutral"  -> SentimentResult.of(Sentiment.NEUTRAL);
                default -> {
                    System.out.println("[Sentiment] unrecognized value: '" + sentiment + "' — using local fallback");
                    yield analyzeLocally(text);
                }
            };

        } catch (java.net.SocketTimeoutException e) {
            System.err.println("[Sentiment] API timed out — using local fallback");
            return analyzeLocally(text);
        } catch (Exception e) {
            System.err.println("[Sentiment] Exception: " + e.getClass().getSimpleName() + " — " + e.getMessage());
            return analyzeLocally(text);
        }
    }

    // ── Local fallback — used when API is unavailable
    private static final java.util.Set<String> POS = java.util.Set.of(
            "good","great","excellent","amazing","wonderful","fantastic","outstanding",
            "superb","perfect","best","brilliant","exceptional","impressive","remarkable",
            "happy","pleased","satisfied","comfortable","confident","calm","peaceful",
            "relaxed","relieved","safe","secure","hopeful","motivated","energized",
            "positive","better","improved","refreshed","lighter","stronger","clearer",
            "helpful","useful","effective","productive","insightful","valuable","meaningful",
            "supportive","understanding","listened","heard","understood","validated",
            "professional","caring","empathetic","compassionate","patient","attentive",
            "focused","structured","organized","thorough","detailed","informative",
            "progress","growth","healing","recovering","advancing","forward",
            "breakthrough","clarity","awareness","change","transformation",
            "grateful","thankful","appreciate","appreciated","recommend","enjoyed",
            "love","like","glad","fortunate","blessed","lucky","welcome","open",
            "helped","worked","learned","gained","achieved","accomplished",
            "resolved","addressed","overcame","managed","handled","coped","dealt"
    );
    private static final java.util.Set<String> NEG = java.util.Set.of(
            "bad","poor","terrible","awful","horrible","worst","disappointing",
            "ineffective","unhelpful","inadequate","insufficient","lacking","weak",
            "unhappy","sad","depressed","anxious","stressed","worried","nervous",
            "uncomfortable","uneasy","tense","overwhelmed","exhausted","drained",
            "frustrated","angry","upset","annoyed","irritated","hurt","hopeless",
            "helpless","lost","stuck","confused","uncertain","afraid","useless",
            "unprofessional","rude","dismissive","cold","distant","rushed","ignored",
            "unheard","misunderstood","judged","criticized","blamed","pressured",
            "disconnected","disengaged","distracted","unorganized","unclear","boring",
            "waste","pointless","irrelevant","generic","shallow",
            "worse","worsened","declined","regressed","failed","struggling","suffering",
            "difficult","hard","painful","traumatic","triggering",
            "misleading","wrong","inappropriate","harmful","cancelled"
    );

    // Negation words — flip the sentiment of the word that follows
    private static final java.util.Set<String> NEGATORS = java.util.Set.of(
            "not","no","never","wasn't","didn't","don't","isn't","aren't","couldn't",
            "wouldn't","shouldn't","hardly","barely","nothing","without"
    );

    private SentimentResult analyzeLocally(String text) {
        String[] words = text.toLowerCase().replaceAll("[^a-z'\\s]", "").split("\\s+");
        int pos = 0, neg = 0;
        for (int i = 0; i < words.length; i++) {
            String w = words[i];
            boolean negated = i > 0 && NEGATORS.contains(words[i - 1]);
            if (POS.contains(w)) {
                if (negated) neg++; // "not good" → negative
                else         pos++;
            } else if (NEG.contains(w)) {
                if (negated) pos++; // "not bad" → positive
                else         neg++;
            }
        }
        System.out.println("[Sentiment] local fallback: pos=" + pos + " neg=" + neg);
        // If no keywords found at all, do a last-resort check for common short positive phrases
        if (pos == 0 && neg == 0) {
            String lower = text.toLowerCase();
            if (lower.contains("thank") || lower.contains("great") || lower.contains("good")
                    || lower.contains("ok") || lower.contains("fine") || lower.contains("alright")
                    || lower.contains("well") || lower.contains("nice") || lower.contains("cool")) {
                return SentimentResult.of(Sentiment.POSITIVE);
            }
            return SentimentResult.of(Sentiment.NEUTRAL);
        }
        if (pos > neg)  return SentimentResult.of(Sentiment.POSITIVE);
        if (neg > pos)  return SentimentResult.of(Sentiment.NEGATIVE);
        // Tie: slightly favor positive (people tend to write neutral-positive feedback)
        return SentimentResult.of(Sentiment.POSITIVE);
    }

    private String extractField(String json, String field) {
        // Try both "field":"value" and "field": "value" (with space)
        String[] patterns = {
                "\"" + field + "\": \"",   // space after colon  ← APILayer format
                "\"" + field + "\":\""      // no space
        };
        for (String search : patterns) {
            int start = json.indexOf(search);
            if (start != -1) {
                start += search.length();
                int end = json.indexOf("\"", start);
                if (end != -1) return json.substring(start, end);
            }
        }
        return "";
    }
}




















