package main.dao;

import main.models.Student;
import main.utils.PasswordHasher;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class StudentDAO {

    private Connection conn;

    public StudentDAO() {
        this.conn = DBConnection.getInstance().getConnection();
    }

    // Register a new student
    public boolean registerStudent(Student student) {
        String sql = "INSERT INTO students (reg_number, full_name, email, phone_number, " +
                     "password_hash, faculty_id, year_of_study, gpa) VALUES (?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, student.getRegNumber());
            ps.setString(2, student.getFullName());
            ps.setString(3, student.getEmail());
            ps.setString(4, student.getPhoneNumber());
            ps.setString(5, PasswordHasher.hash(student.getPasswordHash())); // hash on insert
            ps.setInt(6, student.getFacultyId());
            ps.setInt(7, student.getYearOfStudy());
            ps.setDouble(8, student.getGpa());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ Register student error: " + e.getMessage());
            return false;
        }
    }

    // Find student by reg number
    public Student findByRegNumber(String regNumber) {
        String sql = "SELECT s.*, f.faculty_name FROM students s " +
                     "JOIN faculties f ON s.faculty_id = f.faculty_id " +
                     "WHERE s.reg_number = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, regNumber);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            System.err.println("❌ Find student error: " + e.getMessage());
        }
        return null;
    }

    // Find student by ID
    public Student findById(int studentId) {
        String sql = "SELECT s.*, f.faculty_name FROM students s " +
                     "JOIN faculties f ON s.faculty_id = f.faculty_id " +
                     "WHERE s.student_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            System.err.println("❌ Find student by ID error: " + e.getMessage());
        }
        return null;
    }

    // Get all students in a faculty
    public List<Student> findByFaculty(int facultyId) {
        List<Student> list = new ArrayList<>();
        String sql = "SELECT s.*, f.faculty_name FROM students s " +
                     "JOIN faculties f ON s.faculty_id = f.faculty_id " +
                     "WHERE s.faculty_id = ? AND s.is_active = TRUE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, facultyId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("❌ Find by faculty error: " + e.getMessage());
        }
        return list;
    }

    // Mark student as OTP-verified
    public boolean markVerified(int studentId) {
        String sql = "UPDATE students SET is_verified = TRUE WHERE student_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ Mark verified error: " + e.getMessage());
            return false;
        }
    }

    // Check if reg number already exists
    public boolean regNumberExists(String regNumber) {
        String sql = "SELECT COUNT(*) FROM students WHERE reg_number = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, regNumber);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) {
            System.err.println("❌ Check reg error: " + e.getMessage());
        }
        return false;
    }

    // Check if phone number already exists
    public boolean phoneExists(String phone) {
        String sql = "SELECT COUNT(*) FROM students WHERE phone_number = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, phone);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) {
            System.err.println("❌ Check phone error: " + e.getMessage());
        }
        return false;
    }

    private Student mapRow(ResultSet rs) throws SQLException {
        Student s = new Student();
        s.setStudentId(rs.getInt("student_id"));
        s.setRegNumber(rs.getString("reg_number"));
        s.setFullName(rs.getString("full_name"));
        s.setEmail(rs.getString("email"));
        s.setPhoneNumber(rs.getString("phone_number"));
        s.setPasswordHash(rs.getString("password_hash"));
        s.setFacultyId(rs.getInt("faculty_id"));
        s.setFacultyName(rs.getString("faculty_name"));
        s.setYearOfStudy(rs.getInt("year_of_study"));
        s.setGpa(rs.getDouble("gpa"));
        s.setHasDisciplineCase(rs.getBoolean("has_discipline_case"));
        s.setVerified(rs.getBoolean("is_verified"));
        s.setActive(rs.getBoolean("is_active"));
        s.setCreatedAt(rs.getTimestamp("created_at"));
        return s;
    }
}
