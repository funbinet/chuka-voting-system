package main.dao;

import main.models.AuditLog;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AuditDAO {
    public AuditDAO() {}

    private Connection getConnection() {
        return DBConnection.getInstance().getConnection();
    }

    public void log(Integer studentId, String action, String description) {
        String sql = "INSERT INTO audit_logs (student_id, action, description) VALUES (?, ?, ?)";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            if (studentId != null) {
                ps.setInt(1, studentId);
            } else {
                ps.setNull(1, java.sql.Types.INTEGER);
            }
            ps.setString(2, action);
            ps.setString(3, description);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ Audit Log Error: " + e.getMessage());
        }
    }

    public List<AuditLog> getAuditLogs(String searchTerm, java.util.Date fromDate, java.util.Date toDate) {
        List<AuditLog> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT a.*, s.full_name FROM audit_logs a " +
            "LEFT JOIN students s ON a.student_id = s.student_id WHERE 1=1"
        );
        
        if (searchTerm != null && !searchTerm.isBlank()) {
            sql.append(" AND (a.action LIKE ? OR a.description LIKE ?)");
        }
        if (fromDate != null) {
            sql.append(" AND a.logged_at >= ?");
        }
        if (toDate != null) {
            sql.append(" AND a.logged_at <= ?");
        }
        sql.append(" ORDER BY a.logged_at DESC");

        try (PreparedStatement ps = getConnection().prepareStatement(sql.toString())) {
            int paramIdx = 1;
            if (searchTerm != null && !searchTerm.isBlank()) {
                String pattern = "%" + searchTerm + "%";
                ps.setString(paramIdx++, pattern);
                ps.setString(paramIdx++, pattern);
            }
            if (fromDate != null) {
                ps.setTimestamp(paramIdx++, new Timestamp(fromDate.getTime()));
            }
            if (toDate != null) {
                // To include the whole 'toDate', you might want to adjust it to the end of the day
                // but for now we'll use it as provided.
                ps.setTimestamp(paramIdx++, new Timestamp(toDate.getTime()));
            }

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                AuditLog log = new AuditLog();
                log.setLogId(rs.getInt("log_id"));
                int sId = rs.getInt("student_id");
                log.setStudentId(rs.wasNull() ? null : sId);
                log.setStudentName(rs.getString("full_name"));
                log.setAction(rs.getString("action"));
                log.setDescription(rs.getString("description"));
                log.setLoggedAt(rs.getTimestamp("logged_at"));
                list.add(log);
            }
        } catch (SQLException e) {
            System.err.println("❌ Get Audit Logs Error: " + e.getMessage());
        }
        return list;
    }
}
