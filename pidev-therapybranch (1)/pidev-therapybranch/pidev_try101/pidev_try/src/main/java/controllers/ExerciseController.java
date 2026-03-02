package controllers;

import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import models.Exercise;
import services.ExerciseProgressService;
import services.ExerciseService;
import services.PlanEnrollmentService;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ExerciseController {

    private final ExerciseService exerciseService = new ExerciseService();
    private final ExerciseProgressService progressService = new ExerciseProgressService();
    private final PlanEnrollmentService enrollmentService = new PlanEnrollmentService();

    private int planId;
    private int userId;
    private boolean isOwner;

    private Pane exercisesContainer;

    private Button addExerciseBtn; // owner-only
    private Button followBtn;      // non-owner (can be null)

    private AnchorPane contentArea;

    // snapshot of PlanDetails so forms/details can go back
    private List<Node> planDetailsSnapshot = new ArrayList<>();

    public void setNavigationContext(AnchorPane contentArea) {
        this.contentArea = contentArea;
    }

    public void init(int planId, int userId, boolean isOwner,
                     Pane exercisesContainer,
                     Button addExerciseBtn,
                     Button followBtn) {

        this.planId = planId;
        this.userId = userId;
        this.isOwner = isOwner;
        this.exercisesContainer = exercisesContainer;
        this.addExerciseBtn = addExerciseBtn;
        this.followBtn = followBtn;

        wireButtons();
        refresh();
    }

    private void wireButtons() {
        if (addExerciseBtn != null) {
            addExerciseBtn.setVisible(isOwner);
            addExerciseBtn.setManaged(isOwner);
            addExerciseBtn.setOnAction(e -> handleAddExercise());
        }

        if (followBtn != null) {
            boolean showFollow = !isOwner;
            followBtn.setVisible(showFollow);
            followBtn.setManaged(showFollow);
            followBtn.setOnAction(e -> toggleFollow());
            updateFollowButtonText();
        }
    }

    private void updateFollowButtonText() {
        if (followBtn == null || isOwner) return;
        boolean enrolled = enrollmentService.isEnrolled(planId, userId);
        followBtn.setText(enrolled ? "Unfollow" : "Follow");
    }

    private void toggleFollow() {
        boolean enrolled = enrollmentService.isEnrolled(planId, userId);
        if (enrolled) enrollmentService.unenroll(planId, userId);
        else enrollmentService.enroll(planId, userId);

        updateFollowButtonText();
        refresh();
    }

    public void refresh() {
        if (exercisesContainer == null) return;

        exercisesContainer.getChildren().clear();

        if (exercisesContainer instanceof FlowPane fp) {
            fp.setHgap(15);
            fp.setVgap(15);
            fp.setPadding(new Insets(10));
        } else if (exercisesContainer instanceof VBox vb) {
            vb.setSpacing(10);
            vb.setPadding(new Insets(10));
        }

        List<Exercise> list = exerciseService.getExercisesByPlan(planId);

        if (list.isEmpty()) {
            Label empty = new Label("No exercises yet.");
            empty.setStyle("-fx-text-fill: #666;");
            exercisesContainer.getChildren().add(empty);
            updateFollowButtonText();
            return;
        }

        boolean enrolled = isOwner || enrollmentService.isEnrolled(planId, userId);

        for (Exercise ex : list) {
            exercisesContainer.getChildren().add(createExerciseRow(ex, enrolled));
        }

        updateFollowButtonText();
    }

    private Region createExerciseRow(Exercise ex, boolean enrolled) {
        if (exercisesContainer instanceof FlowPane) {
            return createExerciseCard(ex);
        }
        return createExerciseListRow(ex, enrolled);
    }

    // thumbnail loads from absolute path stored in DB (exercise.image), like Plans
    private Region createExerciseCard(Exercise ex) {
        VBox card = new VBox(10);
        card.setPrefWidth(220);
        card.setPadding(new Insets(12));
        card.setAlignment(Pos.TOP_CENTER);

        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 14;" +
                        "-fx-border-radius: 14;" +
                        "-fx-border-color: #E0E0E0;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.10), 10, 0.2, 0, 2);" +
                        "-fx-cursor: hand;"
        );

        card.setPickOnBounds(true);
        card.setOnMouseClicked(e -> openExerciseDetails(ex));

        ImageView thumb = new ImageView();
        thumb.setFitWidth(190);
        thumb.setFitHeight(110);
        thumb.setPreserveRatio(true);
        thumb.setSmooth(true);

        Image img = loadExerciseImage(ex.getImage());
        if (img != null) thumb.setImage(img);

        Label title = new Label(n(ex.getTitle()));
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        title.setWrapText(true);
        title.setMaxWidth(190);
        title.setAlignment(Pos.CENTER);

        card.getChildren().addAll(thumb, title);
        return card;
    }

    private Region createExerciseListRow(Exercise ex, boolean enrolled) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.TOP_LEFT);
        row.setPadding(new Insets(10));
        row.setStyle("-fx-background-color: white; -fx-border-color: #E0E0E0; -fx-background-radius: 10; -fx-border-radius: 10; -fx-cursor: hand;");

        row.setPickOnBounds(true);
        row.setOnMouseClicked(e -> openExerciseDetails(ex));

        VBox textBox = new VBox(4);
        Label title = new Label(n(ex.getTitle()));
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

        String preview = n(ex.getDescription());
        if (preview.length() > 90) preview = preview.substring(0, 90) + "...";
        Label desc = new Label(preview);
        desc.setStyle("-fx-text-fill: #666;");
        desc.setWrapText(true);

        textBox.getChildren().addAll(title, desc);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        VBox rightBox = new VBox(6);
        rightBox.setMinWidth(240);

        Label meta = new Label(ex.getDuration() + " min  •  " + n(ex.getDifficultyLevel()));
        meta.setStyle("-fx-text-fill: #444;");

        ComboBox<String> status = new ComboBox<>();
        status.getItems().addAll("Not Started", "In Progress", "Done", "Skipped");
        status.setPrefWidth(140);

        String current = progressService.getStatus(ex.getExerciseId(), userId);
        status.setValue(current);
        status.setDisable(!enrolled);

        status.setOnAction(e -> {
            String val = status.getValue();
            if (val == null) return;

            if (!isOwner && !enrollmentService.isEnrolled(planId, userId)) {
                enrollmentService.enroll(planId, userId);
                updateFollowButtonText();
            }
            progressService.upsertStatus(ex.getExerciseId(), userId, val);
        });

        HBox actions = new HBox(8);

        Button viewBtn = new Button("View");
        viewBtn.setOnAction(e -> openExerciseDetails(ex));

        Button editBtn = new Button("Edit");
        Button delBtn = new Button("Delete");

        editBtn.setVisible(isOwner);
        editBtn.setManaged(isOwner);
        delBtn.setVisible(isOwner);
        delBtn.setManaged(isOwner);

        viewBtn.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_CLICKED, javafx.event.Event::consume);
        editBtn.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_CLICKED, javafx.event.Event::consume);
        delBtn.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_CLICKED, javafx.event.Event::consume);
        status.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_CLICKED, javafx.event.Event::consume);

        editBtn.setOnAction(e -> openEditExerciseScreen(ex));
        delBtn.setOnAction(e -> handleDeleteExercise(ex));

        actions.getChildren().addAll(viewBtn, editBtn, delBtn);

        rightBox.getChildren().addAll(meta, status, actions);

        row.getChildren().addAll(textBox, rightBox);
        return row;
    }

    // ===== NAV =====

    private void openExerciseDetails(Exercise ex) {
        if (contentArea == null) return;

        try {
            planDetailsSnapshot = new ArrayList<>(contentArea.getChildren());

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ExerciseDetails.fxml"));
            Parent view = loader.load();

            ExerciseDetailsController controller = loader.getController();
            controller.setContext(contentArea);
            controller.setPreviousContent(planDetailsSnapshot);

            controller.setUserId(userId);
            controller.setOwner(isOwner);
            controller.setPlanId(planId);

            controller.setExercise(ex);

            contentArea.getChildren().setAll(view);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleAddExercise() {
        if (!isOwner || contentArea == null) return;

        try {
            planDetailsSnapshot = new ArrayList<>(contentArea.getChildren());

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ExerciseForm.fxml"));
            Parent view = loader.load();

            ExerciseFormController controller = loader.getController();
            controller.setContext(contentArea);
            controller.setPreviousContent(planDetailsSnapshot);

            controller.setUserId(userId);
            controller.setOwner(isOwner);
            controller.setPlanId(planId);

            controller.setModeAdd();
            controller.setOnSaved(this::refresh);

            contentArea.getChildren().setAll(view);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void openEditExerciseScreen(Exercise ex) {
        if (!isOwner || contentArea == null || ex == null) return;

        try {
            List<Node> snapshot = new ArrayList<>(contentArea.getChildren());
            planDetailsSnapshot = new ArrayList<>(snapshot);

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ExerciseForm.fxml"));
            Parent view = loader.load();

            ExerciseFormController controller = loader.getController();
            controller.setContext(contentArea);
            controller.setPreviousContent(snapshot);
            controller.setPlanPreviousContent(planDetailsSnapshot);

            controller.setUserId(userId);
            controller.setOwner(isOwner);
            controller.setPlanId(planId);

            controller.setModeEdit(ex);
            controller.setOnSaved(this::refresh);

            contentArea.getChildren().setAll(view);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleDeleteExercise(Exercise ex) {
        if (!isOwner) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete this exercise?", ButtonType.CANCEL, ButtonType.OK);
        confirm.setHeaderText(null);

        ButtonType result = confirm.showAndWait().orElse(ButtonType.CANCEL);
        if (result != ButtonType.OK) return;

        exerciseService.deleteExercise(ex.getExerciseId());

        // IMPORTANT: refresh PlanDetails UI immediately (cards removed / image updates)
        refresh();
    }

    // ===== IMAGES =====

    // absolute path in DB, like plans. Fallback to /images/plan.png
    private Image loadExerciseImage(String imagePath) {
        if (imagePath != null && !imagePath.isBlank()) {
            try {
                File f = new File(imagePath);
                if (f.exists() && f.isFile()) {
                    return new Image(f.toURI().toString(), true);
                }
            } catch (Exception ignored) {}
        }

        try (var is = getClass().getResourceAsStream("/images/plan.png")) {
            if (is != null) return new Image(is);
        } catch (Exception ignored) {}

        return null;
    }

    private String n(String s) {
        return s == null ? "" : s;
    }
}
