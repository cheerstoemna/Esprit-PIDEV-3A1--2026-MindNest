package services;

import models.TherapySession;
import utils.MyDB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TherapySessionService {

    private final Connection cnx = MyDB.getInstance().getConnection();

    // ✅ Add new therapy session
    public void addTherapySession(TherapySession session) throws SQLException {
        String sql = "INSERT INTO TherapySession (psychologist_id, user_id, session_date, duration_minutes, session_status, session_notes) VALUES (?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, session.getPsychologistId());
        ps.setInt(2, session.getUserId());
        ps.setTimestamp(3, Timestamp.valueOf(session.getSessionDate()));
        ps.setInt(4, session.getDurationMinutes());
        ps.setString(5, session.getSessionStatus());
        ps.setString(6, session.getSessionNotes());
        ps.executeUpdate();
    }

    // ✅ Get sessions by user ID
    public List<TherapySession> getSessionsByUser(int userId) throws SQLException {
        List<TherapySession> list = new ArrayList<>();
        String sql = "SELECT * FROM TherapySession WHERE user_id = ? ORDER BY session_date";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, userId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            list.add(map(rs));
        }
        return list;
    }

    // ✅ Get all therapy sessions (for admin)
    public List<TherapySession> getAllTherapySessions() throws SQLException {
        List<TherapySession> list = new ArrayList<>();
        String sql = "SELECT * FROM TherapySession ORDER BY session_date";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);
        while (rs.next()) {
            list.add(map(rs));
        }
        return list;
    }

    // ✅ Update full session details
    public void updateSession(TherapySession session) throws SQLException {
        String sql = "UPDATE TherapySession SET session_date=?, duration_minutes=?, session_status=?, session_notes=? WHERE session_id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setTimestamp(1, Timestamp.valueOf(session.getSessionDate()));
        ps.setInt(2, session.getDurationMinutes());
        ps.setString(3, session.getSessionStatus());
        ps.setString(4, session.getSessionNotes());
        ps.setInt(5, session.getSessionId());
        ps.executeUpdate();
    }

    // ✅ NEW: Update only status (for canceling sessions)
    public void updateStatus(int sessionId, String newStatus) throws SQLException {
        String sql = "UPDATE TherapySession SET session_status = ? WHERE session_id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, newStatus);
        ps.setInt(2, sessionId);
        ps.executeUpdate();
    }

    // ✅ Reschedule session (update date and duration)
    public void reschedule(int sessionId, Timestamp newDate, int duration) throws SQLException {
        String sql = "UPDATE TherapySession SET session_date=?, duration_minutes=? WHERE session_id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setTimestamp(1, newDate);
        ps.setInt(2, duration);
        ps.setInt(3, sessionId);
        ps.executeUpdate();
    }

    // ✅ Delete therapy session
    public void deleteTherapySession(int sessionId) throws SQLException {
        String sql = "DELETE FROM TherapySession WHERE session_id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, sessionId);
        ps.executeUpdate();
    }

    // ✅ Map ResultSet to TherapySession object
    private TherapySession map(ResultSet rs) throws SQLException {
        TherapySession t = new TherapySession();
        t.setSessionId(rs.getInt("session_id"));
        t.setPsychologistId(rs.getInt("psychologist_id"));
        t.setUserId(rs.getInt("user_id"));
        t.setSessionDate(rs.getTimestamp("session_date").toLocalDateTime());
        t.setDurationMinutes(rs.getInt("duration_minutes"));
        t.setSessionStatus(rs.getString("session_status"));
        t.setSessionNotes(rs.getString("session_notes"));
        t.setCreatedAt(rs.getTimestamp("created_at"));
        return t;
    }
}