package services;

import models.Content;
import utils.MyDB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ContentService {

    /**
     * CONNECTION NOTE
     * ---------------
     * Your utils.MyDB is a Singleton that holds a shared Connection instance.
     * Because of that, we DO NOT close the Connection in try-with-resources,
     * otherwise we would close it for the whole app.
     *
     * We only close PreparedStatement / ResultSet.
     */
    private Connection conn() throws SQLException {
        Connection c = MyDB.getInstance().getConnection();
        if (c == null) {
            throw new SQLException("MyDB.getConnection() returned null. Check DB name/credentials and that MySQL is running.");
        }
        return c;
    }

    public List<Content> getAllContent() {
        List<Content> list = new ArrayList<>();
        String sql = "SELECT id, title, description, type, source_url, image_url, category, created_at, " +
                "COALESCE(upvotes,0) AS upvotes, COALESCE(downvotes,0) AS downvotes " +
                "FROM content ORDER BY id DESC";

        try {
            Connection c = conn();
            try (PreparedStatement ps = c.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<Content> getLatestContent(int limit) {
        List<Content> list = new ArrayList<>();
        String sql = "SELECT id, title, description, type, source_url, image_url, category, created_at, " +
                "COALESCE(upvotes,0) AS upvotes, COALESCE(downvotes,0) AS downvotes " +
                "FROM content ORDER BY created_at DESC, id DESC LIMIT ?";

        try {
            Connection c = conn();
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setInt(1, limit);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) list.add(map(rs));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * "Top blogs" = most upvoted posts created in last 7 days
     * (fallback to overall best if none this week)
     */
    public List<Content> getTopThisWeek(int limit) {
        List<Content> list = new ArrayList<>();
        String sql = "SELECT id, title, description, type, source_url, image_url, category, created_at, " +
                "COALESCE(upvotes,0) AS upvotes, COALESCE(downvotes,0) AS downvotes " +
                "FROM content " +
                "WHERE created_at >= (NOW() - INTERVAL 7 DAY) " +
                "ORDER BY COALESCE(upvotes,0) DESC, (COALESCE(upvotes,0)-COALESCE(downvotes,0)) DESC, created_at DESC " +
                "LIMIT ?";

        try {
            Connection c = conn();
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setInt(1, limit);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) list.add(map(rs));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!list.isEmpty()) return list;

        // fallback: if no posts in last week
        String sql2 = "SELECT id, title, description, type, source_url, image_url, category, created_at, " +
                "COALESCE(upvotes,0) AS upvotes, COALESCE(downvotes,0) AS downvotes " +
                "FROM content ORDER BY COALESCE(upvotes,0) DESC, created_at DESC LIMIT ?";

        try {
            Connection c = conn();
            try (PreparedStatement ps = c.prepareStatement(sql2)) {
                ps.setInt(1, limit);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) list.add(map(rs));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    public List<String> getDistinctCategories() {
        List<String> cats = new ArrayList<>();
        String sql = "SELECT DISTINCT category FROM content WHERE category IS NOT NULL AND TRIM(category) <> '' ORDER BY category ASC";

        try {
            Connection c = conn();
            try (PreparedStatement ps = c.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) cats.add(rs.getString(1));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return cats;
    }

    public List<Content> getByType(String type) {
        List<Content> list = new ArrayList<>();
        String sql = "SELECT id, title, description, type, source_url, image_url, category, created_at, " +
                "COALESCE(upvotes,0) AS upvotes, COALESCE(downvotes,0) AS downvotes " +
                "FROM content WHERE LOWER(type) = LOWER(?) ORDER BY created_at DESC, id DESC";

        try {
            Connection c = conn();
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, type);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) list.add(map(rs));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public void addContent(Content content) {
        String sql = "INSERT INTO content (title, description, type, source_url, image_url, category) VALUES (?,?,?,?,?,?)";

        try {
            Connection c = conn();
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, content.getTitle());
                ps.setString(2, content.getDescription());
                ps.setString(3, content.getType());
                ps.setString(4, content.getSourceUrl());
                ps.setString(5, content.getImageUrl());
                ps.setString(6, content.getCategory());
                ps.executeUpdate();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateContent(Content content) {
        String sql = "UPDATE content SET title=?, description=?, type=?, source_url=?, image_url=?, category=? WHERE id=?";

        try {
            Connection c = conn();
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, content.getTitle());
                ps.setString(2, content.getDescription());
                ps.setString(3, content.getType());
                ps.setString(4, content.getSourceUrl());
                ps.setString(5, content.getImageUrl());
                ps.setString(6, content.getCategory());
                ps.setInt(7, content.getId());
                ps.executeUpdate();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void deleteContent(int id) {
        String sql = "DELETE FROM content WHERE id=?";

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

    public void upvoteContent(int contentId) {
        vote(contentId, true);
    }

    // Backwards-compatible aliases (some controllers call these names)
    public void upvote(int contentId) {
        upvoteContent(contentId);
    }

    public void downvoteContent(int contentId) {
        vote(contentId, false);
    }

    // Backwards-compatible aliases (some controllers call these names)
    public void downvote(int contentId) {
        downvoteContent(contentId);
    }

    private void vote(int contentId, boolean up) {
        String sql = up
                ? "UPDATE content SET upvotes = COALESCE(upvotes,0) + 1 WHERE id=?"
                : "UPDATE content SET downvotes = COALESCE(downvotes,0) + 1 WHERE id=?";

        try {
            Connection c = conn();
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setInt(1, contentId);
                ps.executeUpdate();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Content map(ResultSet rs) throws SQLException {
        return new Content(
                rs.getInt("id"),
                rs.getString("title"),
                rs.getString("description"),
                rs.getString("type"),
                rs.getString("source_url"),
                rs.getString("image_url"),
                rs.getString("category"),
                rs.getTimestamp("created_at"),
                rs.getInt("upvotes"),
                rs.getInt("downvotes")
        );
    }

    public Content getById(int id) {
        String sql = "SELECT id, title, description, type, source_url, image_url, category, created_at, " +
                "COALESCE(upvotes,0) AS upvotes, COALESCE(downvotes,0) AS downvotes " +
                "FROM content WHERE id = ? LIMIT 1";
        try {
            Connection c = conn();
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return map(rs);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
