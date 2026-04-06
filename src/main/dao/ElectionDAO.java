package main.dao;

import main.models.Election;
import main.utils.Constants;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ElectionDAO {

    public ElectionDAO() {}

    private Connection getConnection() {
        return DBConnection.getInstance().getConnection();
    }

    // Removed hasOverlappingElection as parallel elections are now permitted without timeframe restrictions.

    public int createElection(Election election) {
        String sql = "INSERT INTO elections (title, faculty_id, position_id, start_date, end_date, status, created_by) VALUES (?,?,?,?,?,?,?)";
        try (PreparedStatement ps = getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, election.getTitle());
            if (election.getFacultyId() > 0) ps.setInt(2, election.getFacultyId());
            else ps.setNull(2, Types.INTEGER);
            ps.setInt(3, election.getPositionId());
            ps.setTimestamp(4, election.getStartDate());
            ps.setTimestamp(5, election.getEndDate());
            ps.setString(6, Constants.STATUS_UPCOMING);
            ps.setInt(7, election.getCreatedBy());
            int rows = ps.executeUpdate();
            if (rows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Create election error: " + e.getMessage());
        }
        return -1;
    }

    public Election findById(int electionId) {
        String sql = "SELECT e.*, f.faculty_name, p.position_name FROM elections e " +
                "LEFT JOIN faculties f ON e.faculty_id = f.faculty_id " +
                "JOIN positions p ON e.position_id = p.position_id " +
                "WHERE e.election_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, electionId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapRow(rs);
            }
        } catch (SQLException e) {
            System.err.println("❌ Find election error: " + e.getMessage());
        }
        return null;
    }

    public List<Election> getElectionsByFaculty(int facultyId) {
        List<Election> list = new ArrayList<>();
        String sql = "SELECT e.*, f.faculty_name, p.position_name FROM elections e " +
                "LEFT JOIN faculties f ON e.faculty_id = f.faculty_id " +
                "JOIN positions p ON e.position_id = p.position_id " +
                "WHERE (e.faculty_id = ? OR e.faculty_id IS NULL) ORDER BY e.start_date DESC";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, facultyId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ Get elections by faculty error: " + e.getMessage());
        }
        return list;
    }

    public List<Election> getActiveElectionsByFaculty(int facultyId) {
        List<Election> list = new ArrayList<>();
        String sql = "SELECT e.*, f.faculty_name, p.position_name FROM elections e " +
                "LEFT JOIN faculties f ON e.faculty_id = f.faculty_id " +
                "JOIN positions p ON e.position_id = p.position_id " +
                "WHERE (e.faculty_id = ? OR e.faculty_id IS NULL) AND e.status = ? ORDER BY e.start_date ASC";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, facultyId);
            ps.setString(2, Constants.STATUS_ACTIVE);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ Get active elections error: " + e.getMessage());
        }
        return list;
    }

    public List<Election> getAllActiveElections() {
        List<Election> list = new ArrayList<>();
        String sql = "SELECT e.*, f.faculty_name, p.position_name FROM elections e " +
                "LEFT JOIN faculties f ON e.faculty_id = f.faculty_id " +
                "JOIN positions p ON e.position_id = p.position_id " +
                "WHERE e.status = ? ORDER BY e.start_date ASC";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, Constants.STATUS_ACTIVE);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ Get all active elections error: " + e.getMessage());
        }
        return list;
    }

    public List<Election> getAllElections() {
        List<Election> list = new ArrayList<>();
        String sql = "SELECT e.*, f.faculty_name, p.position_name FROM elections e " +
                "LEFT JOIN faculties f ON e.faculty_id = f.faculty_id " +
                "JOIN positions p ON e.position_id = p.position_id " +
                "ORDER BY e.start_date DESC";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ Get all elections error: " + e.getMessage());
        }
        return list;
    }

    public boolean updateStatus(int electionId, String status) {
        String sql = "UPDATE elections SET status = ? WHERE election_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, electionId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ Update election status error: " + e.getMessage());
            return false;
        }
    }

    public boolean addCandidateToElection(int electionId, int applicationId) {
        String sql = "INSERT INTO election_candidates (election_id, application_id) VALUES (?,?)";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, electionId);
            ps.setInt(2, applicationId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ Add candidate to election error: " + e.getMessage());
            return false;
        }
    }

    public boolean hasCandidates(int electionId) {
        String sql = "SELECT COUNT(*) FROM election_candidates WHERE election_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, electionId);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
            System.err.println("❌ Election candidate count error: " + e.getMessage());
            return false;
        }
    }

    public boolean updateElection(int electionId, String newTitle, Integer facultyId, int positionId, Timestamp start, Timestamp end) {
        String sql = "UPDATE elections SET title = ?, faculty_id = ?, position_id = ?, start_date = ?, end_date = ? WHERE election_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, newTitle);
            if (facultyId != null && facultyId > 0) ps.setInt(2, facultyId);
            else ps.setNull(2, Types.INTEGER);
            ps.setInt(3, positionId);
            ps.setTimestamp(4, start);
            ps.setTimestamp(5, end);
            ps.setInt(6, electionId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ Update election error: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteElection(int electionId) {
        try {
            getConnection().setAutoCommit(false);
            
            // Delete votes attached to this election
            try (PreparedStatement psVotes = getConnection().prepareStatement("DELETE FROM votes WHERE election_id = ?")) {
                psVotes.setInt(1, electionId);
                psVotes.executeUpdate();
            }
            
            // Delete candidates bound to this election
            try (PreparedStatement psCands = getConnection().prepareStatement("DELETE FROM election_candidates WHERE election_id = ?")) {
                psCands.setInt(1, electionId);
                psCands.executeUpdate();
            }
            
            // Finally delete the election itself
            try (PreparedStatement psElec = getConnection().prepareStatement("DELETE FROM elections WHERE election_id = ?")) {
                psElec.setInt(1, electionId);
                int rows = psElec.executeUpdate();
                if (rows == 0) {
                    getConnection().rollback();
                    return false;
                }
            }
            
            getConnection().commit();
            return true;
        } catch (SQLException e) {
            try {
                getConnection().rollback();
            } catch (SQLException rollbackEx) {
                System.err.println("❌ Error rolling back delete election: " + rollbackEx.getMessage());
            }
            System.err.println("❌ Delete election error: " + e.getMessage());
            return false;
        } finally {
            try {
                getConnection().setAutoCommit(true);
            } catch (SQLException e) {
                System.err.println("❌ Error restoring auto-commit: " + e.getMessage());
            }
        }
    }

    private Election mapRow(ResultSet rs) throws SQLException {
        Election e = new Election();
        e.setElectionId(rs.getInt("election_id"));
        e.setTitle(rs.getString("title"));
        e.setFacultyId(rs.getInt("faculty_id"));
        e.setFacultyName(rs.getString("faculty_name") == null ? "University Global" : rs.getString("faculty_name"));
        e.setPositionId(rs.getInt("position_id"));
        e.setPositionName(rs.getString("position_name"));
        e.setStartDate(rs.getTimestamp("start_date"));
        e.setEndDate(rs.getTimestamp("end_date"));
        e.setStatus(rs.getString("status"));
        e.setCreatedBy(rs.getInt("created_by"));
        e.setCreatedAt(rs.getTimestamp("created_at"));
        return e;
    }
}
