package services;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class QuoteService {

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public String fetchQuoteText() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("https://zenquotes.io/api/random"))
                .timeout(Duration.ofSeconds(8))
                .GET()
                .build();

        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200) {
            throw new RuntimeException("Quote API status " + res.statusCode());
        }

        String body = res.body(); // [{"q":"...","a":"..."}]
        String q = extract(body, "\"q\":\"", "\"");
        String a = extract(body, "\"a\":\"", "\"");

        if (q.isBlank()) return "Stay consistent. Small steps compound.";
        if (a.isBlank()) return q;
        return q + " — " + a;
    }

    private String extract(String json, String start, String end) {
        int i = json.indexOf(start);
        if (i < 0) return "";
        i += start.length();
        int j = json.indexOf(end, i);
        if (j < 0) return "";
        return json.substring(i, j)
                .replace("\\n", "\n")
                .replace("\\\"", "\"")
                .replace("\\u2019", "’")
                .replace("\\u201c", "“")
                .replace("\\u201d", "”");
    }
}