package services;

import models.SessionFeedback;
import utils.MyDB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SessionFeedbackService {

    // ✅ Always fetch a fresh (or reconnected) connection per method call.
    private Connection getConnection() {
        return MyDB.getInstance().getConnection();
    }

    // ✅ Add new feedback — patient_id removed (column doesn't exist in DB)
    public void addSessionFeedback(SessionFeedback feedback) throws SQLException {
        String sql = "INSERT INTO SessionFeedback (session_id, rating, feedback_comment) VALUES (?, ?, ?)";
        PreparedStatement ps = getConnection().prepareStatement(sql);
        ps.setInt(1, feedback.getSessionId());
        ps.setInt(2, feedback.getRating());
        ps.setString(3, feedback.getComment());
        ps.executeUpdate();
    }

    // ✅ Get all feedbacks (for admin view)
    public List<SessionFeedback> getAllSessionFeedbacks() throws SQLException {
        List<SessionFeedback> feedbackList = new ArrayList<>();
        String sql = "SELECT * FROM SessionFeedback ORDER BY feedback_date DESC";
        Statement st = getConnection().createStatement();
        ResultSet rs = st.executeQuery(sql);
        while (rs.next()) {
            feedbackList.add(map(rs));
        }
        return feedbackList;
    }

    // ✅ Get feedback comment by session ID
    public String getFeedbackBySessionId(int sessionId) throws SQLException {
        String sql = "SELECT feedback_comment FROM SessionFeedback WHERE session_id = ? LIMIT 1";
        PreparedStatement ps = getConnection().prepareStatement(sql);
        ps.setInt(1, sessionId);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return rs.getString("feedback_comment");
        }
        return null;
    }

    // ✅ Get full feedback object by session ID
    public SessionFeedback getFeedbackObjectBySessionId(int sessionId) throws SQLException {
        String sql = "SELECT * FROM SessionFeedback WHERE session_id = ? LIMIT 1";
        PreparedStatement ps = getConnection().prepareStatement(sql);
        ps.setInt(1, sessionId);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return map(rs);
        }
        return null;
    }

    // ✅ Check if feedback already exists for a session (patient_id removed — check by session_id only)
    public boolean hasUserFeedbackForSession(int userId, int sessionId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM SessionFeedback WHERE session_id = ?";
        PreparedStatement ps = getConnection().prepareStatement(sql);
        ps.setInt(1, sessionId);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return rs.getInt(1) > 0;
        }
        return false;
    }

    // ✅ Update existing feedback
    public void updateSessionFeedback(SessionFeedback feedback) throws SQLException {
        String sql = "UPDATE SessionFeedback SET rating = ?, feedback_comment = ? WHERE feedback_id = ?";
        PreparedStatement ps = getConnection().prepareStatement(sql);
        ps.setInt(1, feedback.getRating());
        ps.setString(2, feedback.getComment());
        ps.setInt(3, feedback.getFeedbackId());
        ps.executeUpdate();
    }

    // ✅ Delete feedback by feedback ID
    public void deleteSessionFeedback(int feedbackId) throws SQLException {
        String sql = "DELETE FROM SessionFeedback WHERE feedback_id = ?";
        PreparedStatement ps = getConnection().prepareStatement(sql);
        ps.setInt(1, feedbackId);
        ps.executeUpdate();
    }

    // ✅ Delete feedback by session ID (for cascade delete)
    public void deleteFeedbackBySessionId(int sessionId) throws SQLException {
        String sql = "DELETE FROM SessionFeedback WHERE session_id = ?";
        PreparedStatement ps = getConnection().prepareStatement(sql);
        ps.setInt(1, sessionId);
        ps.executeUpdate();
    }

    // ✅ Map ResultSet to SessionFeedback object
    private SessionFeedback map(ResultSet rs) throws SQLException {
        SessionFeedback f = new SessionFeedback();
        f.setFeedbackId(rs.getInt("feedback_id"));
        f.setSessionId(rs.getInt("session_id"));
        f.setRating(rs.getInt("rating"));
        f.setComment(rs.getString("feedback_comment"));
        f.setCreatedAt(rs.getTimestamp("feedback_date"));
        return f;
    }
}
