package main.dao;

import main.models.Announcement;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AnnouncementDAO {
    private Connection conn;

    public AnnouncementDAO() {
        this.conn = DBConnection.getInstance().getConnection();
    }

    public List<Announcement> getAllActive() {
        List<Announcement> list = new ArrayList<>();
        String sql = "SELECT a.*, adm.full_name FROM announcements a " +
                     "JOIN admins adm ON a.posted_by = adm.admin_id " +
                     "WHERE a.is_active = TRUE ORDER BY a.posted_at DESC";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public List<Announcement> getAll() {
        List<Announcement> list = new ArrayList<>();
        String sql = "SELECT a.*, adm.full_name FROM announcements a " +
                     "JOIN admins adm ON a.posted_by = adm.admin_id " +
                     "ORDER BY a.posted_at DESC";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public boolean createAnnouncement(int adminId, String title, String body) {
        String sql = "INSERT INTO announcements (title, body, posted_by) VALUES (?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, title);
            ps.setString(2, body);
            ps.setInt(3, adminId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public boolean updateAnnouncement(int id, String title, String body) {
        String sql = "UPDATE announcements SET title=?, body=? WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, title);
            ps.setString(2, body);
            ps.setInt(3, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public boolean deactivate(int id) {
        String sql = "UPDATE announcements SET is_active = FALSE WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
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
