package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import models.Book;

import java.awt.Desktop;
import java.net.URI;
import java.util.List;

public class BookRecommendationController {

    @FXML
    private ListView<Book> bookListView;

    private List<Book> books;

    public void setBooks(List<Book> books) {
        this.books = books;
        bookListView.setItems(javafx.collections.FXCollections.observableArrayList(books));
        bookListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Book book, boolean empty) {
                super.updateItem(book, empty);
                if (empty || book == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Button viewBtn = new Button("View Book");
                    viewBtn.setOnAction(e -> {
                        try {
                            if (!book.getPreviewLink().equals("N/A") && !book.getPreviewLink().isEmpty()) {
                                Desktop.getDesktop().browse(new URI(book.getPreviewLink()));
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    });

                    VBox box = new VBox(5);
                    box.getChildren().addAll(
                            new javafx.scene.control.Label("Title: " + book.getTitle()),
                            new javafx.scene.control.Label("Author(s): " + book.getAuthors()),
                            viewBtn,
                            new javafx.scene.control.Separator()
                    );
                    setGraphic(box);
                }
            }
        });
    }

    @FXML
    private void closeWindow() {
        ((Stage) bookListView.getScene().getWindow()).close();
    }
}