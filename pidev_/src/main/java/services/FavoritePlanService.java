package services;

import utils.MyDB;

import java.sql.*;
import java.util.HashSet;
import java.util.Set;

public class FavoritePlanService {

    private final Connection cnx = MyDB.getInstance().getConnection();

    public Set<Integer> getFavoritePlanIds(int userId) {
        Set<Integer> ids = new HashSet<>();
        String sql = "SELECT planId FROM favorite_plans WHERE userId = ?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) ids.add(rs.getInt("planId"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ids;
    }

    public boolean isFavorite(int userId, int planId) {
        String sql = "SELECT 1 FROM favorite_plans WHERE userId = ? AND planId = ? LIMIT 1";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, planId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void add(int userId, int planId) {
        String sql = "INSERT INTO favorite_plans (userId, planId) VALUES (?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, planId);
            ps.executeUpdate();
        } catch (SQLException e) {
            // ignore duplicate primary key
            if (!"23000".equals(e.getSQLState())) e.printStackTrace();
        }
    }

    public void remove(int userId, int planId) {
        String sql = "DELETE FROM favorite_plans WHERE userId = ? AND planId = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, planId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}