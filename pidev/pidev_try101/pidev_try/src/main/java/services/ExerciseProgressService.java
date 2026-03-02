package services;

import utils.MyDB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ExerciseProgressService {

    private final Connection cnx = MyDB.getInstance().getConnection();

    // Returns the user's status for one exercise. If none exists, returns "Not Started".
    public String getStatus(int exerciseId, int userId) {
        String sql = "SELECT status FROM exercise_progress WHERE exerciseId = ? AND userId = ?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, exerciseId);
            ps.setInt(2, userId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("status");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "Not Started";
    }

    // Insert if not exists, otherwise update (per-user progress)
    public void upsertStatus(int exerciseId, int userId, String status) {
        String sql =
                "INSERT INTO exercise_progress (exerciseId, userId, status) " +
                        "VALUES (?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE status = VALUES(status)";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, exerciseId);
            ps.setInt(2, userId);
            ps.setString(3, status);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteProgressForExercise(int exerciseId) {
        String sql = "DELETE FROM exercise_progress WHERE exerciseId = ?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, exerciseId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
