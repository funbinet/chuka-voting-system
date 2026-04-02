package main.dao;

import main.models.Faculty;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FacultyDAO {

    public FacultyDAO() {}

    private Connection getConnection() {
        return DBConnection.getInstance().getConnection();
    }

    public List<Faculty> getAllFaculties() {
        List<Faculty> list = new ArrayList<>();
        String sql = "SELECT * FROM faculties ORDER BY faculty_name";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ Faculty getAllFaculties error: " + e.getMessage());
        }
        return list;
    }

    public Faculty findById(int id) {
        String sql = "SELECT * FROM faculties WHERE faculty_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            System.err.println("❌ Faculty findById error: " + e.getMessage());
        }
        return null;
    }

    private Faculty mapRow(ResultSet rs) throws SQLException {
        Faculty f = new Faculty();
        f.setFacultyId(rs.getInt("faculty_id"));
        f.setFacultyCode(rs.getString("faculty_code"));
        f.setFacultyName(rs.getString("faculty_name"));
        return f;
    }
}
