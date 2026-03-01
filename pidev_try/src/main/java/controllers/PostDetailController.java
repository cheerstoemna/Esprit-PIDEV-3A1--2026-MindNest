package controllers;

import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import models.AppUser;
import models.Comment;
import models.Content;
import services.AnalyticsService;
import services.CommentService;
import services.ContentService;
import services.SentimentService;
import services.ToxicCommentException;
import utils.ImageUtil;
import utils.UserSession;

import java.awt.Desktop;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

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

    @FXML private VBox commentsBox;

    // ✅ NEW: filter/sort selects
    @FXML private ComboBox<String> sentimentFilterCombo;
    @FXML private ComboBox<String> sortCombo;

    private final ContentService contentService = new ContentService();
    private final CommentService commentService = new CommentService();
    private final AnalyticsService analytics = new AnalyticsService();
    private final SentimentService sentimentService = new SentimentService();

    private final Map<Integer, SentimentService.Result> sentimentCache = new HashMap<>();
    private final Set<Integer> sentimentInFlight = new HashSet<>();

    private List<Comment> allComments = new ArrayList<>();

    private Content content;
    private Runnable onBack;

    public void setOnBack(Runnable onBack) { this.onBack = onBack; }

    @FXML
    private void initialize() {
        // Sentiment filter options
        sentimentFilterCombo.setItems(FXCollections.observableArrayList(
                "All", "Positive", "Neutral", "Negative"
        ));
        sentimentFilterCombo.getSelectionModel().selectFirst();

        // Sort options
        sortCombo.setItems(FXCollections.observableArrayList(
                "Top (score)", "Most upvoted", "Newest", "Oldest"
        ));
        sortCombo.getSelectionModel().selectFirst();

        sentimentFilterCombo.setOnAction(e -> refreshCommentsView());
        sortCombo.setOnAction(e -> refreshCommentsView());
    }

    @FXML
    private void goBack() {
        if (onBack != null) onBack.run();
    }

    public void setContent(Content c) {
        this.content = c;
        if (c == null) return;

        AppUser u = UserSession.get().getUser();
        if (u != null) {
            analytics.logEvent(u.getId(), c.getId(), "VIEW", 1);
        }

        renderContent();
        loadComments();
    }

    private void renderContent() {
        if (content == null) return;

        titleLabel.setText(safe(content.getTitle(), "Untitled"));
        metaLabel.setText(buildMeta(content));
        descLabel.setText(safe(content.getDescription()));
        heroImage.setImage(ImageUtil.loadSmartImage(content.getImageUrl(), 900, 260));

        scoreLabel.setText(String.valueOf(content.getScore()));
        openLinkBtn.setDisable(safe(content.getSourceUrl()).isBlank());
    }

    private Integer requireLoggedUserId() {
        AppUser u = UserSession.get().getUser();
        if (u == null) {
            Alert a = new Alert(Alert.AlertType.WARNING);
            a.setTitle("Not logged in");
            a.setHeaderText(null);
            a.setContentText("Please login to vote/comment.");
            a.showAndWait();
            return null;
        }
        return u.getId();
    }

    @FXML
    public void openSourceLink() {
        if (content == null) return;
        String url = safe(content.getSourceUrl()).trim();
        if (url.isBlank()) return;

        try {
            Desktop.getDesktop().browse(new URI(url));
            AppUser u = UserSession.get().getUser();
            if (u != null) analytics.logEvent(u.getId(), content.getId(), "OPEN_LINK", 2);
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, e.getMessage(), ButtonType.OK).showAndWait();
        }
    }

    @FXML
    public void upvotePost() {
        if (content == null) return;

        Integer uid = requireLoggedUserId();
        if (uid == null) return;

        contentService.upvoteContent(content.getId(), uid);
        analytics.logEvent(uid, content.getId(), "UPVOTE", 4);
        refreshContentFromDb();
    }

    @FXML
    public void downvotePost() {
        if (content == null) return;

        Integer uid = requireLoggedUserId();
        if (uid == null) return;

        contentService.downvoteContent(content.getId(), uid);
        analytics.logEvent(uid, content.getId(), "DOWNVOTE", -2);
        refreshContentFromDb();
    }

    private void refreshContentFromDb() {
        try {
            Content fresh = contentService.getById(content.getId());
            if (fresh != null) {
                this.content = fresh;
                renderContent();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void addComment() {
        if (content == null) return;

        Integer uid = requireLoggedUserId();
        if (uid == null) return;

        String text = commentInput.getText() == null ? "" : commentInput.getText().trim();

        if (text.isBlank()) {
            new Alert(Alert.AlertType.WARNING, "Write a comment first.", ButtonType.OK).showAndWait();
            return;
        }

        if (text.length() > 2000) {
            new Alert(Alert.AlertType.WARNING, "Please keep comments under 2000 characters.", ButtonType.OK).showAndWait();
            return;
        }

        try {
            addCommentBtn.setDisable(true);
            commentService.addComment(content.getId(), uid, text);
        } catch (ToxicCommentException ex) {
            new Alert(Alert.AlertType.WARNING, ex.getMessage(), ButtonType.OK).showAndWait();
            return;
        } catch (Exception ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Failed to post comment: " + ex.getMessage(), ButtonType.OK).showAndWait();
            return;
        } finally {
            addCommentBtn.setDisable(false);
        }

        analytics.logEvent(uid, content.getId(), "COMMENT", 3);
        commentInput.clear();

        // Clear to re-evaluate new list quickly
        sentimentCache.clear();
        sentimentInFlight.clear();

        loadComments();
    }

    private void loadComments() {
        if (content == null) return;

        allComments = commentService.getCommentsByContentId(content.getId());
        if (allComments == null) allComments = new ArrayList<>();

        refreshCommentsView();
        warmupSentiments(allComments);
    }

    // 🔥 Optional: start sentiment jobs so filtering becomes instant after a moment
    private void warmupSentiments(List<Comment> list) {
        for (Comment c : list) {
            requestSentimentIfNeeded(c);
        }
    }

    private void refreshCommentsView() {
        if (commentsBox == null) return;
        commentsBox.getChildren().clear();

        String sentimentMode = sentimentFilterCombo != null ? sentimentFilterCombo.getValue() : "All";
        String sortMode = sortCombo != null ? sortCombo.getValue() : "Top (score)";

        List<Comment> view = new ArrayList<>(allComments);

        // 1) sort
        view.sort(buildComparator(sortMode));

        // 2) filter by sentiment (uses cache; unknown will be hidden if not All)
        if (sentimentMode != null && !"All".equalsIgnoreCase(sentimentMode)) {
            SentimentService.Label target = mapFilterToLabel(sentimentMode);

            view = view.stream()
                    .filter(c -> {
                        SentimentService.Result r = sentimentCache.get(c.getId());
                        if (r == null) {
                            // not ready yet -> request it and hide for now (filter requires known)
                            requestSentimentIfNeeded(c);
                            return false;
                        }
                        return r.getLabel() == target;
                    })
                    .collect(Collectors.toList());
        }

        for (Comment c : view) {
            commentsBox.getChildren().add(buildCommentCard(c));
        }
    }

    private Comparator<Comment> buildComparator(String mode) {
        if (mode == null) mode = "Top (score)";

        return switch (mode) {
            case "Most upvoted" -> Comparator.comparingInt(Comment::getUpvotes).reversed()
                    .thenComparingInt(Comment::getScore).reversed()
                    .thenComparing(Comment::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()));

            case "Newest" -> Comparator.comparing(Comment::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()));

            case "Oldest" -> Comparator.comparing(Comment::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()));

            case "Top (score)" -> Comparator.comparingInt(Comment::getScore).reversed()
                    .thenComparingInt(Comment::getUpvotes).reversed()
                    .thenComparing(Comment::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()));

            default -> Comparator.comparingInt(Comment::getScore).reversed()
                    .thenComparingInt(Comment::getUpvotes).reversed()
                    .thenComparing(Comment::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()));
        };
    }
    private SentimentService.Label mapFilterToLabel(String sentimentMode) {
        if ("Positive".equalsIgnoreCase(sentimentMode)) return SentimentService.Label.POSITIVE;
        if ("Neutral".equalsIgnoreCase(sentimentMode)) return SentimentService.Label.NEUTRAL;
        if ("Negative".equalsIgnoreCase(sentimentMode)) return SentimentService.Label.NEGATIVE;
        return SentimentService.Label.UNKNOWN;
    }

    private VBox buildCommentCard(Comment c) {
        VBox card = new VBox(8);
        card.getStyleClass().add("comment-card");
        card.setPadding(new Insets(12));

        String uname = safe(c.getUserName());
        if (uname.isBlank()) uname = "User #" + c.getUserId();

        Label user = new Label(uname);
        user.getStyleClass().add("comment-user");

        Label badge = new Label("Analyzing…");
        badge.getStyleClass().add("muted");

        // if cached, show instantly
        SentimentService.Result cached = sentimentCache.get(c.getId());
        if (cached != null) {
            renderSentiment(badge, cached);
        } else {
            requestSentimentIfNeeded(c);
        }

        Label date = new Label(c.getCreatedAt() == null ? "" : c.getCreatedAt().toString());
        date.getStyleClass().add("muted");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(10, user, badge, spacer, date);
        header.setAlignment(Pos.CENTER_LEFT);

        Label body = new Label(safe(c.getText()));
        body.setWrapText(true);
        body.getStyleClass().add("comment-text");

        Button up = new Button("▲");
        up.getStyleClass().add("vote-btn");

        Button down = new Button("▼");
        down.getStyleClass().add("vote-btn");

        Label score = new Label(String.valueOf(c.getScore()));
        score.getStyleClass().add("vote-score");

        up.setOnAction(e -> {
            Integer uid = requireLoggedUserId();
            if (uid == null) return;
            commentService.upvoteComment(c.getId(), uid);
            loadComments(); // reload to re-sort properly
        });

        down.setOnAction(e -> {
            Integer uid = requireLoggedUserId();
            if (uid == null) return;
            commentService.downvoteComment(c.getId(), uid);
            loadComments();
        });

        HBox votes = new HBox(8, up, score, down);
        votes.setAlignment(Pos.CENTER_LEFT);

        card.getChildren().addAll(header, body, votes);
        return card;
    }

    private void requestSentimentIfNeeded(Comment c) {
        if (c == null) return;
        int id = c.getId();

        if (sentimentCache.containsKey(id)) return;
        if (sentimentInFlight.contains(id)) return;

        sentimentInFlight.add(id);

        Task<SentimentService.Result> t = new Task<>() {
            @Override
            protected SentimentService.Result call() {
                return sentimentService.analyze(c.getText());
            }
        };

        t.setOnSucceeded(e -> {
            SentimentService.Result r = t.getValue();
            sentimentCache.put(id, r);
            sentimentInFlight.remove(id);

            // Important: if user is filtering by sentiment, refresh when new results arrive
            refreshCommentsView();
        });

        t.setOnFailed(e -> {
            sentimentCache.put(id, SentimentService.Result.unknown("Sentiment failed"));
            sentimentInFlight.remove(id);
            refreshCommentsView();
        });

        Thread th = new Thread(t);
        th.setDaemon(true);
        th.start();
    }

    private void renderSentiment(Label badge, SentimentService.Result r) {
        if (r == null || badge == null) return;

        if (r.getLabel() == SentimentService.Label.UNKNOWN) {
            badge.setText("Sentiment: —");
            return;
        }

        double conf = r.getConfidence();
        String pct = (conf > 0) ? (" " + (int) Math.round(conf * 100) + "%") : "";

        switch (r.getLabel()) {
            case POSITIVE -> badge.setText("🙂 Positive" + pct);
            case NEGATIVE -> badge.setText("🙁 Negative" + pct);
            case NEUTRAL  -> badge.setText("😐 Neutral" + pct);
            default       -> badge.setText("Sentiment: —");
        }
    }

    // -------------------- helpers --------------------

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