package services;

import models.Journal;
import utils.MyDB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class JournalService {

    private Connection cnx;

    public JournalService() {
        try {
            // Directly create connection here
            cnx = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/mindnestt", "root", ""); // adjust username/password
            System.out.println("Database connected!");
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Failed to connect to database!");
        }
    }

    // ================= ADD =================
    public void add(Journal journal) {
        String sql = "INSERT INTO journal(title, content, mood, ai_analysis, created_at) VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, journal.getTitle());
            ps.setString(2, journal.getContent());
            ps.setString(3, journal.getMood());
            ps.setString(4, journal.getAiAnalysis());
            ps.setTimestamp(5, Timestamp.valueOf(journal.getDate()));

            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ================= GET ALL =================
    public List<Journal> getAll() {
        List<Journal> list = new ArrayList<>();
        String sql = "SELECT * FROM journal ORDER BY created_at DESC";

        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                Journal j = new Journal(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("content"),
                        rs.getString("mood"),
                        rs.getString("ai_analysis"),
                        rs.getTimestamp("created_at").toLocalDateTime()
                );
                list.add(j);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }

    // ================= DELETE =================
    public void delete(int id) {
        String sql = "DELETE FROM journal WHERE id = ?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ================= UPDATE =================
    public void update(Journal journal) {
        String sql = "UPDATE journal SET title = ?, content = ?, mood = ?, ai_analysis = ? WHERE id = ?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, journal.getTitle());
            ps.setString(2, journal.getContent());
            ps.setString(3, journal.getMood());
            ps.setString(4, journal.getAiAnalysis());
            ps.setInt(5, journal.getId());

            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}