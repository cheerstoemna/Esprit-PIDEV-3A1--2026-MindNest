package services;

// ═══════════════════════════════════════════════════════════════════════════════
//  FILE: src/main/java/services/CurrencyConversionService.java
//  Uses ExchangeRate API — free tier 1,500 requests/month
//  Docs: https://exchangerate-api.com
// ═══════════════════════════════════════════════════════════════════════════════

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;

public class CurrencyConversionService {

    private static final String API_KEY   = "c35c93b365ee31c7d0ebc7c5";
    private static final String BASE_URL  = "https://v6.exchangerate-api.com/v6/" + API_KEY + "/pair/TND/";
    private static final double BASE_PRICE_TND = 80.0; // fixed session price in TND

    // Currencies to show on each session card
    private static final String[] TARGET_CURRENCIES = { "USD", "EUR", "GBP" };

    // ─────────────────────────────────────────────────────────────────────────
    //  ConversionResult — holds all converted prices for one session card
    // ─────────────────────────────────────────────────────────────────────────
    public static class ConversionResult {
        // key = currency code (USD, EUR, GBP), value = formatted string (e.g. "26.50 USD")
        public final Map<String, String> prices = new LinkedHashMap<>();
        public boolean success = false;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  convert() — fetches live rates and returns all converted prices
    //  Called on a background thread from the controller
    // ─────────────────────────────────────────────────────────────────────────
    // Approximate fallback rates (TND as of early 2026)
    private static final java.util.Map<String, Double> FALLBACK_RATES = java.util.Map.of(
            "USD", 0.3156,
            "EUR", 0.2921,
            "GBP", 0.2487
    );

    public ConversionResult convert() {
        ConversionResult result = new ConversionResult();

        for (String currency : TARGET_CURRENCIES) {
            try {
                double rate = fetchRate(currency);
                double converted = BASE_PRICE_TND * rate;
                result.prices.put(currency, String.format("%.2f %s", converted, currency));
                result.success = true;
                System.out.println("[Currency] 80 TND = " + result.prices.get(currency));
            } catch (Exception e) {
                System.err.println("[Currency] API failed for " + currency + " — using fallback rate");
                double fallback   = FALLBACK_RATES.getOrDefault(currency, 1.0);
                double converted  = BASE_PRICE_TND * fallback;
                result.prices.put(currency, String.format("%.2f %s*", converted, currency));
                result.success = true; // still show the value, just with * to indicate fallback
            }
        }
        return result;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  fetchRate() — calls API for TND → target currency rate
    //  Response: {"result":"success","conversion_rate":0.3156,...}
    // ─────────────────────────────────────────────────────────────────────────
    private double fetchRate(String targetCurrency) throws Exception {
        URL url = new URL(BASE_URL + targetCurrency);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

        int status = conn.getResponseCode();
        InputStream is = status == 200 ? conn.getInputStream() : conn.getErrorStream();
        String json = new Scanner(is, StandardCharsets.UTF_8).useDelimiter("\\A").next();

        System.out.println("[Currency] " + targetCurrency + " response: " + json.substring(0, Math.min(80, json.length())));

        // If API returned an error result, throw so fallback kicks in
        if (json.contains("\"result\":\"error\"")) {
            throw new Exception("API quota exceeded or key error");
        }

        // Parse conversion_rate from JSON
        String search = "\"conversion_rate\":";
        int start = json.indexOf(search);
        if (start == -1) throw new Exception("conversion_rate not found in response");
        start += search.length();
        int end = json.indexOf(",", start);
        if (end == -1) end = json.indexOf("}", start);
        return Double.parseDouble(json.substring(start, end).trim());
    }
}