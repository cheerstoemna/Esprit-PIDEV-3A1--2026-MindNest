package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import models.Comment;
import models.Content;
import services.CommentService;
import services.ContentService;
import utils.ImageUtil;

import java.awt.Desktop;
import java.net.URI;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

public class AdminController {

    @FXML private TableView<Content> contentTable;
    @FXML private TableColumn<Content, Integer> colId;
    @FXML private TableColumn<Content, String> colTitle;
    @FXML private TableColumn<Content, String> colType;
    @FXML private TableColumn<Content, String> colCategory;
    @FXML private TableColumn<Content, Timestamp> colDate;

    @FXML private TextField adminSearch;
    @FXML private ComboBox<String> filterType;
    @FXML private ComboBox<String> filterCategory;

    @FXML private ImageView previewImage;
    @FXML private Label previewTitle;
    @FXML private Label previewMeta;
    @FXML private Label previewDesc;

    @FXML private ListView<Comment> commentsList;
    @FXML private Label commentsHint;

    @FXML private Label statusLabel;

    private final ContentService cs = new ContentService();
    private final CommentService commentService = new CommentService();

    private final ObservableList<Content> master = FXCollections.observableArrayList();
    private final ObservableList<Content> filtered = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("createdAt"));

        filterType.setItems(FXCollections.observableArrayList("All", "Article", "Video", "Podcast"));
        filterType.setValue("All");

        setupCommentsList();

        refreshFromDB();

        adminSearch.textProperty().addListener((o, a, b) -> applyFilters());
        filterType.valueProperty().addListener((o, a, b) -> applyFilters());
        filterCategory.valueProperty().addListener((o, a, b) -> applyFilters());

        applyFilters();
        clearPreview();
    }

    private void refreshFromDB() {
        master.setAll(cs.getAllContent());
        rebuildCategoryFilter();
        status("Loaded " + master.size() + " items.");
    }

    private void rebuildCategoryFilter() {
        Set<String> cats = master.stream()
                .map(c -> safe(c.getCategory()).trim())
                .filter(s -> !s.isBlank())
                .collect(Collectors.toCollection(TreeSet::new));

        List<String> items = new ArrayList<>();
        items.add("All");
        items.addAll(cats);

        filterCategory.setItems(FXCollections.observableArrayList(items));
        if (filterCategory.getValue() == null) filterCategory.setValue("All");
        if (!items.contains(filterCategory.getValue())) filterCategory.setValue("All");
    }

    @FXML
    public void resetFilters() {
        adminSearch.setText("");
        filterType.setValue("All");
        filterCategory.setValue("All");
        applyFilters();
    }

    private void applyFilters() {
        String q = safe(adminSearch.getText()).trim().toLowerCase();
        String type = safe(filterType.getValue(), "All");
        String cat = safe(filterCategory.getValue(), "All");

        List<Content> out = master.stream()
                .filter(c -> q.isEmpty()
                        || safe(c.getTitle()).toLowerCase().contains(q)
                        || safe(c.getDescription()).toLowerCase().contains(q)
                        || safe(c.getCategory()).toLowerCase().contains(q)
                        || safe(c.getSourceUrl()).toLowerCase().contains(q))
                .filter(c -> "All".equalsIgnoreCase(type) || normalizeType(safe(c.getType())).equalsIgnoreCase(type))
                .filter(c -> "All".equalsIgnoreCase(cat) || safe(c.getCategory()).equalsIgnoreCase(cat))
                .sorted(Comparator.comparingInt(Content::getId).reversed())
                .collect(Collectors.toList());

        filtered.setAll(out);
        contentTable.setItems(filtered);

        status("Showing " + filtered.size() + " / " + master.size());
        if (filtered.isEmpty()) clearPreview();
    }

    @FXML
    public void onRowSelect() {
        Content c = contentTable.getSelectionModel().getSelectedItem();
        if (c == null) return;

        previewTitle.setText(safe(c.getTitle(), "Untitled"));
        previewMeta.setText(normalizeType(safe(c.getType())) + " • " + safe(c.getCategory(), "General") + " • score " + c.getScore());
        previewDesc.setText(trim(safe(c.getDescription()), 500));

        Image img = ImageUtil.loadSmartImage(c.getImageUrl(), 360, 200);
        previewImage.setImage(img);

        loadComments(c.getId());
    }

    private void clearPreview() {
        previewTitle.setText("Select a post…");
        previewMeta.setText("");
        previewDesc.setText("");
        previewImage.setImage(null);
        clearComments();
    }

    // -------------------- CRUD --------------------

    @FXML
    public void addContentDialog() {
        Content created = showContentDialog("Add Content", null);
        if (created == null) return;

        cs.addContent(created);
        refreshFromDB();
        applyFilters();
        status("Added: " + safe(created.getTitle()));
    }

    @FXML
    public void editContentDialog() {
        Content selected = contentTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            alert(Alert.AlertType.WARNING, "Select a post", "Pick a row in the table first.");
            return;
        }

        Content updated = showContentDialog("Edit Content", selected);
        if (updated == null) return;

        selected.setTitle(updated.getTitle());
        selected.setDescription(updated.getDescription());
        selected.setType(updated.getType());
        selected.setCategory(updated.getCategory());
        selected.setSourceUrl(updated.getSourceUrl());
        selected.setImageUrl(updated.getImageUrl());

        cs.updateContent(selected);
        refreshFromDB();
        applyFilters();
        status("Updated #" + selected.getId());
    }

    @FXML
    public void deleteContent() {
        Content selected = contentTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            alert(Alert.AlertType.WARNING, "Select a post", "Pick a row in the table first.");
            return;
        }

        if (!confirmDelete("Delete content?", "Delete: " + safe(selected.getTitle(), "(untitled)") + "\nThis cannot be undone.")) return;

        cs.deleteContent(selected.getId());
        refreshFromDB();
        applyFilters();
        clearPreview();
        status("Deleted #" + selected.getId());
    }

    private Content showContentDialog(String title, Content existing) {
        Dialog<Content> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.initModality(Modality.APPLICATION_MODAL);

        DialogPane pane = dialog.getDialogPane();
        pane.getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
        pane.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/dashboard.css")).toExternalForm());
        pane.getStyleClass().add("admin-dialog");

        TextField tfTitle = new TextField(existing == null ? "" : safe(existing.getTitle()));
        TextArea taDesc = new TextArea(existing == null ? "" : safe(existing.getDescription()));
        taDesc.setWrapText(true);
        taDesc.setPrefRowCount(4);

        ComboBox<String> cbType = new ComboBox<>(FXCollections.observableArrayList("Article", "Video", "Podcast"));
        cbType.setValue(existing == null ? "Article" : normalizeType(safe(existing.getType())).isBlank() ? "Article" : normalizeType(safe(existing.getType())));

        // categories from existing posts, editable
        Set<String> cats = master.stream()
                .map(c -> safe(c.getCategory()).trim())
                .filter(s -> !s.isBlank())
                .collect(Collectors.toCollection(TreeSet::new));
        ComboBox<String> cbCategory = new ComboBox<>(FXCollections.observableArrayList(new ArrayList<>(cats)));
        cbCategory.setEditable(true);
        cbCategory.setPromptText("Choose or type a category");
        cbCategory.setValue(existing == null ? "" : safe(existing.getCategory(), ""));

        TextField tfSource = new TextField(existing == null ? "" : safe(existing.getSourceUrl()));
        tfSource.setPromptText("https://...");

        TextField tfImage = new TextField(existing == null ? "" : safe(existing.getImageUrl()));
        tfImage.setPromptText("http(s)://... or file path");

        // two-line layout (not too wide)
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(16));

        // row 0
        grid.add(label("Title"), 0, 0);  grid.add(tfTitle, 1, 0, 3, 1);

        // row 1
        grid.add(label("Type"), 0, 1);       grid.add(cbType, 1, 1);
        grid.add(label("Category"), 2, 1);   grid.add(cbCategory, 3, 1);

        // row 2
        grid.add(label("Source URL"), 0, 2); grid.add(tfSource, 1, 2, 3, 1);

        // row 3
        grid.add(label("Image URL/Path"), 0, 3); grid.add(tfImage, 1, 3, 3, 1);

        // row 4
        grid.add(label("Description"), 0, 4); grid.add(taDesc, 1, 4, 3, 1);

        tfTitle.setPrefWidth(480);
        tfSource.setPrefWidth(480);
        tfImage.setPrefWidth(480);
        cbType.setPrefWidth(180);
        cbCategory.setPrefWidth(240);

        pane.setContent(grid);

        Button okBtn = (Button) pane.lookupButton(ButtonType.OK);
        okBtn.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            String errors = validate(tfTitle.getText(), taDesc.getText(), cbType.getValue(), cbCategory.getEditor().getText(), tfSource.getText(), tfImage.getText());
            if (!errors.isBlank()) {
                ev.consume();
                alert(Alert.AlertType.WARNING, "Fix input", errors);
            }
        });

        dialog.setResultConverter(bt -> {
            if (bt != ButtonType.OK) return null;
            String category = cbCategory.isEditable() ? cbCategory.getEditor().getText() : cbCategory.getValue();

            return new Content(
                    tfTitle.getText().trim(),
                    taDesc.getText().trim(),
                    normalizeType(cbType.getValue()),
                    tfSource.getText().trim(),
                    tfImage.getText().trim(),
                    category == null ? "" : category.trim()
            );
        });

        return dialog.showAndWait().orElse(null);
    }

    private String validate(String title, String desc, String type, String category, String source, String image) {
        StringBuilder sb = new StringBuilder();
        if (title == null || title.trim().isEmpty()) sb.append("• Title is required\n");
        if (desc == null || desc.trim().isEmpty()) sb.append("• Description is required\n");
        if (type == null || type.trim().isEmpty()) sb.append("• Type is required\n");
        if (category == null || category.trim().isEmpty()) sb.append("• Category is required\n");

        if (source != null && !source.trim().isEmpty()) {
            String s = source.trim();
            if (!(s.startsWith("http://") || s.startsWith("https://"))) {
                sb.append("• Source URL must start with http:// or https://\n");
            }
            if (s.length() > 1024) sb.append("• Source URL is too long (max 1024)\n");
        }

        if (image != null && !image.trim().isEmpty()) {
            String s = image.trim();
            if (s.length() > 2048) sb.append("• Image URL/path is too long (max 2048)\n");
            boolean ok = s.startsWith("http://") || s.startsWith("https://") || s.startsWith("file:")
                    || s.contains(":\\") || s.contains("/");
            if (!ok) sb.append("• Image URL/path looks invalid\n");

            // quick precheck for http(s)
            if (s.startsWith("http://") || s.startsWith("https://")) {
                Image test = ImageUtil.loadSmartImage(s, 10, 10);
                if (test == null || test.isError()) {
                    sb.append("• Image URL can't be loaded (check link)\n");
                }
            }
        }

        return sb.toString().trim();
    }

    // -------------------- Comments (Admin) --------------------

    private void setupCommentsList() {
        if (commentsList == null) return;

        commentsList.setCellFactory(lv -> new ListCell<>() {
            private final Label text = new Label();
            private final Label meta = new Label();
            private final Button del = new Button("Delete");
            private final VBox left = new VBox(3);
            private final Region spacer = new Region();
            private final HBox row = new HBox(10);

            {
                text.setWrapText(true);
                text.setStyle("-fx-font-weight: 600; -fx-text-fill: #111827;");
                meta.setStyle("-fx-text-fill:#6b7280; -fx-font-size: 11px;");
                del.getStyleClass().add("danger-pill");

                left.getChildren().addAll(text, meta);

                HBox.setHgrow(spacer, Priority.ALWAYS);
                row.setAlignment(Pos.TOP_LEFT);
                row.getChildren().addAll(left, spacer, del);
                row.setPadding(new Insets(8));
                row.getStyleClass().add("comment-row");
            }

            @Override
            protected void updateItem(Comment c, boolean empty) {
                super.updateItem(c, empty);

                if (empty || c == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                String body = safe(c.getText());
                if (body.isBlank()) body = "(no text)";
                text.setText(body);

                String when = c.getCreatedAt() == null ? "" : c.getCreatedAt().toString();
                meta.setText("id #" + c.getId() + (when.isBlank() ? "" : " • " + when));

                del.setOnAction(e -> {
                    if (!confirmDelete("Delete comment?", "This will remove the comment permanently.")) return;

                    try {
                        commentService.deleteComment(c.getId());
                        Content selected = contentTable.getSelectionModel().getSelectedItem();
                        if (selected != null) loadComments(selected.getId());
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        alert(Alert.AlertType.ERROR, "Delete failed", ex.getMessage());
                    }
                });

                setGraphic(row);
                setText(null);
            }
        });
    }

    private void loadComments(int contentId) {
        if (commentsList == null) return;

        List<Comment> comments = commentService.getCommentsByContentId(contentId);
        commentsList.setItems(FXCollections.observableArrayList(comments));
        if (commentsHint != null) commentsHint.setText(comments.isEmpty() ? "No comments yet." : ("Total: " + comments.size()));
    }

    private void clearComments() {
        if (commentsList != null) commentsList.setItems(FXCollections.observableArrayList());
        if (commentsHint != null) commentsHint.setText("Select a post to view comments.");
    }

    // -------------------- Actions --------------------

    @FXML
    public void openSelectedLink() {
        Content selected = contentTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            alert(Alert.AlertType.INFORMATION, "Nothing selected", "Select a post first.");
            return;
        }
        String url = safe(selected.getSourceUrl()).trim();
        if (url.isBlank()) {
            alert(Alert.AlertType.INFORMATION, "No link", "This content has no source URL.");
            return;
        }
        try {
            Desktop.getDesktop().browse(new URI(url));
        } catch (Exception e) {
            e.printStackTrace();
            alert(Alert.AlertType.ERROR, "Could not open link", e.getMessage());
        }
    }

    @FXML
    public void goToClient() {
        try {
            Stage stage = (Stage) contentTable.getScene().getWindow();
            Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/fxml/ContentDashboard.fxml")));
            Scene scene = new Scene(root);
            var css = getClass().getResource("/css/dashboard.css");
            if (css != null) scene.getStylesheets().add(css.toExternalForm());
            stage.setScene(scene);
            stage.setTitle("MindNest");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            alert(Alert.AlertType.ERROR, "Navigation error", e.getMessage());
        }
    }

    // -------------------- Helpers --------------------

    private void status(String s) {
        if (statusLabel != null) statusLabel.setText(s == null ? "" : s);
    }

    private Label label(String s) {
        Label l = new Label(s);
        l.getStyleClass().add("form-label");
        return l;
    }

    private void alert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private boolean confirmDelete(String title, String msg) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle(title);
        confirm.setHeaderText(null);
        confirm.setContentText(msg);
        Optional<ButtonType> r = confirm.showAndWait();
        return r.isPresent() && r.get() == ButtonType.OK;
    }

    private String safe(String s) { return s == null ? "" : s; }
    private String safe(String s, String fallback) { return (s == null || s.isBlank()) ? fallback : s; }

    private String trim(String s, int max) {
        if (s == null) return "";
        if (s.length() <= max) return s;
        return s.substring(0, Math.max(0, max - 1)) + "…";
    }

    private String normalizeType(String raw) {
        String t = safe(raw).trim().toLowerCase();
        if (t.isEmpty()) return "";
        if (t.contains("article") || t.contains("text") || t.equals("blog") || t.equals("post")) return "Article";
        if (t.contains("video")) return "Video";
        if (t.contains("podcast") || t.contains("audio")) return "Podcast";
        return raw.trim();
    }
}
