package main.dao;

import main.models.Coalition;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CoalitionDAO {

    private Connection getConnection() {
        return DBConnection.getInstance().getConnection();
    }

    public Coalition findById(int coalitionId) {
        String sql = "SELECT * FROM coalitions WHERE coalition_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, coalitionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Coalition c = new Coalition();
                    c.setCoalitionId(rs.getInt("coalition_id"));
                    c.setName(rs.getString("name"));
                    c.setMotto(rs.getString("motto"));
                    c.setCreatedAt(rs.getTimestamp("created_at"));
                    return c;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean existsById(int coalitionId) {
        String sql = "SELECT 1 FROM coalitions WHERE coalition_id = ? LIMIT 1";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, coalitionId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Coalition> getAllCoalitions() {
        List<Coalition> list = new ArrayList<>();
        String sql = "SELECT * FROM coalitions ORDER BY coalition_id ASC";
        try (PreparedStatement ps = getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Coalition c = new Coalition();
                c.setCoalitionId(rs.getInt("coalition_id"));
                c.setName(rs.getString("name"));
                c.setMotto(rs.getString("motto"));
                c.setCreatedAt(rs.getTimestamp("created_at"));
                list.add(c);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public boolean createCoalition(String name, String motto) {
        String sql = "INSERT INTO coalitions (name, motto) VALUES (?, ?)";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, motto);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateCoalition(int id, String name, String motto) {
        String sql = "UPDATE coalitions SET name=?, motto=? WHERE coalition_id=?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, motto);
            ps.setInt(3, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteCoalition(int id) {
        String sql = "DELETE FROM coalitions WHERE coalition_id=?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
