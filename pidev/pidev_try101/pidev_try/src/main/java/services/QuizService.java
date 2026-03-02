package services;

import models.Question;
import models.Quiz;
import utils.MyDB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class QuizService {

    private final Connection cnx = MyDB.getInstance().getConnection();

    // ------------------- CREATE QUIZ -------------------
    public Quiz create(String title, String description, String category) {
        String sql = "INSERT INTO quiz (title, description, category) VALUES (?, ?, ?)";

        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, title);
            ps.setString(2, description);
            ps.setString(3, category);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    return new Quiz(id, title, description, category);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    // ------------------- GET ALL QUIZZES -------------------
    public List<Quiz> getAllQuizzes() {
        List<Quiz> list = new ArrayList<>();
        String sql = "SELECT * FROM quiz";

        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                list.add(new Quiz(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getString("category")
                ));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }

    // ------------------- UPDATE QUIZ -------------------
    public void updateQuiz(Quiz q) {
        String sql = "UPDATE quiz SET title=?, description=?, category=? WHERE id=?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, q.getTitle());
            ps.setString(2, q.getDescription());
            ps.setString(3, q.getCategory());
            ps.setInt(4, q.getId());
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

    // ------------------- ADD QUESTION -------------------
    public void addQuestion(int quizId, String questionText, String a, String b, String c, String d) {
        String sql = """
                INSERT INTO question (quiz_id, question_text, option_a, option_b, option_c, option_d)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, quizId);
            ps.setString(2, questionText);
            ps.setString(3, a);
            ps.setString(4, b);
            ps.setString(5, c);
            ps.setString(6, d);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ------------------- UPDATE QUESTION -------------------
    public void updateQuestion(Question q) {
        String sql = """
                UPDATE question
                SET question_text=?, option_a=?, option_b=?, option_c=?, option_d=?
                WHERE id=?
                """;

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

    // ------------------- DELETE QUESTION -------------------
    public void deleteQuestion(int questionId) {
        String sql = "DELETE FROM question WHERE id=?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, questionId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ------------------- GET QUESTIONS BY QUIZ -------------------
    public List<Question> getQuestionsByQuiz(int quizId) {
        List<Question> list = new ArrayList<>();
        String sql = "SELECT * FROM question WHERE quiz_id=?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, quizId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapQuestion(rs));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }

    private Question mapQuestion(ResultSet rs) throws SQLException {
        return new Question(
                rs.getInt("id"),
                rs.getInt("quiz_id"),
                rs.getString("question_text"),
                rs.getString("option_a"),
                rs.getString("option_b"),
                rs.getString("option_c"),
                rs.getString("option_d")
        );
    }
}
