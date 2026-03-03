package services;

// ═══════════════════════════════════════════════════════════════════════════════
//  FILE: src/main/java/services/ConflictCheckService.java
// ═══════════════════════════════════════════════════════════════════════════════

import models.TherapySession;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ConflictCheckService {

    // ── FIX 1: "Cannot resolve symbol 'DataBaseConnection'" ──────────────────
    //  Open TherapySessionService.java, find where it gets a connection,
    //  and paste that exact line below instead.
    //  Common patterns:
    //    return utils.DataBaseConnection.getConnection();
    //    return utils.DBConnection.getConnection();
    //    return DriverManager.getConnection("jdbc:mysql://localhost:3306/mydb","root","");
    private Connection getConnection() throws SQLException {
        return utils.MyDB.getInstance().getConnection();    }

    // ─────────────────────────────────────────────────────────────────────────
    //  THE ONE METHOD THE CONTROLLER CALLS
    //
    //  excludeSessionId = null  when BOOKING  (new session)
    //                   = session.getId()  when EDITING  (exclude itself)
    // ─────────────────────────────────────────────────────────────────────────
    public ConflictResult checkConflicts(
            int userId, int therapistId,
            LocalDateTime start, int durationMinutes,
            Integer excludeSessionId) throws SQLException {

        LocalDateTime end = start.plusMinutes(durationMinutes);

        List<TherapySession> patientConflicts   = findPatientConflicts(userId,      start, end, excludeSessionId);
        List<TherapySession> therapistConflicts = findTherapistConflicts(therapistId, start, end, excludeSessionId);

        List<LocalDateTime> suggestions = new ArrayList<>();
        if (!patientConflicts.isEmpty() || !therapistConflicts.isEmpty()) {
            suggestions = findFreeSlots(userId, therapistId, start, durationMinutes, excludeSessionId);
        }

        return new ConflictResult(patientConflicts, therapistConflicts, suggestions);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  OVERLAP QUERIES
    //  Two sessions conflict when:  existing.start < newEnd  AND  existing.end > newStart
    //  Touching boundaries (existing.end == newStart) are NOT conflicts.
    // ─────────────────────────────────────────────────────────────────────────
    private List<TherapySession> findPatientConflicts(
            int userId, LocalDateTime start, LocalDateTime end, Integer excludeId) throws SQLException {
        String sql = "SELECT * FROM TherapySession"
                + " WHERE user_id = ?"
                + "   AND session_status NOT IN ('Cancelled')"
                + "   AND session_date < ?"
                + "   AND DATE_ADD(session_date, INTERVAL duration_minutes MINUTE) > ?"
                + (excludeId != null ? " AND session_id != ?" : "");
        return runQuery(sql, userId, end, start, excludeId);
    }

    private List<TherapySession> findTherapistConflicts(
            int therapistId, LocalDateTime start, LocalDateTime end, Integer excludeId) throws SQLException {
        String sql = "SELECT * FROM TherapySession"
                + " WHERE psychologist_id = ?"
                + "   AND session_status NOT IN ('Cancelled')"
                + "   AND session_date < ?"
                + "   AND DATE_ADD(session_date, INTERVAL duration_minutes MINUTE) > ?"
                + (excludeId != null ? " AND session_id != ?" : "");
        return runQuery(sql, therapistId, end, start, excludeId);
    }

    // ── FIX 2: "Missing return statement" ────────────────────────────────────
    //  Was a cascade from Fix 1. The explicit return at the bottom makes it
    //  bulletproof regardless.
    private List<TherapySession> runQuery(
            String sql, int id, LocalDateTime end, LocalDateTime start, Integer excludeId) throws SQLException {

        List<TherapySession> result = new ArrayList<>();
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.setTimestamp(2, Timestamp.valueOf(end));
            ps.setTimestamp(3, Timestamp.valueOf(start));
            if (excludeId != null) ps.setInt(4, excludeId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    TherapySession s = new TherapySession();
                    s.setSessionId(rs.getInt("session_id"));
                    s.setSessionDate(rs.getTimestamp("session_date").toLocalDateTime());
                    s.setDurationMinutes(rs.getInt("duration_minutes"));
                    s.setSessionStatus(rs.getString("session_status"));
                    result.add(s);
                }
            }
        }
        return result; // always reached
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  SLOT SUGGESTION — finds first 5 free 30-min windows for both people
    // ─────────────────────────────────────────────────────────────────────────
    private List<LocalDateTime> findFreeSlots(
            int userId, int therapistId, LocalDateTime from,
            int duration, Integer excludeId) throws SQLException {

        List<LocalDateTime> slots  = new ArrayList<>();
        LocalDateTime       cursor = roundToNext30(from);
        LocalDateTime       limit  = from.plusDays(7);

        try (Connection con = getConnection()) {
            while (cursor.isBefore(limit) && slots.size() < 5) {
                LocalTime t = cursor.toLocalTime();
                if (t.isBefore(LocalTime.of(8, 0))) {
                    cursor = cursor.toLocalDate().atTime(8, 0); continue;
                }
                if (t.isAfter(LocalTime.of(18, 0).minusMinutes(duration))) {
                    cursor = cursor.toLocalDate().plusDays(1).atTime(8, 0); continue;
                }
                LocalDateTime slotEnd = cursor.plusMinutes(duration);
                if (runQueryWithCon(con, "user_id",         userId,      cursor, slotEnd, excludeId).isEmpty()
                        && runQueryWithCon(con, "psychologist_id", therapistId, cursor, slotEnd, excludeId).isEmpty()) {
                    slots.add(cursor);
                }
                cursor = cursor.plusMinutes(30);
            }
        }
        return slots;
    }

    // New helper — same as runQuery() but reuses an existing connection
    private List<TherapySession> runQueryWithCon(
            Connection con, String column, int id,
            LocalDateTime start, LocalDateTime end, Integer excludeId) throws SQLException {

        String sql = "SELECT * FROM TherapySession"
                + " WHERE " + column + " = ?"
                + "   AND session_status NOT IN ('Cancelled')"
                + "   AND session_date < ?"
                + "   AND DATE_ADD(session_date, INTERVAL duration_minutes MINUTE) > ?"
                + (excludeId != null ? " AND session_id != ?" : "");

        List<TherapySession> result = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setTimestamp(2, Timestamp.valueOf(end));
            ps.setTimestamp(3, Timestamp.valueOf(start));
            if (excludeId != null) ps.setInt(4, excludeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    TherapySession s = new TherapySession();
                    s.setSessionId(rs.getInt("session_id"));
                    s.setSessionDate(rs.getTimestamp("session_date").toLocalDateTime());
                    s.setDurationMinutes(rs.getInt("duration_minutes"));
                    s.setSessionStatus(rs.getString("session_status"));
                    result.add(s);
                }
            }
        }
        return result;
    }

    private LocalDateTime roundToNext30(LocalDateTime dt) {
        int rem = dt.getMinute() % 30;
        return rem == 0 ? dt.withSecond(0).withNano(0)
                : dt.plusMinutes(30 - rem).withSecond(0).withNano(0);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  ConflictResult — returned by checkConflicts(), used in controller
    // ═════════════════════════════════════════════════════════════════════════
    public static class ConflictResult {

        // ── FIX 3 & 4: all fields are public, method is public ───────────────
        //  "suggestions has private access"  → fields are public final below
        //  "Cannot resolve method buildMessage" → method is public below
        //  Getters also added for whichever style your IDE prefers.

        public final List<TherapySession> patientConflicts;
        public final List<TherapySession> therapistConflicts;
        public final List<LocalDateTime>  suggestions;

        public ConflictResult(List<TherapySession> pc, List<TherapySession> tc, List<LocalDateTime> s) {
            this.patientConflicts   = pc;
            this.therapistConflicts = tc;
            this.suggestions        = s;
        }

        // Getters (same data as the public fields — both styles work)
        public List<TherapySession> getPatientConflicts()   { return patientConflicts; }
        public List<TherapySession> getTherapistConflicts() { return therapistConflicts; }
        public List<LocalDateTime>  getSuggestions()        { return suggestions; }

        public boolean hasConflict() {
            return !patientConflicts.isEmpty() || !therapistConflicts.isEmpty();
        }

        // Called by showConflictAlert() in ClientTherapyController
        public String buildMessage() {
            StringBuilder sb = new StringBuilder("This time slot is not available.\n\n");

            if (!patientConflicts.isEmpty()) {
                TherapySession c   = patientConflicts.get(0);
                LocalDateTime  end = c.getSessionDate().plusMinutes(c.getDurationMinutes());
                sb.append(String.format("You already have a session from %s to %s.\n",
                        fmt(c.getSessionDate()), fmt(end)));
            }

            if (!therapistConflicts.isEmpty()) {
                TherapySession c   = therapistConflicts.get(0);
                LocalDateTime  end = c.getSessionDate().plusMinutes(c.getDurationMinutes());
                sb.append(String.format("The therapist is already booked from %s to %s.\n",
                        fmt(c.getSessionDate()), fmt(end)));
            }

            if (!suggestions.isEmpty()) {
                sb.append("\nNearest available slots:\n");
                DateTimeFormatter f = DateTimeFormatter.ofPattern("EEE MMM d  'at'  HH:mm");
                for (LocalDateTime slot : suggestions) {
                    sb.append("  •  ").append(slot.format(f)).append("\n");
                }
            } else {
                sb.append("\nNo free slots found in the next 7 days.");
            }

            return sb.toString();
        }

        private String fmt(LocalDateTime dt) {
            return dt.format(DateTimeFormatter.ofPattern("HH:mm"));
        }
    }
}