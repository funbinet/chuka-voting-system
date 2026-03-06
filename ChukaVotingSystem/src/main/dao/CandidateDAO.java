package main.dao;

import main.models.Candidate;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CandidateDAO {

    private Connection conn;

    public CandidateDAO() {
        this.conn = DBConnection.getInstance().getConnection();
    }

    // Student applies for candidacy
    public boolean applyForCandidacy(int studentId, int positionId, String manifesto) {
        String sql = "INSERT INTO candidate_applications (student_id, position_id, manifesto) VALUES (?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setInt(2, positionId);
            ps.setString(3, manifesto);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ Apply candidacy error: " + e.getMessage());
            return false;
        }
    }

    // Get all pending applications (for admin review)
    public List<Candidate> getPendingApplications() {
        return getApplicationsByStatus("PENDING");
    }

    // Get approved candidates for a faculty
    public List<Candidate> getApprovedCandidatesByFaculty(int facultyId) {
        List<Candidate> list = new ArrayList<>();
        String sql = "SELECT ca.*, s.full_name, s.reg_number, p.position_name, s.faculty_id " +
                     "FROM candidate_applications ca " +
                     "JOIN students s ON ca.student_id = s.student_id " +
                     "JOIN positions p ON ca.position_id = p.position_id " +
                     "WHERE ca.status = 'APPROVED' AND s.faculty_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, facultyId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("❌ Get approved candidates error: " + e.getMessage());
        }
        return list;
    }

    // Get approved candidates for an election
    public List<Candidate> getCandidatesForElection(int electionId) {
        List<Candidate> list = new ArrayList<>();
        String sql = "SELECT ca.*, s.full_name, s.reg_number, p.position_name, s.faculty_id " +
                     "FROM election_candidates ec " +
                     "JOIN candidate_applications ca ON ec.application_id = ca.application_id " +
                     "JOIN students s ON ca.student_id = s.student_id " +
                     "JOIN positions p ON ca.position_id = p.position_id " +
                     "WHERE ec.election_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, electionId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("❌ Get candidates for election error: " + e.getMessage());
        }
        return list;
    }

    // Admin approves application
    public boolean approveApplication(int applicationId, int adminId) {
        String sql = "UPDATE candidate_applications SET status='APPROVED', reviewed_by=?, reviewed_at=NOW() " +
                     "WHERE application_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, adminId);
            ps.setInt(2, applicationId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ Approve application error: " + e.getMessage());
            return false;
        }
    }

    // Admin rejects application
    public boolean rejectApplication(int applicationId, int adminId, String reason) {
        String sql = "UPDATE candidate_applications SET status='REJECTED', reviewed_by=?, " +
                     "rejection_reason=?, reviewed_at=NOW() WHERE application_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, adminId);
            ps.setString(2, reason);
            ps.setInt(3, applicationId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ Reject application error: " + e.getMessage());
            return false;
        }
    }

    // Check if student already applied for position
    public boolean hasApplied(int studentId, int positionId) {
        String sql = "SELECT COUNT(*) FROM candidate_applications WHERE student_id=? AND position_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setInt(2, positionId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) {
            System.err.println("❌ Has applied check error: " + e.getMessage());
        }
        return false;
    }

    // Nominate a candidate (peer signature)
    public boolean nominateCandidate(int applicationId, int nominatedBy) {
        String sql = "INSERT INTO nominations (application_id, nominated_by) VALUES (?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, applicationId);
            ps.setInt(2, nominatedBy);
            ps.executeUpdate();
            updateNominationCount(applicationId);
            return true;
        } catch (SQLException e) {
            System.err.println("❌ Nominate error: " + e.getMessage());
            return false;
        }
    }

    private void updateNominationCount(int applicationId) throws SQLException {
        String sql = "UPDATE candidate_applications SET nomination_count = " +
                     "(SELECT COUNT(*) FROM nominations WHERE application_id=?) WHERE application_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, applicationId);
            ps.setInt(2, applicationId);
            ps.executeUpdate();
        }
    }

    private List<Candidate> getApplicationsByStatus(String status) {
        List<Candidate> list = new ArrayList<>();
        String sql = "SELECT ca.*, s.full_name, s.reg_number, p.position_name, s.faculty_id " +
                     "FROM candidate_applications ca " +
                     "JOIN students s ON ca.student_id = s.student_id " +
                     "JOIN positions p ON ca.position_id = p.position_id " +
                     "WHERE ca.status = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("❌ Get applications by status error: " + e.getMessage());
        }
        return list;
    }

    private Candidate mapRow(ResultSet rs) throws SQLException {
        Candidate c = new Candidate();
        c.setApplicationId(rs.getInt("application_id"));
        c.setStudentId(rs.getInt("student_id"));
        c.setStudentName(rs.getString("full_name"));
        c.setRegNumber(rs.getString("reg_number"));
        c.setPositionId(rs.getInt("position_id"));
        c.setPositionName(rs.getString("position_name"));
        c.setManifesto(rs.getString("manifesto"));
        c.setNominationCount(rs.getInt("nomination_count"));
        c.setStatus(rs.getString("status"));
        c.setRejectionReason(rs.getString("rejection_reason"));
        c.setAppliedAt(rs.getTimestamp("applied_at"));
        c.setFacultyId(rs.getInt("faculty_id"));
        return c;
    }
}
