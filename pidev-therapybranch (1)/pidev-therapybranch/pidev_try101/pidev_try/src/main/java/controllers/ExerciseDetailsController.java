package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import models.Exercise;
import services.ExerciseProgressService;
import services.ExerciseService;
import services.PlanEnrollmentService;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ExerciseDetailsController {

    @FXML private ImageView exerciseImageView;

    @FXML private Label titleLabel;
    @FXML private Label durationLabel;
    @FXML private Label difficultyLabel;
    @FXML private Label descriptionLabel;
    @FXML private Label videoLabel;

    @FXML private ComboBox<String> statusCombo;

    @FXML private Button editBtn;
    @FXML private Button deleteBtn;

    private AnchorPane contentArea;
    private List<Node> previousContent;

    // refresh callback (PlanDetails refreshExercises)
    private Runnable onPlanRefresh;
    public void setOnPlanRefresh(Runnable onPlanRefresh) { this.onPlanRefresh = onPlanRefresh; }

    private final ExerciseService exerciseService = new ExerciseService();
    private final ExerciseProgressService progressService = new ExerciseProgressService();
    private final PlanEnrollmentService enrollmentService = new PlanEnrollmentService();

    private Exercise exercise;
    private int userId;
    private boolean isOwner;
    private int planId;

    public void setContext(AnchorPane contentArea) { this.contentArea = contentArea; }
    public void setPreviousContent(List<Node> previousContent) { this.previousContent = previousContent; }

    public void setUserId(int userId) { this.userId = userId; }
    public void setOwner(boolean owner) { this.isOwner = owner; updateOwnerUI(); }
    public void setPlanId(int planId) { this.planId = planId; }

    public void setExercise(Exercise exercise) {
        this.exercise = exercise;
        render();
    }

    private void updateOwnerUI() {
        if (editBtn != null) { editBtn.setVisible(isOwner); editBtn.setManaged(isOwner); }
        if (deleteBtn != null) { deleteBtn.setVisible(isOwner); deleteBtn.setManaged(isOwner); }
    }

    private void render() {
        if (exercise == null) return;

        updateOwnerUI();

        titleLabel.setText(n(exercise.getTitle()));
        descriptionLabel.setText(n(exercise.getDescription()));
        durationLabel.setText("Duration: " + exercise.getDuration() + " min");
        difficultyLabel.setText("Difficulty: " + n(exercise.getDifficultyLevel()));

        videoLabel.setText("No video yet.");

        statusCombo.getItems().setAll("Not Started", "In Progress", "Done", "Skipped");

        String current = progressService.getStatus(exercise.getExerciseId(), userId);
        statusCombo.setValue(current);

        int effectivePlanId = (exercise.getPlanId() > 0) ? exercise.getPlanId() : planId;
        boolean enrolled = isOwner || enrollmentService.isEnrolled(effectivePlanId, userId);
        statusCombo.setDisable(!enrolled);

        statusCombo.setOnAction(e -> {
            String val = statusCombo.getValue();
            if (val == null) return;

            int pid = (exercise.getPlanId() > 0) ? exercise.getPlanId() : planId;

            if (!isOwner && !enrollmentService.isEnrolled(pid, userId)) {
                enrollmentService.enroll(pid, userId);
                statusCombo.setDisable(false);
            }

            progressService.upsertStatus(exercise.getExerciseId(), userId, val);
        });

        Image img = loadExerciseImage(exercise.getImage());
        if (img != null) exerciseImageView.setImage(img);
    }

    private Image loadExerciseImage(String imagePath) {
        if (imagePath != null && !imagePath.isBlank()) {
            try {
                File f = new File(imagePath);
                if (f.exists() && f.isFile()) {
                    return new Image(f.toURI().toString(), true);
                }
            } catch (Exception ignored) {}
        }

        try (InputStream is = getClass().getResourceAsStream("/images/plan.png")) {
            if (is != null) return new Image(is);
        } catch (Exception ignored) {}

        return null;
    }

    @FXML
    private void onDelete() {
        if (!isOwner || exercise == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete this exercise?", ButtonType.CANCEL, ButtonType.OK);
        confirm.setHeaderText(null);
        ButtonType result = confirm.showAndWait().orElse(ButtonType.CANCEL);
        if (result != ButtonType.OK) return;

        exerciseService.deleteExercise(exercise.getExerciseId());

        // refresh PlanDetails (if still reachable through contentArea properties)
        triggerPlanRefresh();

        goBack();
    }

    @FXML
    private void onEdit() {
        if (!isOwner || exercise == null || contentArea == null) return;

        try {
            List<Node> exerciseDetailsSnapshot = new ArrayList<>(contentArea.getChildren());

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ExerciseForm.fxml"));
            Parent view = loader.load();

            ExerciseFormController controller = loader.getController();
            controller.setContext(contentArea);

            controller.setPreviousContent(exerciseDetailsSnapshot);
            controller.setPlanPreviousContent(previousContent);

            controller.setUserId(userId);
            controller.setOwner(isOwner);

            int effectivePlanId = (exercise.getPlanId() > 0) ? exercise.getPlanId() : planId;
            controller.setPlanId(effectivePlanId);

            controller.setModeEdit(exercise);

            controller.setOnSaved(() -> {
                render();
                triggerPlanRefresh();
            });

            contentArea.getChildren().setAll(view);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void goBack() {
        if (contentArea != null && previousContent != null) {
            contentArea.getChildren().setAll(previousContent);
            triggerPlanRefresh();
        }
    }

    // Prefer explicit callback; fallback to AnchorPane properties (set in PlanDetailsController.setContext)
    private void triggerPlanRefresh() {
        if (onPlanRefresh != null) {
            onPlanRefresh.run();
            return;
        }
        if (contentArea != null) {
            Object o = contentArea.getProperties().get("planDetailsController");
            if (o instanceof PlanDetailsController pdc) {
                pdc.refreshExercises();
            }
        }
    }

    private String n(String s) { return s == null ? "" : s; }
}
