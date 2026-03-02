package controllers;

// ═══════════════════════════════════════════════════════════════════════════════
//  FILE: src/main/java/controllers/ClientTherapyController.java
//  REPLACE your existing file with this one.
//
//  What changed vs your old file:
//    1. Added import for ConflictCheckService  (line ~30)
//    2. Added conflictService field            (line ~65)
//    3. book() now runs conflict check first   (marked with ── NEW ──)
//    4. updateSelected() same                  (marked with ── NEW ──)
//    Everything else is identical to your working file.
// ═══════════════════════════════════════════════════════════════════════════════

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import models.SessionFeedback;
import models.TherapySession;
import services.ConflictCheckService;              // ── NEW ── (1 of 2 changes at the top)
import services.ConflictCheckService.ConflictResult;// ── NEW ──
import services.SessionFeedbackService;
import services.SessionSummaryPDFService;
import services.TherapySessionService;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javafx.application.Platform;
import javafx.concurrent.Task;
import services.SentimentAnalysisService;
import services.SentimentAnalysisService.SentimentResult;
import services.EmailNotificationService;
import services.NtfyNotificationService;
import services.CurrencyConversionService;
import services.CurrencyConversionService.ConversionResult;

public class ClientTherapyController {

    @FXML private ListView<String> therapistList;
    @FXML private DatePicker datePicker;
    @FXML private ComboBox<String> timeCombo;
    @FXML private ComboBox<Integer> durationCombo;
    @FXML private TextArea notesArea;

    @FXML private VBox sessionsContainer;
    @FXML private VBox mainContentVBox;
    @FXML private Label lblSessionCount;

    @FXML private Button btnBook;
    @FXML private Button btnRefresh;
    @FXML private Button btnBook1;

    @FXML private Button btnFilterAll;
    @FXML private Button btnFilterScheduled;
    @FXML private Button btnFilterCompleted;
    @FXML private Button btnFilterCancelled;

    private final TherapySessionService  sessionService  = new TherapySessionService();
    private final SessionFeedbackService feedbackService = new SessionFeedbackService();
    private final SessionSummaryPDFService pdfService    = new SessionSummaryPDFService();
    private final ConflictCheckService     conflictService   = new ConflictCheckService();
    private final SentimentAnalysisService sentimentService  = new SentimentAnalysisService();
    private final EmailNotificationService  emailService      = new EmailNotificationService();
    private final CurrencyConversionService currencyService   = new CurrencyConversionService();
    private final NtfyNotificationService   ntfyService       = new NtfyNotificationService();

    private final ObservableList<TherapySession> mySessions = FXCollections.observableArrayList();

    private int selectedPsychologistId = 1;
    private TherapySession selectedSession = null;
    private String currentFilter = "All";

    // ── TODO: replace getUserId() with your real logged-in user ID ──────────
    // When you add authentication, do:
    //   private int getUserId() = SessionManager.getCurrentUser().getId();
    // For now, it stays as 1.
    private int getUserId() { return utils.UserSession.get().userId(); }

    private ReminderBannerController reminderBannerController;

