package controllers;

// ═══════════════════════════════════════════════════════════════════════════════
//  FILE: src/main/java/controllers/ReminderBannerController.java
//
//  ENHANCED: Shows a highlighted detail card for EACH upcoming session
//  instead of just saying "you have 2 sessions in 24 hours"
//
//  Each card shows:
//    ⚡/⏰/📅 Urgency badge + therapist name
//    📅 Full date + time
//    ⏱ Duration  •  Status badge
//    🗒 Notes preview (if any)
//    [View Session →] button
// ═══════════════════════════════════════════════════════════════════════════════

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import models.TherapySession;
import services.SessionReminderService;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

public class ReminderBannerController {

    @FXML private VBox reminderContainer;

    private final SessionReminderService reminderService = new SessionReminderService();

    private static final Map<Integer, String> THERAPIST_NAMES = Map.of(
            1, "Dr. Mark Thompson",
            2, "Dr. Lina Ben Ali",
            3, "Dr. Sami Gharbi"
    );

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("EEEE, MMMM d 'at' h:mm a");

    private Runnable onViewSession;
    private Runnable onViewAll;

    // ─────────────────────────────────────────────────────────────────────────
    //  CALLED FROM ClientTherapyController after FXML loads
    // ─────────────────────────────────────────────────────────────────────────
    public void loadReminders(int userId) {
        try {
            List<TherapySession> upcoming = reminderService.getUpcomingSessions(userId, 24);

            if (upcoming.isEmpty()) {
                hideBanner();
                return;
            }

            // Clear and rebuild
            reminderContainer.getChildren().clear();
            reminderContainer.setSpacing(0);
            reminderContainer.setStyle("-fx-background-radius: 8; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.12), 8, 0, 0, 2);");

            // Dark header
            reminderContainer.getChildren().add(buildHeader(upcoming.size()));

            // One highlighted card per session
            for (TherapySession session : upcoming) {
                reminderContainer.getChildren().add(buildSessionCard(session));
            }

            showBanner();

        } catch (SQLException e) {
            hideBanner();
            System.err.println("ReminderBanner: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  HEADER BAR
    // ─────────────────────────────────────────────────────────────────────────
    private HBox buildHeader(int count) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 16, 10, 16));
        row.setStyle("-fx-background-color: #064e3b; -fx-background-radius: 8 8 0 0;");

        Label bell  = new Label("🔔");
        bell.setStyle("-fx-font-size: 15px;");

        Label title = new Label(
                count == 1
                        ? "You have 1 upcoming session in the next 24 hours"
                        : "You have " + count + " upcoming sessions in the next 24 hours"
        );
        title.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #d1fae5;");
        HBox.setHgrow(title, Priority.ALWAYS);

        Button dismissAll = new Button("✕  Dismiss all");
        dismissAll.setStyle(
                "-fx-background-color: rgba(255,255,255,0.1); -fx-text-fill: #6ee7b7;" +
                        "-fx-font-size: 11px; -fx-cursor: hand; -fx-padding: 3 10; -fx-background-radius: 4;"
        );
        dismissAll.setOnAction(e -> hideBanner());

        row.getChildren().addAll(bell, title, dismissAll);
        return row;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  SESSION DETAIL CARD — one per upcoming session
    // ─────────────────────────────────────────────────────────────────────────
    private VBox buildSessionCard(TherapySession session) {
        long hoursUntil = ChronoUnit.HOURS.between(LocalDateTime.now(), session.getSessionDate());
        long minsUntil  = ChronoUnit.MINUTES.between(LocalDateTime.now(), session.getSessionDate());

        // Urgency colours
        String urgencyEmoji, urgencyText, cardBg, accentColor, urgencyTextColor;
        if (minsUntil < 60) {
            urgencyEmoji    = "⚡";
            urgencyText     = "Starting in " + minsUntil + " min!";
            cardBg          = "#fff1f2";
            accentColor     = "#ef4444";
            urgencyTextColor = "#7f1d1d";
        } else if (hoursUntil < 3) {
            urgencyEmoji    = "⏰";
            urgencyText     = "In " + hoursUntil + " hour" + (hoursUntil == 1 ? "" : "s");
            cardBg          = "#fffbeb";
            accentColor     = "#f59e0b";
            urgencyTextColor = "#78350f";
        } else {
            urgencyEmoji    = "📅";
            urgencyText     = "In " + hoursUntil + " hours";
            cardBg          = "#f0fdf4";
            accentColor     = "#10b981";
            urgencyTextColor = "#064e3b";
        }

        // Card wrapper
        VBox card = new VBox(10);
        card.setPadding(new Insets(14, 16, 14, 16));
        card.setStyle(
                "-fx-background-color: " + cardBg + ";" +
                        "-fx-border-color: " + accentColor + ";" +
                        "-fx-border-width: 0 0 0 5;"
        );

        // ── Row 1: urgency badge + therapist + dismiss ────────────────────
        HBox row1 = new HBox(10);
        row1.setAlignment(Pos.CENTER_LEFT);

        Label badge = new Label(urgencyEmoji + "  " + urgencyText);
        badge.setStyle(
                "-fx-background-color: " + accentColor + "; -fx-text-fill: white;" +
                        "-fx-font-size: 11px; -fx-font-weight: bold;" +
                        "-fx-background-radius: 20; -fx-padding: 3 12 3 12;"
        );

        String therapistName = THERAPIST_NAMES.getOrDefault(session.getPsychologistId(), "Your Therapist");
        Label therapistLbl = new Label("👨‍⚕️  " + therapistName);
        therapistLbl.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #111827;");
        HBox.setHgrow(therapistLbl, Priority.ALWAYS);

        Button dismissBtn = new Button("✕");
        dismissBtn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #9ca3af;" +
                        "-fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 0 4;"
        );
        dismissBtn.setOnAction(e -> {
            reminderContainer.getChildren().remove(card);
            if (reminderContainer.getChildren().size() <= 1) hideBanner();
        });

        row1.getChildren().addAll(badge, therapistLbl, dismissBtn);

        // ── Row 2: full date + time ───────────────────────────────────────
        Label dateLbl = new Label("📅  " + session.getSessionDate().format(DATE_FMT));
        dateLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #1f2937; -fx-font-weight: 600;");

        // ── Row 3: duration + status badges ──────────────────────────────
        HBox row3 = new HBox(8);
        row3.setAlignment(Pos.CENTER_LEFT);

        Label durationBadge = new Label("⏱  " + session.getDurationMinutes() + " min");
        durationBadge.setStyle(
                "-fx-background-color: #374151; -fx-text-fill: white;" +
                        "-fx-font-size: 11px; -fx-font-weight: bold;" +
                        "-fx-background-radius: 10; -fx-padding: 3 10;"
        );

        String statusColor = "Scheduled".equals(session.getSessionStatus()) ? "#1d4ed8" : "#059669";
        Label statusBadge = new Label(session.getSessionStatus());
        statusBadge.setStyle(
                "-fx-background-color: " + statusColor + "; -fx-text-fill: white;" +
                        "-fx-font-size: 11px; -fx-font-weight: bold;" +
                        "-fx-background-radius: 10; -fx-padding: 3 10;"
        );

        row3.getChildren().addAll(durationBadge, statusBadge);

        card.getChildren().addAll(row1, dateLbl, row3);

        // ── Row 4: notes preview (if any) ─────────────────────────────────
        String notes = session.getSessionNotes();
        if (notes != null && !notes.isBlank()) {
            String preview = notes.length() > 90 ? notes.substring(0, 87) + "..." : notes;
            Label notesLbl = new Label("🗒  " + preview);
            notesLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #6b7280; -fx-font-style: italic;");
            notesLbl.setWrapText(true);
            card.getChildren().add(notesLbl);
        }

        // ── Row 5: View button ────────────────────────────────────────────
        HBox actionRow = new HBox();
        actionRow.setAlignment(Pos.CENTER_RIGHT);

        Button viewBtn = new Button("View Session  →");
        viewBtn.setStyle(
                "-fx-background-color: " + accentColor + "; -fx-text-fill: white;" +
                        "-fx-font-size: 11px; -fx-font-weight: bold;" +
                        "-fx-background-radius: 6; -fx-padding: 6 16; -fx-cursor: hand;"
        );
        viewBtn.setOnAction(e -> { if (onViewSession != null) onViewSession.run(); });

        actionRow.getChildren().add(viewBtn);
        card.getChildren().add(actionRow);

        return card;
    }

    // ─────────────────────────────────────────────────────────────────────────
    private void showBanner() { reminderContainer.setVisible(true);  reminderContainer.setManaged(true);  }
    private void hideBanner()  { reminderContainer.setVisible(false); reminderContainer.setManaged(false); }

    public void setOnViewSession(Runnable cb) { this.onViewSession = cb; }
    public void setOnViewAll(Runnable cb)     { this.onViewAll = cb; }
}