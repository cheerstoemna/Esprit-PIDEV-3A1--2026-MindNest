package controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.TextAlignment;
import models.CoachingPlan;
import services.AdviceService;
import services.CoachingPlanService;
import services.ExerciseProgressService;
import services.ExerciseService;
import services.FavoritePlanService;
import services.QuoteService;
import services.TranslateService;
import utils.AppState;
import utils.UserSession;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DashboardController {

    @FXML private AnchorPane contentArea;

    @FXML private HBox favoritesContainer;
    @FXML private HBox myPlansContainer;
    @FXML private HBox otherPlansContainer;

    // NEW: section headers
    @FXML private Label quoteTitleLabel;
    @FXML private Label adviceTitleLabel;

    @FXML private Label quoteLabel;
    @FXML private Label adviceLabel;

    // language row
    @FXML private ComboBox<String> langCombo;
    @FXML private Button applyLangBtn;

    // static UI labels/buttons from dashboard.fxml
    @FXML private Label languageLabel;
    @FXML private Label favoritesTitle;
    @FXML private Label myPlansTitle;
    @FXML private Label otherPlansTitle;
    @FXML private Button addPlanBtn;

    private final CoachingPlanService planService = new CoachingPlanService();
    private final QuoteService quoteService = new QuoteService();
    private final AdviceService adviceService = new AdviceService();

    private final ExerciseService exerciseService = new ExerciseService();
    private final ExerciseProgressService progressService = new ExerciseProgressService();

    private final TranslateService translateService = new TranslateService();
    private final FavoritePlanService favoriteService = new FavoritePlanService();

    private final int loggedInUserId = UserSession.get().userId();

    private String lastQuoteOriginal = null;
    private String lastAdviceOriginal = null;

    @FXML
    public void initialize() {
        if (favoritesContainer == null || myPlansContainer == null || otherPlansContainer == null
                || contentArea == null || quoteLabel == null) {
            throw new IllegalStateException(
                    "FXML injection failed. Check fx:id values: favoritesContainer, myPlansContainer, otherPlansContainer, contentArea, quoteLabel."
            );
        }

        if (langCombo != null) {
            langCombo.getItems().setAll("English (en)", "Français (fr)");
            String cur = AppState.getCoachingLang();
            langCombo.setValue("fr".equals(cur) ? "Français (fr)" : "English (en)");
        }

        applyStaticUiLanguage();

        loadPlans();
        loadQuoteAsync();
        loadAdviceAsync();
    }

    public void refreshQuote() {
        loadQuoteAsync();
    }

    @FXML
    private void onApplyLanguage() {
        if (langCombo == null || langCombo.getValue() == null) return;

        String selected = langCombo.getValue();
        String lang = selected.contains("(fr)") ? "fr" : "en";
        AppState.setCoachingLang(lang);

        applyStaticUiLanguage();
        loadPlans();
        translateExistingQuoteAsync();
        translateExistingAdviceAsync();
    }

    private void applyStaticUiLanguage() {
        String lang = AppState.getCoachingLang();
        if (lang == null) lang = "en";

        if ("fr".equals(lang)) {
            if (languageLabel != null) languageLabel.setText("Langue :");
            if (applyLangBtn != null) applyLangBtn.setText("Appliquer");
            if (addPlanBtn != null) addPlanBtn.setText("+ Ajouter un plan");

            if (favoritesTitle != null) favoritesTitle.setText("Favoris");
            if (myPlansTitle != null) myPlansTitle.setText("Mes plans");
            if (otherPlansTitle != null) otherPlansTitle.setText("Explorer d’autres plans");

            // NEW: header translations
            if (quoteTitleLabel != null) quoteTitleLabel.setText("Une phrase de sagesse");
            if (adviceTitleLabel != null) adviceTitleLabel.setText("Essayez ce conseil");

        } else {
            if (languageLabel != null) languageLabel.setText("Language:");
            if (applyLangBtn != null) applyLangBtn.setText("Apply");
            if (addPlanBtn != null) addPlanBtn.setText("+ Add Plan");

            if (favoritesTitle != null) favoritesTitle.setText("Favorites");
            if (myPlansTitle != null) myPlansTitle.setText("My Coaching Plans");
            if (otherPlansTitle != null) otherPlansTitle.setText("Explore Other Plans");

            // NEW: header translations
            if (quoteTitleLabel != null) quoteTitleLabel.setText("One-line wisdom");
            if (adviceTitleLabel != null) adviceTitleLabel.setText("Try this tip");
        }
    }

    private void loadPlans() {
        favoritesContainer.getChildren().clear();
        myPlansContainer.getChildren().clear();
        otherPlansContainer.getChildren().clear();

        List<CoachingPlan> plans = planService.getAllPlans();

        Set<Integer> favIds = favoriteService.getFavoritePlanIds(loggedInUserId);
        if (favIds == null) favIds = new HashSet<>();

        for (CoachingPlan plan : plans) {
            boolean isFav = favIds.contains(plan.getPlanId());

            if (isFav) favoritesContainer.getChildren().add(createPlanCard(plan, true));

            if (plan.getUserId() == loggedInUserId) {
                myPlansContainer.getChildren().add(createPlanCard(plan, isFav));
            } else {
                otherPlansContainer.getChildren().add(createPlanCard(plan, isFav));
            }
        }
    }

    private void loadQuoteAsync() {
        quoteLabel.setText("Loading quote...");

        Thread t = new Thread(() -> {
            try {
                String quote = quoteService.fetchQuoteText();
                lastQuoteOriginal = quote;

                String display = quote;
                String lang = AppState.getCoachingLang();
                if (lang != null && !"en".equals(lang)) {
                    try {
                        display = translateService.translate(quote, lang);
                    } catch (Exception te) {
                        te.printStackTrace();
                        display = quote;
                    }
                }

                String finalDisplay = display;
                Platform.runLater(() -> quoteLabel.setText(finalDisplay));

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> quoteLabel.setText("Could not load quote."));
            }
        });

        t.setDaemon(true);
        t.start();
    }

    private void translateExistingQuoteAsync() {
        if (lastQuoteOriginal == null || lastQuoteOriginal.isBlank()) {
            loadQuoteAsync();
            return;
        }

        Thread t = new Thread(() -> {
            try {
                String display = lastQuoteOriginal;
                String lang = AppState.getCoachingLang();

                if (lang != null && !"en".equals(lang)) {
                    display = translateService.translate(lastQuoteOriginal, lang);
                }

                String finalDisplay = display;
                Platform.runLater(() -> quoteLabel.setText(finalDisplay));

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> quoteLabel.setText(lastQuoteOriginal));
            }
        });

        t.setDaemon(true);
        t.start();
    }

    private void loadAdviceAsync() {
        if (adviceLabel == null) return;

        adviceLabel.setText("Loading advice...");

        Thread t = new Thread(() -> {
            try {
                String advice = adviceService.fetchAdvice();
                lastAdviceOriginal = advice;

                String display = advice;
                String lang = AppState.getCoachingLang();
                if (lang != null && !"en".equals(lang)) {
                    try {
                        display = translateService.translate(advice, lang);
                    } catch (Exception ignored) {
                        display = advice;
                    }
                }

                String finalDisplay = display;
                Platform.runLater(() -> adviceLabel.setText(finalDisplay));

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> adviceLabel.setText("Could not load advice."));
            }
        });

        t.setDaemon(true);
        t.start();
    }

    private void translateExistingAdviceAsync() {
        if (adviceLabel == null) return;

        if (lastAdviceOriginal == null || lastAdviceOriginal.isBlank()) {
            loadAdviceAsync();
            return;
        }

        String lang = AppState.getCoachingLang();
        if (lang == null || "en".equals(lang)) {
            adviceLabel.setText(lastAdviceOriginal);
            return;
        }

        Thread t = new Thread(() -> {
            try {
                String tr = translateService.translate(lastAdviceOriginal, lang);
                Platform.runLater(() -> adviceLabel.setText(tr));
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> adviceLabel.setText(lastAdviceOriginal));
            }
        });

        t.setDaemon(true);
        t.start();
    }

    private AnchorPane createPlanCard(CoachingPlan plan, boolean isFavorite) {
        AnchorPane card = new AnchorPane();
        card.setPrefSize(160, 190);
        card.setMinSize(160, 190);
        card.setMaxSize(160, 190);

        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 15;" +
                        "-fx-border-radius: 15;" +
                        "-fx-border-color: #E0E0E0;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.12), 10, 0.2, 0, 2);" +
                        "-fx-cursor: hand;"
        );

        ImageView planImage = new ImageView();
        planImage.setFitWidth(120);
        planImage.setFitHeight(100);
        planImage.setPreserveRatio(true);
        planImage.setSmooth(true);
        planImage.setLayoutX(20);
        planImage.setLayoutY(15);

        Image img = loadPlanImage(plan.getImagePath());
        if (img != null) planImage.setImage(img);

        Button heartBtn = new Button(isFavorite ? "♥" : "♡");
        heartBtn.setFocusTraversable(false);
        heartBtn.setLayoutX(130);
        heartBtn.setLayoutY(6);
        heartBtn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-font-size: 16px;" +
                        "-fx-text-fill: " + (isFavorite ? "#e53935" : "#777") + ";" +
                        "-fx-cursor: hand;"
        );

        heartBtn.setOnMouseClicked(e -> {
            e.consume();
            int pid = plan.getPlanId();

            if (favoriteService.isFavorite(loggedInUserId, pid)) {
                favoriteService.remove(loggedInUserId, pid);
            } else {
                favoriteService.add(loggedInUserId, pid);
            }
            loadPlans();
        });

        Label planTitle = new Label();
        planTitle.setLayoutX(15);
        planTitle.setLayoutY(125);
        planTitle.setPrefWidth(130);
        planTitle.setWrapText(true);
        planTitle.setTextAlignment(TextAlignment.CENTER);
        planTitle.setStyle("-fx-font-size:14px; -fx-font-weight:bold;");
        planTitle.setAlignment(javafx.geometry.Pos.CENTER);

        String rawTitle = (plan.getTitle() == null) ? "" : plan.getTitle();
        planTitle.setText(rawTitle);

        String lang = AppState.getCoachingLang();
        if (lang != null && !"en".equals(lang) && !rawTitle.isBlank()) {
            Thread t = new Thread(() -> {
                try {
                    String tr = translateService.translate(rawTitle, lang);
                    Platform.runLater(() -> planTitle.setText(tr));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
            t.setDaemon(true);
            t.start();
        }

        int planId = plan.getPlanId();
        int total = exerciseService.getExercisesByPlan(planId).size();
        int completed = progressService.countCompletedForPlan(planId, loggedInUserId);

        ProgressBar pb = new ProgressBar(total == 0 ? 0.0 : (double) completed / (double) total);
        pb.setPrefWidth(130);
        pb.setPrefHeight(8);
        pb.setLayoutX(15);
        pb.setLayoutY(168);

        Label progressText = new Label(completed + "/" + total);
        progressText.setLayoutX(15);
        progressText.setLayoutY(154);
        progressText.setPrefWidth(130);
        progressText.setAlignment(javafx.geometry.Pos.CENTER);
        progressText.setStyle("-fx-font-size:11px; -fx-text-fill:#666;");

        card.getChildren().addAll(planImage, heartBtn, planTitle, progressText, pb);

        card.setPickOnBounds(true);
        card.setOnMouseClicked(e -> openPlanDetails(plan));

        return card;
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

    private void openPlanDetails(CoachingPlan plan) {
        try {
            List<Node> snapshot = new ArrayList<>(contentArea.getChildren());

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/PlanDetails.fxml"));
            Parent view = loader.load();

            PlanDetailsController controller = loader.getController();
            controller.setContext(contentArea);
            controller.setPreviousContent(snapshot);
            controller.setOnReturnRefresh(this::loadPlans);

            controller.setOwner(plan.getUserId() == loggedInUserId);
            controller.setLoggedInUserId(loggedInUserId);
            controller.setPlan(plan);

            contentArea.getChildren().setAll(view);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void openAddPlan() {
        try {
            List<Node> snapshot = new ArrayList<>(contentArea.getChildren());

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AddPlan.fxml"));
            Parent view = loader.load();

            AddPlanController controller = loader.getController();
            controller.setContext(contentArea);
            controller.setPreviousContent(snapshot);
            controller.setOnReturnRefresh(this::loadPlans);
            controller.setLoggedInUserId(loggedInUserId);

            contentArea.getChildren().setAll(view);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}