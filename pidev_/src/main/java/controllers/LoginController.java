package controllers;

import db.DBConnection;
import models.AppUser;
import models.Role;
import services.CaptchaVerificationService;
import services.CaptchaServer;
import utils.UserSession;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.net.URL;
import java.sql.*;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

public class LoginController implements Initializable {

    @FXML private TextField     emailField;
    @FXML private PasswordField passwordField;
    @FXML private Button        openCaptchaBtn;
    @FXML private Label         captchaStatusLabel;
    @FXML private Label         errorLabel;
    @FXML private Button        loginButton;

    private String captchaToken = null;

    private final CaptchaVerificationService captchaService = new CaptchaVerificationService();

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Start the local HTTP server once on init
        try {
            CaptchaServer.start();
        } catch (Exception e) {
            System.err.println("[CAPTCHA] Failed to start server: " + e.getMessage());
        }
    }

    /**
     * Opens a modal popup with a full-size WebView loading the reCAPTCHA.
     * Triggered by the "I'm not a robot" button in the card.
     */
    @FXML
    private void handleOpenCaptcha() {
        // Create a brand new WebView for the popup — no sizing conflict
        WebView popupWebView = new WebView();
        popupWebView.setPrefSize(640, 740);
        popupWebView.setContextMenuEnabled(false);

        WebEngine engine = popupWebView.getEngine();

        // Enable JavaScript and fix image rendering in JavaFX 17 WebKit
        engine.setJavaScriptEnabled(true);

        // When page finishes loading, inject CSS to force images visible
        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                engine.executeScript(
                        "var s = document.createElement('style');" +
                                "s.innerHTML = 'img { display:block !important; visibility:visible !important; opacity:1 !important; } " +
                                ".rc-imageselect-tile { visibility:visible !important; opacity:1 !important; }';" +
                                "document.head.appendChild(s);"
                );
            }
        });

        // Load captcha served over localhost
        engine.load("http://localhost:8765/captcha");

        // JS → Java bridge inside the popup
        engine.setOnAlert(event -> {
            String msg = event.getData();

            if (msg.startsWith("CAPTCHA_TOKEN:")) {
                captchaToken = msg.substring("CAPTCHA_TOKEN:".length());
                Platform.runLater(() -> {
                    // Close the popup
                    Stage popup = (Stage) popupWebView.getScene().getWindow();
                    popup.close();

                    // Update the button to show verified state
                    openCaptchaBtn.setText("✔  Verified");
                    openCaptchaBtn.setStyle(
                            "-fx-background-color: #eafaf1;" +
                                    "-fx-border-color: #2ecc71;" +
                                    "-fx-border-radius: 6;" +
                                    "-fx-background-radius: 6;" +
                                    "-fx-font-size: 13px;" +
                                    "-fx-text-fill: #27ae60;" +
                                    "-fx-font-family: 'Segoe UI';" +
                                    "-fx-alignment: CENTER_LEFT;" +
                                    "-fx-padding: 0 12;"
                    );
                    openCaptchaBtn.setDisable(true);

                    captchaStatusLabel.setText("✔ CAPTCHA verified successfully");
                    captchaStatusLabel.setStyle(
                            "-fx-font-size: 11px; -fx-text-fill: #2ecc71; -fx-font-family: 'Segoe UI';"
                    );
                    captchaStatusLabel.setVisible(true);
                    captchaStatusLabel.setManaged(true);

                    // Unlock the login button
                    loginButton.setDisable(false);
                });

            } else if ("CAPTCHA_EXPIRED".equals(msg)) {
                Platform.runLater(() -> {
                    captchaToken = null;
                    loginButton.setDisable(true);
                    captchaStatusLabel.setText("⚠ CAPTCHA expired. Please verify again.");
                    captchaStatusLabel.setStyle(
                            "-fx-font-size: 11px; -fx-text-fill: #e67e22; -fx-font-family: 'Segoe UI';"
                    );
                    captchaStatusLabel.setVisible(true);
                    captchaStatusLabel.setManaged(true);
                });

            } else if ("CAPTCHA_ERROR".equals(msg)) {
                Platform.runLater(() -> {
                    captchaToken = null;
                    captchaStatusLabel.setText("✖ CAPTCHA error. Check your connection.");
                    captchaStatusLabel.setStyle(
                            "-fx-font-size: 11px; -fx-text-fill: #e74c3c; -fx-font-family: 'Segoe UI';"
                    );
                    captchaStatusLabel.setVisible(true);
                    captchaStatusLabel.setManaged(true);
                });
            }
        });

        // Build and show the popup stage
        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initStyle(StageStyle.UTILITY);
        popup.setTitle("Security Verification — MindNest");
        popup.setResizable(false);

        StackPane root = new StackPane(popupWebView);
        root.setStyle("-fx-background-color: white;");
        popup.setScene(new Scene(root, 640, 740));

        // Center over login window
        Stage loginStage = (Stage) loginButton.getScene().getWindow();
        popup.setX(loginStage.getX() + (loginStage.getWidth()  - 640) / 2);
        popup.setY(loginStage.getY() + (loginStage.getHeight() - 740) / 2);

        popup.showAndWait();
    }

    @FXML
    void handleLogin() {
        String email    = emailField.getText().trim();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            showInlineError("Please fill in all fields.");
            return;
        }

        if (!isValidEmail(email)) {
            showInlineError("Invalid email format. e.g. user@example.com");
            highlightError(emailField);
            return;
        }

        if (captchaToken == null) {
            showInlineError("Please complete the CAPTCHA verification.");
            return;
        }

        resetStyle(emailField);
        hideError();

        loginButton.setDisable(true);
        loginButton.setText("Verifying...");

        captchaService.verifyTokenAsync(captchaToken, isValid -> {
            Platform.runLater(() -> {
                loginButton.setText("Continue");

                if (!isValid) {
                    showInlineError("CAPTCHA verification failed. Please try again.");
                    loginButton.setDisable(false);
                    resetCaptchaButton();
                    return;
                }

                authenticateUser(email, password);
            });
        });
    }

    private void authenticateUser(String email, String password) {
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

                AppUser user = new AppUser(rs.getInt("id"), rs.getString("name"), appRole);
                UserSession.get().setUser(user);
                redirectToTherapyModule();

            } else {
                showInlineError("Invalid email or password.");
                highlightError(emailField);
                highlightError(passwordField);
                loginButton.setDisable(false);
                resetCaptchaButton();
            }

        } catch (Exception e) {
            e.printStackTrace();
            showInlineError("Database error: " + e.getMessage());
            loginButton.setDisable(false);
        }
    }

    /** Resets the "I'm not a robot" button back to unchecked state */
    private void resetCaptchaButton() {
        captchaToken = null;
        loginButton.setDisable(true);
        openCaptchaBtn.setText("☐  I'm not a robot");
        openCaptchaBtn.setDisable(false);
        openCaptchaBtn.setStyle(
                "-fx-background-color: #f9f9f9;" +
                        "-fx-border-color: #d0d0d0;" +
                        "-fx-border-radius: 6;" +
                        "-fx-background-radius: 6;" +
                        "-fx-font-size: 13px;" +
                        "-fx-text-fill: #444;" +
                        "-fx-font-family: 'Segoe UI';" +
                        "-fx-cursor: hand;" +
                        "-fx-alignment: CENTER_LEFT;" +
                        "-fx-padding: 0 12;"
        );
        captchaStatusLabel.setVisible(false);
        captchaStatusLabel.setManaged(false);
    }

    private void redirectToTherapyModule() throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/TherapyLayout.fxml"));
        Scene scene = new Scene(loader.load(), 1200, 720);

        // ✅ add this line (adjust path if your css is not in /css/)
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());

        Stage stage = (Stage) emailField.getScene().getWindow();
        stage.setScene(scene);
        stage.setTitle("MindNest");
    }

    private boolean isValidEmail(String email) {
        return EMAIL_PATTERN.matcher(email).matches();
    }

    private void highlightError(TextField field) {
        field.setStyle("-fx-border-color: #e74c3c; -fx-border-width: 2; -fx-border-radius: 6;");
    }

    private void resetStyle(TextField field) {
        field.setStyle("");
    }

    private void showInlineError(String msg) {
        errorLabel.setText("⚠ " + msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void hideError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    private void showAlert(String msg) {
        new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK).showAndWait();
    }

    @FXML
    public void goForgotPassword(ActionEvent e) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/forgot_password.fxml"));
        Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
    }

    public void goSignup(ActionEvent e) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/signup.fxml"));
        Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
    }
}