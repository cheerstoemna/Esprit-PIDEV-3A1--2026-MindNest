package app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        // Real login system
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/login.fxml"));
        stage.setScene(new Scene(loader.load()));
        stage.setTitle("MindNest");
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}