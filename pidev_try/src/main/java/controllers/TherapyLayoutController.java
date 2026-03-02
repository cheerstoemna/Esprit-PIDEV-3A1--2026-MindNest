package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import models.Role;
import utils.UserSession;

import java.io.IOException;

public class TherapyLayoutController {

    @FXML private BorderPane rootPane;
    @FXML private Label      userLabel;
    @FXML private Label      themeLabel;
    @FXML private Button     btnTheme;
    @FXML private Button     btnClient;
    @FXML private Button     btnAdmin;
    @FXML private StackPane  contentHost;

    private boolean darkMode    = false;
    private Parent  currentView = null;

    // Dark palette
    private static final String BG_DEEP   = "#1a1a2e";
    private static final String BG_CARD   = "#162032";
    private static final String BG_PANEL  = "#0d1b2a";
    private static final String BORDER    = "#1e3a56";

    @FXML
    public void initialize() {
        boolean isAdmin = UserSession.get().role() == Role.ADMIN;
        userLabel.setText(
                (UserSession.get().user() == null ? "Guest" : UserSession.get().user().getFullName())
                        + " • " + UserSession.get().role()
        );
        btnAdmin.setVisible(isAdmin);
        btnAdmin.setManaged(isAdmin);

        if (isAdmin) load("/fxml/TherapyAdminDashboard.fxml");
        else         load("/fxml/TherapyClientDashboard.fxml");
    }

    @FXML private void openClient() { load("/fxml/TherapyClientDashboard.fxml"); }
    @FXML private void openAdmin()  { load("/fxml/TherapyAdminDashboard.fxml");  }

    @FXML
    private void toggleTheme() {
        darkMode = !darkMode;
        applyTheme();
    }

    private void applyTheme() {
        String darkUrl  = getClass().getResource("/css/therapy-dark.css").toExternalForm();
        String lightUrl = getClass().getResource("/css/therapy.css").toExternalForm();

        // 1 — swap on rootPane itself (FXML declares stylesheet here, not on scene)
        var rootSheets = rootPane.getStylesheets();
        if (darkMode) {
            rootSheets.removeIf(s -> s.contains("therapy.css") && !s.contains("dark"));
            if (!rootSheets.contains(darkUrl)) rootSheets.add(darkUrl);
        } else {
            rootSheets.removeIf(s -> s.contains("therapy-dark.css"));
            if (!rootSheets.contains(lightUrl)) rootSheets.add(lightUrl);
        }

        // 2 — swap stylesheets on scene
        var sceneSheets = rootPane.getScene().getStylesheets();
        if (darkMode) {
            sceneSheets.removeIf(s -> s.contains("therapy.css") && !s.contains("dark"));
            if (!sceneSheets.contains(darkUrl)) sceneSheets.add(darkUrl);
        } else {
            sceneSheets.removeIf(s -> s.contains("therapy-dark.css"));
            if (!sceneSheets.contains(lightUrl)) sceneSheets.add(lightUrl);
        }

        // 3 — swap stylesheets on the inner loaded view
        if (currentView != null) {
            var viewSheets = currentView.getStylesheets();
            if (darkMode) {
                viewSheets.removeIf(s -> s.contains("therapy.css") && !s.contains("dark"));
                if (!viewSheets.contains(darkUrl)) viewSheets.add(darkUrl);
            } else {
                viewSheets.removeIf(s -> s.contains("therapy-dark.css"));
                if (!viewSheets.contains(lightUrl)) viewSheets.add(lightUrl);
            }
        }

        // 4 — walk the ENTIRE scene graph and override every inline background
        walkAndRecolor(rootPane);

        // 4 — update button/label text
        if (darkMode) {
            btnTheme.setText("☀ Light");
            themeLabel.setText("Theme: Dark");
        } else {
            btnTheme.setText("🌙 Dark");
            themeLabel.setText("Theme: Calm Light");
        }
    }

    /**
     * Recursively visits every node.
     * If it has an inline background-color style, we override it.
     */
    private void walkAndRecolor(Node node) {
        if (node == null) return;

        if (darkMode) {
            // Force backgrounds dark on container nodes
            if (node instanceof VBox || node instanceof HBox ||
                    node instanceof AnchorPane || node instanceof BorderPane ||
                    node instanceof StackPane || node instanceof GridPane ||
                    node instanceof Pane) {

                String current = node.getStyle();

                // If it has an inline background set, override it
                if (current != null && current.contains("-fx-background-color")) {
                    // Preserve other inline styles, just swap the color
                    String updated = current
                            .replaceAll("-fx-background-color\\s*:[^;]+;?", "")
                            .trim();

                    // Decide shade based on role in layout
                    String newBg = BG_CARD;
                    if (node instanceof HBox && ((HBox)node).getStyleClass().contains("topbar")) {
                        newBg = BG_PANEL;
                    }
                    node.setStyle(updated + " -fx-background-color:" + newBg + ";");
                }
            }
        } else {
            // Light mode: remove our dark override if we added one
            String current = node.getStyle();
            if (current != null && (
                    current.contains(BG_DEEP) ||
                            current.contains(BG_CARD) ||
                            current.contains(BG_PANEL))) {
                // Remove our injected dark bg — let CSS take over
                node.setStyle(current
                        .replaceAll("-fx-background-color\\s*:" + BG_DEEP + ";?", "")
                        .replaceAll("-fx-background-color\\s*:" + BG_CARD + ";?", "")
                        .replaceAll("-fx-background-color\\s*:" + BG_PANEL + ";?", "")
                        .trim()
                );
            }
        }

        // Recurse into children
        if (node instanceof Parent p) {
            for (Node child : p.getChildrenUnmodifiable()) {
                walkAndRecolor(child);
            }
        }
    }

    private void load(String path) {
        try {
            Parent view = FXMLLoader.load(getClass().getResource(path));
            currentView = view;
            contentHost.getChildren().setAll(view);
            if (rootPane.getScene() != null) applyTheme();
        } catch (IOException e) {
            throw new RuntimeException("Cannot load: " + path, e);
        }
    }
}










