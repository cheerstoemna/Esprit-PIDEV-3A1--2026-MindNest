package controllers;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.util.Duration;
import models.CoachingPlan;
import services.CoachingPlanService;
import services.ExerciseProgressService;
import services.ExerciseService;
import services.TranslateService;
import utils.AppState;
import utils.UserSession;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class PlanDetailsController {

    @FXML private ImageView planHeaderImage;

    @FXML private TextField titleField;
    @FXML private TextArea descriptionArea;
    @FXML private TextArea goalsArea;

    @FXML private Button editBtn;
    @FXML private Button deleteBtn;
    @FXML private Button saveBtn;
    @FXML private Button cancelBtn;
    @FXML private Button addExerciseBtn;

    @FXML private Button backBtn;

    @FXML private Label progressTitleLabel;
    @FXML private Label descriptionLabelTitle;
    @FXML private Label goalsLabelTitle;
    @FXML private Label exercisesLabelTitle;

    @FXML private Label progressLabel;
    @FXML private ProgressBar progressBar;

    @FXML private FlowPane exercisesCardsContainer;

    @FXML private Button analyticsBtn;

    @FXML private Label badgesTitleLabel;

    @FXML private HBox badgeFirst;
    @FXML private Label badgeFirstIcon;
    @FXML private Label badgeFirstText;

    @FXML private HBox badgeHalf;
    @FXML private Label badgeHalfIcon;
    @FXML private Label badgeHalfText;

    @FXML private HBox badgeFull;
    @FXML private Label badgeFullIcon;
    @FXML private Label badgeFullText;

    // Toast overlay (PlanDetails.fxml has toastLayer + toastLabel)
    @FXML private AnchorPane toastLayer;
    @FXML private Label toastLabel;

    private boolean firstWasUnlocked = false;
    private boolean halfWasUnlocked = false;
    private boolean fullWasUnlocked = false;

    private final CoachingPlanService planService = new CoachingPlanService();
    private final ExerciseService exerciseService = new ExerciseService();
    private final ExerciseProgressService progressService = new ExerciseProgressService();
    private final TranslateService translateService = new TranslateService();

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

    private String rawTitle, rawDesc, rawGoals;

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

        applyStaticUiLanguage();
    }

    public void setPlan(CoachingPlan plan) {
        this.currentPlan = plan;
        if (plan == null) return;

        rawTitle = n(plan.getTitle());
        rawDesc  = n(plan.getDescription());
        rawGoals = n(plan.getGoals());

        titleField.setText(rawTitle);
        descriptionArea.setText(rawDesc);
        goalsArea.setText(rawGoals);

        if (planHeaderImage != null) {
            Image img = loadPlanImage(plan.getImagePath());
            if (img != null) planHeaderImage.setImage(img);
        }

        if (exercisesCardsContainer != null) {
            exercisesCardsContainer.setHgap(15);
            exercisesCardsContainer.setVgap(15);

            exerciseController.setNavigationContext(contentArea);

            // ✅ Follow removed: pass null for follow button
            exerciseController.init(
                    plan.getPlanId(),
                    loggedInUserId,
                    isOwner,
                    exercisesCardsContainer,
                    addExerciseBtn,
                    null
            );
        }

        applyStaticUiLanguage();
        applyPlanTranslationAsync();
        refreshProgress();

        // opening shouldn't toast
        syncBadgeFlags();
        refreshBadges(false);
    }

    private void applyStaticUiLanguage() {
        String lang = AppState.getCoachingLang();
        if (lang == null) lang = "en";

        if ("fr".equals(lang)) {
            if (backBtn != null) backBtn.setText("← Retour");
            if (editBtn != null) editBtn.setText("Modifier");
            if (deleteBtn != null) deleteBtn.setText("Supprimer");
            if (saveBtn != null) saveBtn.setText("Enregistrer");
            if (cancelBtn != null) cancelBtn.setText("Annuler");
            if (addExerciseBtn != null) addExerciseBtn.setText("+ Ajouter un exercice");

            if (progressTitleLabel != null) progressTitleLabel.setText("Progrès :");
            if (descriptionLabelTitle != null) descriptionLabelTitle.setText("Description");
            if (goalsLabelTitle != null) goalsLabelTitle.setText("Objectifs");
            if (exercisesLabelTitle != null) exercisesLabelTitle.setText("Exercices");

            if (analyticsBtn != null) analyticsBtn.setText("Voir l’analytique");

            if (badgesTitleLabel != null) badgesTitleLabel.setText("Badges :");
            if (badgeFirstText != null) badgeFirstText.setText("Première réussite");
            if (badgeHalfText != null) badgeHalfText.setText("À mi-chemin");
            if (badgeFullText != null) badgeFullText.setText("Plan terminé");
        } else {
            if (backBtn != null) backBtn.setText("← Back");
            if (editBtn != null) editBtn.setText("Edit");
            if (deleteBtn != null) deleteBtn.setText("Delete");
            if (saveBtn != null) saveBtn.setText("Save");
            if (cancelBtn != null) cancelBtn.setText("Cancel");
            if (addExerciseBtn != null) addExerciseBtn.setText("+ Add Exercise");

            if (progressTitleLabel != null) progressTitleLabel.setText("Progress:");
            if (descriptionLabelTitle != null) descriptionLabelTitle.setText("Description");
            if (goalsLabelTitle != null) goalsLabelTitle.setText("Goals");
            if (exercisesLabelTitle != null) exercisesLabelTitle.setText("Exercises");

            if (analyticsBtn != null) analyticsBtn.setText("View Analytics");

            if (badgesTitleLabel != null) badgesTitleLabel.setText("Badges:");
            if (badgeFirstText != null) badgeFirstText.setText("First completion");
            if (badgeHalfText != null) badgeHalfText.setText("Halfway");
            if (badgeFullText != null) badgeFullText.setText("Plan completed");
        }
    }

    private void applyPlanTranslationAsync() {
        String lang = AppState.getCoachingLang();
        if (lang == null || "en".equals(lang)) {
            if (!titleField.isEditable()) titleField.setText(rawTitle);
            if (!descriptionArea.isEditable()) descriptionArea.setText(rawDesc);
            if (!goalsArea.isEditable()) goalsArea.setText(rawGoals);
            return;
        }

        if (titleField.isEditable() || descriptionArea.isEditable() || goalsArea.isEditable()) return;

        Thread t = new Thread(() -> {
            try {
                String tTitle = translateService.translate(rawTitle, lang);
                String tDesc  = translateService.translate(rawDesc, lang);
                String tGoals = translateService.translate(rawGoals, lang);

                Platform.runLater(() -> {
                    if (!titleField.isEditable()) titleField.setText(tTitle);
                    if (!descriptionArea.isEditable()) descriptionArea.setText(tDesc);
                    if (!goalsArea.isEditable()) goalsArea.setText(tGoals);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private void refreshProgress() {
        if (currentPlan == null || progressLabel == null || progressBar == null) return;

        int planId = currentPlan.getPlanId();
        int total = exerciseService.getExercisesByPlan(planId).size();
        int completed = progressService.countCompletedForPlan(planId, loggedInUserId);

        boolean fr = "fr".equals(AppState.getCoachingLang());
        progressLabel.setText(fr ? (completed + "/" + total + " terminés") : (completed + "/" + total + " completed"));
        progressBar.setProgress(total == 0 ? 0.0 : (double) completed / (double) total);
    }

    private void syncBadgeFlags() {
        if (currentPlan == null) return;

        int planId = currentPlan.getPlanId();
        int total = exerciseService.getExercisesByPlan(planId).size();
        int completed = progressService.countCompletedForPlan(planId, loggedInUserId);

        firstWasUnlocked = completed >= 1;
        halfWasUnlocked  = total > 0 && completed >= (int) Math.ceil(total / 2.0);
        fullWasUnlocked  = total > 0 && completed == total;
    }

    private void refreshBadges(boolean showToast) {
        if (currentPlan == null) return;

        int planId = currentPlan.getPlanId();
        int total = exerciseService.getExercisesByPlan(planId).size();
        int completed = progressService.countCompletedForPlan(planId, loggedInUserId);

        boolean firstUnlocked = completed >= 1;
        boolean halfUnlocked  = total > 0 && completed >= (int) Math.ceil(total / 2.0);
        boolean fullUnlocked  = total > 0 && completed == total;

        applyBadgeStyle(badgeFirst, badgeFirstIcon, firstUnlocked, "blue");
        applyBadgeStyle(badgeHalf, badgeHalfIcon, halfUnlocked, "gold");
        applyBadgeStyle(badgeFull, badgeFullIcon, fullUnlocked, "green");

        if (showToast) {
            boolean fr = "fr".equals(AppState.getCoachingLang());

            if (!firstWasUnlocked && firstUnlocked) {
                showBadgeToast(fr ? "🎉 Badge débloqué : Première réussite !" : "🎉 Badge unlocked: First completion!");
            }
            if (!halfWasUnlocked && halfUnlocked) {
                showBadgeToast(fr ? "🔥 Badge débloqué : À mi-chemin !" : "🔥 Badge unlocked: Halfway there!");
            }
            if (!fullWasUnlocked && fullUnlocked) {
                showBadgeToast(fr ? "🏆 Badge débloqué : Plan terminé !" : "🏆 Badge unlocked: Plan completed!");
            }
        }

        firstWasUnlocked = firstUnlocked;
        halfWasUnlocked = halfUnlocked;
        fullWasUnlocked = fullUnlocked;
    }

    private void showBadgeToast(String message) {
        if (toastLabel == null) return;

        toastLabel.setText(message);
        toastLabel.setOpacity(0);
        toastLabel.setTranslateY(-10);

        toastLabel.setVisible(true);
        toastLabel.setManaged(true);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(250), toastLabel);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        TranslateTransition slideIn = new TranslateTransition(Duration.millis(250), toastLabel);
        slideIn.setFromY(-10);
        slideIn.setToY(0);

        PauseTransition hold = new PauseTransition(Duration.seconds(1.2));

        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), toastLabel);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);

        TranslateTransition slideOut = new TranslateTransition(Duration.millis(300), toastLabel);
        slideOut.setFromY(0);
        slideOut.setToY(-6);

        ParallelTransition in = new ParallelTransition(fadeIn, slideIn);
        ParallelTransition out = new ParallelTransition(fadeOut, slideOut);

        SequentialTransition seq = new SequentialTransition(in, hold, out);
        seq.setOnFinished(e -> {
            toastLabel.setVisible(false);
            toastLabel.setManaged(false);
        });
        seq.playFromStart();
    }

    private void applyBadgeStyle(HBox box, Label icon, boolean unlocked, String theme) {
        if (box == null || icon == null) return;

        if (!unlocked) {
            icon.setText("○");
            icon.setStyle("-fx-text-fill:#777;");
            box.setStyle("-fx-background-color:#f6f7fb; -fx-background-radius:999; -fx-padding:6 10; " +
                    "-fx-border-radius:999; -fx-border-color:#e6e8f0;");
            return;
        }

        icon.setText("✓");

        String bg, fg;
        switch (theme) {
            case "green" -> { bg = "#e7f8ee"; fg = "#1b7a3a"; }
            case "gold"  -> { bg = "#fff3db"; fg = "#8a5a00"; }
            default      -> { bg = "#e8f1ff"; fg = "#2457b2"; }
        }

        box.setStyle("-fx-background-color:" + bg + "; -fx-background-radius:999; -fx-padding:6 10; " +
                "-fx-border-radius:999; -fx-border-color:#e6e8f0;");
        icon.setStyle("-fx-text-fill:" + fg + "; -fx-font-weight:bold;");
    }

    private Image loadPlanImage(String imagePath) {
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
    private void startEdit() {
        if (!isOwner || currentPlan == null) return;

        originalTitle = titleField.getText();
        originalDesc  = descriptionArea.getText();
        originalGoals = goalsArea.getText();

        setEditMode(true);
    }

    @FXML
    private void cancelEdit() {
        titleField.setText(rawTitle);
        descriptionArea.setText(rawDesc);
        goalsArea.setText(rawGoals);

        setEditMode(false);
        applyPlanTranslationAsync();
    }

    @FXML
    private void saveEdit() {
        if (!isOwner || currentPlan == null) return;

        String newTitle = titleField.getText() == null ? "" : titleField.getText().trim();
        String newDesc  = descriptionArea.getText() == null ? "" : descriptionArea.getText().trim();
        String newGoals = goalsArea.getText() == null ? "" : goalsArea.getText().trim();

        if (newTitle.isEmpty()) { new Alert(Alert.AlertType.WARNING, "Title is required.").showAndWait(); return; }
        if (newDesc.isEmpty())  { new Alert(Alert.AlertType.WARNING, "Description is required.").showAndWait(); return; }
        if (newGoals.isEmpty()) { new Alert(Alert.AlertType.WARNING, "Goals is required.").showAndWait(); return; }

        currentPlan.setTitle(newTitle);
        currentPlan.setDescription(newDesc);
        currentPlan.setGoals(newGoals);

        planService.updatePlan(currentPlan);

        rawTitle = newTitle;
        rawDesc  = newDesc;
        rawGoals = newGoals;

        setEditMode(false);

        if (onReturnRefresh != null) onReturnRefresh.run();
        refreshExercises();
        applyPlanTranslationAsync();
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
    private void openAnalytics() {
        if (contentArea == null || currentPlan == null) return;

        try {
            List<Node> snapshot = new ArrayList<>(contentArea.getChildren());

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/PlanAnalytics.fxml"));
            Parent view = loader.load();

            PlanAnalyticsController controller = loader.getController();
            controller.setContext(contentArea);
            controller.setPreviousContent(snapshot);
            controller.setUserId(loggedInUserId);
            controller.setPlan(currentPlan);

            contentArea.getChildren().setAll(view);

        } catch (Exception e) {
            e.printStackTrace();
        }
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

    public void refreshExercises() {
        if (currentPlan == null || exercisesCardsContainer == null) return;
        exerciseController.setNavigationContext(contentArea);
        exerciseController.refresh();
        refreshProgress();
        refreshBadges(true);
    }

    private String n(String s) { return s == null ? "" : s; }
}