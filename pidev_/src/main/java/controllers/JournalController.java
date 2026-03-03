package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyEvent;
import javafx.stage.Popup;
import javafx.geometry.Point2D;
import models.Journal;
import models.CompletionData;
import services.JournalService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import services.GeminiPro;

public class JournalController {

    @FXML private TableView<Journal> journalTable;
    @FXML private TableColumn<Journal, Integer> idCol;
    @FXML private TableColumn<Journal, String> titleCol;
    @FXML private TableColumn<Journal, String> contentCol;
    @FXML private TableColumn<Journal, String> moodCol;
    @FXML private TableColumn<Journal, LocalDateTime> dateCol;
    @FXML private TextField searchField;
    @FXML private TextField titleField;
    @FXML private TextArea journalTextArea;
    @FXML private TextField moodField;

    @FXML private Button addBtn;
    @FXML private Button updateBtn;
    @FXML private Button deleteBtn;
    @FXML private Button sortDateBtn;
    private JournalService journalService;
    private ObservableList<Journal> journalList;
    private GeminiPro geminiPro = new GeminiPro();
    // Autocomplete popup
    private Popup suggestionPopup;
    private ListView<String> suggestionList;

    @FXML
    public void initialize() {
        sortDateBtn.setOnAction(e -> {
            journalTable.getSortOrder().clear();
            dateCol.setSortType(TableColumn.SortType.DESCENDING); // or ASCENDING
            journalTable.getSortOrder().add(dateCol);
        });
        // Initialize journal service and table
        journalService = new JournalService();
        journalList = FXCollections.observableArrayList(journalService.getAll());
        journalTable.setItems(journalList);
        searchField.textProperty().addListener((obs, oldText, newText) -> filterJournals(newText));
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        contentCol.setCellValueFactory(new PropertyValueFactory<>("content"));
        moodCol.setCellValueFactory(new PropertyValueFactory<>("mood"));
        dateCol.setCellValueFactory(new PropertyValueFactory<>("date"));
        dateCol.setCellFactory(column -> new TableCell<Journal, LocalDateTime>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" :
                        item.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            }
        });

        // Initialize buttons
        addBtn.setOnAction(e -> addJournal());
        updateBtn.setOnAction(e -> updateJournal());
        deleteBtn.setOnAction(e -> deleteJournal());

        // Table selection
        journalTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                titleField.setText(newSel.getTitle());
                journalTextArea.setText(newSel.getContent());
                moodField.setText(newSel.getMood());
            }
        });

        // --- Autocomplete setup ---
        suggestionList = new ListView<>();
        suggestionList.setPrefWidth(400);
        suggestionList.setPrefHeight(120);
        suggestionPopup = new Popup();
        suggestionPopup.getContent().add(suggestionList);
        suggestionPopup.setAutoHide(true);

        journalTextArea.addEventFilter(KeyEvent.KEY_RELEASED, this::onKeyTyped);

        suggestionList.setOnMouseClicked(e -> {
            String selected = suggestionList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                insertCompletion(selected);
                suggestionPopup.hide();
            }
        });
    }
    private void filterJournals(String query) {
        if (query == null || query.isEmpty()) {
            journalTable.setItems(journalList); // show all if empty
            return;
        }

        String lowerQuery = query.toLowerCase();

        ObservableList<Journal> filtered = FXCollections.observableArrayList();
        for (Journal j : journalList) {
            if (j.getTitle().toLowerCase().contains(lowerQuery) ||
                    j.getContent().toLowerCase().contains(lowerQuery) ||
                    j.getMood().toLowerCase().contains(lowerQuery)) {
                filtered.add(j);
            }
        }

        journalTable.setItems(filtered);
    }
    // =================== CRUD ===================
    private void addJournal() {
        if (!validateInput()) return;

        Journal j = new Journal();
        j.setTitle(titleField.getText().trim());
        j.setContent(journalTextArea.getText().trim());
        j.setMood(moodField.getText().trim());
        j.setAiAnalysis("");
        j.setDate(LocalDateTime.now());

        journalService.add(j);
        refreshTable();
        clearFields();
        showInfo("Journal added successfully.");
    }

    private void updateJournal() {
        Journal selected = journalTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showWarning("Please select a journal to update."); return; }
        if (!validateInput()) return;

        selected.setTitle(titleField.getText().trim());
        selected.setContent(journalTextArea.getText().trim());
        selected.setMood(moodField.getText().trim());

        journalService.update(selected);
        refreshTable();
        clearFields();
        showInfo("Journal updated successfully.");
    }

    private void deleteJournal() {
        Journal selected = journalTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showWarning("Please select a journal to delete."); return; }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Confirmation");
        confirm.setHeaderText("Are you sure you want to delete this journal?");
        confirm.setContentText("Title: " + selected.getTitle());

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                journalService.delete(selected.getId());
                refreshTable();
                clearFields();
                showInfo("Journal deleted successfully.");
            }
        });
    }

    private boolean validateInput() {
        String title = titleField.getText().trim();
        String content = journalTextArea.getText().trim();

        if (title.isEmpty() || content.isEmpty()) { showWarning("Title and Content are required."); return false; }
        if (title.length() < 3) { showWarning("Title must be at least 3 characters."); return false; }
        if (content.length() < 5) { showWarning("Content must be at least 5 characters."); return false; }
        return true;
    }

    private void refreshTable() { journalList.setAll(journalService.getAll()); }

    private void clearFields() {
        titleField.clear();
        journalTextArea.clear();
        moodField.clear();
        journalTable.getSelectionModel().clearSelection();
        suggestionPopup.hide();
    }

    private void showWarning(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Warning"); alert.setHeaderText(null); alert.setContentText(msg); alert.showAndWait();
    }

    private void showInfo(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Info"); alert.setHeaderText(null); alert.setContentText(msg); alert.showAndWait();
    }

    // =================== Autocomplete ===================
    private void onKeyTyped(KeyEvent event) {
        String text = journalTextArea.getText();
        String lastWords = getLastFewWords(text, 2);
        List<String> suggestions = CompletionData.getSuggestions(lastWords);

        if (suggestions.isEmpty()) {
            suggestionPopup.hide();
        } else {
            suggestionList.setItems(FXCollections.observableArrayList(suggestions));

            Point2D p = journalTextArea.localToScreen(0, journalTextArea.getHeight());
            suggestionPopup.show(journalTextArea, p.getX(), p.getY());
        }
    }

    private String getLastFewWords(String text, int count) {
        String[] words = text.split("\\s+");
        if (words.length == 0) return "";
        int start = Math.max(words.length - count, 0);
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < words.length; i++) {
            sb.append(words[i]);
            if (i < words.length - 1) sb.append(" ");
        }
        return sb.toString();
    }

    private void insertCompletion(String completion) {
        String text = journalTextArea.getText();
        String[] words = text.split("\\s+");
        int remove = Math.min(2, words.length);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < words.length - remove; i++) {
            sb.append(words[i]).append(" ");
        }
        sb.append(completion).append(" ");
        journalTextArea.setText(sb.toString());
        journalTextArea.positionCaret(sb.length());
    }

    @FXML
    private void saveJournalEntry() {
        System.out.println("Saved: " + journalTextArea.getText());
    }
    // =================== AI Mood Analysis ===================
    @FXML
    private void analyzeMood() {
        String content = journalTextArea.getText().trim();
        if (content.isEmpty()) {
            showWarning("Write journal content first.");
            return;
        }

        String prompt = "Analyze this journal entry. Summarize the mood and give a short comforting advice. " +
                "Do NOT chat or ask questions, only analyze and give one advice sentence:\n\n" + content;

        // GeminiPro service call
        String result = geminiPro.analyzeText(prompt);

        TextArea textArea = new TextArea(result);
        textArea.setWrapText(true);
        textArea.setEditable(false);
        textArea.setPrefWidth(400);
        textArea.setPrefHeight(250);

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("AI Analysis");
        alert.setHeaderText("Your Journal Analysis");
        alert.getDialogPane().setContent(textArea);
        alert.showAndWait();
    }

}