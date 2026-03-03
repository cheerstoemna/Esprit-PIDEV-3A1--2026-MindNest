package services ;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * CaptchaVerificationService
 *
 * Sends the reCAPTCHA user-response token to Google's siteverify API
 * for server-side validation.
 *
 * WHY SERVER-SIDE VERIFICATION IS REQUIRED:
 * ──────────────────────────────────────────
 * Client-side (JavaScript) CAPTCHA can be bypassed by an attacker who simply
 * skips calling your login endpoint. You MUST verify the token on your server
 * by calling Google's API to confirm the challenge was genuinely solved.
 *
 * Google's Verification Endpoint:
 *   POST https://www.google.com/recaptcha/api/siteverify
 *   Parameters:
 *     secret   → Your SECRET key (keep this private, never expose to client)
 *     response → The token received from the user's browser
 *     remoteip → (Optional) User's IP address for extra validation
 *
 * Response JSON example:
 *   {
 *     "success": true,
 *     "challenge_ts": "2024-01-01T00:00:00Z",
 *     "hostname": "yourdomain.com",
 *     "error-codes": []
 *   }
 */
public class CaptchaVerificationService {

    // ─── Configuration ────────────────────────────────────────────────────────

    /**
     * YOUR SECRET KEY — Get this from https://www.google.com/recaptcha/admin
     *
     * IMPORTANT SECURITY NOTE:
     *  - This key must NEVER be sent to the client/frontend.
     *  - In production, load this from environment variables or a config file,
     *    NOT hardcoded here.
     *  - Example: System.getenv("RECAPTCHA_SECRET_KEY")
     */
    private static final String SECRET_KEY = "6Ld0TnosAAAAABo6S4DVBwogaovyV1g1lwkafPVL";

    /** Google's reCAPTCHA verification endpoint */
    private static final String VERIFY_URL = "https://www.google.com/recaptcha/api/siteverify";

    /** Background thread pool for async HTTP calls (avoids blocking JavaFX thread) */
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "captcha-verifier");
        t.setDaemon(true);
        return t;
    });

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ─── Public API ───────────────────────────────────────────────────────────

    /**
     * Verifies the reCAPTCHA token asynchronously.
     * The callback is invoked with `true` if the token is valid, `false` otherwise.
     *
     * Always call Platform.runLater() in the callback when updating JavaFX UI.
     *
     * @param token    The response token from the reCAPTCHA widget
     * @param callback Receives `true` if CAPTCHA is valid, `false` if not
     */
    public void verifyTokenAsync(String token, Consumer<Boolean> callback) {
        executor.submit(() -> {
            boolean result = false;
            try {
                result = verifyToken(token);
            } catch (Exception e) {
                System.err.println("[CAPTCHA] Verification error: " + e.getMessage());
                e.printStackTrace();
            }
            callback.accept(result);
        });
    }

    /**
     * Synchronously verifies the token with Google's API.
     * Do NOT call this on the JavaFX Application Thread — use verifyTokenAsync() instead.
     *
     * @param token The reCAPTCHA user response token
     * @return true if verification succeeded, false otherwise
     */
    public boolean verifyToken(String token) throws Exception {
        // Build POST body
        String postData = "secret=" + URLEncoder.encode(SECRET_KEY, StandardCharsets.UTF_8)
                        + "&response=" + URLEncoder.encode(token, StandardCharsets.UTF_8);

        // Open connection to Google's API
        URL url = new URL(VERIFY_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setConnectTimeout(5000);  // 5 second timeout
        conn.setReadTimeout(5000);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setRequestProperty("Content-Length", String.valueOf(postData.length()));

        // Send request
        try (OutputStream os = conn.getOutputStream()) {
            os.write(postData.getBytes(StandardCharsets.UTF_8));
        }

        // Read response
        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            System.err.println("[CAPTCHA] HTTP error from Google API: " + responseCode);
            return false;
        }

        StringBuilder responseBody = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                responseBody.append(line);
            }
        }

        // Parse JSON response
        JsonNode json = objectMapper.readTree(responseBody.toString());
        boolean success = json.path("success").asBoolean(false);

        System.out.println("[CAPTCHA] Verification result: " + success);
        System.out.println("[CAPTCHA] Full response: " + responseBody);

        return success;
    }

    /**
     * Shutdown the executor when the application closes.
     * Call this from your Application.stop() method.
     */
    public void shutdown() {
        executor.shutdown();
    }
}
