package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.StackPane;
import javafx.scene.control.Button;
import javafx.scene.Parent;

public class MainController {

    @FXML
    private Button quizBtn;
    @FXML
    private Button journalBtn;
    @FXML
    private StackPane contentPane;

    @FXML
    public void initialize() {
        quizBtn.setOnAction(e -> loadContent("/fxml/quizz.fxml"));
        journalBtn.setOnAction(e -> loadContent("/fxml/journal.fxml"));

        // Load quiz by default
        loadContent("/fxml/quizz.fxml");
    }

    private void loadContent(String fxmlPath) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            contentPane.getChildren().clear();
            contentPane.getChildren().add(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
