package services;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GeminiPro {

    private static final String API_KEY = "AIzaSyC7mtvVIeC7tH7blljFzCnePW_h7P72e8Y"; // <-- put your Gemini API key here

    private int currentModelIndex = 0;
    private static final String[] MODELS = {
            "gemini-2.0-flash",  // safest free model
            "gemini-2.5-flash",
            "gemini-2.5-pro"
    };

    // ANSI colors for console (optional)
    private static final String RESET = "\u001B[0m";
    private static final String GREEN = "\u001B[32m";
    private static final String RED = "\u001B[31m";
    private static final String YELLOW = "\u001B[33m";
    private static final String BOLD = "\u001B[1m";

    public String analyzeText(String prompt) {
        boolean success = false;
        String text = "";

        while (!success && currentModelIndex < MODELS.length) {
            String model = MODELS[currentModelIndex];
            try {
                String response = sendRequest(prompt, model);
                text = extractText(response);
                success = true;
            } catch (QuotaExceededException e) {
                currentModelIndex++;
            } catch (Exception e) {
                return "Error: " + e.getMessage();
            }
        }

        if (!success) return "All models quota exceeded, try again later.";
        return text;
    }

    private String sendRequest(String prompt, String model) throws Exception {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                + model + ":generateContent?key=" + API_KEY;

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();

        String escapedPrompt = prompt.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n");
        String jsonBody = "{\"contents\": [{\"parts\":[{\"text\": \"" + escapedPrompt + "\"}]}]}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 429) throw new QuotaExceededException();
        if (response.statusCode() != 200)
            throw new RuntimeException("API error (" + response.statusCode() + "): " + response.body());

        return response.body();
    }

    private String extractText(String json) {
        Pattern pattern = Pattern.compile("\"text\":\\s*\"(.*?)\"([\\s,}]|$)", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return matcher.group(1).replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\");
        }
        return "No text found in response.";
    }

    private static class QuotaExceededException extends Exception {
    }

    // --- Test ---
    public static void main(String[] args) {
        GeminiPro gemini = new GeminiPro();
        String result = gemini.analyzeText("Hello, analyze this journal entry: I feel happy today!");
        System.out.println(GREEN + BOLD + "Gemini Response:\n" + RESET + result);
    }
}