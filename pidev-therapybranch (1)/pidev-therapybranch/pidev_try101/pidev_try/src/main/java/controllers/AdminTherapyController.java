package controllers;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import models.SessionFeedback;
import models.TherapySession;
import services.SessionFeedbackService;
import services.TherapySessionService;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

public class AdminTherapyController {

    // ================= SESSION TABLE =================
    @FXML private TableView<TherapySession> sessionTable;
    @FXML private TableColumn<TherapySession, Integer> colId;
    @FXML private TableColumn<TherapySession, Integer> colPsy;
    @FXML private TableColumn<TherapySession, Integer> colUser;
    @FXML private TableColumn<TherapySession, String> colDate;
    @FXML private TableColumn<TherapySession, Integer> colDuration;
    @FXML private TableColumn<TherapySession, String> colStatus;

    // ================= STATS LABELS =================
    @FXML private Label lblTotalSessions;
    @FXML private Label lblCompleted;
    @FXML private Label lblScheduled;
    @FXML private Label lblAvgRating;

    // ================= FEEDBACK TABLE =================
    @FXML private TableView<SessionFeedback> feedbackTable;
    @FXML private TableColumn<SessionFeedback, Integer> fId;
    @FXML private TableColumn<SessionFeedback, Integer> fSessionId;
    @FXML private TableColumn<SessionFeedback, Integer> fRating;
    @FXML private TableColumn<SessionFeedback, String> fComment;

    // ================= EDIT FIELDS =================
    @FXML private DatePicker rescheduleDate;
    @FXML private TextField rescheduleTime;
    @FXML private ComboBox<Integer> durationCombo;
    @FXML private ComboBox<String> statusCombo;

    @FXML private Button btnApply;
    @FXML private Button btnDelete;
    @FXML private Button btnRefresh;
    @FXML private Button btnDeleteFeedback;

    private final TherapySessionService sessionService = new TherapySessionService();
    private final SessionFeedbackService feedbackService = new SessionFeedbackService();

    private final ObservableList<TherapySession> sessions = FXCollections.observableArrayList();
    private final ObservableList<SessionFeedback> feedbacks = FXCollections.observableArrayList();

