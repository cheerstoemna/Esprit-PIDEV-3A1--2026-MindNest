package services;

import com.sun.net.httpserver.HttpServer;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;

/**
 * CaptchaServer
 *
 * Starts a tiny local HTTP server that serves captcha.html at:
 *   http://localhost:8765/captcha
 *
 * WHY THIS IS NEEDED:
 * When JavaFX WebView loads a file via file:// path, Google reCAPTCHA
 * rejects it because "file://" is not a valid registered domain.
 * By serving it over http://localhost, Google sees "localhost" which
 * matches the domain you registered in the reCAPTCHA admin console.
 */
public class CaptchaServer {

    private static HttpServer server;
    private static final int PORT = 8765;

    /**
     * Starts the local HTTP server (only once — safe to call multiple times).
     * @return the URL to load in the WebView: "http://localhost:8765/captcha"
     */
    public static String start() throws Exception {
        // Prevent starting the server twice if login screen is re-opened
        if (server != null) {
            return "http://localhost:" + PORT + "/captcha";
        }

        server = HttpServer.create(new InetSocketAddress("localhost", PORT), 0);

        server.createContext("/captcha", exchange -> {
            // Read captcha.html from the root of the resources folder
            InputStream is = CaptchaServer.class
                    .getClassLoader()
                    .getResourceAsStream("captcha.html");

            if (is == null) {
                String error = "captcha.html not found in resources";
                exchange.sendResponseHeaders(404, error.length());
                exchange.getResponseBody().write(error.getBytes());
                exchange.getResponseBody().close();
                return;
            }

            byte[] bytes = is.readAllBytes();
            is.close();

            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
            exchange.sendResponseHeaders(200, bytes.length);

            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        });

        server.setExecutor(null); // use default executor
        server.start();

        System.out.println("[CaptchaServer] Started at http://localhost:" + PORT + "/captcha");
        return "http://localhost:" + PORT + "/captcha";
    }

    /**
     * Stops the server. Call this from your Application.stop() method.
     */
    public static void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
            System.out.println("[CaptchaServer] Stopped.");
        }
    }
}
