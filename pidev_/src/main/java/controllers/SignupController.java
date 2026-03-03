package controllers;

import db.DBConnection;
import models.AppUser;
import models.Role;
import services.EmailValidationService;
import utils.UserSession;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.*;
import java.util.Objects;
import java.util.regex.Pattern;

public class SignupController {

    @FXML private TextField        nameField;
    @FXML private TextField        emailField;
    @FXML private PasswordField    passwordField;
    @FXML private PasswordField    confirmField;
    @FXML private TextField        ageField;
    @FXML private ComboBox<String> genderBox;
    @FXML private ComboBox<String> roleBox;

    // Inline status label — shown below the email field
    // (we add this to the FXML too — see signup.fxml update)
    @FXML private Label emailStatusLabel;

    // Signup button reference to disable during validation
    @FXML private Button signupButton;

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    private final EmailValidationService emailValidator = new EmailValidationService();

    @FXML
    void handleSignup() {

        // ── Step 1: Check empty fields ────────────────────────────────
        if (anyEmpty()) {
            showAlert("Please fill in all fields.");
            return;
        }

        // ── Step 2: Validate email format ─────────────────────────────
        String email = emailField.getText().trim();
        if (!isValidEmail(email)) {
            showAlert("Invalid email format.\nEmail must be like: user@example.com");
            highlightError(emailField);
            return;
        }
        resetStyle(emailField);

        // ── Step 3: Check password match ──────────────────────────────
        if (!passwordField.getText().equals(confirmField.getText())) {
            showAlert("Passwords do not match.");
            highlightError(passwordField);
            highlightError(confirmField);
            return;
        }
        resetStyle(passwordField);
        resetStyle(confirmField);

        // ── Step 4: Validate age ──────────────────────────────────────
        int age;
        try {
            age = Integer.parseInt(ageField.getText().trim());
            if (age < 1 || age > 120) {
                showAlert("Please enter a valid age (1-120).");
                highlightError(ageField);
                return;
            }
            resetStyle(ageField);
        } catch (NumberFormatException e) {
            showAlert("Age must be a number.");
            highlightError(ageField);
            return;
        }

        // ── Step 5: Validate password length ──────────────────────────
        if (passwordField.getText().length() < 6) {
            showAlert("Password must be at least 6 characters.");
            highlightError(passwordField);
            return;
        }

        // ── Step 6: Validate name ─────────────────────────────────────
        if (nameField.getText().trim().length() < 2) {
            showAlert("Name must be at least 2 characters.");
            highlightError(nameField);
            return;
        }
        resetStyle(nameField);

        // ── Step 7: Email validation (DNS + AbstractAPI) ──────────────
        // Disable button and show loading status while validating
        signupButton.setDisable(true);
        signupButton.setText("Validating email...");
        showEmailStatus("⏳ Checking email address...", "#f39c12", true);

        final int finalAge = age;

        // Run on background thread — never block JavaFX thread
        new Thread(() -> {
            EmailValidationService.ValidationResult result =
                    emailValidator.validate(email);

            Platform.runLater(() -> {
                signupButton.setDisable(false);
                signupButton.setText("Create Account");

                if (!result.valid) {
                    // Email failed validation
                    showEmailStatus("✖ " + result.message, "#e74c3c", true);
                    highlightError(emailField);
                    return;
                }

                // Email is valid — show green checkmark and proceed
                showEmailStatus("✔ Email verified", "#2ecc71", true);
                resetStyle(emailField);

                // All validations passed → insert user
                insertUser(
                        nameField.getText().trim(),
                        email,
                        passwordField.getText(),
                        finalAge,
                        genderBox.getValue(),
                        roleBox.getValue()
                );
            });
        }).start();
    }

    private void insertUser(String name, String email, String pass,
                            int age, String gender, String dbRole) {
        String sql = """
            INSERT INTO users(name, email, password, age, gender, role)
            VALUES (?, ?, ?, ?, ?, ?)
        """;

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, name);
            ps.setString(2, email);
            ps.setString(3, pass);
            ps.setInt(4, age);
            ps.setString(5, gender);
            ps.setString(6, dbRole);
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                int userId = keys.getInt(1);
                Role appRole = switch (dbRole) {
                    case "ADMIN"     -> Role.ADMIN;
                    case "THERAPIST" -> Role.THERAPIST;
                    default          -> Role.CLIENT;
                };
                AppUser user = new AppUser(userId, name, appRole);
                UserSession.get().setUser(user);
            }

            showAlert("Account created! Welcome, " + name + " 🎉");
            goLogin(null);

        } catch (Exception e) {
            e.printStackTrace();
            if (e.getMessage().contains("Duplicate")) {
                showAlert("This email is already registered. Please use a different email.");
                highlightError(emailField);
                showEmailStatus("✖ Email already registered", "#e74c3c", true);
            } else {
                showAlert("Database error: " + e.getMessage());
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // UI Helpers
    // ─────────────────────────────────────────────────────────────────

    private void showEmailStatus(String msg, String color, boolean visible) {
        emailStatusLabel.setText(msg);
        emailStatusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " + color +
                "; -fx-font-family: 'Segoe UI';");
        emailStatusLabel.setVisible(visible);
        emailStatusLabel.setManaged(visible);
    }

    private boolean isValidEmail(String email) {
        return EMAIL_PATTERN.matcher(email).matches();
    }

    private boolean anyEmpty() {
        return nameField.getText().trim().isEmpty()
                || emailField.getText().trim().isEmpty()
                || passwordField.getText().isEmpty()
                || confirmField.getText().isEmpty()
                || ageField.getText().trim().isEmpty()
                || genderBox.getValue() == null
                || roleBox.getValue() == null;
    }

    private void highlightError(TextField field) {
        field.setStyle("-fx-border-color: #e74c3c; -fx-border-width: 2; -fx-border-radius: 6;");
    }

    private void resetStyle(TextField field) {
        field.setStyle("");
    }

    private void showAlert(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK).showAndWait();
    }

    public void goLogin(ActionEvent e) throws Exception {
        Parent root = FXMLLoader.load(
                Objects.requireNonNull(getClass().getResource("/fxml/login.fxml")));
        Stage stage = (Stage) nameField.getScene().getWindow();
        stage.setScene(new Scene(root));
    }
}