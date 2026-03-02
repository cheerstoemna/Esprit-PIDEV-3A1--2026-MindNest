package controllers;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import models.SessionFeedback;
import models.TherapySession;
import services.SessionFeedbackService;
import services.TherapySessionService;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ClientTherapyController {

    @FXML private ListView<String> therapistList;
    @FXML private DatePicker datePicker;
    @FXML private ComboBox<String> timeCombo;
    @FXML private ComboBox<Integer> durationCombo;
    @FXML private TextArea notesArea;

    @FXML private VBox sessionsContainer;  // Container for session cards
    @FXML private Label lblSessionCount;

    @FXML private Button btnBook;
    @FXML private Button btnRefresh;
    @FXML private Button btnBook1;

    // Filter buttons
    @FXML private Button btnFilterAll;
    @FXML private Button btnFilterScheduled;
    @FXML private Button btnFilterCompleted;
    @FXML private Button btnFilterCancelled;

    private final TherapySessionService sessionService = new TherapySessionService();
    private final SessionFeedbackService feedbackService = new SessionFeedbackService();
    private final ObservableList<TherapySession> mySessions = FXCollections.observableArrayList();

    private int selectedPsychologistId = 1;
    private TherapySession selectedSession = null;  // Currently selected session
    private String currentFilter = "All";  // Current filter

    // TEMPORARY: Hardcoded user ID for testing
    private final int TEMP_USER_ID = 1;

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

        timeCombo.getItems().addAll("09:00", "10:30", "14:00", "15:30", "17:00");
        timeCombo.setPromptText("Select time");
        timeCombo.setEditable(true);

        durationCombo.getItems().addAll(30, 45, 60, 90);
        durationCombo.setPromptText("Select duration");

        // ✅ ADD ICONS TO BUTTONS
        addIcon(btnBook, FontAwesomeIcon.CHECK_CIRCLE, "#ffffff");
        addIcon(btnBook1, FontAwesomeIcon.PENCIL, "#047857");
        addIcon(btnRefresh, FontAwesomeIcon.REFRESH, "#5a7571");

        // Button handlers
        btnBook.setOnAction(e -> book());
        btnRefresh.setOnAction(e -> reload());
        btnBook1.setOnAction(e -> updateSelected());

        // Filter buttons
        btnFilterAll.setOnAction(e -> filterSessions("All"));
        btnFilterScheduled.setOnAction(e -> filterSessions("Scheduled"));
        btnFilterCompleted.setOnAction(e -> filterSessions("Completed"));
        btnFilterCancelled.setOnAction(e -> filterSessions("Cancelled"));

        reload();
    }

    // ✅ HELPER METHOD: Add icon to button
    private void addIcon(Button button, FontAwesomeIcon icon, String color) {
        FontAwesomeIconView iconView = new FontAwesomeIconView(icon);
        iconView.setSize("14");
        iconView.setFill(Color.web(color));
        button.setGraphic(iconView);
    }

    private void reload() {
        try {
            // Load sessions
            ObservableList<TherapySession> sessionsList = FXCollections.observableArrayList(
                    sessionService.getSessionsByUser(TEMP_USER_ID)
            );

            // Attach feedback to each session
            for (TherapySession session : sessionsList) {
                String feedback = feedbackService.getFeedbackBySessionId(session.getSessionId());
                session.setFeedbackComment(feedback != null ? feedback : "");
            }

            mySessions.setAll(sessionsList);

            // Update count
            lblSessionCount.setText(mySessions.size() + " sessions");

            // Render cards
            renderSessionCards();

        } catch (SQLException e) {
            showError("Failed to load sessions", e.getMessage());
        }
    }

    private void filterSessions(String filter) {
        currentFilter = filter;

        // Update button styles
        btnFilterAll.getStyleClass().removeAll("filter-btn-active", "filter-btn");
        btnFilterScheduled.getStyleClass().removeAll("filter-btn-active", "filter-btn");
        btnFilterCompleted.getStyleClass().removeAll("filter-btn-active", "filter-btn");
        btnFilterCancelled.getStyleClass().removeAll("filter-btn-active", "filter-btn");

        btnFilterAll.getStyleClass().add(filter.equals("All") ? "filter-btn-active" : "filter-btn");
        btnFilterScheduled.getStyleClass().add(filter.equals("Scheduled") ? "filter-btn-active" : "filter-btn");
        btnFilterCompleted.getStyleClass().add(filter.equals("Completed") ? "filter-btn-active" : "filter-btn");
        btnFilterCancelled.getStyleClass().add(filter.equals("Cancelled") ? "filter-btn-active" : "filter-btn");

        renderSessionCards();
    }

    private void renderSessionCards() {
        sessionsContainer.getChildren().clear();

        // Filter sessions based on current filter
        List<TherapySession> filteredSessions = mySessions.stream()
                .filter(s -> currentFilter.equals("All") || s.getSessionStatus().equalsIgnoreCase(currentFilter))
                .toList();

        if (filteredSessions.isEmpty()) {
            Label emptyLabel = new Label("No sessions found");
            emptyLabel.setStyle("-fx-text-fill: #a3bdb8; -fx-font-size: 14px;");
            VBox.setMargin(emptyLabel, new Insets(40, 0, 0, 0));
            sessionsContainer.getChildren().add(emptyLabel);
            return;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE, MMM dd, yyyy 'at' HH:mm");

        for (TherapySession session : filteredSessions) {
            VBox card = createSessionCard(session, formatter);
            sessionsContainer.getChildren().add(card);
        }
    }

    private VBox createSessionCard(TherapySession session, DateTimeFormatter formatter) {
        VBox card = new VBox(12);
        card.getStyleClass().add("session-card");
        card.setPadding(new Insets(18));

        // Header with ID and Status
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        Label idLabel = new Label("#" + session.getSessionId());
        idLabel.setStyle("-fx-font-weight: 700; -fx-font-size: 16px; -fx-text-fill: #2d5550;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label statusBadge = new Label(session.getSessionStatus());
        statusBadge.getStyleClass().add("status-badge");
        if (session.getSessionStatus().equalsIgnoreCase("Completed")) {
            statusBadge.getStyleClass().add("status-completed");
        } else if (session.getSessionStatus().equalsIgnoreCase("Scheduled")) {
            statusBadge.getStyleClass().add("status-scheduled");
        } else {
            statusBadge.getStyleClass().add("status-cancelled");
        }

        header.getChildren().addAll(idLabel, spacer, statusBadge);

        // Date and Time with icon
        HBox dateBox = new HBox(8);
        dateBox.setAlignment(Pos.CENTER_LEFT);
        FontAwesomeIconView calIcon = new FontAwesomeIconView(FontAwesomeIcon.CALENDAR);
        calIcon.setSize("13");
        calIcon.setFill(Color.web("#5a7571"));
        Label dateLabel = new Label(session.getSessionDate().format(formatter));
        dateLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #5a7571;");
        dateBox.getChildren().addAll(calIcon, dateLabel);

        // Therapist and Duration
        HBox details = new HBox(20);

        HBox therapistBox = new HBox(6);
        therapistBox.setAlignment(Pos.CENTER_LEFT);
        FontAwesomeIconView docIcon = new FontAwesomeIconView(FontAwesomeIcon.USER_MD);
        docIcon.setSize("13");
        docIcon.setFill(Color.web("#5a7571"));
        Label therapistLabel = new Label("Dr. " + getTherapistName(session.getPsychologistId()));
        therapistLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #5a7571;");
        therapistBox.getChildren().addAll(docIcon, therapistLabel);

        HBox durationBox = new HBox(6);
        durationBox.setAlignment(Pos.CENTER_LEFT);
        FontAwesomeIconView clockIcon = new FontAwesomeIconView(FontAwesomeIcon.CLOCK_ALT);
        clockIcon.setSize("13");
        clockIcon.setFill(Color.web("#5a7571"));
        Label durationLabel = new Label(session.getDurationMinutes() + " min");
        durationLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #5a7571;");
        durationBox.getChildren().addAll(clockIcon, durationLabel);

        details.getChildren().addAll(therapistBox, durationBox);

        // Feedback (if exists)
        VBox feedbackBox = new VBox(6);
        if (session.getFeedbackComment() != null && !session.getFeedbackComment().isEmpty()) {
            HBox feedbackHeader = new HBox(6);
            feedbackHeader.setAlignment(Pos.CENTER_LEFT);
            FontAwesomeIconView chatIcon = new FontAwesomeIconView(FontAwesomeIcon.COMMENT);
            chatIcon.setSize("12");
            chatIcon.setFill(Color.web("#7a9794"));
            Label feedbackLabel = new Label("Feedback:");
            feedbackLabel.setStyle("-fx-font-weight: 600; -fx-font-size: 12px; -fx-text-fill: #7a9794;");
            feedbackHeader.getChildren().addAll(chatIcon, feedbackLabel);

            Label commentLabel = new Label(session.getFeedbackComment());
            commentLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #5a7571; -fx-wrap-text: true;");
            commentLabel.setWrapText(true);

            feedbackBox.getChildren().addAll(feedbackHeader, commentLabel);
        }

        // Action Buttons
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
                Button editFeedbackBtn = createIconButton("Edit Feedback", FontAwesomeIcon.PENCIL, "card-action-btn");
                editFeedbackBtn.setOnAction(e -> editFeedback(session));

                Button deleteFeedbackBtn = createIconButton("Delete Feedback", FontAwesomeIcon.TRASH, "card-action-btn-danger");
                deleteFeedbackBtn.setOnAction(e -> deleteFeedback(session));

                actions.getChildren().addAll(editFeedbackBtn, deleteFeedbackBtn);
            }
        }

        Button deleteBtn = createIconButton("Delete Session", FontAwesomeIcon.TRASH, "card-action-btn-danger");
        deleteBtn.setOnAction(e -> deleteSession(session));
        actions.getChildren().add(deleteBtn);

        card.getChildren().addAll(header, dateBox, details);
        if (!feedbackBox.getChildren().isEmpty()) {
            card.getChildren().add(feedbackBox);
        }
        card.getChildren().add(actions);

        return card;
    }

    private String getTherapistName(int id) {
        return switch (id) {
            case 1 -> "Mark Thompson";
            case 2 -> "Lina Ben Ali";
            case 3 -> "Sami Gharbi";
            default -> "Unknown";
        };
    }

    // ✅ HELPER METHOD: Create button with icon
    private Button createIconButton(String text, FontAwesomeIcon icon, String styleClass) {
        Button btn = new Button(text);
        btn.getStyleClass().add(styleClass);

        FontAwesomeIconView iconView = new FontAwesomeIconView(icon);
        iconView.setSize("11");
        btn.setGraphic(iconView);

        return btn;
    }

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

        showInfo("Session loaded for editing. Click 'Update' to save changes.");
    }

    private void book() {
        LocalDate date = datePicker.getValue();
        String timeStr = timeCombo.getValue();
        Integer duration = durationCombo.getValue();

        if (date == null) {
            showError("Validation Error", "Please select a DATE.");
            return;
        }

        if (timeStr == null || timeStr.trim().isEmpty()) {
            showError("Validation Error", "Please select a TIME.");
            return;
        }

        if (duration == null) {
            showError("Validation Error", "Please select a DURATION.");
            return;
        }

        LocalTime time;
        try {
            time = LocalTime.parse(timeStr);
        } catch (Exception e) {
            showError("Invalid Time", "Time must be in format HH:mm (e.g., 09:00)");
            return;
        }

        LocalDateTime dt = LocalDateTime.of(date, time);

        String status;
        if (dt.isBefore(LocalDateTime.now())) {
            status = "Completed";
        } else {
            status = "Scheduled";
        }

        TherapySession session = new TherapySession(
                selectedPsychologistId,
                TEMP_USER_ID,
                dt,
                duration,
                status,
                notesArea.getText() == null ? "" : notesArea.getText().trim()
        );

        try {
            sessionService.addTherapySession(session);
            showInfo("Session booked as: " + status);
            clearForm();
            reload();
        } catch (SQLException e) {
            showError("Booking failed", e.getMessage());
        }
    }

    private void updateSelected() {
        if (selectedSession == null) {
            showInfo("Please select a session card first by clicking 'Edit'.");
            return;
        }

        if (!"Scheduled".equalsIgnoreCase(selectedSession.getSessionStatus())) {
            showInfo("Only SCHEDULED sessions can be edited.");
            return;
        }

        LocalDate date = datePicker.getValue();
        String timeStr = timeCombo.getValue();
        Integer duration = durationCombo.getValue();

        if (date == null) {
            showError("Validation Error", "Please select a DATE.");
            return;
        }

        if (timeStr == null || timeStr.trim().isEmpty()) {
            showError("Validation Error", "Please select a TIME.");
            return;
        }

        if (duration == null) {
            showError("Validation Error", "Please select a DURATION.");
            return;
        }

        LocalDateTime newDateTime;
        try {
            newDateTime = LocalDateTime.of(date, LocalTime.parse(timeStr));
        } catch (Exception e) {
            showError("Invalid Time", "Time must be in format HH:mm (e.g., 09:00)");
            return;
        }

        try {
            selectedSession.setSessionDate(newDateTime);
            selectedSession.setDurationMinutes(duration);
            selectedSession.setSessionNotes(notesArea.getText());

            sessionService.updateSession(selectedSession);

            showInfo("Session updated.");
            clearForm();
            selectedSession = null;
            reload();

        } catch (SQLException e) {
            showError("Update failed", e.getMessage());
        }
    }

    private void cancelSession(TherapySession session) {
        try {
            sessionService.updateStatus(session.getSessionId(), "Cancelled");
            reload();
            showInfo("Session cancelled.");
        } catch (SQLException e) {
            showError("Cancel failed", e.getMessage());
        }
    }

    private void deleteSession(TherapySession session) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete session #" + session.getSessionId() + "?\n\n⚠️ This will also delete any feedback.",
                ButtonType.YES, ButtonType.NO);

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try {
                    SessionFeedback feedback = feedbackService.getFeedbackObjectBySessionId(session.getSessionId());
                    if (feedback != null) {
                        feedbackService.deleteSessionFeedback(feedback.getFeedbackId());
                    }

                    sessionService.deleteTherapySession(session.getSessionId());

                    reload();
                    showInfo("Session deleted.");
                } catch (SQLException e) {
                    showError("Delete failed", e.getMessage());
                }
            }
        });
    }

    private void leaveFeedback(TherapySession session) {
        try {
            if (feedbackService.hasUserFeedbackForSession(TEMP_USER_ID, session.getSessionId())) {
                showInfo("You already left feedback for this session.");
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
            SessionFeedback existingFeedback = feedbackService.getFeedbackObjectBySessionId(session.getSessionId());

            if (existingFeedback == null) {
                showInfo("No feedback found.");
                return;
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Feedback.fxml"));
            Parent root = loader.load();

            FeedbackController controller = loader.getController();
            controller.setExistingFeedback(existingFeedback);

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

            if (feedback == null) {
                showInfo("No feedback found.");
                return;
            }

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "Delete your feedback?",
                    ButtonType.YES, ButtonType.NO);

            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.YES) {
                    try {
                        feedbackService.deleteSessionFeedback(feedback.getFeedbackId());
                        reload();
                        showInfo("Feedback deleted.");
                    } catch (SQLException e) {
                        showError("Delete failed", e.getMessage());
                    }
                }
            });

        } catch (SQLException e) {
            showError("Error", e.getMessage());
        }
    }

    private void clearForm() {
        datePicker.setValue(null);
        timeCombo.setValue(null);
        durationCombo.setValue(null);
        notesArea.clear();
    }

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