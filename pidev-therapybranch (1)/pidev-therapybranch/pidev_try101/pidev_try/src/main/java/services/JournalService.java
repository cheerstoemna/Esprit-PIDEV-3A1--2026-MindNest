package services;

import models.Journal;
import utils.MyDB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class JournalService {

    private final Connection cnx = MyDB.getInstance().getConnection();

    // ---------------- ADD ----------------
    public void add(Journal journal) {
        String sql = "INSERT INTO journal(title, content, mood, created_at) VALUES (?, ?, ?, ?)";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {

            ps.setString(1, journal.getTitle());
            ps.setString(2, journal.getContent());
            ps.setString(3, journal.getMood());
            ps.setTimestamp(4, Timestamp.valueOf(journal.getDate()));

            ps.executeUpdate();

            System.out.println("Journal added successfully!");

        } catch (SQLException e) {
            System.err.println("Error adding journal: " + e.getMessage());
        }
    }


    // ---------------- GET ALL ----------------
    public List<Journal> getAll() {

        List<Journal> list = new ArrayList<>();

        String sql = "SELECT * FROM journal ORDER BY created_at DESC";

        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {

                Journal j = new Journal(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("content")
                );

                // optional if your constructor supports it
                j.setMood(rs.getString("mood"));
                j.setDate(rs.getTimestamp("created_at").toLocalDateTime());

                list.add(j);
            }

        } catch (SQLException e) {

            System.err.println("Error fetching journals: " + e.getMessage());

        }

        return list;
    }


    // ---------------- DELETE ----------------
    public void delete(int id) {

        String sql = "DELETE FROM journal WHERE id = ?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {

            ps.setInt(1, id);

            int rows = ps.executeUpdate();

            if (rows > 0) {

                System.out.println("Journal deleted successfully!");

            } else {

                System.out.println("No journal found with ID: " + id);

            }

        } catch (SQLException e) {

            System.err.println("Error deleting journal: " + e.getMessage());

        }
    }


    // ---------------- UPDATE ----------------
    public void update(Journal journal) {

        String sql = "UPDATE journal SET title = ?, content = ?, mood = ? WHERE id = ?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {

            ps.setString(1, journal.getTitle());

            ps.setString(2, journal.getContent());

            ps.setString(3, journal.getMood());

            ps.setInt(4, journal.getId());

            int rows = ps.executeUpdate();

            if (rows > 0) {

                System.out.println("Journal updated successfully!");

            } else {

                System.out.println("No journal found with ID: " + journal.getId());

            }

        } catch (SQLException e) {

            System.err.println("Error updating journal: " + e.getMessage());

        }
    }

}