    // ─────────────────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        therapistList.getItems().addAll(
                "Dr. Mark Thompson",
                "Dr. Lina Ben Ali",
                "Dr. Sami Gharbi"
        );
        therapistList.getSelectionModel().select(0);
        therapistList.getSelectionModel().selectedIndexProperty().addListener((obs, o, n) -> {
            int idx = n == null ? 0 : n.intValue();
            selectedPsychologistId = idx + 1;
        });

        timeCombo.getItems().addAll(
                "08:00","08:30","09:00","09:30","10:00","10:30",
                "11:00","11:30","12:00","12:30","13:00","13:30",
                "14:00","14:30","15:00","15:30","16:00","16:30","17:00","17:30"
        );
        durationCombo.getItems().addAll(30, 45, 60, 90);
        durationCombo.setValue(60);

        btnBook.setOnAction(e -> book());
        btnBook1.setOnAction(e -> updateSelected());
        btnRefresh.setOnAction(e -> reload());

        btnFilterAll.setOnAction(e       -> filterSessions("All"));
        btnFilterScheduled.setOnAction(e -> filterSessions("Scheduled"));
        btnFilterCompleted.setOnAction(e -> filterSessions("Completed"));
        btnFilterCancelled.setOnAction(e -> filterSessions("Cancelled"));

        reload();
        loadReminderBanner();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // REMINDER BANNER
    // ─────────────────────────────────────────────────────────────────────────
    private void loadReminderBanner() {
        if (mainContentVBox == null) {
            System.err.println("loadReminderBanner: mainContentVBox is null — " +
                    "add fx:id=\"mainContentVBox\" to your root VBox in TherapyClientDashboard.fxml");
            return;
        }
        java.net.URL bannerUrl = getClass().getResource("/fxml/ReminderBanner.fxml");
        if (bannerUrl == null) {
            System.out.println("ReminderBanner.fxml not found — skipping banner.");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(bannerUrl);
            Node bannerNode = loader.load();
            reminderBannerController = loader.getController();
            mainContentVBox.getChildren().add(0, bannerNode);
            reminderBannerController.loadReminders(getUserId());
            reminderBannerController.setOnViewSession(this::handleFilterScheduled);
            reminderBannerController.setOnViewAll(this::handleFilterAll);
        } catch (IOException e) {
            System.err.println("Could not load ReminderBanner.fxml: " + e.getMessage());
        }
    }

    private void handleFilterScheduled() { filterSessions("Scheduled"); }
    private void handleFilterAll()       { filterSessions("All"); }

    // ─────────────────────────────────────────────────────────────────────────
    // PDF DOWNLOAD
    // ─────────────────────────────────────────────────────────────────────────
    private void handleDownloadPDF(TherapySession session) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Session Summary");
        fileChooser.setInitialFileName("MindNest_Session_" + session.getSessionId() + ".pdf");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF Files", "*.pdf")
        );

        File file = fileChooser.showSaveDialog(mainContentVBox.getScene().getWindow());
        if (file == null) return;

        SessionFeedback feedback = null;
        try {
            feedback = feedbackService.getFeedbackObjectBySessionId(session.getSessionId());
        } catch (SQLException e) {
            // feedback is optional — continue without it
        }

        try {
            pdfService.generateSummary(session, feedback, file.getAbsolutePath());
            showInfo("PDF Saved", "Session summary saved to:\n" + file.getAbsolutePath());
        } catch (Exception e) {
            showError("PDF Error", "Could not generate PDF: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LOAD & FILTER
    // ─────────────────────────────────────────────────────────────────────────
    private void reload() {
        try {
            ObservableList<TherapySession> sessionsList = FXCollections.observableArrayList(
                    sessionService.getSessionsByUser(getUserId())
            );
            for (TherapySession session : sessionsList) {
                String feedback = feedbackService.getFeedbackBySessionId(session.getSessionId());
                session.setFeedbackComment(feedback != null ? feedback : "");
            }
            mySessions.setAll(sessionsList);
            lblSessionCount.setText(mySessions.size() + " sessions");
            renderSessionCards();
        } catch (SQLException e) {
            showError("Failed to load sessions", e.getMessage());
        }
    }

    private void filterSessions(String filter) {
        currentFilter = filter;
        btnFilterAll.getStyleClass().removeAll("filter-btn-active", "filter-btn");
        btnFilterScheduled.getStyleClass().removeAll("filter-btn-active", "filter-btn");
        btnFilterCompleted.getStyleClass().removeAll("filter-btn-active", "filter-btn");
        btnFilterCancelled.getStyleClass().removeAll("filter-btn-active", "filter-btn");
        btnFilterAll.getStyleClass().add(filter.equals("All")       ? "filter-btn-active" : "filter-btn");
        btnFilterScheduled.getStyleClass().add(filter.equals("Scheduled") ? "filter-btn-active" : "filter-btn");
        btnFilterCompleted.getStyleClass().add(filter.equals("Completed") ? "filter-btn-active" : "filter-btn");
        btnFilterCancelled.getStyleClass().add(filter.equals("Cancelled") ? "filter-btn-active" : "filter-btn");
        renderSessionCards();
    }

    private void renderSessionCards() {
        sessionsContainer.getChildren().clear();
        List<TherapySession> filtered = mySessions.stream()
                .filter(s -> currentFilter.equals("All") || s.getSessionStatus().equalsIgnoreCase(currentFilter))
                .toList();
        if (filtered.isEmpty()) {
            Label emptyLabel = new Label("No sessions found");
            emptyLabel.setStyle("-fx-text-fill: #a3bdb8; -fx-font-size: 14px;");
            VBox.setMargin(emptyLabel, new Insets(40, 0, 0, 0));
            sessionsContainer.getChildren().add(emptyLabel);
            return;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE, MMM dd, yyyy 'at' HH:mm");
        for (TherapySession session : filtered) {
            sessionsContainer.getChildren().add(createSessionCard(session, formatter));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SESSION CARD BUILDER
    // ─────────────────────────────────────────────────────────────────────────
    private VBox createSessionCard(TherapySession session, DateTimeFormatter formatter) {
        VBox card = new VBox(12);
        card.getStyleClass().add("session-card");
        card.setPadding(new Insets(18));

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        Label idLabel = new Label("#" + session.getSessionId());
        idLabel.setStyle("-fx-font-weight: 700; -fx-font-size: 16px; -fx-text-fill: #2d5550;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label statusBadge = new Label(session.getSessionStatus());
        statusBadge.getStyleClass().add("status-badge");
        if      (session.getSessionStatus().equalsIgnoreCase("Completed"))  statusBadge.getStyleClass().add("status-completed");
        else if (session.getSessionStatus().equalsIgnoreCase("Scheduled"))  statusBadge.getStyleClass().add("status-scheduled");
        else                                                                 statusBadge.getStyleClass().add("status-cancelled");
        header.getChildren().addAll(idLabel, spacer, statusBadge);

        HBox dateBox = new HBox(8);
        dateBox.setAlignment(Pos.CENTER_LEFT);
        FontAwesomeIconView calIcon = new FontAwesomeIconView(FontAwesomeIcon.CALENDAR);
        calIcon.setSize("13"); calIcon.setFill(Color.web("#5a7571"));
        Label dateLabel = new Label(session.getSessionDate().format(formatter));
        dateLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #5a7571;");
        dateBox.getChildren().addAll(calIcon, dateLabel);

        HBox details = new HBox(20);
        HBox therapistBox = new HBox(6);
        therapistBox.setAlignment(Pos.CENTER_LEFT);
        FontAwesomeIconView docIcon = new FontAwesomeIconView(FontAwesomeIcon.USER_MD);
        docIcon.setSize("13"); docIcon.setFill(Color.web("#5a7571"));
        Label therapistLabel = new Label("Dr. " + getTherapistName(session.getPsychologistId()));
        therapistLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #5a7571;");
        therapistBox.getChildren().addAll(docIcon, therapistLabel);
        HBox durationBox = new HBox(6);
        durationBox.setAlignment(Pos.CENTER_LEFT);
        FontAwesomeIconView clockIcon = new FontAwesomeIconView(FontAwesomeIcon.CLOCK_ALT);
        clockIcon.setSize("13"); clockIcon.setFill(Color.web("#5a7571"));
        Label durationLabel = new Label(session.getDurationMinutes() + " min");
        durationLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #5a7571;");
        durationBox.getChildren().addAll(clockIcon, durationLabel);
        details.getChildren().addAll(therapistBox, durationBox);

        VBox feedbackBox = new VBox(6);
        if (session.getFeedbackComment() != null && !session.getFeedbackComment().isEmpty()) {

            // ── Feedback header row: icon + "Feedback:" label + sentiment badge ──
            HBox feedbackHeader = new HBox(8);
            feedbackHeader.setAlignment(Pos.CENTER_LEFT);

            FontAwesomeIconView chatIcon = new FontAwesomeIconView(FontAwesomeIcon.COMMENT);
            chatIcon.setSize("12"); chatIcon.setFill(Color.web("#7a9794"));

            Label feedbackLabel = new Label("Feedback:");
            feedbackLabel.setStyle("-fx-font-weight: 600; -fx-font-size: 12px; -fx-text-fill: #7a9794;");

            // Sentiment badge — starts as "..." while API call runs in background
            Label sentimentBadge = new Label("  analyzing...  ");
            sentimentBadge.setStyle(
                    "-fx-background-color: #f3f4f6; -fx-text-fill: #6b7280;" +
                            "-fx-background-radius: 12; -fx-font-size: 10px; -fx-font-weight: 700;" +
                            "-fx-padding: 2 8 2 8;"
            );

            feedbackHeader.getChildren().addAll(chatIcon, feedbackLabel, sentimentBadge);

            // Feedback comment text
            Label commentLabel = new Label(session.getFeedbackComment());
            commentLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #5a7571;");
            commentLabel.setWrapText(true);

            feedbackBox.getChildren().addAll(feedbackHeader, commentLabel);

            // ── Background thread: call API, update badge when done ───────────
            String commentText = session.getFeedbackComment();
            Task<SentimentResult> task = new Task<>() {
                @Override protected SentimentResult call() {
                    return sentimentService.analyze(commentText);
                }
            };
            task.setOnSucceeded(e -> {
                SentimentResult result = task.getValue();
                // Update badge on JavaFX thread
                Platform.runLater(() -> sentimentBadge.setStyle(
                        "-fx-background-color: " + result.bgColor + ";" +
                                "-fx-text-fill: "        + result.color   + ";" +
                                "-fx-background-radius: 12; -fx-font-size: 10px; -fx-font-weight: 700;" +
                                "-fx-padding: 2 8 2 8;"
                ));
                Platform.runLater(() -> sentimentBadge.setText("  " + result.label + "  "));
            });
            task.setOnFailed(e ->
                    Platform.runLater(() -> sentimentBadge.setText("  — unknown  "))
            );
            new Thread(task).start(); // fire and forget
        }

        // ── Currency conversion widget ────────────────────────────────────
        HBox currencyBox = new HBox(10);
        currencyBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label priceLabel = new Label("💰 80 TND");
        priceLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #2d7d6f;");

        Label usdLabel = new Label("···");
        Label eurLabel = new Label("···");
        Label gbpLabel = new Label("···");

        for (Label lbl : new Label[]{usdLabel, eurLabel, gbpLabel}) {
            lbl.setStyle(
                    "-fx-background-color: #f0fdf4; -fx-text-fill: #166534;" +
                            "-fx-background-radius: 8; -fx-font-size: 10px; -fx-font-weight: 700;" +
                            "-fx-padding: 2 8 2 8; -fx-border-color: #bbf7d0; -fx-border-radius: 8;"
            );
        }

        currencyBox.getChildren().addAll(priceLabel, usdLabel, eurLabel, gbpLabel);

        // Fetch live rates in background
        Task<ConversionResult> currencyTask = new Task<>() {
            @Override protected ConversionResult call() {
                return currencyService.convert();
            }
        };
        currencyTask.setOnSucceeded(e -> {
            ConversionResult result = currencyTask.getValue();
            Platform.runLater(() -> {
                usdLabel.setText(result.prices.getOrDefault("USD", "— USD"));
                eurLabel.setText(result.prices.getOrDefault("EUR", "— EUR"));
                gbpLabel.setText(result.prices.getOrDefault("GBP", "— GBP"));
            });
        });
        new Thread(currencyTask).start();

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_LEFT);

        if (session.getSessionStatus().equalsIgnoreCase("Scheduled")) {
            Button editBtn = createIconButton("Edit", FontAwesomeIcon.PENCIL, "card-action-btn");
            editBtn.setOnAction(e -> selectSessionForEdit(session));
            Button cancelBtn = createIconButton("Cancel", FontAwesomeIcon.TIMES, "card-action-btn-danger");
            cancelBtn.setOnAction(e -> cancelSession(session));
            actions.getChildren().addAll(editBtn, cancelBtn);
        }

        if (session.getSessionStatus().equalsIgnoreCase("Completed")) {
            if (session.getFeedbackComment().isEmpty()) {
                Button feedbackBtn = createIconButton("Leave Feedback", FontAwesomeIcon.STAR, "card-action-btn-primary");
                feedbackBtn.setOnAction(e -> leaveFeedback(session));
                actions.getChildren().add(feedbackBtn);
            } else {
                Button editFeedbackBtn   = createIconButton("Edit Feedback",   FontAwesomeIcon.PENCIL, "card-action-btn");
                Button deleteFeedbackBtn = createIconButton("Delete Feedback", FontAwesomeIcon.TRASH,  "card-action-btn-danger");
                editFeedbackBtn.setOnAction(e   -> editFeedback(session));
                deleteFeedbackBtn.setOnAction(e -> deleteFeedback(session));
                actions.getChildren().addAll(editFeedbackBtn, deleteFeedbackBtn);
            }
            Button downloadBtn = createIconButton("Summary PDF", FontAwesomeIcon.DOWNLOAD, "card-action-btn");
            downloadBtn.setOnAction(e -> handleDownloadPDF(session));
            actions.getChildren().add(downloadBtn);
        }

        Button deleteBtn = createIconButton("Delete Session", FontAwesomeIcon.TRASH, "card-action-btn-danger");
        deleteBtn.setOnAction(e -> deleteSession(session));
        actions.getChildren().add(deleteBtn);

        card.getChildren().addAll(header, dateBox, details);
        card.getChildren().add(currencyBox);
        if (!feedbackBox.getChildren().isEmpty()) card.getChildren().add(feedbackBox);
        card.getChildren().add(actions);
        return card;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BOOK A SESSION  ◄── conflict check added here
    // ─────────────────────────────────────────────────────────────────────────
    private void book() {
        // ── Step 1: validate the form (same as before) ────────────────────────
        LocalDate date    = datePicker.getValue();
        String timeStr    = timeCombo.getValue();
        Integer duration  = durationCombo.getValue();

        if (date == null)                         { showError("Validation Error", "Please select a DATE.");     return; }
        if (timeStr == null || timeStr.isBlank()) { showError("Validation Error", "Please select a TIME.");     return; }
        if (duration == null)                     { showError("Validation Error", "Please select a DURATION."); return; }

        LocalTime time;
        try { time = LocalTime.parse(timeStr); }
        catch (Exception e) { showError("Invalid Time", "Format must be HH:mm (e.g. 09:00)"); return; }

        LocalDateTime start = LocalDateTime.of(date, time);

        // ── NEW ── Step 2: conflict check BEFORE writing to DB ─────────────────
        try {
            ConflictResult conflict = conflictService.checkConflicts(
                    getUserId(),           // patient ID  — swap for real user when ready
                    selectedPsychologistId, // therapist ID
                    start,
                    duration,
                    null                    // null = new booking, not an edit
            );

            if (conflict.hasConflict()) {
                // Show the conflict message + suggested free slots
                showConflictAlert(conflict, date, timeStr);
                return;  // ← STOP. Do NOT save to DB.
            }

        } catch (SQLException e) {
            showError("Conflict Check Failed", "Could not verify availability: " + e.getMessage());
            return;
        }
        // ── END NEW ────────────────────────────────────────────────────────────

        // ── Step 3: no conflict — save as usual ──────────────────────────────
        String status = start.isBefore(LocalDateTime.now()) ? "Completed" : "Scheduled";
        TherapySession session = new TherapySession(
                selectedPsychologistId, getUserId(), start, duration,
                status, notesArea.getText() == null ? "" : notesArea.getText().trim()
        );
        try {
            sessionService.addTherapySession(session);
            showInfo("Booked", "Session booked as: " + status);
            // ── Send booking confirmation email ───────────────────────────────
            emailService.sendBookingConfirmation(session, getTherapistName(selectedPsychologistId));
            ntfyService.notifyBooked(session, getTherapistName(selectedPsychologistId));
            clearForm();
            reload();
        } catch (SQLException e) {
            showError("Booking Failed", e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UPDATE A SESSION  ◄── conflict check added here
    // ─────────────────────────────────────────────────────────────────────────
    private void updateSelected() {
        if (selectedSession == null) {
            showInfo("No session selected", "Click 'Edit' on a session card first.");
            return;
        }
        if (!"Scheduled".equalsIgnoreCase(selectedSession.getSessionStatus())) {
            showInfo("Not editable", "Only SCHEDULED sessions can be edited.");
            return;
        }

        // ── Step 1: validate the form ─────────────────────────────────────────
        LocalDate date    = datePicker.getValue();
        String timeStr    = timeCombo.getValue();
        Integer duration  = durationCombo.getValue();

        if (date == null)                         { showError("Validation Error", "Please select a DATE.");     return; }
        if (timeStr == null || timeStr.isBlank()) { showError("Validation Error", "Please select a TIME.");     return; }
        if (duration == null)                     { showError("Validation Error", "Please select a DURATION."); return; }

        LocalDateTime start = LocalDateTime.of(date, LocalTime.parse(timeStr));

        // ── NEW ── Step 2: conflict check — pass the session's own ID so it
        //          doesn't block itself when you're just moving it to a new time ──
        try {
            ConflictResult conflict = conflictService.checkConflicts(
                    getUserId(),                       // patient ID — swap for real user when ready
                    selectedSession.getPsychologistId(), // therapist ID stays the same
                    start,
                    duration,
                    selectedSession.getSessionId()       // ← exclude self so it won't conflict with its old slot
            );

            if (conflict.hasConflict()) {
                showConflictAlert(conflict, date, timeStr);
                return;  // ← STOP. Do NOT save to DB.
            }

        } catch (SQLException e) {
            showError("Conflict Check Failed", "Could not verify availability: " + e.getMessage());
            return;
        }
        // ── END NEW ────────────────────────────────────────────────────────────

        // ── Step 3: no conflict — update as usual ────────────────────────────
        try {
            selectedSession.setSessionDate(start);
            selectedSession.setDurationMinutes(duration);
            selectedSession.setSessionNotes(notesArea.getText());
            sessionService.updateSession(selectedSession);
            showInfo("Updated", "Session updated successfully.");
            // ── Send update notification email ────────────────────────────────
            emailService.sendUpdateNotice(selectedSession, getTherapistName(selectedSession.getPsychologistId()));
            ntfyService.notifyUpdated(selectedSession, getTherapistName(selectedSession.getPsychologistId()));
            clearForm();
            selectedSession = null;
            reload();
        } catch (Exception e) {
            showError("Update Failed", e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CONFLICT ALERT  ◄── shows what the conflict is + suggested slots
    //
    // The suggested slots are shown as clickable buttons.
    // Clicking one pre-fills the date/time pickers so the user
    // can confirm with one more click.
    // ─────────────────────────────────────────────────────────────────────────
    private void showConflictAlert(ConflictResult conflict, LocalDate pickedDate, String pickedTime) {
        // Build the message text
        StringBuilder msg = new StringBuilder(conflict.buildMessage());

        // If there are suggestions, show them as a dialog with buttons
        if (!conflict.suggestions.isEmpty()) {
            // Create a custom dialog
            Dialog<LocalDateTime> dialog = new Dialog<>();
            dialog.setTitle("Scheduling Conflict");
            dialog.setHeaderText("⚠  This time slot is not available");
            dialog.setContentText(null);

            VBox content = new VBox(12);
            content.setPadding(new Insets(10, 0, 0, 0));

            // Conflict reason text
            Label reasonLabel = new Label(conflict.buildMessage());
            reasonLabel.setWrapText(true);
            reasonLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #374151;");
            content.getChildren().add(reasonLabel);

            // Suggested slots heading
            Label suggestionLabel = new Label("Click a slot to pre-fill the form:");
            suggestionLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #047857;");
            content.getChildren().add(suggestionLabel);

            // One button per suggested slot
            FlowPane slotButtons = new FlowPane(8, 8);
            DateTimeFormatter btnFmt = DateTimeFormatter.ofPattern("EEE MMM d  HH:mm");

            for (LocalDateTime slot : conflict.suggestions) {
                Button slotBtn = new Button(slot.format(btnFmt));
                slotBtn.setStyle(
                        "-fx-background-color: #f0fdf4;" +
                                "-fx-border-color: #6ee7b7;" +
                                "-fx-border-radius: 6px; -fx-background-radius: 6px;" +
                                "-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #047857;" +
                                "-fx-cursor: hand; -fx-padding: 6 12 6 12;"
                );
                slotBtn.setOnAction(e -> {
                    // Pre-fill the date picker and time combo with this slot
                    datePicker.setValue(slot.toLocalDate());
                    String slotTime = String.format("%02d:%02d",
                            slot.getHour(), slot.getMinute());
                    timeCombo.setValue(slotTime);
                    dialog.close();
                });
                slotButtons.getChildren().add(slotBtn);
            }
            content.getChildren().add(slotButtons);

            dialog.getDialogPane().setContent(content);
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            dialog.showAndWait();

        } else {
            // No suggestions — just show a plain error
            showError("Scheduling Conflict", conflict.buildMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // REMAINING CRUD  (unchanged from your original)
    // ─────────────────────────────────────────────────────────────────────────
    private void cancelSession(TherapySession session) {
        try {
            sessionService.updateStatus(session.getSessionId(), "Cancelled");
            reload();
            showInfo("Cancelled", "Session cancelled.");
            // ── Send cancellation notification email ──────────────────────────
            emailService.sendCancellationNotice(session, getTherapistName(session.getPsychologistId()));
            ntfyService.notifyCancelled(session, getTherapistName(session.getPsychologistId()));
        } catch (SQLException e) {
            showError("Cancel Failed", e.getMessage());
        }
    }

    private void deleteSession(TherapySession session) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete session #" + session.getSessionId() + "?\n\n⚠ This will also delete any feedback.",
                ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try {
                    SessionFeedback feedback = feedbackService.getFeedbackObjectBySessionId(session.getSessionId());
                    if (feedback != null) feedbackService.deleteSessionFeedback(feedback.getFeedbackId());
                    sessionService.deleteTherapySession(session.getSessionId());
                    reload();
                    showInfo("Deleted", "Session deleted.");
                } catch (SQLException e) {
                    showError("Delete Failed", e.getMessage());
                }
            }
        });
    }

    private void leaveFeedback(TherapySession session) {
        try {
            if (feedbackService.hasUserFeedbackForSession(getUserId(), session.getSessionId())) {
                showInfo("Already submitted", "You already left feedback for this session.");
                return;
            }
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Feedback.fxml"));
            Parent root = loader.load();
            FeedbackController controller = loader.getController();
            controller.setSessionId(session.getSessionId());
            Stage stage = new Stage();
            stage.setTitle("Leave Feedback");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
            reload();
        } catch (Exception e) {
            showError("Error", e.getMessage());
        }
    }

    private void editFeedback(TherapySession session) {
        try {
            SessionFeedback existing = feedbackService.getFeedbackObjectBySessionId(session.getSessionId());
            if (existing == null) { showInfo("Not found", "No feedback found."); return; }
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Feedback.fxml"));
            Parent root = loader.load();
            FeedbackController controller = loader.getController();
            controller.setExistingFeedback(existing);
            Stage stage = new Stage();
            stage.setTitle("Edit Feedback");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
            reload();
        } catch (Exception e) {
            showError("Error", e.getMessage());
        }
    }

    private void deleteFeedback(TherapySession session) {
        try {
            SessionFeedback feedback = feedbackService.getFeedbackObjectBySessionId(session.getSessionId());
            if (feedback == null) { showInfo("Not found", "No feedback found."); return; }
            new Alert(Alert.AlertType.CONFIRMATION, "Delete your feedback?", ButtonType.YES, ButtonType.NO)
                    .showAndWait().ifPresent(r -> {
                        if (r == ButtonType.YES) {
                            try {
                                feedbackService.deleteSessionFeedback(feedback.getFeedbackId());
                                reload();
                                showInfo("Deleted", "Feedback deleted.");
                            } catch (SQLException e) {
                                showError("Delete Failed", e.getMessage());
                            }
                        }
                    });
        } catch (SQLException e) {
            showError("Error", e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS  (unchanged)
    // ─────────────────────────────────────────────────────────────────────────
    private void selectSessionForEdit(TherapySession session) {
        selectedSession = session;
        datePicker.setValue(session.getSessionDate().toLocalDate());
        timeCombo.setValue(String.format("%02d:%02d",
                session.getSessionDate().getHour(),
                session.getSessionDate().getMinute()));
        durationCombo.setValue(session.getDurationMinutes());
        notesArea.setText(session.getSessionNotes());
        selectedPsychologistId = session.getPsychologistId();
        therapistList.getSelectionModel().select(selectedPsychologistId - 1);
        showInfo("Session loaded", "Edit the fields above then click 'Update'.");
    }

    private String getTherapistName(int id) {
        return switch (id) {
            case 1 -> "Mark Thompson";
            case 2 -> "Lina Ben Ali";
            case 3 -> "Sami Gharbi";
            default -> "Unknown";
        };
    }

    private Button createIconButton(String text, FontAwesomeIcon icon, String styleClass) {
        Button btn = new Button(text);
        btn.getStyleClass().add(styleClass);
        FontAwesomeIconView iconView = new FontAwesomeIconView(icon);
        iconView.setSize("11");
        btn.setGraphic(iconView);
        return btn;
    }

    private void clearForm() {
        datePicker.setValue(null);
        timeCombo.setValue(null);
        durationCombo.setValue(null);
        notesArea.clear();
    }

    private void showInfo(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private void showError(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setHeaderText(title);
        a.setContentText(msg);
        a.showAndWait();
    }
}