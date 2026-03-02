package controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import models.Comment;
import models.Content;
import services.CommentService;
import services.ContentService;
import utils.ImageUtil;

import java.awt.Desktop;
import java.net.URI;
import java.util.List;

public class PostDetailController {

    @FXML private Label titleLabel;
    @FXML private Label metaLabel;
    @FXML private ImageView heroImage;
    @FXML private Label descLabel;

    @FXML private Button backBtn;
    @FXML private Button openLinkBtn;

    @FXML private Button upBtn;
    @FXML private Button downBtn;
    @FXML private Label scoreLabel;

    @FXML private TextArea commentInput;
    @FXML private Button addCommentBtn;
    @FXML private ListView<Comment> commentsList;

    private final ContentService contentService = new ContentService();
    private final CommentService commentService = new CommentService();

    private Content content;

    // ✅ back callback injected by ContentController
    private Runnable onBack;

    public void setOnBack(Runnable onBack) {
        this.onBack = onBack;
    }

    @FXML
    private void goBack() {
        if (onBack != null) {
            onBack.run();   // ✅ returns to ContentController snapshot (home/forum with filters)
        }
    }

    @FXML
    public void initialize() {
        if (commentsList != null) {
            commentsList.setCellFactory(lv -> new ListCell<>() {
                @Override
                protected void updateItem(Comment c, boolean empty) {
                    super.updateItem(c, empty);
                    if (empty || c == null) { setGraphic(null); setText(null); return; }

                    VBox box = new VBox(4);
                    box.getStyleClass().add("comment-card");
                    box.setPadding(new Insets(10));

                    Label body = new Label(c.getText() == null ? "" : c.getText());
                    body.setWrapText(true);
                    body.getStyleClass().add("comment-text");

                    Label meta = new Label((c.getCreatedAt() == null ? "" : c.getCreatedAt().toString()));
                    meta.getStyleClass().add("muted");

                    Button up = new Button("▲");
                    up.getStyleClass().add("vote-btn");
                    Button down = new Button("▼");
                    down.getStyleClass().add("vote-btn");
                    Label score = new Label(String.valueOf(c.getScore()));
                    score.getStyleClass().add("vote-score");

                    up.setOnAction(e -> {
                        commentService.upvoteComment(c.getId());
                        c.setUpvotes(c.getUpvotes() + 1);
                        score.setText(String.valueOf(c.getScore()));
                    });

                    down.setOnAction(e -> {
                        commentService.downvoteComment(c.getId());
                        c.setDownvotes(c.getDownvotes() + 1);
                        score.setText(String.valueOf(c.getScore()));
                    });

                    VBox voteBox = new VBox(4, up, score, down);
                    voteBox.setAlignment(Pos.TOP_CENTER);

                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);

                    HBox row = new HBox(10, box, spacer, voteBox);
                    row.setAlignment(Pos.TOP_LEFT);

                    box.getChildren().addAll(body, meta);

                    setGraphic(row);
                    setText(null);
                }
            });
        }
    }

    public void setContent(Content c) {
        this.content = c;
        if (c == null) return;

        titleLabel.setText(safe(c.getTitle(), "Untitled"));
        metaLabel.setText(buildMeta(c));
        descLabel.setText(safe(c.getDescription()));

        heroImage.setImage(ImageUtil.loadSmartImage(c.getImageUrl(), 900, 260));

        scoreLabel.setText(String.valueOf(c.getScore()));

        loadComments();

        openLinkBtn.setDisable(safe(c.getSourceUrl()).isBlank());
    }

    @FXML
    public void openSourceLink() {
        if (content == null) return;
        String url = safe(content.getSourceUrl()).trim();
        if (url.isBlank()) return;

        try {
            Desktop.getDesktop().browse(new URI(url));
        } catch (Exception e) {
            e.printStackTrace();
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setTitle("Could not open link");
            a.setHeaderText(null);
            a.setContentText(e.getMessage());
            a.showAndWait();
        }
    }

    @FXML
    public void upvotePost() {
        if (content == null) return;
        contentService.upvoteContent(content.getId());
        content.setUpvotes(content.getUpvotes() + 1);
        scoreLabel.setText(String.valueOf(content.getScore()));
    }

    @FXML
    public void downvotePost() {
        if (content == null) return;
        contentService.downvoteContent(content.getId());
        content.setDownvotes(content.getDownvotes() + 1);
        scoreLabel.setText(String.valueOf(content.getScore()));
    }

    @FXML
    public void addComment() {
        if (content == null) return;

        String text = commentInput.getText() == null ? "" : commentInput.getText().trim();

        if (text.isBlank()) {
            Alert a = new Alert(Alert.AlertType.WARNING);
            a.setTitle("Missing comment");
            a.setHeaderText(null);
            a.setContentText("Write a comment first.");
            a.showAndWait();
            return;
        }

        if (text.length() > 2000) {
            Alert a = new Alert(Alert.AlertType.WARNING);
            a.setTitle("Comment too long");
            a.setHeaderText(null);
            a.setContentText("Please keep comments under 2000 characters.");
            a.showAndWait();
            return;
        }

        commentService.addComment(content.getId(), text);
        commentInput.clear();
        loadComments();
    }

    private void loadComments() {
        if (content == null) return;
        List<Comment> list = commentService.getCommentsByContentId(content.getId());
        commentsList.setItems(FXCollections.observableArrayList(list));
    }

    private String buildMeta(Content c) {
        String t = normalizeType(c.getType());
        if (t.isBlank()) t = "Article";
        String cat = safe(c.getCategory(), "General");
        return t + " • " + cat;
    }

    private String normalizeType(String raw) {
        String t = safe(raw).trim().toLowerCase();
        if (t.isEmpty()) return "";
        if (t.contains("article") || t.contains("text") || t.equals("blog") || t.equals("post")) return "Article";
        if (t.contains("video")) return "Video";
        if (t.contains("podcast") || t.contains("audio")) return "Podcast";
        return raw.trim();
    }

    private String safe(String s) { return s == null ? "" : s; }
    private String safe(String s, String fallback) { return (s == null || s.isBlank()) ? fallback : s; }
}
