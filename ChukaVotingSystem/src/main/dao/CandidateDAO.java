package main.dao;

import main.models.Candidate;
import main.utils.Constants;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CandidateDAO {

    private final Connection conn;

    public CandidateDAO() {
        this.conn = DBConnection.getInstance().getConnection();
    }

    public boolean applyForCandidacy(int studentId, int positionId, String manifesto) {
        String sql = "INSERT INTO candidate_applications (student_id, position_id, manifesto, status) VALUES (?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setInt(2, positionId);
            ps.setString(3, manifesto);
            ps.setString(4, Constants.APP_PENDING);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("âŒ Apply candidacy error: " + e.getMessage());
            return false;
        }
    }

    public List<Candidate> getPendingApplications() {
        return getApplicationsByStatus(Constants.APP_PENDING);
    }

    public List<Candidate> getPendingApplicationsByFaculty(int facultyId) {
        List<Candidate> list = new ArrayList<>();
        String sql = baseApplicationQuery() + " WHERE ca.status = ? AND s.faculty_id = ? ORDER BY ca.applied_at ASC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, Constants.APP_PENDING);
            ps.setInt(2, facultyId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("âŒ Get pending by faculty error: " + e.getMessage());
        }
        return list;
    }

    public List<Candidate> getApprovedCandidatesByFaculty(int facultyId) {
        List<Candidate> list = new ArrayList<>();
        String sql = baseApplicationQuery() + " WHERE ca.status = ? AND s.faculty_id = ? ORDER BY p.position_name, s.full_name";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, Constants.APP_APPROVED);
            ps.setInt(2, facultyId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("âŒ Get approved candidates error: " + e.getMessage());
        }
        return list;
    }

    public List<Candidate> getApprovedCandidatesAcrossFaculties() {
        List<Candidate> list = new ArrayList<>();
        String sql = baseApplicationQuery() + " WHERE ca.status = ? ORDER BY f.faculty_name, p.position_name, s.full_name";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, Constants.APP_APPROVED);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ Get approved candidates across faculties error: " + e.getMessage());
        }
        return list;
    }

    public List<Candidate> getCandidatesForElection(int electionId) {
        List<Candidate> list = new ArrayList<>();
        String sql = baseApplicationQuery() +
                " JOIN election_candidates ec ON ec.application_id = ca.application_id" +
                " WHERE ec.election_id = ? AND ca.status = ? ORDER BY p.position_name, s.full_name";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, electionId);
            ps.setString(2, Constants.APP_APPROVED);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("âŒ Get candidates for election error: " + e.getMessage());
        }
        return list;
    }

    public boolean approveApplication(int applicationId, int adminId) {
        String sql = "UPDATE candidate_applications SET status=?, reviewed_by=?, reviewed_at=NOW(), rejection_reason=NULL " +
                "WHERE application_id=? AND status=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, Constants.APP_APPROVED);
            ps.setInt(2, adminId);
            ps.setInt(3, applicationId);
            ps.setString(4, Constants.APP_PENDING);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("âŒ Approve application error: " + e.getMessage());
            return false;
        }
    }

    public boolean rejectApplication(int applicationId, int adminId, String reason) {
        String sql = "UPDATE candidate_applications SET status=?, reviewed_by=?, rejection_reason=?, reviewed_at=NOW() " +
                "WHERE application_id=? AND status=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, Constants.APP_REJECTED);
            ps.setInt(2, adminId);
            ps.setString(3, reason);
            ps.setInt(4, applicationId);
            ps.setString(5, Constants.APP_PENDING);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("âŒ Reject application error: " + e.getMessage());
            return false;
        }
    }

    public String getApplicationStatus(int studentId, int positionId) {
        String sql = "SELECT status FROM candidate_applications WHERE student_id=? AND position_id=? ORDER BY applied_at DESC LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setInt(2, positionId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("status");
            }
        } catch (SQLException e) {
            System.err.println("âŒ Get status error: " + e.getMessage());
        }
        return null;
    }

    public boolean hasAnyActiveApplication(int studentId) {
        String sql = "SELECT COUNT(*) FROM candidate_applications WHERE student_id=? AND status IN (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setString(2, Constants.APP_PENDING);
            ps.setString(3, Constants.APP_APPROVED);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
            System.err.println("âŒ Active application check error: " + e.getMessage());
            return false;
        }
    }

    public boolean nominateCandidate(int applicationId, int nominatedBy) {
        String insertSql = "INSERT INTO nominations (application_id, nominated_by) VALUES (?,?)";
        String updateSql = "UPDATE candidate_applications SET nomination_count = " +
                "(SELECT COUNT(*) FROM nominations WHERE application_id=?) WHERE application_id=?";

        try {
            conn.setAutoCommit(false);
            try (PreparedStatement psInsert = conn.prepareStatement(insertSql);
                 PreparedStatement psUpdate = conn.prepareStatement(updateSql)) {
                psInsert.setInt(1, applicationId);
                psInsert.setInt(2, nominatedBy);
                psInsert.executeUpdate();

                psUpdate.setInt(1, applicationId);
                psUpdate.setInt(2, applicationId);
                psUpdate.executeUpdate();
            }
            conn.commit();
            return true;
        } catch (SQLException e) {
            try {
                conn.rollback();
            } catch (SQLException rollbackError) {
                rollbackError.printStackTrace();
            }
            System.err.println("âŒ Nominate error: " + e.getMessage());
            return false;
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean hasStudentNominated(int applicationId, int nominatedBy) {
        String sql = "SELECT COUNT(*) FROM nominations WHERE application_id=? AND nominated_by=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, applicationId);
            ps.setInt(2, nominatedBy);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
            System.err.println("âŒ Existing nomination check error: " + e.getMessage());
            return false;
        }
    }

    public int getTargetElectionId(int facultyId) {
        String sql = "SELECT election_id FROM elections WHERE faculty_id=? AND status=? ORDER BY start_date ASC LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, facultyId);
            ps.setString(2, Constants.STATUS_UPCOMING);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("election_id");
            }
        } catch (SQLException e) {
            System.err.println("âŒ Get target election error: " + e.getMessage());
        }
        return -1;
    }

    public boolean addCandidateToElection(int electionId, int applicationId) {
        String sql = "INSERT INTO election_candidates (election_id, application_id) VALUES (?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, electionId);
            ps.setInt(2, applicationId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("âŒ Add to election candidates error: " + e.getMessage());
            return false;
        }
    }

    public Candidate findById(int applicationId) {
        String sql = baseApplicationQuery() + " WHERE ca.application_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, applicationId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapRow(rs);
            }
        } catch (SQLException e) {
            System.err.println("âŒ Find candidate by ID error: " + e.getMessage());
        }
        return null;
    }

    public Integer findFacultyIdByPosition(int positionId) {
        String sql = "SELECT faculty_id FROM positions WHERE position_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, positionId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("faculty_id");
            }
        } catch (SQLException e) {
            System.err.println("âŒ Position faculty lookup error: " + e.getMessage());
        }
        return null;
    }

    public boolean isSameStudentAsApplication(int applicationId, int studentId) {
        String sql = "SELECT COUNT(*) FROM candidate_applications WHERE application_id=? AND student_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, applicationId);
            ps.setInt(2, studentId);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
            System.err.println("âŒ Self nomination check error: " + e.getMessage());
            return false;
        }
    }

    public int countApprovedCandidatesByFaculty(int facultyId) {
        String sql = "SELECT COUNT(*) FROM candidate_applications ca " +
                "JOIN students s ON ca.student_id = s.student_id WHERE ca.status=? AND s.faculty_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, Constants.APP_APPROVED);
            ps.setInt(2, facultyId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            System.err.println("âŒ Count approved candidates error: " + e.getMessage());
            return 0;
        }
    }

    private List<Candidate> getApplicationsByStatus(String status) {
        List<Candidate> list = new ArrayList<>();
        String sql = baseApplicationQuery() + " WHERE ca.status = ? ORDER BY ca.applied_at ASC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("âŒ Get applications by status error: " + e.getMessage());
        }
        return list;
    }

    private String baseApplicationQuery() {
        return "SELECT ca.*, s.full_name, s.reg_number, s.year_of_study, s.gpa, s.faculty_id, " +
                "f.faculty_name, p.position_name " +
                "FROM candidate_applications ca " +
                "JOIN students s ON ca.student_id = s.student_id " +
                "JOIN positions p ON ca.position_id = p.position_id " +
                "JOIN faculties f ON s.faculty_id = f.faculty_id";
    }

    private Candidate mapRow(ResultSet rs) throws SQLException {
        Candidate c = new Candidate();
        c.setApplicationId(rs.getInt("application_id"));
        c.setStudentId(rs.getInt("student_id"));
        c.setStudentName(rs.getString("full_name"));
        c.setRegNumber(rs.getString("reg_number"));
        c.setYearOfStudy(rs.getInt("year_of_study"));
        c.setGpa(rs.getDouble("gpa"));
        c.setPositionId(rs.getInt("position_id"));
        c.setPositionName(rs.getString("position_name"));
        c.setManifesto(rs.getString("manifesto"));
        c.setNominationCount(rs.getInt("nomination_count"));
        c.setStatus(rs.getString("status"));
        c.setRejectionReason(rs.getString("rejection_reason"));
        c.setAppliedAt(rs.getTimestamp("applied_at"));
        c.setReviewedAt(rs.getTimestamp("reviewed_at"));
        c.setReviewedBy(rs.getInt("reviewed_by"));
        c.setFacultyId(rs.getInt("faculty_id"));
        c.setFacultyName(rs.getString("faculty_name"));
        return c;
    }
}
