package services;

import models.Comment;
import utils.MyDB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CommentService {

    // NEW: moderation client
    private final PerspectiveModerationService moderation = new PerspectiveModerationService();

    private Connection conn() throws SQLException {
        Connection c = MyDB.getInstance().getConnection();
        if (c == null) throw new SQLException("MyDB.getConnection() returned null.");
        return c;
    }

    public void addComment(int contentId, int userId, String text) {

        // ✅ NEW: block toxic comments BEFORE inserting
        if (moderation.isToxic(text)) {
            throw new ToxicCommentException("Your comment looks toxic/abusive. Please rephrase and try again.");
        }

        String sql = "INSERT INTO comments (content_id, user_id, text, upvotes, upvoters_json, downvoters_json) " +
                "VALUES (?, ?, ?, 0, '[]', '[]')";

        try {
            Connection c = conn();
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setInt(1, contentId);
                ps.setInt(2, userId);
                ps.setString(3, text);
                ps.executeUpdate();
            }
        } catch (ToxicCommentException e) {
            // let controller handle it
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("DB insert failed: " + e.getMessage(), e);
        }
    }

    public List<Comment> getCommentsByContentId(int contentId) {
        List<Comment> list = new ArrayList<>();

        String sql =
                "SELECT c.id, c.content_id, c.user_id, c.text, c.created_at, " +
                        "       COALESCE(c.upvotes,0) AS upvotes, c.upvoters_json, c.downvoters_json, " +
                        "       u.name AS user_name " +
                        "FROM comments c " +
                        "LEFT JOIN users u ON u.id = c.user_id " +
                        "WHERE c.content_id = ? " +
                        "ORDER BY c.created_at DESC, c.id DESC";

        try {
            Connection c = conn();
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setInt(1, contentId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        List<Integer> downList = parseJsonIds(rs.getString("downvoters_json"));
                        int downvotes = downList.size(); // ✅ computed

                        list.add(new Comment(
                                rs.getInt("id"),
                                rs.getInt("content_id"),
                                rs.getInt("user_id"),
                                rs.getString("user_name"),
                                rs.getString("text"),
                                rs.getTimestamp("created_at"),
                                rs.getInt("upvotes"),
                                downvotes
                        ));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    // ---------- TOGGLE VOTING ----------

    public void upvoteComment(int commentId, int userId) {
        toggleVote(commentId, userId, true);
    }

    public void downvoteComment(int commentId, int userId) {
        toggleVote(commentId, userId, false);
    }

    private void toggleVote(int commentId, int userId, boolean up) {
        String select = "SELECT COALESCE(upvotes,0) AS upvotes, upvoters_json, downvoters_json FROM comments WHERE id=?";
        String update = "UPDATE comments SET upvotes=?, upvoters_json=?, downvoters_json=? WHERE id=?";

        Connection c = null;
        boolean oldAuto = true;

        try {
            c = conn();
            oldAuto = c.getAutoCommit();
            c.setAutoCommit(false);

            int upvotes;
            List<Integer> upList;
            List<Integer> downList;

            try (PreparedStatement ps = c.prepareStatement(select)) {
                ps.setInt(1, commentId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return;

                    upvotes = rs.getInt("upvotes");
                    upList = parseJsonIds(rs.getString("upvoters_json"));
                    downList = parseJsonIds(rs.getString("downvoters_json"));
                }
            }

            boolean hadUp = upList.contains(userId);
            boolean hadDown = downList.contains(userId);

            if (up) {
                if (hadUp) {
                    upList.remove((Integer) userId);
                    upvotes = Math.max(0, upvotes - 1);
                } else {
                    upList.add(userId);
                    upvotes = upvotes + 1;

                    // switch: remove downvote if existed (no counter column to update)
                    if (hadDown) downList.remove((Integer) userId);
                }
            } else {
                if (hadDown) {
                    downList.remove((Integer) userId);
                } else {
                    downList.add(userId);

                    // switch: remove upvote if existed
                    if (hadUp) {
                        upList.remove((Integer) userId);
                        upvotes = Math.max(0, upvotes - 1);
                    }
                }
            }

            try (PreparedStatement ps = c.prepareStatement(update)) {
                ps.setInt(1, upvotes);
                ps.setString(2, toJson(upList));
                ps.setString(3, toJson(downList));
                ps.setInt(4, commentId);
                ps.executeUpdate();
            }

            c.commit();

        } catch (Exception e) {
            try { if (c != null) c.rollback(); } catch (Exception ignored) {}
            e.printStackTrace();
        } finally {
            try { if (c != null) c.setAutoCommit(oldAuto); } catch (Exception ignored) {}
        }
    }

    // ---------- JSON helpers ----------

    private List<Integer> parseJsonIds(String json) {
        List<Integer> list = new ArrayList<>();
        if (json == null) return list;
        json = json.trim();
        if (json.length() < 2) return list;
        if (!json.startsWith("[") || !json.endsWith("]")) return list;

        String inner = json.substring(1, json.length() - 1).trim();
        if (inner.isEmpty()) return list;

        String[] parts = inner.split(",");
        for (String p : parts) {
            String s = p.trim();
            if (!s.isEmpty()) {
                try { list.add(Integer.parseInt(s)); } catch (NumberFormatException ignored) {}
            }
        }
        return list;
    }

    private String toJson(List<Integer> list) {
        return list.toString();
    }

    public void deleteComment(int id) {
        String sql = "DELETE FROM comments WHERE id=?";
        try {
            Connection c = conn();
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}