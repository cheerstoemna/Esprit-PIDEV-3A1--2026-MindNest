package services;

import utils.MyDB;

import java.sql.Connection;
import java.sql.PreparedStatement;

/**
 * Analytics logging for recommendations.
 *
 * Fixes "recommendations not updating":
 * - UPVOTE/DOWNVOTE/OPEN_LINK are now idempotent (no duplicates for same user+content+type).
 * - UPVOTE removes DOWNVOTE (and vice versa) so preferences reflect the current signal.
 */
public class AnalyticsService {

    public void logEvent(int userId, int contentId, String type, int weight) {

        String normalized = (type == null) ? "" : type.trim().toUpperCase();

        String deleteSame = """
            DELETE FROM user_content_events
            WHERE user_id = ? AND content_id = ? AND event_type = ?
        """;

        String deleteOppositeVote = """
            DELETE FROM user_content_events
            WHERE user_id = ? AND content_id = ? AND event_type = ?
        """;

        String insert = """
            INSERT INTO user_content_events (user_id, content_id, event_type, weight)
            VALUES (?, ?, ?, ?)
        """;

        try {
            Connection c = MyDB.getInstance().getConnection();

            // For vote-like events, prevent duplicates
            boolean dedupe = normalized.equals("UPVOTE")
                    || normalized.equals("DOWNVOTE")
                    || normalized.equals("OPEN_LINK");

            if (dedupe) {
                // Remove same event (idempotent)
                try (PreparedStatement ps = c.prepareStatement(deleteSame)) {
                    ps.setInt(1, userId);
                    ps.setInt(2, contentId);
                    ps.setString(3, normalized);
                    ps.executeUpdate();
                }

                // If this is a vote, remove the opposite vote too
                if (normalized.equals("UPVOTE") || normalized.equals("DOWNVOTE")) {
                    String opposite = normalized.equals("UPVOTE") ? "DOWNVOTE" : "UPVOTE";
                    try (PreparedStatement ps = c.prepareStatement(deleteOppositeVote)) {
                        ps.setInt(1, userId);
                        ps.setInt(2, contentId);
                        ps.setString(3, opposite);
                        ps.executeUpdate();
                    }
                }
            }

            // Insert fresh
            try (PreparedStatement ps = c.prepareStatement(insert)) {
                ps.setInt(1, userId);
                ps.setInt(2, contentId);
                ps.setString(3, normalized);
                ps.setInt(4, weight);
                ps.executeUpdate();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}