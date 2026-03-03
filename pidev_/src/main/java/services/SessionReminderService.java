package services;

import models.TherapySession;
import utils.MyDB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * SessionReminderService
 * Fetches sessions that are coming up soon so the app can warn the user.
 *
 * Usage in controller:
 *   SessionReminderService reminderService = new SessionReminderService();
 *   List<TherapySession> reminders = reminderService.getUpcomingSessions(userId, 24);
 */
public class SessionReminderService {

    private final Connection cnx = MyDB.getInstance().getConnection();

    // ─────────────────────────────────────────────────────────────────────────
    // GET SESSIONS IN THE NEXT N HOURS (for a specific user)
    // ─────────────────────────────────────────────────────────────────────────
    /**
     * Returns scheduled sessions for a user that start within the next [hours] hours.
     * E.g. pass hours=24 for "tomorrow's sessions", hours=1 for "sessions in the next hour".
     */
    public List<TherapySession> getUpcomingSessions(int userId, int hours) throws SQLException {
        List<TherapySession> list = new ArrayList<>();

        String sql = "SELECT * FROM TherapySession " +
                "WHERE user_id = ? " +
                "  AND session_status = 'Scheduled' " +
                "  AND session_date BETWEEN NOW() AND DATE_ADD(NOW(), INTERVAL ? HOUR) " +
                "ORDER BY session_date ASC";

        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, userId);
        ps.setInt(2, hours);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            list.add(map(rs));
        }
        return list;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET THE NEXT SINGLE UPCOMING SESSION (for a "next session" banner)
    // ─────────────────────────────────────────────────────────────────────────
    /**
     * Returns the very next scheduled session for a user, or null if none.
     */
    public TherapySession getNextSession(int userId) throws SQLException {
        String sql = "SELECT * FROM TherapySession " +
                "WHERE user_id = ? " +
                "  AND session_status = 'Scheduled' " +
                "  AND session_date > NOW() " +
                "ORDER BY session_date ASC " +
                "LIMIT 1";

        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, userId);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            return map(rs);
        }
        return null;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CHECK IF USER HAS A SESSION TODAY
    // ─────────────────────────────────────────────────────────────────────────
    public boolean hasSessionToday(int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM TherapySession " +
                "WHERE user_id = ? " +
                "  AND session_status = 'Scheduled' " +
                "  AND DATE(session_date) = CURDATE()";

        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, userId);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            return rs.getInt(1) > 0;
        }
        return false;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET SESSIONS IN THE NEXT N DAYS (broader overview)
    // ─────────────────────────────────────────────────────────────────────────
    public List<TherapySession> getSessionsInNextDays(int userId, int days) throws SQLException {
        List<TherapySession> list = new ArrayList<>();

        String sql = "SELECT * FROM TherapySession " +
                "WHERE user_id = ? " +
                "  AND session_status = 'Scheduled' " +
                "  AND session_date BETWEEN NOW() AND DATE_ADD(NOW(), INTERVAL ? DAY) " +
                "ORDER BY session_date ASC";

        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, userId);
        ps.setInt(2, days);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            list.add(map(rs));
        }
        return list;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPER: Map ResultSet row → TherapySession object
    // ─────────────────────────────────────────────────────────────────────────
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
