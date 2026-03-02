package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import models.Journal;
import services.JournalService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class JournalController {

    @FXML private TableView<Journal> journalTable;
    @FXML private TableColumn<Journal, String> titleCol;
    @FXML private TableColumn<Journal, String> contentCol;
    @FXML private TableColumn<Journal, String> moodCol;
    @FXML private TableColumn<Journal, LocalDateTime> dateCol;

    @FXML private TextField titleField;
    @FXML private TextArea contentField;
    @FXML private TextField moodField;

    @FXML private Button addBtn;
    @FXML private Button updateBtn;
    @FXML private Button deleteBtn;

    private JournalService journalService;
    private ObservableList<Journal> journalList;

    @FXML
    public void initialize() {
        journalService = new JournalService();
        journalList = FXCollections.observableArrayList(journalService.getAll());
        journalTable.setItems(journalList);

        // Map table columns
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        contentCol.setCellValueFactory(new PropertyValueFactory<>("content"));
        moodCol.setCellValueFactory(new PropertyValueFactory<>("mood"));
        dateCol.setCellValueFactory(new PropertyValueFactory<>("date"));

        // Format date nicely
        dateCol.setCellFactory(column -> new TableCell<Journal, LocalDateTime>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            }
        });

        // Buttons
        addBtn.setOnAction(e -> addJournal());
        updateBtn.setOnAction(e -> updateJournal());
        deleteBtn.setOnAction(e -> deleteJournal());

        // When a row is selected, populate fields
        journalTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                titleField.setText(newSel.getTitle());
                contentField.setText(newSel.getContent());
                moodField.setText(newSel.getMood());
            }
        });
    }

    private void addJournal() {
        Journal j = new Journal(); // Make sure Journal has a no-arg constructor
        j.setTitle(titleField.getText());
        j.setContent(contentField.getText());
        j.setMood(moodField.getText());
        j.setDate(LocalDateTime.now());

        journalService.add(j);
        refreshTable();
        clearFields();
    }

    private void updateJournal() {
        Journal selected = journalTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            selected.setTitle(titleField.getText());
            selected.setContent(contentField.getText());
            selected.setMood(moodField.getText());
            journalService.update(selected);
            refreshTable();
            clearFields();
        }
    }

    private void deleteJournal() {
        Journal selected = journalTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            journalService.delete(selected.getId());
            refreshTable();
            clearFields();
        }
    }

    private void refreshTable() {
        journalList.setAll(journalService.getAll());
    }

    private void clearFields() {
        titleField.clear();
        contentField.clear();
        moodField.clear();
    }
}
