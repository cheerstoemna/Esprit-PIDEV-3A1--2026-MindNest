package controllers;

import db.DBConnection;
import models.AppUser;
import models.Role;
import utils.UserSession;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.*;
import java.util.regex.Pattern;

public class LoginController {

    @FXML private TextField     emailField;
    @FXML private PasswordField passwordField;

    // Email regex: must contain @ and a dot after @
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    @FXML
    void handleLogin() {
        String email    = emailField.getText().trim();
        String password = passwordField.getText();

        // ── Validation ───────────────────────────────────────────────
        if (email.isEmpty() || password.isEmpty()) {
            showAlert("Please fill in all fields.");
            return;
        }

        if (!isValidEmail(email)) {
            showAlert("Invalid email format.\nEmail must contain '@' and a domain (e.g. user@example.com)");
            highlightError(emailField);
            return;
        }

        // ── Reset field style if valid ────────────────────────────────
        resetStyle(emailField);

        // ── Database check ────────────────────────────────────────────
        String sql = """
            SELECT id, name, email, role
            FROM users
            WHERE email=? AND password=? AND status=1
        """;

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, email);
            ps.setString(2, password);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String dbRole = rs.getString("role");
                Role appRole = switch (dbRole) {
                    case "ADMIN"     -> Role.ADMIN;
                    case "THERAPIST" -> Role.THERAPIST;
                    default          -> Role.CLIENT;
                };

                AppUser user = new AppUser(
                        rs.getInt("id"),
                        rs.getString("name"),
                        appRole
                );
                UserSession.get().setUser(user);

                redirectToTherapyModule();

            } else {
                showAlert("Invalid email or password.");
                highlightError(emailField);
                highlightError(passwordField);
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Database error: " + e.getMessage());
        }
    }

    private void redirectToTherapyModule() throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/TherapyLayout.fxml"));
        Scene scene = new Scene(loader.load(), 1200, 720);

        Stage stage = (Stage) emailField.getScene().getWindow();
        stage.setScene(scene);
        stage.setTitle("MindNest");
    }

    // ── Validation Helpers ────────────────────────────────────────────

    private boolean isValidEmail(String email) {
        return EMAIL_PATTERN.matcher(email).matches();
    }

    private void highlightError(TextField field) {
        field.setStyle("-fx-border-color: #e74c3c; -fx-border-width: 2; -fx-border-radius: 6;");
    }

    private void resetStyle(TextField field) {
        field.setStyle("");
    }

    private void showAlert(String msg) {
        new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK).showAndWait();
    }

    public void goSignup(ActionEvent e) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/signup.fxml"));
        Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
    }
}