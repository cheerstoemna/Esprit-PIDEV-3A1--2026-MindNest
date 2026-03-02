package services;

import models.CoachingPlan;
import utils.MyDB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CoachingPlanService {

    private Connection cnx = MyDB.getInstance().getConnection();

    // CREATE
    public void addCoachingPlan(CoachingPlan plan) {
        String sql = "INSERT INTO coaching_plan (userId, title, description, goals, image_path) VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, plan.getUserId());
            ps.setString(2, plan.getTitle());
            ps.setString(3, plan.getDescription());
            ps.setString(4, plan.getGoals());
            ps.setString(5, plan.getImagePath());

            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // READ
    public List<CoachingPlan> getAllPlans() {
        List<CoachingPlan> plans = new ArrayList<>();
        String sql = "SELECT * FROM coaching_plan";

        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                CoachingPlan p = new CoachingPlan();

                p.setPlanId(rs.getInt("planId"));
                p.setUserId(rs.getInt("userId"));
                p.setTitle(rs.getString("title"));
                p.setDescription(rs.getString("description"));
                p.setGoals(rs.getString("goals"));
                p.setImagePath(rs.getString("image_path"));

                plans.add(p);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return plans;
    }

    // UPDATE
    public void updatePlan(CoachingPlan plan) {
        String sql = "UPDATE coaching_plan SET title=?, description=?, goals=?, image_path=? WHERE planId=?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, plan.getTitle());
            ps.setString(2, plan.getDescription());
            ps.setString(3, plan.getGoals());
            ps.setString(4, plan.getImagePath());
            ps.setInt(5, plan.getPlanId());

            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // DELETE
    public void deletePlan(int planId) {
        String sql = "DELETE FROM coaching_plan WHERE planId=?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, planId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
