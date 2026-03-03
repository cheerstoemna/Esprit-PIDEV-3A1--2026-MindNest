package services;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class TranslateService {

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(6))
            .build();

    /**
     * Uses MyMemory public API (no key).
     * Example:
     *   https://api.mymemory.translated.net/get?q=Hello&langpair=en|fr
     */
    public String translate(String text, String targetLang) throws Exception {
        if (text == null || text.isBlank()) return "";

        // In your app, your source content is basically English.
        String sourceLang = "en";

        String url = "https://api.mymemory.translated.net/get?q=" +
                urlEncode(text) +
                "&langpair=" + urlEncode(sourceLang + "|" + targetLang);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(12))
                .GET()
                .build();

        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200) {
            throw new RuntimeException("Translate status " + res.statusCode() + " body=" + res.body());
        }

        // JSON contains: responseData.translatedText
        String body = res.body();
        String translated = extract(body, "\"translatedText\":\"", "\"");
        if (translated.isBlank()) {
            throw new RuntimeException("No translatedText in response: " + body);
        }

        // Fix: convert uXXXX sequences + common escapes/entities
        translated = unescapeUnicodeAndCommon(translated);

        return translated;
    }

    private String urlEncode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private String extract(String json, String start, String end) {
        int i = json.indexOf(start);
        if (i < 0) return "";
        i += start.length();
        int j = json.indexOf(end, i);
        if (j < 0) return "";
        return json.substring(i, j);
    }

    // Converts things like \u00e9, \u2014, \n, \" and some HTML entities
    private String unescapeUnicodeAndCommon(String s) {
        if (s == null || s.isEmpty()) return s;

        // First handle common HTML entities MyMemory sometimes returns
        s = s.replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&amp;", "&");

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); ) {
            char c = s.charAt(i);

            // uXXXX
            if (c == '\\' && i + 1 < s.length() && s.charAt(i + 1) == 'u' && i + 6 <= s.length()) {
                String hex = s.substring(i + 2, i + 6);
                try {
                    sb.append((char) Integer.parseInt(hex, 16));
                    i += 6;
                    continue;
                } catch (NumberFormatException ignored) {
                    // fall through
                }
            }

            // other escapes: \n, \t, \", \\
            if (c == '\\' && i + 1 < s.length()) {
                char n = s.charAt(i + 1);
                if (n == 'n') { sb.append('\n'); i += 2; continue; }
                if (n == 't') { sb.append('\t'); i += 2; continue; }
                if (n == '"') { sb.append('"'); i += 2; continue; }
                if (n == '\\') { sb.append('\\'); i += 2; continue; }
            }

            sb.append(c);
            i++;
        }
        return sb.toString();
    }
}