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
     * MyDB is a Singleton that holds a shared Connection instance.
     * We DO NOT close the Connection (only statements/resultsets).
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
        String sql = "INSERT INTO content (title, description, type, source_url, image_url, category, upvotes, downvotes, upvoters_json, downvoters_json) " +
                "VALUES (?,?,?,?,?, ?, 0, 0, '[]', '[]')";

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

    // -------------------------
    // ✅ TOGGLE VOTING (NEW)
    // -------------------------

    /**
     * New API (recommended): pass userId (toggle + switch)
     */
    public void upvoteContent(int contentId, int userId) {
        toggleVote(contentId, userId, true);
    }

    public void downvoteContent(int contentId, int userId) {
        toggleVote(contentId, userId, false);
    }

    /**
     * Backwards-compatible: if old controllers still call these, we vote as "anonymous"
     * (won't toggle properly). Prefer updating controllers to pass userId.
     */
    public void upvoteContent(int contentId) {
        vote(contentId, true);
    }

    public void upvote(int contentId) {
        upvoteContent(contentId);
    }

    public void downvoteContent(int contentId) {
        vote(contentId, false);
    }

    public void downvote(int contentId) {
        downvoteContent(contentId);
    }

    // Old behavior (no user) — kept for compatibility
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

    /**
     * Toggle logic using upvoters_json / downvoters_json.
     * - upvote:
     *    if already upvoted -> remove upvote
     *    else -> add upvote and remove possible downvote
     * - downvote:
     *    if already downvoted -> remove downvote
     *    else -> add downvote and remove possible upvote
     */
    private void toggleVote(int contentId, int userId, boolean up) {
        String select = "SELECT COALESCE(upvotes,0) AS upvotes, COALESCE(downvotes,0) AS downvotes, upvoters_json, downvoters_json FROM content WHERE id=?";

        String update = "UPDATE content SET upvotes=?, downvotes=?, upvoters_json=?, downvoters_json=? WHERE id=?";

        Connection c = null;
        boolean oldAuto = true;

        try {
            c = conn();
            oldAuto = c.getAutoCommit();
            c.setAutoCommit(false);

            int upvotes;
            int downvotes;
            List<Integer> upList;
            List<Integer> downList;

            try (PreparedStatement ps = c.prepareStatement(select)) {
                ps.setInt(1, contentId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return;

                    upvotes = rs.getInt("upvotes");
                    downvotes = rs.getInt("downvotes");
                    upList = parseJsonIds(rs.getString("upvoters_json"));
                    downList = parseJsonIds(rs.getString("downvoters_json"));
                }
            }

            boolean hadUp = upList.contains(userId);
            boolean hadDown = downList.contains(userId);

            if (up) {
                if (hadUp) {
                    // toggle off upvote
                    upList.remove((Integer) userId);
                    upvotes = Math.max(0, upvotes - 1);
                } else {
                    // add upvote
                    upList.add(userId);
                    upvotes = upvotes + 1;

                    // remove downvote if existed
                    if (hadDown) {
                        downList.remove((Integer) userId);
                        downvotes = Math.max(0, downvotes - 1);
                    }
                }
            } else {
                if (hadDown) {
                    // toggle off downvote
                    downList.remove((Integer) userId);
                    downvotes = Math.max(0, downvotes - 1);
                } else {
                    // add downvote
                    downList.add(userId);
                    downvotes = downvotes + 1;

                    // remove upvote if existed
                    if (hadUp) {
                        upList.remove((Integer) userId);
                        upvotes = Math.max(0, upvotes - 1);
                    }
                }
            }

            try (PreparedStatement ps = c.prepareStatement(update)) {
                ps.setInt(1, upvotes);
                ps.setInt(2, downvotes);
                ps.setString(3, toJson(upList));
                ps.setString(4, toJson(downList));
                ps.setInt(5, contentId);
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

    // -------------------------
    // Helpers for JSON columns
    // -------------------------

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
        return list.toString(); // produces [1, 2, 3]
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
    public List<Content> getRecommended(int userId, int limit) {
        List<Content> list = new ArrayList<>();

        try {
            Connection c = conn();

            /*
             * FIX #1: Preferences should not be dominated by repeated logs.
             * Use the best (max) weight per content_id for this user, then sum by (category,type).
             * This makes recommendations actually shift when you upvote different kinds of content.
             */
            String prefSql = """
            SELECT COALESCE(NULLIF(TRIM(c.category),''), 'General') AS category,
                   COALESCE(NULLIF(TRIM(c.type),''), 'Article')     AS type,
                   SUM(x.best_weight) AS score
            FROM (
                SELECT content_id, MAX(weight) AS best_weight
                FROM user_content_events
                WHERE user_id = ?
                GROUP BY content_id
            ) x
            JOIN content c ON c.id = x.content_id
            GROUP BY COALESCE(NULLIF(TRIM(c.category),''), 'General'),
                     COALESCE(NULLIF(TRIM(c.type),''), 'Article')
            ORDER BY score DESC
            LIMIT 3
        """;

            List<String> cats = new ArrayList<>();
            List<String> types = new ArrayList<>();

            try (PreparedStatement ps = c.prepareStatement(prefSql)) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        cats.add(rs.getString("category"));
                        types.add(rs.getString("type"));
                    }
                }
            }

            // If no history -> fallback
            if (cats.isEmpty()) {
                return dedupeById(getTopThisWeek(limit));
            }

            // Build OR conditions: (category=? AND LOWER(type)=LOWER(?)) OR ...
            StringBuilder wherePairs = new StringBuilder();
            for (int i = 0; i < cats.size(); i++) {
                if (i > 0) wherePairs.append(" OR ");
                wherePairs.append("(COALESCE(NULLIF(TRIM(category),''),'General') = ? AND LOWER(type) = LOWER(?))");
            }

            /*
             * FIX #2: DISTINCT to protect against any future join changes.
             * FIX #3: Exclude already-interacted content (same as your old logic).
             */
            String sql = """
            SELECT DISTINCT id,title,description,type,source_url,image_url,category,created_at,
                   COALESCE(upvotes,0) AS upvotes,
                   COALESCE(downvotes,0) AS downvotes
            FROM content
            WHERE ( %s )
              AND id NOT IN (
                            SELECT content_id
                            FROM user_content_events
                            WHERE user_id = ?
                              AND event_type IN ('UPVOTE','DOWNVOTE','COMMENT','OPEN_LINK')
                          )
            ORDER BY (COALESCE(upvotes,0) - COALESCE(downvotes,0)) DESC,
                     created_at DESC
            LIMIT ?
        """.formatted(wherePairs);

            try (PreparedStatement ps = c.prepareStatement(sql)) {
                int idx = 1;

                for (int i = 0; i < cats.size(); i++) {
                    ps.setString(idx++, cats.get(i));
                    ps.setString(idx++, types.get(i));
                }

                ps.setInt(idx++, userId);
                ps.setInt(idx, limit);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) list.add(map(rs));
                }
            }

            // ✅ Bulletproof: remove duplicates by id in Java too
            list = dedupeById(list);

            // If still not enough results, fill with category-only (also deduped)
            if (list.size() < limit) {
                int remaining = limit - list.size();

                String placeholders = String.join(",", cats.stream().map(x -> "?").toList());

                String fillSql = """
                SELECT DISTINCT id,title,description,type,source_url,image_url,category,created_at,
                       COALESCE(upvotes,0) AS upvotes,
                       COALESCE(downvotes,0) AS downvotes
                FROM content
                WHERE COALESCE(NULLIF(TRIM(category),''),'General') IN (%s)
                  AND id NOT IN (SELECT content_id FROM user_content_events WHERE user_id = ?)
                  AND id NOT IN (%s)
                ORDER BY (COALESCE(upvotes,0) - COALESCE(downvotes,0)) DESC,
                         created_at DESC
                LIMIT ?
            """.formatted(placeholders, alreadyInListPlaceholders(list.size()));

                try (PreparedStatement ps = c.prepareStatement(fillSql)) {
                    int idx = 1;

                    for (String cat : cats) ps.setString(idx++, cat);
                    ps.setInt(idx++, userId);

                    for (Content x : list) ps.setInt(idx++, x.getId());

                    ps.setInt(idx, remaining);

                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) list.add(map(rs));
                    }
                }

                list = dedupeById(list);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list.stream().limit(limit).collect(java.util.stream.Collectors.toList());
    }

    // helper for dynamic "NOT IN (?, ?, ?)" placeholders
    private String alreadyInListPlaceholders(int n) {
        if (n <= 0) return "0"; // NOT IN (0) does nothing for positive ids
        return String.join(",", java.util.Collections.nCopies(n, "?"));
    }
    private List<Content> dedupeById(List<Content> input) {
        if (input == null || input.isEmpty()) return new ArrayList<>();
        java.util.Map<Integer, Content> unique = new java.util.LinkedHashMap<>();
        for (Content c : input) {
            if (c != null) unique.put(c.getId(), c);
        }
        return new ArrayList<>(unique.values());
    }
}