package services;

// ═══════════════════════════════════════════════════════════════════════════════
//  FILE: src/main/java/services/NtfyNotificationService.java
//
//  Uses ntfy.sh — completely free push notifications, no signup, no key
//  Docs: https://ntfy.sh
//
//  SETUP (30 seconds):
//    1. Install "ntfy" app on your phone (Play Store / App Store)
//    2. Open the app → tap "+" → subscribe to topic: mindnest-emna
//    3. Done — you'll receive push notifications whenever a session event happens
//
//  HOW IT WORKS:
//    Java sends a POST to https://ntfy.sh/mindnest-emna
//    The ntfy app on your phone is subscribed to that topic
//    → notification appears instantly
// ═══════════════════════════════════════════════════════════════════════════════

import models.TherapySession;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;

public class NtfyNotificationService {

    // ── Topic — unique to your app (change if you want a different channel) ──
    private static final String TOPIC   = "mindnest-emna";
    private static final String API_URL = "https://ntfy.sh/" + TOPIC;

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("EEE MMM d 'at' HH:mm");

    // ─────────────────────────────────────────────────────────────────────────
    //  PUBLIC METHODS
    // ─────────────────────────────────────────────────────────────────────────

    /** Called after a session is booked */
    public void notifyBooked(TherapySession session, String therapistName) {
        push(
                "🌿 Session Confirmed — MindNest",
                "Your session with Dr. " + therapistName + " is booked!\n"
                        + "📅 " + session.getSessionDate().format(FMT) + "\n"
                        + "⏱ " + session.getDurationMinutes() + " min  •  See you there!",
                "white_check_mark,calendar,tada",
                "high"
        );
    }

    /** Called after a session is cancelled */
    public void notifyCancelled(TherapySession session, String therapistName) {
        push(
                "❌ Session Cancelled — MindNest",
                "Your session with Dr. " + therapistName + " has been cancelled.\n"
                        + "📅 " + session.getSessionDate().format(FMT) + "\n"
                        + "You can book a new session anytime.",
                "x,calendar",
                "default"
        );
    }

    /** Called after a session is updated */
    public void notifyUpdated(TherapySession session, String therapistName) {
        push(
                "✏️ Session Updated — MindNest",
                "Your session with Dr. " + therapistName + " has new details.\n"
                        + "📅 " + session.getSessionDate().format(FMT) + "\n"
                        + "⏱ " + session.getDurationMinutes() + " min  •  Check the app for details.",
                "pencil,calendar",
                "default"
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  INTERNAL
    // ─────────────────────────────────────────────────────────────────────────
    private void push(String title, String message, String tags, String priority) {
        new Thread(() -> {
            try {
                URL url = new URL(API_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                // ntfy uses headers to set title, tags, priority
                conn.setRequestProperty("Title",    title);
                conn.setRequestProperty("Tags",     tags);
                conn.setRequestProperty("Priority", priority);
                conn.setRequestProperty("Content-Type", "text/plain");

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(message.getBytes(StandardCharsets.UTF_8));
                }

                int status = conn.getResponseCode();
                System.out.println("[Ntfy] status=" + status + " | " + title);

            } catch (Exception e) {
                System.err.println("[Ntfy] Failed: " + e.getMessage());
            }
        }).start();
    }
}