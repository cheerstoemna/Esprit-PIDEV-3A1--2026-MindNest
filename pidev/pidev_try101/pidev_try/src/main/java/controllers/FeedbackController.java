package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import models.SessionFeedback;
import services.SessionFeedbackService;

import java.sql.SQLException;

public class FeedbackController {

    @FXML private Slider ratingSlider;
    @FXML private TextArea commentArea;

    private final SessionFeedbackService feedbackService = new SessionFeedbackService();

    private int sessionId;
    private SessionFeedback existingFeedback;  // For editing
    private boolean isEditMode = false;

    // ✅ For creating new feedback
    public void setSessionId(int sessionId) {
        this.sessionId = sessionId;
        this.isEditMode = false;
    }

    // ✅ For editing existing feedback
    public void setExistingFeedback(SessionFeedback feedback) {
        this.existingFeedback = feedback;
        this.sessionId = feedback.getSessionId();
        this.isEditMode = true;

        // Pre-fill the form
        ratingSlider.setValue(feedback.getRating());
        commentArea.setText(feedback.getComment());
    }

    @FXML
    private void handleSubmit() {

        int rating = (int) ratingSlider.getValue();
        String comment = commentArea.getText().trim();

        if (comment.isEmpty()) {
            showError("Validation", "Please enter a comment.");
            return;
        }

        try {
            if (isEditMode) {
                // ✅ UPDATE existing feedback
                existingFeedback.setRating(rating);
                existingFeedback.setComment(comment);
                feedbackService.updateSessionFeedback(existingFeedback);
                showInfo("Feedback updated successfully!");
            } else {
                // ✅ CREATE new feedback
                SessionFeedback feedback = new SessionFeedback(sessionId, rating, comment);
                feedbackService.addSessionFeedback(feedback);
                showInfo("Feedback submitted successfully!");
            }

            // Close window
            closeWindow();

        } catch (SQLException e) {
            showError("Database Error", e.getMessage());
        }
    }

    private void closeWindow() {
        Stage stage = (Stage) commentArea.getScene().getWindow();
        stage.close();
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