package services;

import models.Comment;
import utils.MyDB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CommentService {

    /**
     * CONNECTION NOTE
     * ---------------
     * utils.MyDB stores a shared Connection instance.
     * Do NOT close the Connection in try-with-resources.
     */
    private Connection conn() throws SQLException {
        Connection c = MyDB.getInstance().getConnection();
        if (c == null) {
            throw new SQLException("MyDB.getConnection() returned null. Check MySQL is running and DB credentials.");
        }
        return c;
    }

    public void addComment(Comment c) {
        // NOTE: We keep using the existing table structure (no new columns required).
        String sql = "INSERT INTO comments (content_id, user_id, text, upvotes) VALUES (?, ?, ?, ?)";

        try {
            Connection cnx = conn();
            try (PreparedStatement pst = cnx.prepareStatement(sql)) {
                pst.setInt(1, c.getContentId());
                pst.setInt(2, 1);
                pst.setString(3, c.getText());
                pst.setInt(4, 0);
                pst.executeUpdate();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<Comment> getCommentsByContent(int contentId) {
        List<Comment> list = new ArrayList<>();
        String sql = "SELECT id, content_id, user_id, text, COALESCE(upvotes,0) AS upvotes, created_at " +
                "FROM comments WHERE content_id = ? ORDER BY created_at DESC, id DESC";

        try {
            Connection cnx = conn();
            try (PreparedStatement pst = cnx.prepareStatement(sql)) {
                pst.setInt(1, contentId);
                try (ResultSet rs = pst.executeQuery()) {
                    while (rs.next()) {
                        list.add(new Comment(
                                rs.getInt("id"),
                                rs.getInt("content_id"),
                                rs.getString("text"),
                                rs.getTimestamp("created_at"),
                                rs.getInt("upvotes"),
                                0 // downvotes not stored (or set from DB if you have it)
                        ));

                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    // Backward-compatible name (some controllers call this)
    public List<Comment> getCommentsByContentId(int contentId) {
        return getCommentsByContent(contentId);
    }


    public void upvoteComment(int commentId) {
        String sql = "UPDATE comments SET upvotes = COALESCE(upvotes,0) + 1 WHERE id = ?";

        try {
            Connection cnx = conn();
            try (PreparedStatement pst = cnx.prepareStatement(sql)) {
                pst.setInt(1, commentId);
                pst.executeUpdate();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Downvote without adding a new DB column:
     * we decrement upvotes down to 0.
     */
    public void downvoteComment(int commentId) {
        String sql = "UPDATE comments SET upvotes = GREATEST(COALESCE(upvotes,0) - 1, 0) WHERE id = ?";

        try {
            Connection cnx = conn();
            try (PreparedStatement pst = cnx.prepareStatement(sql)) {
                pst.setInt(1, commentId);
                pst.executeUpdate();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void deleteComment(int id) {
        String sql = "DELETE FROM comments WHERE id = ?";

        try {
            Connection cnx = conn();
            try (PreparedStatement pst = cnx.prepareStatement(sql)) {
                pst.setInt(1, id);
                pst.executeUpdate();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void addComment(int contentId, String text) {
        addComment(new Comment(contentId, text));
    }

}
