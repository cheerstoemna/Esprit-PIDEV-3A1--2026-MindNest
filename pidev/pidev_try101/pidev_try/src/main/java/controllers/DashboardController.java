package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.TextAlignment;
import models.CoachingPlan;
import services.CoachingPlanService;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import utils.UserSession;
public class DashboardController {

    @FXML private AnchorPane contentArea;
    @FXML private HBox myPlansContainer;
    @FXML private HBox otherPlansContainer;

    private final CoachingPlanService planService = new CoachingPlanService();
    private final int loggedInUserId = UserSession.get().userId();

    @FXML
    public void initialize() {
        if (myPlansContainer == null || otherPlansContainer == null || contentArea == null) {
            throw new IllegalStateException(
                    "FXML injection failed. Check fx:id values: contentArea, myPlansContainer, otherPlansContainer."
            );
        }

        // spacing is controlled by FXML: <HBox spacing="15" ... />
        loadPlans();
    }

    private void loadPlans() {
        myPlansContainer.getChildren().clear();
        otherPlansContainer.getChildren().clear();

        List<CoachingPlan> plans = planService.getAllPlans();

        for (CoachingPlan plan : plans) {
            AnchorPane card = createPlanCard(plan);

            if (plan.getUserId() == loggedInUserId) {
                myPlansContainer.getChildren().add(card);
            } else {
                otherPlansContainer.getChildren().add(card);
            }
        }
    }

    private AnchorPane createPlanCard(CoachingPlan plan) {
        AnchorPane card = new AnchorPane();
        card.setPrefSize(160, 180);
        card.setMinSize(160, 180);
        card.setMaxSize(160, 180);

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
        if (img != null) {
            planImage.setImage(img);
        }

        Label planTitle = new Label(plan.getTitle() == null ? "" : plan.getTitle());
        planTitle.setLayoutX(15);
        planTitle.setLayoutY(125);
        planTitle.setPrefWidth(130);
        planTitle.setWrapText(true);
        planTitle.setTextAlignment(TextAlignment.CENTER);
        planTitle.setStyle("-fx-font-size:14px; -fx-font-weight:bold;");
        planTitle.setAlignment(javafx.geometry.Pos.CENTER);

        card.getChildren().addAll(planImage, planTitle);

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
