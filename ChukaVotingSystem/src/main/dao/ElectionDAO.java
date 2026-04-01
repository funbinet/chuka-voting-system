package main.dao;

import main.models.Election;
import main.utils.Constants;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ElectionDAO {

    private final Connection conn;

    public ElectionDAO() {
        this.conn = DBConnection.getInstance().getConnection();
    }

    public boolean hasOverlappingElection(int facultyId, Timestamp start, Timestamp end) {
        String sql = "SELECT COUNT(*) FROM elections WHERE faculty_id = ? AND status <> ? " +
                "AND start_date < ? AND end_date > ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, facultyId);
            ps.setString(2, Constants.STATUS_CLOSED);
            ps.setTimestamp(3, end);
            ps.setTimestamp(4, start);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
            System.err.println("âŒ Overlap check error: " + e.getMessage());
            return false;
        }
    }

    public boolean createElection(Election election) {
        String sql = "INSERT INTO elections (title, faculty_id, start_date, end_date, status, created_by) VALUES (?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, election.getTitle());
            ps.setInt(2, election.getFacultyId());
            ps.setTimestamp(3, election.getStartDate());
            ps.setTimestamp(4, election.getEndDate());
            ps.setString(5, Constants.STATUS_UPCOMING);
            ps.setInt(6, election.getCreatedBy());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("âŒ Create election error: " + e.getMessage());
            return false;
        }
    }

    public Election findById(int electionId) {
        String sql = "SELECT e.*, f.faculty_name FROM elections e " +
                "JOIN faculties f ON e.faculty_id = f.faculty_id WHERE e.election_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, electionId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapRow(rs);
            }
        } catch (SQLException e) {
            System.err.println("âŒ Find election error: " + e.getMessage());
        }
        return null;
    }

    public List<Election> getElectionsByFaculty(int facultyId) {
        List<Election> list = new ArrayList<>();
        String sql = "SELECT e.*, f.faculty_name FROM elections e " +
                "JOIN faculties f ON e.faculty_id = f.faculty_id WHERE e.faculty_id = ? ORDER BY e.start_date DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, facultyId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("âŒ Get elections by faculty error: " + e.getMessage());
        }
        return list;
    }

    public List<Election> getActiveElectionsByFaculty(int facultyId) {
        List<Election> list = new ArrayList<>();
        String sql = "SELECT e.*, f.faculty_name FROM elections e " +
                "JOIN faculties f ON e.faculty_id = f.faculty_id WHERE e.faculty_id = ? AND e.status = ? ORDER BY e.start_date ASC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, facultyId);
            ps.setString(2, Constants.STATUS_ACTIVE);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("âŒ Get active elections error: " + e.getMessage());
        }
        return list;
    }

    public List<Election> getAllElections() {
        List<Election> list = new ArrayList<>();
        String sql = "SELECT e.*, f.faculty_name FROM elections e " +
                "JOIN faculties f ON e.faculty_id = f.faculty_id ORDER BY e.start_date DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("âŒ Get all elections error: " + e.getMessage());
        }
        return list;
    }

    public boolean updateStatus(int electionId, String status) {
        String sql = "UPDATE elections SET status = ? WHERE election_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, electionId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("âŒ Update election status error: " + e.getMessage());
            return false;
        }
    }

    public boolean addCandidateToElection(int electionId, int applicationId) {
        String sql = "INSERT INTO election_candidates (election_id, application_id) VALUES (?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, electionId);
            ps.setInt(2, applicationId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("âŒ Add candidate to election error: " + e.getMessage());
            return false;
        }
    }

    public boolean hasCandidates(int electionId) {
        String sql = "SELECT COUNT(*) FROM election_candidates WHERE election_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, electionId);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
            System.err.println("âŒ Election candidate count error: " + e.getMessage());
            return false;
        }
    }

    private Election mapRow(ResultSet rs) throws SQLException {
        Election e = new Election();
        e.setElectionId(rs.getInt("election_id"));
        e.setTitle(rs.getString("title"));
        e.setFacultyId(rs.getInt("faculty_id"));
        e.setFacultyName(rs.getString("faculty_name"));
        e.setStartDate(rs.getTimestamp("start_date"));
        e.setEndDate(rs.getTimestamp("end_date"));
        e.setStatus(rs.getString("status"));
        e.setCreatedBy(rs.getInt("created_by"));
        e.setCreatedAt(rs.getTimestamp("created_at"));
        return e;
    }
}
