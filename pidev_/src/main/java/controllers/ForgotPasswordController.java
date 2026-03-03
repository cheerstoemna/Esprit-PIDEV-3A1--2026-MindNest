package controllers;

import services.EmailService;
import db.DBConnection;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.*;
import java.util.ResourceBundle;

/**
 * ForgotPasswordController
 *
 * Flow:
 *  Step 1 — User enters their email → OTP is generated and sent
 *  Step 2 — User enters the OTP they received
 *  Step 3 — User sets a new password
 */
public class ForgotPasswordController implements Initializable {

    // ── Step 1: Email entry ───────────────────────────────────────────
    @FXML private javafx.scene.layout.VBox  stepEmailPane;
    @FXML private TextField                  emailField;
    @FXML private Button                     sendOtpButton;
    @FXML private Label                      step1ErrorLabel;

    // ── Step 2: OTP verification ──────────────────────────────────────
    @FXML private javafx.scene.layout.VBox  stepOtpPane;
    @FXML private TextField                  otpField;
    @FXML private Button                     verifyOtpButton;
    @FXML private Label                      step2ErrorLabel;
    @FXML private Label                      otpSentLabel;

    // ── Step 3: New password ──────────────────────────────────────────
    @FXML private javafx.scene.layout.VBox  stepPasswordPane;
    @FXML private PasswordField              newPasswordField;
    @FXML private PasswordField              confirmPasswordField;
    @FXML private Button                     resetButton;
    @FXML private Label                      step3ErrorLabel;

    // ── Internal state ────────────────────────────────────────────────
    private String generatedOtp   = null;
    private String verifiedEmail  = null;
    private final EmailService emailService = new EmailService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        showStep(1);
    }

    // ─────────────────────────────────────────────────────────────────
    // STEP 1 — Send OTP
    // ─────────────────────────────────────────────────────────────────

    @FXML
    private void handleSendOtp() {
        String email = emailField.getText().trim();

        if (email.isEmpty()) {
            showError(step1ErrorLabel, "Please enter your email address.");
            return;
        }

        if (!emailExistsInDB(email)) {
            showError(step1ErrorLabel, "No account found with this email.");
            return;
        }

        sendOtpButton.setDisable(true);
        sendOtpButton.setText("Sending...");
        hideError(step1ErrorLabel);

        // Generate a 6-digit OTP
        generatedOtp  = String.valueOf((int)(Math.random() * 900000) + 100000);
        verifiedEmail = email;

        // Send email on background thread
        new Thread(() -> {
            boolean sent = emailService.sendOtp(email, generatedOtp);
            Platform.runLater(() -> {
                sendOtpButton.setDisable(false);
                sendOtpButton.setText("Send Code");

                if (sent) {
                    otpSentLabel.setText("✔ Code sent to " + email);
                    showStep(2);
                } else {
                    showError(step1ErrorLabel, "Failed to send email. Check your connection.");
                    generatedOtp  = null;
                    verifiedEmail = null;
                }
            });
        }).start();
    }

    // ─────────────────────────────────────────────────────────────────
    // STEP 2 — Verify OTP
    // ─────────────────────────────────────────────────────────────────

    @FXML
    private void handleVerifyOtp() {
        String enteredOtp = otpField.getText().trim();

        if (enteredOtp.isEmpty()) {
            showError(step2ErrorLabel, "Please enter the code you received.");
            return;
        }

        if (!enteredOtp.equals(generatedOtp)) {
            showError(step2ErrorLabel, "Incorrect code. Please try again.");
            otpField.clear();
            return;
        }

        // OTP correct → go to step 3
        hideError(step2ErrorLabel);
        showStep(3);
    }

    @FXML
    private void handleResendOtp() {
        otpField.clear();
        hideError(step2ErrorLabel);
        showStep(1);
        generatedOtp  = null;
        verifiedEmail = null;
    }

    // ─────────────────────────────────────────────────────────────────
    // STEP 3 — Reset Password
    // ─────────────────────────────────────────────────────────────────

    @FXML
    private void handleResetPassword() {
        String newPass     = newPasswordField.getText();
        String confirmPass = confirmPasswordField.getText();

        if (newPass.isEmpty() || confirmPass.isEmpty()) {
            showError(step3ErrorLabel, "Please fill in both password fields.");
            return;
        }

        if (newPass.length() < 6) {
            showError(step3ErrorLabel, "Password must be at least 6 characters.");
            return;
        }

        if (!newPass.equals(confirmPass)) {
            showError(step3ErrorLabel, "Passwords do not match.");
            return;
        }

        // Update password in DB
        if (updatePasswordInDB(verifiedEmail, newPass)) {
            // Show success alert then redirect to login
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.INFORMATION
            );
            alert.setTitle("Password Reset Successful");
            alert.setHeaderText(null);
            alert.setContentText("✔ Your password has been reset successfully! Please log in.");
            alert.showAndWait();

            // Navigate back to login
            try {
                javafx.scene.Parent root = javafx.fxml.FXMLLoader.load(
                        getClass().getResource("/fxml/login.fxml")
                );
                javafx.stage.Stage stage = (javafx.stage.Stage)
                        resetButton.getScene().getWindow();
                stage.setScene(new javafx.scene.Scene(root));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else {
            showError(step3ErrorLabel, "Database error. Please try again.");
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Navigation
    // ─────────────────────────────────────────────────────────────────

    /** Shows only the requested step pane, hides all others */
    private void showStep(int step) {
        stepEmailPane.setVisible(step == 1);    stepEmailPane.setManaged(step == 1);
        stepOtpPane.setVisible(step == 2);      stepOtpPane.setManaged(step == 2);
        stepPasswordPane.setVisible(step == 3); stepPasswordPane.setManaged(step == 3);
    }

    @FXML
    public void goLogin(ActionEvent e) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/login.fxml"));
        Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
    }

    // ─────────────────────────────────────────────────────────────────
    // DB Helpers
    // ─────────────────────────────────────────────────────────────────

    private boolean emailExistsInDB(String email) {
        String sql = "SELECT id FROM users WHERE email = ? AND status = 1";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
            return ps.executeQuery().next();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean updatePasswordInDB(String email, String newPassword) {
        // NOTE: If your project uses hashed passwords (e.g. BCrypt),
        // replace newPassword with BCrypt.hashpw(newPassword, BCrypt.gensalt())
        String sql = "UPDATE users SET password = ? WHERE email = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, newPassword);
            ps.setString(2, email);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // UI Helpers
    // ─────────────────────────────────────────────────────────────────

    private void showError(Label label, String msg) {
        label.setText("⚠ " + msg);
        label.setVisible(true);
        label.setManaged(true);
    }

    private void hideError(Label label) {
        label.setVisible(false);
        label.setManaged(false);
    }
}