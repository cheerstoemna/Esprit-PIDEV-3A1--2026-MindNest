package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import models.Role;
import utils.UserSession;

import java.io.IOException;

public class TherapyLayoutController {

    @FXML private StackPane contentHost;
    @FXML private Label     userLabel;
    @FXML private Button    btnDarkMode;

    @FXML private VBox   patientSection;
    @FXML private Button btnClient;
    @FXML private Button btnJournal;
    @FXML private Button btnClientContent;
    @FXML private Button btnCoaching;
    @FXML private Button btnProfile;

    @FXML private VBox   adminSection;
    @FXML private Button btnUserDashboard; // ← NEW
    @FXML private Button btnAdmin;
    @FXML private Button btnAdminContent;

    private boolean darkMode   = false;
    private String  darkCssUrl = null;
    private Parent  currentView = null;

    @FXML
    public void initialize() {
        var darkRes = getClass().getResource("/css/therapy-dark.css");
        if (darkRes != null) darkCssUrl = darkRes.toExternalForm();

        var user = UserSession.get().getUser();
        userLabel.setText("User: " + user.getName());
        setupRoleBasedUI(user.getRole());

        // Admin lands on User Dashboard by default
        if (user.getRole() == Role.ADMIN) openUserDashboard();
        else                              openClient();
    }

    // ── DARK MODE ─────────────────────────────────────────────────────
    @FXML
    private void toggleTheme() {
        if (contentHost == null || darkCssUrl == null) return;
        javafx.scene.Scene scene = contentHost.getScene();
        if (scene == null) return;

        darkMode = !darkMode;

        if (darkMode) {
            forceDark(scene.getStylesheets());
            if (currentView != null) forceDark(currentView.getStylesheets());
            if (btnDarkMode != null) btnDarkMode.setText("☀  Light");
        } else {
            forceLight(scene.getStylesheets());
            if (currentView != null) forceLight(currentView.getStylesheets());
            if (btnDarkMode != null) btnDarkMode.setText("🌙 Dark");
        }
    }

    private void forceDark(java.util.List<String> sheets) {
        if (sheets != null && !sheets.contains(darkCssUrl)) sheets.add(darkCssUrl);
    }

    private void forceLight(java.util.List<String> sheets) {
        if (sheets != null) sheets.remove(darkCssUrl);
    }

    // ── ROLE-BASED SIDEBAR ────────────────────────────────────────────
    private void setupRoleBasedUI(Role role) {
        boolean admin = (role == Role.ADMIN);
        setSection(patientSection, !admin);
        setSection(adminSection,    admin);
    }

    private void setSection(VBox box, boolean visible) {
        if (box != null) { box.setVisible(visible); box.setManaged(visible); }
    }

    // ── NAVIGATION ────────────────────────────────────────────────────

    /** ✅ NEW — Opens the User Dashboard with charts, filters, export */
    @FXML
    public void openUserDashboard() {
        if (!isAdmin()) return;
        loadIntoHost("/fxml/UserDashboard.fxml");
        setActive(btnUserDashboard);
    }

    @FXML private void openClient() {
        if (isAdmin()) return;
        loadIntoHost("/fxml/TherapyClientDashboard.fxml"); setActive(btnClient);
    }
    @FXML private void openJournal() {
        if (isAdmin()) return;
        loadIntoHost("/fxml/main.fxml"); setActive(btnJournal);
    }
    @FXML private void openContent() {
        if (isAdmin()) return;
        loadIntoHost("/fxml/ContentDashboard.fxml"); setActive(btnClientContent);
    }
    @FXML private void openCoaching() {
        if (isAdmin()) return;
        loadIntoHost("/fxml/dashboard.fxml"); setActive(btnCoaching);
    }
    @FXML private void openProfile() {
        if (isAdmin()) return;
        loadIntoHost("/fxml/profile.fxml"); setActive(btnProfile);
    }
    @FXML private void openAdmin() {
        if (!isAdmin()) return;
        loadIntoHost("/fxml/TherapyAdminDashboard.fxml"); setActive(btnAdmin);
    }
    @FXML private void openAdminContent() {
        if (!isAdmin()) return;
        loadIntoHost("/fxml/AdminDashboard.fxml"); setActive(btnAdminContent);
    }

    @FXML private void handleLogout() {
        try {
            UserSession.get().clear();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            ((Stage) userLabel.getScene().getWindow()).setScene(new Scene(loader.load()));
        } catch (IOException e) { e.printStackTrace(); }
    }

    private boolean isAdmin() {
        return UserSession.get().getUser().getRole() == Role.ADMIN;
    }

    private void loadIntoHost(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent view = loader.load();
            currentView = view;
            if (darkMode) forceDark(view.getStylesheets());
            contentHost.getChildren().setAll(view);
        } catch (IOException e) {
            System.err.println("❌ Failed to load: " + fxmlPath);
            e.printStackTrace();
        }
    }

    private void setActive(Button active) {
        for (Button b : new Button[]{btnClient, btnJournal, btnClientContent,
                btnCoaching, btnProfile, btnUserDashboard, btnAdmin, btnAdminContent}) {
            if (b != null) b.getStyleClass().remove("sidebar-nav-btn-active");
        }
        if (active != null && !active.getStyleClass().contains("sidebar-nav-btn-active"))
            active.getStyleClass().add("sidebar-nav-btn-active");
    }
}