package test;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import models.AppUser;
import models.Role;
import utils.UserSession;

public class MainFX extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        Role role = Role.CLIENT;
        if (!getParameters().getRaw().isEmpty() && "admin".equalsIgnoreCase(getParameters().getRaw().get(0))) {
            role = Role.ADMIN;
        }

        UserSession.get().setUser(new AppUser(
                role == Role.ADMIN ? 999 : 1,
                role == Role.ADMIN ? "Admin" : "Client User",
                role
        ));

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/TherapyLayout.fxml"));
        Scene scene = new Scene(loader.load(), 1200, 720);

        stage.setTitle("MindNest • Therapy Module");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
