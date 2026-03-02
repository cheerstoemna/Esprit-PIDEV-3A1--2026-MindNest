package services;

import utils.MyDB;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class AnswerService {

    private final Connection cnx = MyDB.getInstance().getConnection();

    public void saveAnswer(int quizId, int questionId, String answer) {

        String sql = """
                INSERT INTO user_answer (quiz_id, question_id, selected_answer)
                VALUES (?, ?, ?)
                """;

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {

            ps.setInt(1, quizId);
            ps.setInt(2, questionId);
            ps.setString(3, answer);

            ps.executeUpdate();

            System.out.println("Answer saved!");

        } catch (SQLException e) {

            System.err.println("Error saving answer: " + e.getMessage());

        }
    }
}
