package main.dao;

import main.models.AdminNotification;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AdminNotificationDAO {

    public AdminNotificationDAO() {}

    private Connection getConnection() {
        return DBConnection.getInstance().getConnection();
    }

    public boolean createNotification(AdminNotification notification) {
        String sql = "INSERT INTO admin_notifications (admin_id, title, message, is_read) VALUES (?, ?, ?, false)";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            if (notification.getAdminId() > 0) {
                ps.setInt(1, notification.getAdminId());
            } else {
                ps.setNull(1, java.sql.Types.INTEGER);
            }
            ps.setString(2, notification.getTitle());
            ps.setString(3, notification.getMessage());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ Create notification error: " + e.getMessage());
            return false;
        }
    }

    public List<AdminNotification> getUnreadNotificationsForAdmin(int adminId) {
        List<AdminNotification> list = new ArrayList<>();
        // Fetch notifications specifically for this admin AND global ones (admin_id IS NULL)
        String sql = "SELECT * FROM admin_notifications WHERE (admin_id = ? OR admin_id IS NULL) AND is_read = false ORDER BY created_at DESC";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, adminId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ Get notifications error: " + e.getMessage());
        }
        return list;
    }

    public boolean markAsRead(int notificationId) {
        String sql = "UPDATE admin_notifications SET is_read = true WHERE id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, notificationId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ Mark as read error: " + e.getMessage());
            return false;
        }
    }

    private AdminNotification mapRow(ResultSet rs) throws SQLException {
        AdminNotification an = new AdminNotification();
        an.setId(rs.getInt("id"));
        an.setAdminId(rs.getInt("admin_id")); // Returns 0 if NULL, which is fine
        an.setTitle(rs.getString("title"));
        an.setMessage(rs.getString("message"));
        an.setRead(rs.getBoolean("is_read"));
        an.setCreatedAt(rs.getTimestamp("created_at"));
        return an;
    }
}
