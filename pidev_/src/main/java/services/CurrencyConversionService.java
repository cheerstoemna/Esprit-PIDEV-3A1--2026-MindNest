package services;

// ═══════════════════════════════════════════════════════════════════════════════
//  FILE: src/main/java/services/CurrencyConversionService.java
//
//  FIX: Switched from exchangerate-api.com (paid key, expired/rate-limited)
//       to open.er-api.com — completely FREE, no API key required,
//       same JSON format, 1,500 requests/month free tier.
//
//  Endpoint: https://open.er-api.com/v6/latest/TND
// ═══════════════════════════════════════════════════════════════════════════════

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;

public class CurrencyConversionService {

    // No API key needed — completely free endpoint
    private static final String BASE_URL        = "https://open.er-api.com/v6/latest/TND";
    private static final double BASE_PRICE_TND  = 80.0;
    private static final String[] TARGET_CURRENCIES = { "USD", "EUR", "GBP" };

    // ─────────────────────────────────────────────────────────────────────────
    public static class ConversionResult {
        public final Map<String, String> prices = new LinkedHashMap<>();
        public boolean success = false;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  convert() — fetches all rates in a SINGLE API call (more efficient)
    // ─────────────────────────────────────────────────────────────────────────
    public ConversionResult convert() {
        ConversionResult result = new ConversionResult();

        try {
            String json = fetchJson(BASE_URL);
            System.out.println("[Currency] Response: " + json.substring(0, Math.min(120, json.length())));

            for (String currency : TARGET_CURRENCIES) {
                try {
                    double rate      = extractRate(json, currency);
                    double converted = BASE_PRICE_TND * rate;
                    result.prices.put(currency, String.format("%.2f %s", converted, currency));
                    result.success = true;
                    System.out.println("[Currency] 80 TND = " + result.prices.get(currency));
                } catch (Exception e) {
                    System.err.println("[Currency] Could not parse " + currency + ": " + e.getMessage());
                    result.prices.put(currency, "— " + currency);
                }
            }

        } catch (Exception e) {
            System.err.println("[Currency] API call failed: " + e.getMessage());
            for (String c : TARGET_CURRENCIES) result.prices.put(c, "— " + c);
        }

        return result;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  fetchJson() — simple HTTP GET, returns full response body
    // ─────────────────────────────────────────────────────────────────────────
    private String fetchJson(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(6000);
        conn.setReadTimeout(6000);
        conn.setRequestProperty("Accept", "application/json");

        int status = conn.getResponseCode();
        InputStream is = (status == 200) ? conn.getInputStream() : conn.getErrorStream();
        return new Scanner(is, StandardCharsets.UTF_8).useDelimiter("\\A").next();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  extractRate() — parses the rate for a given currency from JSON
    //
    //  Response format from open.er-api.com:
    //  {
    //    "result": "success",
    //    "rates": {
    //      "USD": 0.3156,
    //      "EUR": 0.2890,
    //      ...
    //    }
    //  }
    // ─────────────────────────────────────────────────────────────────────────
    private double extractRate(String json, String currency) throws Exception {
        // Look for "USD": 0.3156  (or with spaces around the colon)
        String search = "\"" + currency + "\":";
        int idx = json.indexOf(search);
        if (idx == -1) {
            // Try with a space: "USD": 0.3156
            search = "\"" + currency + "\": ";
            idx = json.indexOf(search);
        }
        if (idx == -1) throw new Exception("Currency " + currency + " not found in response");

        int start = idx + search.length();
        // Skip any extra whitespace
        while (start < json.length() && (json.charAt(start) == ' ' || json.charAt(start) == '\t')) start++;

        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '.')) end++;

        if (start == end) throw new Exception("Could not parse rate value for " + currency);
        return Double.parseDouble(json.substring(start, end));
    }
}