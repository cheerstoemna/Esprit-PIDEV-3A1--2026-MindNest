package controllers;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import models.CoachingPlan;
import services.CoachingPlanService;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import utils.UserSession;

public class AddPlanController {

    @FXML private TextField titleField;
    @FXML private TextArea descriptionArea;
    @FXML private TextArea goalsArea;

    @FXML private ImageView previewImage;
    @FXML private Label imageNameLabel;

    private final CoachingPlanService planService = new CoachingPlanService();
    private int loggedInUserId = UserSession.get().userId();

    private AnchorPane contentArea;
    private List<Node> previousContent = new ArrayList<>();
    private Runnable onReturnRefresh;

    // store absolute file path in DB
    private String selectedImagePath;

    public void setContext(AnchorPane contentArea) { this.contentArea = contentArea; }

    public void setPreviousContent(List<Node> previousContent) {
        this.previousContent = (previousContent == null) ? new ArrayList<>() : new ArrayList<>(previousContent);
    }

    public void setOnReturnRefresh(Runnable onReturnRefresh) { this.onReturnRefresh = onReturnRefresh; }

    public void setLoggedInUserId(int loggedInUserId) { this.loggedInUserId = loggedInUserId; }

    @FXML
    private void handleUploadImage() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose plan image");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        var window = (contentArea != null && contentArea.getScene() != null)
                ? contentArea.getScene().getWindow()
                : (titleField != null && titleField.getScene() != null ? titleField.getScene().getWindow() : null);

        File file = chooser.showOpenDialog(window);
        if (file == null) return;

        try {
            String ext = getExtension(file.getName());
            String newName = "plan_" + UUID.randomUUID() + ext;

            // safer than relative path: put under user home
            Path baseDir = Path.of(System.getProperty("user.home"), "MindNest", "app_data", "images", "plans");
            Files.createDirectories(baseDir);

            Path targetFile = baseDir.resolve(newName);
            Files.copy(file.toPath(), targetFile, StandardCopyOption.REPLACE_EXISTING);

            selectedImagePath = targetFile.toAbsolutePath().toString();

            if (imageNameLabel != null) imageNameLabel.setText(file.getName());
            if (previewImage != null) previewImage.setImage(new Image(targetFile.toUri().toString(), true));

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Failed to upload image.").showAndWait();
        }
    }

    @FXML
    private void handleCreatePlan() {
        String title = titleField.getText() == null ? "" : titleField.getText().trim();
        String desc  = descriptionArea.getText() == null ? "" : descriptionArea.getText().trim();
        String goals = goalsArea.getText() == null ? "" : goalsArea.getText().trim();

        if (title.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Title is required.").showAndWait();
            return;
        }
        if (desc.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Description is required.").showAndWait();
            return;
        }
        if (goals.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Goals is required.").showAndWait();
            return;
        }

        CoachingPlan plan = new CoachingPlan(
                loggedInUserId,
                title,
                desc,
                goals,
                (selectedImagePath == null || selectedImagePath.isBlank()) ? null : selectedImagePath
        );

        planService.addCoachingPlan(plan);

        if (onReturnRefresh != null) onReturnRefresh.run();
        goBack();
    }


    @FXML
    private void cancel() { goBack(); }

    @FXML
    private void goBack() {
        if (contentArea == null) return;

        contentArea.getChildren().clear();
        if (previousContent != null && !previousContent.isEmpty()) {
            contentArea.getChildren().addAll(previousContent);
        }

        if (onReturnRefresh != null) onReturnRefresh.run();
    }

    private String getExtension(String name) {
        int dot = name.lastIndexOf('.');
        return (dot >= 0) ? name.substring(dot) : "";
    }
}
