package services;

import models.Exercise;
import utils.MyDB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ExerciseService {

    private final Connection cnx = MyDB.getInstance().getConnection();

    public void addExercise(Exercise ex) {
        String sql = "INSERT INTO exercise (planId, title, description, duration, difficultyLevel, image) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, ex.getPlanId());
            ps.setString(2, safeTrim(ex.getTitle()));
            ps.setString(3, ex.getDescription()); // keep as-is (can be long)
            ps.setInt(4, ex.getDuration());
            ps.setString(5, safeTrim(ex.getDifficultyLevel()));
            ps.setString(6, safeTrim(ex.getImage())); // absolute path string

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    ex.setExerciseId(keys.getInt(1));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<Exercise> getExercisesByPlan(int planId) {
        List<Exercise> list = new ArrayList<>();
        String sql = "SELECT exerciseId, planId, title, description, duration, difficultyLevel, image " +
                "FROM exercise WHERE planId=? ORDER BY exerciseId DESC";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, planId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Exercise e = new Exercise();
                    e.setExerciseId(rs.getInt("exerciseId"));
                    e.setPlanId(rs.getInt("planId"));
                    e.setTitle(rs.getString("title"));
                    e.setDescription(rs.getString("description"));
                    e.setDuration(rs.getInt("duration"));
                    e.setDifficultyLevel(rs.getString("difficultyLevel"));
                    e.setImage(rs.getString("image")); // absolute path
                    list.add(e);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }

    public void updateExercise(Exercise ex) {
        String sql = "UPDATE exercise SET title=?, description=?, duration=?, difficultyLevel=?, image=? " +
                "WHERE exerciseId=?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, safeTrim(ex.getTitle()));
            ps.setString(2, ex.getDescription());
            ps.setInt(3, ex.getDuration());
            ps.setString(4, safeTrim(ex.getDifficultyLevel()));
            ps.setString(5, safeTrim(ex.getImage())); // absolute path string
            ps.setInt(6, ex.getExerciseId());

            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteExercise(int exerciseId) {
        String sql = "DELETE FROM exercise WHERE exerciseId=?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, exerciseId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private String safeTrim(String s) {
        return (s == null) ? null : s.trim();
    }
}
