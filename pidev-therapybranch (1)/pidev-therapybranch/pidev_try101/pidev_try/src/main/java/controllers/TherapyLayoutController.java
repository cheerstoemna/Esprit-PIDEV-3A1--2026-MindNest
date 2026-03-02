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
    @FXML private Label userLabel;

    // Patient buttons
    @FXML private VBox patientSection;
    @FXML private Button btnClient;
    @FXML private Button btnJournal;
    @FXML private Button btnClientContent;
    @FXML private Button btnCoaching;
    @FXML private Button btnProfile;

    // Admin section
    @FXML private VBox adminSection;
    @FXML private Button btnAdmin;
    @FXML private Button btnAdminContent;

    @FXML
    public void initialize() {
        var user = UserSession.get().getUser();

        // Display logged-in user's name
        userLabel.setText("User: " + user.getName());

        // Setup role-based visibility
        setupRoleBasedUI(user.getRole());

        // Load default screen based on role
        if (user.getRole() == Role.ADMIN) {
            openAdmin();
        } else {
            openClient();
        }
    }

    private void setupRoleBasedUI(Role role) {
        if (role == Role.ADMIN) {
            // ADMIN: Hide patient section, show admin section
            if (patientSection != null) {
                patientSection.setVisible(false);
                patientSection.setManaged(false);
            }
            if (adminSection != null) {
                adminSection.setVisible(true);
                adminSection.setManaged(true);
            }

        } else {
            // PATIENT/THERAPIST: Show patient section, hide admin section
            if (patientSection != null) {
                patientSection.setVisible(true);
                patientSection.setManaged(true);
            }
            if (adminSection != null) {
                adminSection.setVisible(false);
                adminSection.setManaged(false);
            }
        }
    }

    @FXML
    private void openClient() {
        // PATIENT ONLY
        if (UserSession.get().getUser().getRole() == Role.ADMIN) {
            System.out.println("⛔ Access denied: Patient feature only");
            return;
        }
        loadIntoHost("/fxml/TherapyClientDashboard.fxml");
        setActive(btnClient);
    }

    @FXML
    private void openJournal() {
        // PATIENT ONLY
        if (UserSession.get().getUser().getRole() == Role.ADMIN) {
            System.out.println("⛔ Access denied: Patient feature only");
            return;
        }
        loadIntoHost("/fxml/main.fxml");
        setActive(btnJournal);
    }

    @FXML
    private void openContent() {
        // PATIENT ONLY
        if (UserSession.get().getUser().getRole() == Role.ADMIN) {
            System.out.println("⛔ Access denied: Patient feature only");
            return;
        }
        loadIntoHost("/fxml/ContentDashboard.fxml");
        setActive(btnClientContent);
    }

    @FXML
    private void openCoaching() {
        // PATIENT ONLY
        if (UserSession.get().getUser().getRole() == Role.ADMIN) {
            System.out.println("⛔ Access denied: Patient feature only");
            return;
        }
        loadIntoHost("/fxml/dashboard.fxml");
        setActive(btnCoaching);
    }

    @FXML
    private void openProfile() {
        // PATIENT ONLY
        if (UserSession.get().getUser().getRole() == Role.ADMIN) {
            System.out.println("⛔ Access denied: Patient feature only");
            return;
        }
        loadIntoHost("/fxml/profile.fxml");
        setActive(btnProfile);
    }

    @FXML
    private void openAdmin() {
        // ADMIN ONLY - User Management
        if (UserSession.get().getUser().getRole() != Role.ADMIN) {
            System.out.println("⛔ Access denied: Admin only");
            return;
        }
        loadIntoHost("/fxml/TherapyAdminDashboard.fxml");
        setActive(btnAdmin);
    }

    @FXML
    private void openAdminContent() {
        // ADMIN ONLY - Manage Content
        if (UserSession.get().getUser().getRole() != Role.ADMIN) {
            System.out.println("⛔ Access denied: Admin only");
            return;
        }
        loadIntoHost("/fxml/AdminDashboard.fxml");
        setActive(btnAdminContent);
    }

    @FXML
    private void handleLogout() {
        try {
            UserSession.get().clear();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Scene scene = new Scene(loader.load());

            Stage stage = (Stage) userLabel.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("MindNest - Login");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadIntoHost(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent view = loader.load();
            contentHost.getChildren().setAll(view);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("❌ Failed to load: " + fxmlPath);
        }
    }

    private void setActive(Button activeButton) {
        if (btnClient != null) btnClient.getStyleClass().remove("nav-btn-active");
        if (btnJournal != null) btnJournal.getStyleClass().remove("nav-btn-active");
        if (btnClientContent != null) btnClientContent.getStyleClass().remove("nav-btn-active");
        if (btnCoaching != null) btnCoaching.getStyleClass().remove("nav-btn-active");
        if (btnProfile != null) btnProfile.getStyleClass().remove("nav-btn-active");
        if (btnAdmin != null) btnAdmin.getStyleClass().remove("nav-btn-active");
        if (btnAdminContent != null) btnAdminContent.getStyleClass().remove("nav-btn-active");

        if (activeButton != null && !activeButton.getStyleClass().contains("nav-btn-active")) {
            activeButton.getStyleClass().add("nav-btn-active");
        }
    }
}