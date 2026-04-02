package main.services;

import main.dao.CandidateDAO;
import main.dao.StudentDAO;
import main.models.Candidate;
import main.models.Student;
import main.utils.Constants;

import java.util.List;

public class CandidateService {

    private final CandidateDAO candidateDAO;
    private final StudentDAO studentDAO;
    private final main.dao.AdminNotificationDAO notificationDAO;
    private final AuditService auditService;
    private String lastMessage;

    public CandidateService() {
        this.candidateDAO = new CandidateDAO();
        this.studentDAO = new StudentDAO();
        this.notificationDAO = new main.dao.AdminNotificationDAO();
        this.auditService = AuditService.getInstance();
        this.lastMessage = "";
    }

    public String addCandidateDirectly(String regNumber, int positionId, String manifesto, int adminId) {
        Student student = studentDAO.findByRegNumber(regNumber);
        if (student == null) {
            lastMessage = "Student with registration number " + regNumber + " not found.";
            return lastMessage;
        }

        // Canonical Positions support:
        // If the position is faculty-tied (Chairman, Secretary, Treasurer), 
        // we implicitly use the student's own faculty in this modular design.
        String posName = "";
        try (java.sql.Connection conn = main.dao.DBConnection.getInstance().getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement("SELECT position_name FROM positions WHERE position_id=?")) {
            ps.setInt(1, positionId);
            java.sql.ResultSet rs = ps.executeQuery();
            if (rs.next()) posName = rs.getString("position_name").toLowerCase();
        } catch (java.sql.SQLException e) { e.printStackTrace(); }

        if (posName.contains("faculty")) {
            // For faculty positions, the candidate must belong to a faculty (which they do)
            // Existing logic checked positionFacultyId == student.getFacultyId()
            // But now positions have faculty_id = NULL. So we just assume it's for the student's faculty.
        }

        if (candidateDAO.hasAnyActiveApplication(student.getStudentId())) {
            lastMessage = "This student is already a candidate in the system.";
            return lastMessage;
        }

        boolean success = candidateDAO.applyForCandidacy(student.getStudentId(), positionId, manifesto != null ? manifesto.trim() : "");
        if (!success) {
            lastMessage = "Failed to add the candidate application.";
            return lastMessage;
        }

        List<Candidate> pending = candidateDAO.getPendingApplications();
        int applicationId = -1;
        for (Candidate c : pending) {
            if (c.getStudentId() == student.getStudentId() && c.getPositionId() == positionId) {
                applicationId = c.getApplicationId();
                break;
            }
        }

        if (applicationId == -1) {
             lastMessage = "Candidate added but could not retrieve application ID for immediate approval.";
             return lastMessage;
        }

        boolean approved = candidateDAO.approveApplication(applicationId, adminId);
        if (approved) {
            int targetElectionId = candidateDAO.getTargetElectionId(student.getFacultyId());
            if (targetElectionId != -1) {
                candidateDAO.addCandidateToElection(targetElectionId, applicationId);
                lastMessage = "Candidate added and approved for the upcoming election successfully.";
            } else {
                lastMessage = "Candidate added and approved successfully (no active election found to link yet).";
            }
            auditService.log(null, "CANDIDATE_ADDED_BY_ADMIN", 
                "Admin " + adminId + " added " + student.getFullName() + " as a candidate for position " + positionId);
        } else {
            lastMessage = "Candidate added but auto-approval failed.";
        }

        return lastMessage;
    }

    public boolean applyForCandidacyStudent(int studentId, int positionId, String manifesto, main.models.Position position) {
        Student student = studentDAO.findById(studentId);
        if (student == null) {
            lastMessage = "Student not found.";
            return false;
        }

        // Validate generic/position-specific rules
        String posName = position.getPositionName().toLowerCase();

        // 1. Faculty validation
        // Null or 0 faculty means it's a cross-faculty role (e.g. Male Resident) according to schema
        if (position.getFacultyId() > 0) {
            if (position.getFacultyId() != student.getFacultyId()) {
                lastMessage = "You cannot apply for a position outside your faculty.";
                return false;
            }
        }

        // 2. Gender validation
        if (posName.contains("male") && !posName.contains("female")) {
            if (!"MALE".equalsIgnoreCase(student.getGender())) {
                lastMessage = "This position is strictly for male candidates.";
                return false;
            }
        } else if (posName.contains("female")) {
            if (!"FEMALE".equalsIgnoreCase(student.getGender())) {
                lastMessage = "This position is strictly for female candidates.";
                return false;
            }
        }

        // 3. Residency validation
        if (posName.contains("non-resident") || posName.contains("non resident")) {
            if (student.isResident()) {
                lastMessage = "This position is strictly for non-resident candidates.";
                return false;
            }
        } else if (posName.contains("resident") && !posName.contains("non")) {
            if (!student.isResident()) {
                lastMessage = "This position is strictly for resident candidates.";
                return false;
            }
        }

        // 4. Duplicate checks
        if (candidateDAO.hasAnyActiveApplication(studentId)) {
            lastMessage = "You already have an active application.";
            return false;
        }

        boolean success = candidateDAO.applyForCandidacy(studentId, positionId, manifesto != null ? manifesto.trim() : "");
        if (success) {
            main.models.AdminNotification notif = new main.models.AdminNotification(
                0, // 0 = global for all admins
                "New Candidate Application",
                student.getFullName() + " applied for " + position.getPositionName()
            );
            notificationDAO.createNotification(notif);
            
            auditService.log(studentId, "APPLIED_CANDIDACY", "Applied for position: " + position.getPositionName());
            lastMessage = "Application submitted successfully and is pending admin review.";
            return true;
        } else {
            lastMessage = "An error occurred while submitting your application.";
            return false;
        }
    }

    public String approveStudentApplication(int applicationId, int adminId, int facultyId) {
        boolean approved = candidateDAO.approveApplication(applicationId, adminId);
        if (approved) {
            int targetElectionId = candidateDAO.getTargetElectionId(facultyId);
            if (targetElectionId != -1) {
                candidateDAO.addCandidateToElection(targetElectionId, applicationId);
                auditService.log(null, "CANDIDATE_APPROVED", "Admin " + adminId + " approved application " + applicationId);
                return "Application approved and added to live election.";
            } else {
                return "Application approved (no active election found yet).";
            }
        }
        return "Failed to approve application.";
    }

    public List<Candidate> getAllApprovedCandidates() {
        // We'll just return all approved candidates across all faculties
        return candidateDAO.getApprovedCandidatesAcrossFaculties();
    }

    public List<Candidate> getApprovedCandidatesByFaculty(int facultyId) {
        return candidateDAO.getApprovedCandidatesByFaculty(facultyId);
    }

    public List<Candidate> getCandidatesForElection(int electionId) {
        return candidateDAO.getCandidatesForElection(electionId);
    }

    public List<Candidate> getPendingApplications() {
        return candidateDAO.getPendingApplications();
    }

    public boolean removeCandidate(int applicationId, int adminId) {
        // Implementation for removing/rejecting an already approved candidate if needed
        boolean ok = candidateDAO.rejectApplication(applicationId, adminId, "Removed by administrator.");
        if (ok) {
            auditService.log(null, "CANDIDATE_REMOVED", "Admin " + adminId + " removed candidate application " + applicationId);
        }
        return ok;
    }

    public String getLastMessage() {
        return lastMessage;
    }
}