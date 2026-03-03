package services;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Minimal YouTube Data API v3 search importer (no external JSON libs).
 * Uses endpoint:
 * https://www.googleapis.com/youtube/v3/search?part=snippet&type=video&q=...&maxResults=...&key=...
 *
 * Note: this imports public search results only (no OAuth).
 */
public class YouTubeImportService {

    // ✅ put your API key here
    private static final String YOUTUBE_API_KEY = "AIzaSyBbZIDMYLyebtL0OrjONjeZQDenG7RnOPQ";

    private static final String API = "https://www.googleapis.com/youtube/v3/search";
    private static final Duration TIMEOUT = Duration.ofSeconds(25);

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public static class YouTubeVideo {
        public String videoId;
        public String title;
        public String description;
        public String channelTitle;
        public String thumbnailUrl;

        // filled by AdminController (category chosen by admin)
        public String category;

        @Override
        public String toString() {
            return title;
        }
    }

    public List<YouTubeVideo> searchVideos(String query, int maxResults) {
        if (YOUTUBE_API_KEY == null || YOUTUBE_API_KEY.isBlank() || YOUTUBE_API_KEY.contains("PUT_YOUR")) {
            throw new RuntimeException("YouTube API key missing. Set YOUTUBE_API_KEY in YouTubeImportService.");
        }

        try {
            String q = URLEncoder.encode(query, StandardCharsets.UTF_8);
            int n = Math.max(1, Math.min(maxResults, 25));

            String url = API
                    + "?part=snippet"
                    + "&type=video"
                    + "&maxResults=" + n
                    + "&q=" + q
                    + "&key=" + URLEncoder.encode(YOUTUBE_API_KEY, StandardCharsets.UTF_8);

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(TIMEOUT)
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (res.statusCode() < 200 || res.statusCode() >= 300) {
                throw new RuntimeException("YouTube API error HTTP " + res.statusCode() + ": " + shorten(res.body(), 240));
            }

            return parseSearchResponse(res.body());

        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Parses items with:
     * - "videoId":"..."
     * - "title":"..."
     * - "description":"..."
     * - "channelTitle":"..."
     * - thumbnails: prefer "high" url then fallback to any "url"
     *
     * This is intentionally minimal; for production use a JSON parser.
     */
    private List<YouTubeVideo> parseSearchResponse(String json) {
        List<YouTubeVideo> out = new ArrayList<>();
        if (json == null) return out;

        // split into "items" blocks crudely
        Pattern itemPattern = Pattern.compile("\\{\\s*\"kind\"\\s*:\\s*\"youtube#searchResult\".*?\\}\\s*\\}", Pattern.DOTALL);
        Matcher itemMatcher = itemPattern.matcher(json);

        while (itemMatcher.find()) {
            String block = itemMatcher.group();

            String videoId = extract(block, "\"videoId\"\\s*:\\s*\"(.*?)\"");
            if (videoId == null || videoId.isBlank()) continue;

            String title = unescape(extract(block, "\"title\"\\s*:\\s*\"(.*?)\""));
            String desc = unescape(extract(block, "\"description\"\\s*:\\s*\"(.*?)\""));
            String channel = unescape(extract(block, "\"channelTitle\"\\s*:\\s*\"(.*?)\""));

            // try high thumbnail first
            String thumb = extract(block, "\"high\"\\s*:\\s*\\{[^\\}]*\"url\"\\s*:\\s*\"(.*?)\"");
            if (thumb == null) {
                // fallback to any url
                thumb = extract(block, "\"url\"\\s*:\\s*\"(.*?)\"");
            }
            thumb = unescape(thumb);

            YouTubeVideo v = new YouTubeVideo();
            v.videoId = videoId;
            v.title = title == null ? "" : title;
            v.description = desc == null ? "" : desc;
            v.channelTitle = channel == null ? "" : channel;
            v.thumbnailUrl = (thumb == null || thumb.isBlank())
                    ? ("https://i.ytimg.com/vi/" + videoId + "/hqdefault.jpg")
                    : thumb;

            out.add(v);
        }

        return out;
    }

    private String extract(String s, String regex) {
        if (s == null) return null;
        Matcher m = Pattern.compile(regex, Pattern.DOTALL).matcher(s);
        return m.find() ? m.group(1) : null;
    }

    private String unescape(String s) {
        if (s == null) return null;
        return s.replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }

    private String shorten(String s, int max) {
        if (s == null) return "";
        String t = s.trim();
        if (t.length() <= max) return t;
        return t.substring(0, max) + "...";
    }
}