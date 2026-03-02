package services;

import utils.MyDB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class PlanEnrollmentService {

    private final Connection cnx = MyDB.getInstance().getConnection();

    public boolean isEnrolled(int planId, int userId) {
        String sql = "SELECT 1 FROM plan_enrollment WHERE planId = ? AND userId = ? LIMIT 1";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, planId);
            ps.setInt(2, userId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void enroll(int planId, int userId) {
        // safe even if already enrolled (due to UNIQUE planId+userId)
        String sql =
                "INSERT INTO plan_enrollment (planId, userId) " +
                        "VALUES (?, ?) " +
                        "ON DUPLICATE KEY UPDATE planId = planId";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, planId);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void unenroll(int planId, int userId) {
        String sql = "DELETE FROM plan_enrollment WHERE planId = ? AND userId = ?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, planId);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