    // =======================================================
    @FXML
    public void initialize() {

        // ===== SESSION TABLE =====
        colId.setCellValueFactory(cd -> new SimpleIntegerProperty(cd.getValue().getSessionId()).asObject());
        colPsy.setCellValueFactory(cd -> new SimpleIntegerProperty(cd.getValue().getPsychologistId()).asObject());
        colUser.setCellValueFactory(cd -> new SimpleIntegerProperty(cd.getValue().getUserId()).asObject());
        colDate.setCellValueFactory(cd -> new SimpleStringProperty(String.valueOf(cd.getValue().getSessionDate())));
        colDuration.setCellValueFactory(cd -> new SimpleIntegerProperty(cd.getValue().getDurationMinutes()).asObject());
        colStatus.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getSessionStatus()));

        sessionTable.setItems(sessions);

        // ✅ ADD ICONS TO BUTTONS
        addIcon(btnRefresh, FontAwesomeIcon.REFRESH, "#5a7571");
        addIcon(btnDelete, FontAwesomeIcon.TRASH, "#d14343");
        addIcon(btnApply, FontAwesomeIcon.CHECK_CIRCLE, "#ffffff");
        addIcon(btnDeleteFeedback, FontAwesomeIcon.TRASH, "#d14343");

        // Session selection listener
        sessionTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                rescheduleDate.setValue(newSel.getSessionDate().toLocalDate());
                rescheduleTime.setText(String.format("%02d:%02d",
                        newSel.getSessionDate().getHour(),
                        newSel.getSessionDate().getMinute()));
                durationCombo.setValue(newSel.getDurationMinutes());
                statusCombo.setValue(newSel.getSessionStatus());
            }
        });

        durationCombo.getItems().addAll(30, 45, 60, 90);
        statusCombo.getItems().addAll("Scheduled", "Completed", "Cancelled");

        // ===== FEEDBACK TABLE =====
        fId.setCellValueFactory(cd -> new SimpleIntegerProperty(cd.getValue().getFeedbackId()).asObject());
        fSessionId.setCellValueFactory(cd -> new SimpleIntegerProperty(cd.getValue().getSessionId()).asObject());
        fRating.setCellValueFactory(cd -> new SimpleIntegerProperty(cd.getValue().getRating()).asObject());
        fComment.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getComment()));

        feedbackTable.setItems(feedbacks);

        // Buttons
        btnApply.setOnAction(e -> applyChanges());
        btnDelete.setOnAction(e -> deleteSelected());
        btnRefresh.setOnAction(e -> reload());
        btnDeleteFeedback.setOnAction(e -> deleteFeedbackSelected());

        reload();
    }

    // ✅ HELPER METHOD: Add icon to button
    private void addIcon(Button button, FontAwesomeIcon icon, String color) {
        FontAwesomeIconView iconView = new FontAwesomeIconView(icon);
        iconView.setSize("14");
        iconView.setFill(Color.web(color));
        button.setGraphic(iconView);
    }

    // =======================================================
    private void reload() {
        try {
            // Load ALL sessions for admin
            List<TherapySession> sessionsListRaw = sessionService.getAllTherapySessions();

            // wrap it as ObservableList
            ObservableList<TherapySession> sessionsList = FXCollections.observableArrayList(sessionsListRaw);

            // attach feedback to each session
            for (TherapySession session : sessionsList) {
                String feedback = feedbackService.getFeedbackBySessionId(session.getSessionId());
                session.setFeedbackComment(feedback != null ? feedback : "");
            }

            // update tables
            sessions.setAll(sessionsList);
            feedbacks.setAll(feedbackService.getAllSessionFeedbacks());

            // ✅ Update stats
            updateStats();

        } catch (SQLException e) {
            showError("Failed to load sessions", e.getMessage());
        }
    }

    // ✅ NEW: Update statistics
    private void updateStats() {
        int total = sessions.size();
        long completed = sessions.stream().filter(s -> "Completed".equalsIgnoreCase(s.getSessionStatus())).count();
        long scheduled = sessions.stream().filter(s -> "Scheduled".equalsIgnoreCase(s.getSessionStatus())).count();

        // Calculate average rating
        double avgRating = 0.0;
        if (!feedbacks.isEmpty()) {
            avgRating = feedbacks.stream()
                    .mapToInt(SessionFeedback::getRating)
                    .average()
                    .orElse(0.0);
        }

        lblTotalSessions.setText(String.valueOf(total));
        lblCompleted.setText(String.valueOf(completed));
        lblScheduled.setText(String.valueOf(scheduled));
        lblAvgRating.setText(String.format("%.1f ⭐", avgRating));
    }

    // =======================================================
    private void applyChanges() {

        TherapySession s = sessionTable.getSelectionModel().getSelectedItem();
        if (s == null) {
            showInfo("Select a session first.");
            return;
        }

        try {
            // ✅ STEP 1: Update date in object
            if (rescheduleDate.getValue() != null &&
                    rescheduleTime.getText() != null &&
                    !rescheduleTime.getText().isBlank()) {

                String[] parts = rescheduleTime.getText().split(":");
                int hh = Integer.parseInt(parts[0]);
                int mm = Integer.parseInt(parts[1]);

                LocalDateTime dt = rescheduleDate.getValue().atTime(hh, mm);

                // ✅ Update the object BEFORE saving
                s.setSessionDate(dt);
            }

            // ✅ STEP 2: Update duration if changed
            if (durationCombo.getValue() != null) {
                s.setDurationMinutes(durationCombo.getValue());
            }

            // ✅ STEP 3: Update status if changed
            if (statusCombo.getValue() != null) {
                s.setSessionStatus(statusCombo.getValue());
            }

            // ✅ STEP 4: Save ALL changes to database
            sessionService.updateSession(s);

            // ✅ STEP 5: Reload to show changes
            reload();
            showInfo("Session updated successfully.");

        } catch (Exception e) {
            showError("Update failed", e.getMessage());
        }
    }

    // =======================================================
    // ✅ FIXED: Delete feedback first, then session (avoid foreign key error)
    private void deleteSelected() {

        TherapySession s = sessionTable.getSelectionModel().getSelectedItem();
        if (s == null) {
            showInfo("Select a session first.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete session #" + s.getSessionId() + "?\n\n⚠️ This will also delete any feedback for this session.",
                ButtonType.OK, ButtonType.CANCEL);

        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                try {
                    // ✅ STEP 1: Delete feedback first (if exists)
                    SessionFeedback feedback = feedbackService.getFeedbackObjectBySessionId(s.getSessionId());
                    if (feedback != null) {
                        feedbackService.deleteSessionFeedback(feedback.getFeedbackId());
                    }

                    // ✅ STEP 2: Now delete session (no foreign key error)
                    sessionService.deleteTherapySession(s.getSessionId());

                    reload();
                    showInfo("Session deleted successfully.");
                } catch (SQLException e) {
                    showError("Delete failed", e.getMessage());
                }
            }
        });
    }

    // =======================================================
    // ✅ NEW: Delete inappropriate feedback
    private void deleteFeedbackSelected() {

        SessionFeedback selected = feedbackTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showError("Error", "No feedback selected.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete feedback #" + selected.getFeedbackId() + "?\n\nReason: Inappropriate content",
                ButtonType.OK, ButtonType.CANCEL);

        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                try {
                    feedbackService.deleteSessionFeedback(selected.getFeedbackId());
                    reload();
                    showInfo("Feedback deleted successfully.");
                } catch (SQLException e) {
                    showError("Delete failed", e.getMessage());
                }
            }
        });
    }

    // =======================================================
    private void showInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
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