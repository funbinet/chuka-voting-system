package main.dao;

import main.models.Announcement;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AnnouncementDAO {
    public AnnouncementDAO() {}

    private Connection getConnection() {
        return DBConnection.getInstance().getConnection();
    }

    public List<Announcement> getAllActive() {
        List<Announcement> list = new ArrayList<>();
        String sql = "SELECT a.*, COALESCE(adm.full_name, 'System Admin') AS full_name FROM announcements a " +
                     "LEFT JOIN admins adm ON a.posted_by = adm.admin_id " +
                     "WHERE a.is_active = TRUE ORDER BY a.posted_at DESC";
        try (Statement st = getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public List<Announcement> getAll() {
        List<Announcement> list = new ArrayList<>();
        String sql = "SELECT a.*, COALESCE(adm.full_name, 'System Admin') AS full_name FROM announcements a " +
                     "LEFT JOIN admins adm ON a.posted_by = adm.admin_id " +
                     "ORDER BY a.posted_at DESC";
        try (Statement st = getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public boolean createAnnouncement(int adminId, String title, String body) {
        String sql = "INSERT INTO announcements (title, body, posted_by) VALUES (?,?,?)";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, title);
            ps.setString(2, body);
            ps.setInt(3, adminId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public boolean updateAnnouncement(int id, String title, String body) {
        String sql = "UPDATE announcements SET title=?, body=? WHERE id=?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, title);
            ps.setString(2, body);
            ps.setInt(3, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public boolean deactivate(int id) {
        String sql = "UPDATE announcements SET is_active = FALSE WHERE id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public int deactivateAllActive() {
        String sql = "UPDATE announcements SET is_active = FALSE WHERE is_active = TRUE";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            return ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public boolean deleteAnnouncement(int id) {
        String sql = "DELETE FROM announcements WHERE id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public int deleteAllAnnouncements() {
        String sql = "DELETE FROM announcements";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            int affected = ps.executeUpdate();

            // Keep IDs predictable after a full clear.
            try (PreparedStatement reset = getConnection().prepareStatement("ALTER TABLE announcements AUTO_INCREMENT = 1")) {
                reset.executeUpdate();
            } catch (SQLException ignored) {
                // Not fatal if auto-increment reset is not supported by current engine/settings.
            }

            return affected;
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    private Announcement mapRow(ResultSet rs) throws SQLException {
        Announcement a = new Announcement();
        a.setId(rs.getInt("id"));
        a.setTitle(rs.getString("title"));
        a.setBody(rs.getString("body"));
        a.setPostedBy(rs.getInt("posted_by"));
        a.setAdminName(rs.getString("full_name"));
        a.setPostedAt(rs.getTimestamp("posted_at"));
        a.setActive(rs.getBoolean("is_active"));
        return a;
    }
}
