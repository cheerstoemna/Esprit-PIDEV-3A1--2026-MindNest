package controllers;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.FlowPane;
import models.CoachingPlan;
import services.CoachingPlanService;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import utils.UserSession;
public class PlanDetailsController {

    @FXML private ImageView planHeaderImage;

    @FXML private TextField titleField;
    @FXML private TextArea descriptionArea;
    @FXML private TextArea goalsArea;

    @FXML private Button editBtn;
    @FXML private Button deleteBtn;
    @FXML private Button saveBtn;
    @FXML private Button cancelBtn;

    @FXML private FlowPane exercisesCardsContainer;
    @FXML private Button addExerciseBtn;
    @FXML private Button followBtn;

    private final CoachingPlanService planService = new CoachingPlanService();
    private final ExerciseController exerciseController = new ExerciseController();

    private AnchorPane contentArea;
    private List<Node> previousContent = new ArrayList<>();
    private Runnable onReturnRefresh;

    private CoachingPlan currentPlan;
    private boolean isOwner = false;
    private int loggedInUserId = UserSession.get().userId();

    private String originalTitle;
    private String originalDesc;
    private String originalGoals;

    // IMPORTANT: store controller reference on the shared contentArea so other screens can refresh us
    public void setContext(AnchorPane contentArea) {
        this.contentArea = contentArea;
        if (this.contentArea != null) {
            this.contentArea.getProperties().put("planDetailsController", this);
        }
    }

    public void setPreviousContent(List<Node> previousContent) {
        this.previousContent = (previousContent == null) ? new ArrayList<>() : new ArrayList<>(previousContent);
    }

    public void setOnReturnRefresh(Runnable onReturnRefresh) { this.onReturnRefresh = onReturnRefresh; }

    public void setLoggedInUserId(int userId) { this.loggedInUserId = userId; }

    public void setOwner(boolean owner) {
        this.isOwner = owner;

        if (editBtn != null) { editBtn.setVisible(owner); editBtn.setManaged(owner); }
        if (deleteBtn != null) { deleteBtn.setVisible(owner); deleteBtn.setManaged(owner); }

        if (addExerciseBtn != null) { addExerciseBtn.setVisible(owner); addExerciseBtn.setManaged(owner); }

        if (followBtn != null) { followBtn.setVisible(!owner); followBtn.setManaged(!owner); }
    }

    public void setPlan(CoachingPlan plan) {
        this.currentPlan = plan;
        if (plan == null) return;

        titleField.setText(n(plan.getTitle()));
        descriptionArea.setText(n(plan.getDescription()));
        goalsArea.setText(n(plan.getGoals()));

        // header image
        if (planHeaderImage != null) {
            Image img = loadPlanImage(plan.getImagePath());
            if (img != null) planHeaderImage.setImage(img);
        }

        // exercises cards
        if (exercisesCardsContainer != null) {
            exercisesCardsContainer.setHgap(15);
            exercisesCardsContainer.setVgap(15);

            // IMPORTANT: set navigation context first
            exerciseController.setNavigationContext(contentArea);

            exerciseController.init(
                    plan.getPlanId(),
                    loggedInUserId,
                    isOwner,
                    exercisesCardsContainer,
                    addExerciseBtn,
                    followBtn
            );
        }
    }

    private Image loadPlanImage(String imagePath) {
        // DB absolute path
        if (imagePath != null && !imagePath.isBlank()) {
            try {
                File f = new File(imagePath);
                if (f.exists() && f.isFile()) {
                    return new Image(f.toURI().toString(), true);
                }
            } catch (Exception ignored) {}
        }

        // resource fallback
        try (InputStream is = getClass().getResourceAsStream("/images/plan.png")) {
            if (is != null) return new Image(is);
        } catch (Exception ignored) {}

        return null;
    }

    @FXML
    private void startEdit() {
        if (!isOwner || currentPlan == null) return;

        originalTitle = titleField.getText();
        originalDesc  = descriptionArea.getText();
        originalGoals = goalsArea.getText();

        setEditMode(true);
    }

    @FXML
    private void cancelEdit() {
        titleField.setText(originalTitle);
        descriptionArea.setText(originalDesc);
        goalsArea.setText(originalGoals);
        setEditMode(false);
    }

    @FXML
    private void saveEdit() {
        if (!isOwner || currentPlan == null) return;

        String newTitle = titleField.getText() == null ? "" : titleField.getText().trim();
        String newDesc  = descriptionArea.getText() == null ? "" : descriptionArea.getText().trim();
        String newGoals = goalsArea.getText() == null ? "" : goalsArea.getText().trim();

        if (newTitle.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Title is required.").showAndWait();
            return;
        }
        if (newDesc.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Description is required.").showAndWait();
            return;
        }
        if (newGoals.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Goals is required.").showAndWait();
            return;
        }

        currentPlan.setTitle(newTitle);
        currentPlan.setDescription(newDesc);
        currentPlan.setGoals(newGoals);

        planService.updatePlan(currentPlan);

        setEditMode(false);

        if (onReturnRefresh != null) onReturnRefresh.run();
        refreshExercises();
    }


    @FXML
    private void deletePlan() {
        if (!isOwner || currentPlan == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete this plan?", ButtonType.CANCEL, ButtonType.OK);
        confirm.setHeaderText(null);

        ButtonType result = confirm.showAndWait().orElse(ButtonType.CANCEL);
        if (result != ButtonType.OK) return;

        planService.deletePlan(currentPlan.getPlanId());

        if (onReturnRefresh != null) onReturnRefresh.run();
        goBack();
    }

    @FXML
    private void goBack() {
        if (contentArea == null) return;
        contentArea.getChildren().setAll(previousContent);
        if (onReturnRefresh != null) onReturnRefresh.run();
    }

    private void setEditMode(boolean editing) {
        titleField.setEditable(editing);
        descriptionArea.setEditable(editing);
        goalsArea.setEditable(editing);

        if (editBtn != null) { editBtn.setVisible(!editing); editBtn.setManaged(!editing); }
        if (deleteBtn != null) { deleteBtn.setVisible(!editing); deleteBtn.setManaged(!editing); }

        if (saveBtn != null) { saveBtn.setVisible(editing); saveBtn.setManaged(editing); }
        if (cancelBtn != null) { cancelBtn.setVisible(editing); cancelBtn.setManaged(editing); }
    }

    private String n(String s) { return s == null ? "" : s; }

    // Call this after exercise add/edit/delete to rebuild cards from DB
    public void refreshExercises() {
        if (currentPlan == null || exercisesCardsContainer == null) return;

        // make sure navigation context stays valid
        exerciseController.setNavigationContext(contentArea);

        // IMPORTANT: do NOT re-wire buttons again; just refresh list from DB
        exerciseController.refresh();
    }
}
