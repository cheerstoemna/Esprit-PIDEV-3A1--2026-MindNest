package utils;

import javafx.scene.image.Image;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ImageUtil {
    private static final Map<String, String> CACHE = new ConcurrentHashMap<>();

    /**
     * Backward compatible wrapper (some controllers call this name).
     */
    public static Image loadSmartImage(String urlOrPath, double w, double h) {
        return loadSmart(urlOrPath, w, h);
    }

    /**
     * Loads:
     * - http/https URL (downloads with a User-Agent so sites that block JavaFX default still work)
     * - file:/ URL
     * - Windows path / relative file path
     */
    public static Image loadSmart(String urlOrPath, double w, double h) {
        try {
            if (urlOrPath == null) return null;
            String raw = urlOrPath.trim();
            if (raw.isEmpty()) return null;

            // http(s) : download to temp (cached)
            if (raw.startsWith("http://") || raw.startsWith("https://")) {
                String local = CACHE.get(raw);
                if (local == null || !Files.exists(Path.of(local))) {
                    local = downloadToTemp(raw);
                    if (local != null) CACHE.put(raw, local);
                }
                if (local != null) {
                    return new Image(new File(local).toURI().toString(), w, h, true, true, true);
                }
                // fallback to direct load
                return new Image(raw, w, h, true, true, true);
            }

            // file URL
            if (raw.startsWith("file:")) {
                return new Image(raw, w, h, true, true, true);
            }

            // treat as local file path
            File f = new File(raw);
            if (f.exists()) {
                return new Image(f.toURI().toString(), w, h, true, true, true);
            }

            // resource path
            var res = ImageUtil.class.getResource(raw.startsWith("/") ? raw : "/" + raw);
            if (res != null) {
                return new Image(res.toExternalForm(), w, h, true, true, true);
            }
        } catch (Exception ignored) { }
        return null;
    }

    private static String downloadToTemp(String url) {
        try {
            URL u = URI.create(url).toURL();
            URLConnection conn = u.openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (MindNest JavaFX)");
            conn.setConnectTimeout(6000);
            conn.setReadTimeout(10000);

            String suffix = ".img";
            String contentType = conn.getContentType();
            if (contentType != null) {
                if (contentType.contains("png")) suffix = ".png";
                else if (contentType.contains("jpeg") || contentType.contains("jpg")) suffix = ".jpg";
                else if (contentType.contains("webp")) suffix = ".webp";
            }
            Path temp = Files.createTempFile("mindnest_img_", suffix);
            temp.toFile().deleteOnExit();

            try (InputStream in = conn.getInputStream();
                 FileOutputStream out = new FileOutputStream(temp.toFile())) {
                in.transferTo(out);
            }
            return temp.toString();
        } catch (Exception ignored) {
            return null;
        }
    }
}
