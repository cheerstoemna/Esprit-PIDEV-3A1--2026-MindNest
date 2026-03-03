package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import models.Exercise;
import services.ExerciseService;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ExerciseFormController {

    @FXML private Label formTitleLabel; // optional (only if you add fx:id in FXML)
    @FXML private TextField titleField;
    @FXML private TextArea descriptionArea;
    @FXML private Spinner<Integer> durationSpinner;
    @FXML private ChoiceBox<String> difficultyChoice;

    // plan-style upload UI: add these to FXML
    @FXML private Label imageStatusLabel;
    @FXML private Button uploadImageBtn;

    private final ExerciseService exerciseService = new ExerciseService();

    private AnchorPane contentArea;

    // screen to return to on Cancel (and after Save in ADD mode)
    private List<Node> previousContent = new ArrayList<>();

    // behind ExerciseDetails (PlanDetails screen) so back works correctly after EDIT save
    private List<Node> planPreviousContent = new ArrayList<>();

    // callbacks
    private Runnable onSaved;

    private Exercise exercise;
    private int userId;
    private boolean isOwner;
    private int planId;

    // absolute path chosen from FileChooser (like plans)
    private String selectedImagePath;

    private enum Mode { ADD, EDIT }
    private Mode mode = Mode.ADD;

    public void setContext(AnchorPane contentArea) { this.contentArea = contentArea; }

    public void setPreviousContent(List<Node> previousContent) {
        this.previousContent = previousContent == null ? new ArrayList<>() : new ArrayList<>(previousContent);
    }

    public void setPlanPreviousContent(List<Node> planPreviousContent) {
        this.planPreviousContent = planPreviousContent == null ? new ArrayList<>() : new ArrayList<>(planPreviousContent);
    }

    public void setOnSaved(Runnable onSaved) { this.onSaved = onSaved; }

    public void setUserId(int userId) { this.userId = userId; }
    public void setOwner(boolean owner) { this.isOwner = owner; updateOwnerControls(); }
    public void setPlanId(int planId) { this.planId = planId; }

    // ===== MODES =====

    public void setModeAdd() {
        this.mode = Mode.ADD;
        this.exercise = new Exercise();
        this.exercise.setPlanId(planId);
        populateForMode();
    }

    public void setModeEdit(Exercise exercise) {
        this.mode = Mode.EDIT;
        this.exercise = exercise;
        populateForMode();
    }

    @FXML
    private void initialize() {
        // safe init (controller loads before mode is set)
        if (durationSpinner != null && durationSpinner.getValueFactory() == null) {
            durationSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 240, 10));
            durationSpinner.setEditable(true);
        }
        if (difficultyChoice != null && difficultyChoice.getItems().isEmpty()) {
            difficultyChoice.getItems().setAll("Easy", "Medium", "Hard");
            difficultyChoice.setValue("Easy");
        }
        updateImageLabel();
        updateOwnerControls();
    }

    private void populateForMode() {
        if (formTitleLabel != null) {
            formTitleLabel.setText(mode == Mode.ADD ? "Add Exercise" : "Edit Exercise");
        }

        int dur = (exercise == null || exercise.getDuration() <= 0) ? 10 : exercise.getDuration();
        durationSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 240, dur));
        durationSpinner.setEditable(true);

        difficultyChoice.getItems().setAll("Easy", "Medium", "Hard");
        difficultyChoice.setValue((exercise == null || exercise.getDifficultyLevel() == null || exercise.getDifficultyLevel().isBlank())
                ? "Easy"
                : exercise.getDifficultyLevel());

        titleField.setText(exercise == null || exercise.getTitle() == null ? "" : exercise.getTitle());
        descriptionArea.setText(exercise == null || exercise.getDescription() == null ? "" : exercise.getDescription());

        selectedImagePath = (exercise == null) ? null : exercise.getImage();
        updateImageLabel();

        updateOwnerControls();
    }

    private void updateOwnerControls() {
        if (uploadImageBtn != null) {
            uploadImageBtn.setDisable(!isOwner);
            uploadImageBtn.setVisible(isOwner);
            uploadImageBtn.setManaged(isOwner);
        }
    }

    private void updateImageLabel() {
        if (imageStatusLabel == null) return;

        if (selectedImagePath == null || selectedImagePath.isBlank()) {
            imageStatusLabel.setText("No image selected");
        } else {
            imageStatusLabel.setText(new File(selectedImagePath).getName());
        }
    }

    // ===== ACTIONS =====

    @FXML
    private void onUploadImage() {
        if (!isOwner) return;

        FileChooser fc = new FileChooser();
        fc.setTitle("Choose exercise image");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        var window = (contentArea != null && contentArea.getScene() != null) ? contentArea.getScene().getWindow() : null;
        File chosen = fc.showOpenDialog(window);
        if (chosen == null) return;

        selectedImagePath = chosen.getAbsolutePath();
        updateImageLabel();
    }

    @FXML
    private void onSave() {
        if (!isOwner || contentArea == null || exercise == null) return;

        // commit spinner editor text into value
        try { durationSpinner.increment(0); } catch (Exception ignored) {}

        String title = titleField.getText() == null ? "" : titleField.getText().trim();
        String desc  = descriptionArea.getText() == null ? "" : descriptionArea.getText().trim();
        Integer dur  = durationSpinner.getValue();
        String diff  = difficultyChoice.getValue();

        if (title.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Title is required.").showAndWait();
            return;
        }
        if (desc.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Description is required.").showAndWait();
            return;
        }
        if (dur == null || dur <= 0) {
            new Alert(Alert.AlertType.WARNING, "Duration must be greater than 0.").showAndWait();
            return;
        }
        if (diff == null || diff.isBlank()) {
            new Alert(Alert.AlertType.WARNING, "Difficulty is required.").showAndWait();
            return;
        }

        exercise.setTitle(title);
        exercise.setDescription(desc);
        exercise.setDuration(dur);
        exercise.setDifficultyLevel(diff);

        // optional image
        exercise.setImage((selectedImagePath == null || selectedImagePath.isBlank()) ? null : selectedImagePath);

        if (mode == Mode.ADD) {
            exercise.setPlanId(planId);
            exerciseService.addExercise(exercise);
            if (onSaved != null) onSaved.run();
            contentArea.getChildren().setAll(previousContent);
            return;
        }

        exerciseService.updateExercise(exercise);
        if (onSaved != null) onSaved.run();
        goToExerciseDetails();
    }


    @FXML
    private void onCancel() {
        if (contentArea != null && previousContent != null) {
            contentArea.getChildren().setAll(previousContent);
        }
    }

    // ===== NAV =====

    private void goToExerciseDetails() {
        if (contentArea == null || exercise == null) return;

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ExerciseDetails.fxml"));
            Parent view = loader.load();

            ExerciseDetailsController controller = loader.getController();
            controller.setContext(contentArea);
            controller.setPreviousContent(planPreviousContent);

            controller.setUserId(userId);
            controller.setOwner(isOwner);

            int effectivePlanId = (exercise.getPlanId() > 0) ? exercise.getPlanId() : planId;
            controller.setPlanId(effectivePlanId);

            // pass refresh callback forward too
            controller.setOnPlanRefresh(this::triggerPlanRefreshFromAnchorPane);

            controller.setExercise(exercise);

            contentArea.getChildren().setAll(view);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Key fix:
     * You are navigating "back" by restoring a snapshot list of Nodes.
     * That snapshot is stale (it contains the old exercise cards).
     * So we must explicitly tell the real PlanDetailsController to rebuild the cards from DB.
     *
     * This assumes PlanDetailsController.setContext(...) stores itself in AnchorPane properties:
     * contentArea.getProperties().put("planDetailsController", this);
     */
    private void triggerPlanRefreshFromAnchorPane() {
        if (contentArea == null) return;
        Object o = contentArea.getProperties().get("planDetailsController");
        if (o instanceof PlanDetailsController pdc) {
            pdc.refreshExercises();
        }
    }
}
