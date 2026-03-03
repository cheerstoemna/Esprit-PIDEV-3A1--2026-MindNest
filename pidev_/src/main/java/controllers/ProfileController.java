package controllers;

import db.DBConnection;
import utils.UserSession;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.sql.*;
import java.util.Optional;
import java.util.ResourceBundle;

public class ProfileController implements Initializable {

    @FXML private Label         welcomeLabel;
    @FXML private Label         emailLabel;
    @FXML private TextArea      goalsField;
    @FXML private TextArea      interestsField;
    @FXML private TextArea      bioField;
    @FXML private Button        saveBtn;
    @FXML private Button        showProfileBtn;
    @FXML private Button        deleteBtn;
    @FXML private Button        deleteGoalsBtn;
    @FXML private Button        deleteInterestsBtn;
    @FXML private Button        deleteBioBtn;
    @FXML private Label         statusLabel;

    private boolean isUpdate = false;
    private int profileId = 0;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        var user = UserSession.get().getUser();

        welcomeLabel.setText("Welcome, " + user.getName() + " 👋");
        emailLabel.setText("User ID: " + user.getId() + " • Role: " + user.getRole());

        // Fields start empty
        clearFields();
        showInfo("👆 Click 'Show Profile Details' to load your profile");
    }

    // ══════════════════════════════════════════════════════════════════
    // READ - Show profile details
    // ══════════════════════════════════════════════════════════════════

    @FXML
    void handleShowProfile() {
        loadProfile();
    }

    private void loadProfile() {
        String sql = "SELECT * FROM profiles WHERE user_id = ?";

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, UserSession.get().getUser().getId());
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                isUpdate = true;
                profileId = rs.getInt("id");
                goalsField.setText(rs.getString("goals") != null ? rs.getString("goals") : "");
                interestsField.setText(rs.getString("interests") != null ? rs.getString("interests") : "");
                bioField.setText(rs.getString("bio") != null ? rs.getString("bio") : "");
                showSuccess("✅ Profile loaded successfully!");
                showProfileBtn.setText("🔄 Refresh Profile");
            } else {
                isUpdate = false;
                profileId = 0;
                clearFields();
                showInfo("📝 No profile found. Fill the fields and click 'Save Profile'.");
                showProfileBtn.setText("📋 Show Profile Details");
            }

        } catch (Exception e) {
            e.printStackTrace();
            showError("❌ Error loading profile: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // CREATE / UPDATE - Save profile
    // ══════════════════════════════════════════════════════════════════

    @FXML
    void handleSave() {
        String goals     = goalsField.getText().trim();
        String interests = interestsField.getText().trim();
        String bio       = bioField.getText().trim();

        if (goals.isEmpty() && interests.isEmpty() && bio.isEmpty()) {
            showAlert("Please fill in at least one field before saving.");
            return;
        }

        if (isUpdate) {
            updateProfile(goals, interests, bio);
        } else {
            insertProfile(goals, interests, bio);
        }
    }

    private void insertProfile(String goals, String interests, String bio) {
        String sql = "INSERT INTO profiles(user_id, goals, interests, bio) VALUES(?,?,?,?)";

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, UserSession.get().getUser().getId());
            ps.setString(2, goals);
            ps.setString(3, interests);
            ps.setString(4, bio);
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) profileId = keys.getInt(1);

            isUpdate = true;

            // Clear fields and show success after save
            clearFields();
            showSuccess("✅ Profile created! Click 'Show Profile Details' to view it.");
            showProfileBtn.setText("📋 Show Profile Details");

        } catch (Exception e) {
            e.printStackTrace();
            showError("❌ Error creating profile: " + e.getMessage());
        }
    }

    private void updateProfile(String goals, String interests, String bio) {
        String sql = "UPDATE profiles SET goals=?, interests=?, bio=? WHERE user_id=?";

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, goals);
            ps.setString(2, interests);
            ps.setString(3, bio);
            ps.setInt(4, UserSession.get().getUser().getId());

            int rows = ps.executeUpdate();

            if (rows > 0) {
                // Clear fields and show success after update
                clearFields();
                showSuccess("✅ Profile updated! Click 'Show Profile Details' to view it.");
                showProfileBtn.setText("📋 Show Profile Details");
            } else {
                showError("❌ No profile found to update.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            showError("❌ Error updating profile: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // DELETE - Delete entire profile
    // ══════════════════════════════════════════════════════════════════

    @FXML
    void handleDelete() {
        if (!isUpdate) {
            showAlert("No profile to delete. Please load your profile first.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Profile");
        confirm.setHeaderText("Delete your entire profile?");
        confirm.setContentText("This will permanently delete all your profile information.");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            deleteEntireProfile();
        }
    }

    private void deleteEntireProfile() {
        String sql = "DELETE FROM profiles WHERE user_id = ?";

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, UserSession.get().getUser().getId());
            int rows = ps.executeUpdate();

            if (rows > 0) {
                clearFields();
                isUpdate = false;
                profileId = 0;
                showProfileBtn.setText("📋 Show Profile Details");
                showSuccess("🗑️ Profile deleted successfully!");
            } else {
                showError("❌ No profile found to delete.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            showError("❌ Error deleting profile: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // DELETE BY FIELD - Delete individual fields
    // ══════════════════════════════════════════════════════════════════

    @FXML
    void handleDeleteGoals() {
        if (!isUpdate) {
            showAlert("Please load your profile first.");
            return;
        }
        if (goalsField.getText().trim().isEmpty()) {
            showAlert("Goals field is already empty.");
            return;
        }

        if (confirmFieldDelete("Goals")) {
            deleteField("goals");
            goalsField.clear();
            showSuccess("🗑️ Goals cleared successfully!");
        }
    }

    @FXML
    void handleDeleteInterests() {
        if (!isUpdate) {
            showAlert("Please load your profile first.");
            return;
        }
        if (interestsField.getText().trim().isEmpty()) {
            showAlert("Interests field is already empty.");
            return;
        }

        if (confirmFieldDelete("Interests & Hobbies")) {
            deleteField("interests");
            interestsField.clear();
            showSuccess("🗑️ Interests cleared successfully!");
        }
    }

    @FXML
    void handleDeleteBio() {
        if (!isUpdate) {
            showAlert("Please load your profile first.");
            return;
        }
        if (bioField.getText().trim().isEmpty()) {
            showAlert("Bio field is already empty.");
            return;
        }

        if (confirmFieldDelete("Bio")) {
            deleteField("bio");
            bioField.clear();
            showSuccess("🗑️ Bio cleared successfully!");
        }
    }

    /**
     * Clears a specific field in the database (sets to NULL)
     */
    private void deleteField(String fieldName) {
        String sql = "UPDATE profiles SET " + fieldName + " = NULL WHERE user_id = ?";

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, UserSession.get().getUser().getId());
            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
            showError("❌ Error clearing field: " + e.getMessage());
        }
    }

    /**
     * Confirmation dialog for field deletion
     */
    private boolean confirmFieldDelete(String fieldName) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Clear Field");
        confirm.setHeaderText("Clear '" + fieldName + "'?");
        confirm.setContentText("This will permanently remove your " + fieldName + " information.");

        Optional<ButtonType> result = confirm.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    // ══════════════════════════════════════════════════════════════════
    // CANCEL
    // ══════════════════════════════════════════════════════════════════

    @FXML
    void handleCancel() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Cancel");
        confirm.setHeaderText("Discard unsaved changes?");
        confirm.setContentText("Your changes will not be saved and fields will be cleared.");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            clearFields();
            showProfileBtn.setText("📋 Show Profile Details");
            showInfo("ℹ️ Changes discarded. Fields cleared.");
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // UTILITY METHODS
    // ══════════════════════════════════════════════════════════════════

    private void clearFields() {
        goalsField.clear();
        interestsField.clear();
        bioField.clear();
    }

    private void showSuccess(String msg) {
        statusLabel.setText(msg);
        statusLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
    }

    private void showError(String msg) {
        statusLabel.setText(msg);
        statusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
    }

    private void showInfo(String msg) {
        statusLabel.setText(msg);
        statusLabel.setStyle("-fx-text-fill: #3498db; -fx-font-weight: bold;");
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        alert.setTitle("Notice");
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}