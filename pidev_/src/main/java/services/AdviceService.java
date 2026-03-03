package services;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AdviceService {

    private final HttpClient client = HttpClient.newHttpClient();

    // captures JSON string contents, including escaped quotes
    private static final Pattern ADVICE_PATTERN =
            Pattern.compile("\"advice\"\\s*:\\s*\"((?:\\\\.|[^\"])*)\"");

    public String fetchAdvice() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("https://api.adviceslip.com/advice"))
                .GET()
                .header("Accept", "application/json")
                .header("User-Agent", "MindNest/1.0")
                .header("Cache-Control", "no-cache")
                .build();

        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());

        if (res.statusCode() != 200) {
            throw new RuntimeException("Advice API HTTP " + res.statusCode() + " body=" + res.body());
        }

        String body = res.body();

        Matcher m = ADVICE_PATTERN.matcher(body);
        if (!m.find()) {
            throw new RuntimeException("Advice field not found. body=" + body);
        }

        String advice = m.group(1);

        // minimal JSON unescape
        advice = advice
                .replace("\\\"", "\"")
                .replace("\\n", " ")
                .replace("\\r", " ")
                .replace("\\t", " ")
                .replace("\\\\", "\\");

        return advice.trim();
    }
}