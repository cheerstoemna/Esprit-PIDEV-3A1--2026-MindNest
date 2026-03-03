package controllers;

import javafx.application.Platform;
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
import services.TranslateService;
import utils.AppState;

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

    // static UI nodes (ExerciseDetails.fxml must have these fx:id)
    @FXML private Button backBtn;
    @FXML private Label statusTitleLabel;
    @FXML private Label descriptionTitleLabel;

    @FXML private ComboBox<String> statusCombo;

    @FXML private Button editBtn;
    @FXML private Button deleteBtn;

    private AnchorPane contentArea;
    private List<Node> previousContent;

    private Runnable onPlanRefresh;
    public void setOnPlanRefresh(Runnable onPlanRefresh) { this.onPlanRefresh = onPlanRefresh; }

    private final ExerciseService exerciseService = new ExerciseService();
    private final ExerciseProgressService progressService = new ExerciseProgressService();
    private final TranslateService translateService = new TranslateService();

    private Exercise exercise;
    private int userId;
    private boolean isOwner;
    private int planId;

    private boolean initializingStatus = false;

    // raw text cache for translation (dynamic content)
    private String rawTitle = "";
    private String rawDesc = "";

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

    private void applyStaticUiLanguage() {
        String lang = AppState.getCoachingLang();
        if (lang == null) lang = "en";

        if ("fr".equals(lang)) {
            if (backBtn != null) backBtn.setText("← Retour");
            if (editBtn != null) editBtn.setText("Modifier");
            if (deleteBtn != null) deleteBtn.setText("Supprimer");
            if (statusTitleLabel != null) statusTitleLabel.setText("Mon statut :");
            if (descriptionTitleLabel != null) descriptionTitleLabel.setText("Description");
        } else {
            if (backBtn != null) backBtn.setText("← Back");
            if (editBtn != null) editBtn.setText("Edit");
            if (deleteBtn != null) deleteBtn.setText("Delete");
            if (statusTitleLabel != null) statusTitleLabel.setText("My Status:");
            if (descriptionTitleLabel != null) descriptionTitleLabel.setText("Description");
        }
    }

    private void applyExerciseTranslationAsync() {
        String lang = AppState.getCoachingLang();
        if (lang == null || "en".equals(lang)) {
            titleLabel.setText(rawTitle);
            descriptionLabel.setText(rawDesc);
            return;
        }

        Thread t = new Thread(() -> {
            try {
                String tTitle = translateService.translate(rawTitle, lang);
                String tDesc  = translateService.translate(rawDesc, lang);

                Platform.runLater(() -> {
                    titleLabel.setText(tTitle);
                    descriptionLabel.setText(tDesc);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private void render() {
        if (exercise == null) return;

        updateOwnerUI();
        applyStaticUiLanguage();

        rawTitle = n(exercise.getTitle());
        rawDesc  = n(exercise.getDescription());

        titleLabel.setText(rawTitle);
        descriptionLabel.setText(rawDesc);

        boolean fr = "fr".equals(AppState.getCoachingLang());
        durationLabel.setText((fr ? "Durée: " : "Duration: ") + exercise.getDuration() + " min");
        difficultyLabel.setText((fr ? "Difficulté: " : "Difficulty: ") + n(exercise.getDifficultyLevel()));

        // status always editable for everyone
        statusCombo.setDisable(false);

        // ---- STATUS COMBO: display translated, store English ----
        String NOT_STARTED_D = fr ? "Pas commencé" : "Not Started";
        String IN_PROGRESS_D = fr ? "En cours" : "In Progress";
        String COMPLETED_D   = fr ? "Terminé" : "Completed";
        String SKIPPED_D     = fr ? "Ignoré" : "Skipped";

        statusCombo.getItems().setAll(NOT_STARTED_D, IN_PROGRESS_D, COMPLETED_D, SKIPPED_D);

        initializingStatus = true;
        String currentStored = progressService.getStatus(exercise.getExerciseId(), userId);
        if ("Done".equalsIgnoreCase(currentStored)) currentStored = "Completed";

        String currentDisplay = switch (currentStored) {
            case "In Progress" -> IN_PROGRESS_D;
            case "Completed"   -> COMPLETED_D;
            case "Skipped"     -> SKIPPED_D;
            default            -> NOT_STARTED_D;
        };

        statusCombo.setValue(currentDisplay);
        initializingStatus = false;

        statusCombo.setOnAction(e -> {
            if (initializingStatus) return;

            String chosenDisplay = statusCombo.getValue();
            if (chosenDisplay == null) return;

            String chosenStored =
                    chosenDisplay.equals(IN_PROGRESS_D) ? "In Progress" :
                            chosenDisplay.equals(COMPLETED_D)   ? "Completed" :
                                    chosenDisplay.equals(SKIPPED_D)     ? "Skipped" :
                                            "Not Started";

            progressService.upsertStatus(exercise.getExerciseId(), userId, chosenStored);
            triggerPlanRefresh();
        });
        // ---- end status combo ----

        Image img = loadExerciseImage(exercise.getImage());
        if (img != null) exerciseImageView.setImage(img);

        applyExerciseTranslationAsync();
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

        try (InputStream is = getClass().getResourceAsStream("/fxml/images/plan.png")) {
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