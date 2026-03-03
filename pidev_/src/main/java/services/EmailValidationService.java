package services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Hashtable;

/**
 * EmailValidationService
 *
 * Two-layer email validation:
 *
 * Layer 1 — DNS/MX check (no API, offline-capable)
 *   Checks if the email domain has real mail servers (MX records).
 *   e.g. "user@fakeddomain123.com" → no MX → rejected
 *
 * Layer 2 — AbstractAPI (with API key)
 *   Checks if email is deliverable, disposable, or has typos.
 *   e.g. "user@guerrillamail.com" → disposable → rejected
 *
 * SETUP:
 * ─────────────────────────────────────────────────
 * 1. Go to https://www.abstractapi.com/
 * 2. Sign up for free (100 requests/month free)
 * 3. Go to "Email Validation" API → copy your API key
 * 4. Paste it in ABSTRACT_API_KEY below
 * ─────────────────────────────────────────────────
 */
public class EmailValidationService {

    // ── Replace with your AbstractAPI key ────────────────────────────
    private static final String ABSTRACT_API_KEY = "4f360c3cdea4401db0e5c13687d4e883";
    // ─────────────────────────────────────────────────────────────────

    private static final String ABSTRACT_API_URL =
        "https://emailvalidation.abstractapi.com/v1/?api_key=%s&email=%s";

    /**
     * Full validation result returned to the caller.
     */
    public static class ValidationResult {
        public final boolean valid;
        public final String  message;

        public ValidationResult(boolean valid, String message) {
            this.valid   = valid;
            this.message = message;
        }
    }

    /**
     * Validates the email using both DNS check and AbstractAPI.
     * Called on a background thread — never on the JavaFX thread.
     *
     * @param email the email to validate
     * @return ValidationResult with valid=true/false and a message
     */
    public ValidationResult validate(String email) {

        // ── Layer 1: DNS / MX record check (no API needed) ───────────
        ValidationResult dnsResult = checkMxRecord(email);
        if (!dnsResult.valid) return dnsResult;

        // ── Layer 2: AbstractAPI deep check ───────────────────────────
        return checkWithAbstractApi(email);
    }

    // ─────────────────────────────────────────────────────────────────
    // Layer 1 — MX Record Check
    // ─────────────────────────────────────────────────────────────────

    private ValidationResult checkMxRecord(String email) {
        try {
            String domain = email.substring(email.indexOf('@') + 1);

            Hashtable<String, String> env = new Hashtable<>();
            env.put("java.naming.factory.initial",
                    "com.sun.jndi.dns.DnsContextFactory");
            env.put("java.naming.provider.url", "dns:");

            InitialDirContext ctx = new InitialDirContext(env);
            Attributes attrs = ctx.getAttributes(domain, new String[]{"MX"});

            if (attrs.get("MX") == null) {
                return new ValidationResult(false,
                    "This email domain has no mail server.\nPlease use a real email address.");
            }

            return new ValidationResult(true, "MX check passed");

        } catch (Exception e) {
            // DNS lookup failed — domain likely doesn't exist
            return new ValidationResult(false,
                "Email domain does not exist.\nPlease check your email address.");
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Layer 2 — AbstractAPI Check
    // ─────────────────────────────────────────────────────────────────

    private ValidationResult checkWithAbstractApi(String email) {
        try {
            String url = ABSTRACT_API_URL.formatted(ABSTRACT_API_KEY, email);

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

            HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                // API unreachable — fail open (allow signup to proceed)
                System.err.println("[EmailValidation] AbstractAPI unreachable: " + response.statusCode());
                return new ValidationResult(true, "API check skipped");
            }

            JsonNode json = new ObjectMapper().readTree(response.body());

            // ── Check deliverability ──────────────────────────────────
            String deliverability = json.path("deliverability").asText("");
            if ("UNDELIVERABLE".equalsIgnoreCase(deliverability)) {
                return new ValidationResult(false,
                    "This email address is not deliverable.\nPlease use a valid email.");
            }

            // ── Check disposable (temp mail) ──────────────────────────
            boolean isDisposable = json.path("is_disposable_email")
                                       .path("value").asBoolean(false);
            if (isDisposable) {
                return new ValidationResult(false,
                    "Temporary/disposable emails are not allowed.\nPlease use your real email.");
            }

            // ── Check MX records (AbstractAPI also verifies this) ─────
            boolean hasMx = json.path("is_mx_found")
                                .path("value").asBoolean(true);
            if (!hasMx) {
                return new ValidationResult(false,
                    "This email domain cannot receive emails.\nPlease use a different email.");
            }

            // ── Check SMTP validity ───────────────────────────────────
            boolean isSmtpValid = json.path("is_smtp_valid")
                                      .path("value").asBoolean(true);
            if (!isSmtpValid) {
                return new ValidationResult(false,
                    "This email address does not exist on the mail server.\nPlease double-check your email.");
            }

            return new ValidationResult(true, "Email is valid ✔");

        } catch (Exception e) {
            // If API fails for any reason, fail open
            System.err.println("[EmailValidation] AbstractAPI error: " + e.getMessage());
            return new ValidationResult(true, "API check skipped");
        }
    }
}
