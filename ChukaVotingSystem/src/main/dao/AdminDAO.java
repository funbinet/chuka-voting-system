package main.dao;

import main.models.Admin;
import java.sql.*;

public class AdminDAO {

    private Connection conn;

    public AdminDAO() {
        this.conn = DBConnection.getInstance().getConnection();
    }

    public Admin findByEmail(String email) {
        String sql = "SELECT * FROM admins WHERE email = ? AND is_active = TRUE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            System.err.println("❌ Admin findByEmail error: " + e.getMessage());
        }
        return null;
    }

    public Admin findById(int adminId) {
        String sql = "SELECT * FROM admins WHERE admin_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, adminId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            System.err.println("❌ Admin findById error: " + e.getMessage());
        }
        return null;
    }

    private Admin mapRow(ResultSet rs) throws SQLException {
        Admin a = new Admin();
        a.setAdminId(rs.getInt("admin_id"));
        a.setFullName(rs.getString("full_name"));
        a.setEmail(rs.getString("email"));
        a.setPhoneNumber(rs.getString("phone_number"));
        a.setPasswordHash(rs.getString("password_hash"));
        a.setActive(rs.getBoolean("is_active"));
        a.setCreatedAt(rs.getTimestamp("created_at"));
        return a;
    }
}
