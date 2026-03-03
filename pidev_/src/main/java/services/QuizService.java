package services;

import db.DBConnection;
import models.Question;
import models.Quiz;
import utils.MyDB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class QuizService {

    private final Connection cnx = DBConnection.getConnection();

    // ------------------- CREATE QUIZ -------------------
    public Quiz create(String title, String description, String category, boolean pinned) {
        String sql = "INSERT INTO quiz (title, description, category, pinned) VALUES (?, ?, ?, ?)";

        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, title);
            ps.setString(2, description);
            ps.setString(3, category);
            ps.setBoolean(4, pinned);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    return new Quiz(id, title, description, category, pinned);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // ------------------- GET ALL QUIZZES (Pinned on top) -------------------
    public List<Quiz> getAllQuizzes() {
        List<Quiz> list = new ArrayList<>();
        String sql = "SELECT * FROM quiz ORDER BY pinned DESC, id ASC";

        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                list.add(new Quiz(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getString("category"),
                        rs.getBoolean("pinned")
                ));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }

    // ------------------- UPDATE QUIZ -------------------
    public void updateQuiz(Quiz q) {
        String sql = "UPDATE quiz SET title=?, description=?, category=?, pinned=? WHERE id=?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, q.getTitle());
            ps.setString(2, q.getDescription());
            ps.setString(3, q.getCategory());
            ps.setBoolean(4, q.isPinned());
            ps.setInt(5, q.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ------------------- PIN / UNPIN QUIZ -------------------
    public void setPinned(int quizId, boolean pinned) {
        String sql = "UPDATE quiz SET pinned=? WHERE id=?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setBoolean(1, pinned);
            ps.setInt(2, quizId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ------------------- DELETE QUIZ -------------------
    public void deleteQuiz(int quizId) {
        String sql = "DELETE FROM quiz WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, quizId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ------------------- QUESTIONS -------------------
    public void addQuestion(int quizId, String questionText, String optionA, String optionB, String optionC, String optionD) {
        String sql = "INSERT INTO question (quiz_id, question_text, option_a, option_b, option_c, option_d) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, quizId);
            ps.setString(2, questionText);
            ps.setString(3, optionA);
            ps.setString(4, optionB);
            ps.setString(5, optionC);
            ps.setString(6, optionD);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updateQuestion(Question q) {
        String sql = "UPDATE question SET question_text=?, option_a=?, option_b=?, option_c=?, option_d=? WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, q.getQuestionText());
            ps.setString(2, q.getOptionA());
            ps.setString(3, q.getOptionB());
            ps.setString(4, q.getOptionC());
            ps.setString(5, q.getOptionD());
            ps.setInt(6, q.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteQuestion(int questionId) {
        String sql = "DELETE FROM question WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, questionId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<Question> getQuestionsByQuiz(int quizId) {
        List<Question> list = new ArrayList<>();
        String sql = "SELECT * FROM question WHERE quiz_id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, quizId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Question(
                            rs.getInt("id"),
                            rs.getInt("quiz_id"),
                            rs.getString("question_text"),
                            rs.getString("option_a"),
                            rs.getString("option_b"),
                            rs.getString("option_c"),
                            rs.getString("option_d")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
    public void searchQuizzes(String titleKeyword, String category, int limit, int offset) {
        String searchSQL = "SELECT * FROM quiz " +
                "WHERE title LIKE ? AND category = ? " +
                "ORDER BY created_at DESC " +
                "LIMIT ? OFFSET ?";

        try (Connection connection = MyDB.getInstance().getConnection();
             PreparedStatement ps = connection.prepareStatement(searchSQL)) {

            ps.setString(1, titleKeyword + "%"); // example: "Math%"
            ps.setString(2, category);           // example: "Science"
            ps.setInt(3, limit);                 // e.g., 20 results per page
            ps.setInt(4, offset);                // e.g., 0 for page 1

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int id = rs.getInt("id");
                String title = rs.getString("title");
                String description = rs.getString("description");
                String cat = rs.getString("category");
                Timestamp createdAt = rs.getTimestamp("created_at");

                System.out.println(id + " | " + title + " | " + cat + " | " + createdAt);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}