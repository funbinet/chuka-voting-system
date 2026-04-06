package main.services;

import main.dao.CandidateDAO;
import main.dao.CoalitionDAO;
import main.dao.StudentDAO;
import main.models.Candidate;
import main.models.Student;
import main.utils.Constants;
import main.utils.PositionRules;

import java.util.List;

public class CandidateService {

    private final CandidateDAO candidateDAO;
    private final CoalitionDAO coalitionDAO;
    private final StudentDAO studentDAO;
    private final main.dao.AdminNotificationDAO notificationDAO;
    private final AuditService auditService;
    private String lastMessage;

    public CandidateService() {
        this.candidateDAO = new CandidateDAO();
        this.coalitionDAO = new CoalitionDAO();
        this.studentDAO = new StudentDAO();
        this.notificationDAO = new main.dao.AdminNotificationDAO();
        this.auditService = AuditService.getInstance();
        this.lastMessage = "";
    }

    public String addCandidateDirectly(String regNumber, int positionId, String manifesto, int adminId, Integer coalitionId) {
        Student student = studentDAO.findByRegNumber(regNumber);
        if (student == null) {
            lastMessage = "Student with registration number " + regNumber + " not found.";
            return lastMessage;
        }

        String posName = "";
        try (java.sql.Connection conn = main.dao.DBConnection.getInstance().getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement("SELECT position_name FROM positions WHERE position_id=?")) {
            ps.setInt(1, positionId);
            java.sql.ResultSet rs = ps.executeQuery();
            if (rs.next()) posName = rs.getString("position_name").toLowerCase();
        } catch (java.sql.SQLException e) { e.printStackTrace(); }
        PositionRules.PositionCategory category = PositionRules.classify(posName);

        if (candidateDAO.hasAnyActiveApplication(student.getStudentId())) {
            lastMessage = "This student is already a candidate in the system.";
            return lastMessage;
        }

        Integer normalizedCoalitionId = normalizeCoalitionId(coalitionId);
        boolean success = candidateDAO.applyForCandidacy(student.getStudentId(), positionId, manifesto != null ? manifesto.trim() : "", normalizedCoalitionId);
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
            Integer targetFacultyId = category == PositionRules.PositionCategory.FACULTY_CHAIRMAN
                    ? student.getFacultyId()
                    : null;
            int targetElectionId = candidateDAO.getTargetElectionId(targetFacultyId, positionId);
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

    public boolean applyForCandidacyStudent(int studentId, int positionId, String manifesto, main.models.Position position, Integer coalitionId) {
        Student student = studentDAO.findById(studentId);
        if (student == null) {
            lastMessage = "Student not found.";
            return false;
        }

        if (!validateStudentBaselineEligibility(student)) {
            return false;
        }

        PositionRules.PositionCategory category = PositionRules.classify(position.getPositionName());
        if (!PositionRules.isCanonical(category)) {
            lastMessage = "Unsupported position mapping detected. Please contact an administrator to refresh the position setup.";
            return false;
        }

        position.setPositionName(PositionRules.canonicalLabel(category));

        if (category == PositionRules.PositionCategory.FACULTY_CHAIRMAN && student.getFacultyId() <= 0) {
            lastMessage = "Your profile has no faculty assigned. Please contact an administrator before applying for Faculty Chairman.";
            return false;
        }

        if (!PositionRules.isGenderEligible(student.getGender(), category)) {
            lastMessage = "You cannot apply for " + position.getPositionName() + ". Required gender: "
                    + PositionRules.requiredGender(category) + ". Your profile gender: "
                    + PositionRules.displayGender(student.getGender()) + ".";
            return false;
        }

        if (!PositionRules.isResidencyEligible(student.isResident(), category)) {
            lastMessage = "You cannot apply for " + position.getPositionName() + ". Required residency: "
                    + PositionRules.requiredResidencyLabel(category) + ". Your profile residency: "
                    + PositionRules.profileResidencyLabel(student.isResident()) + ".";
            return false;
        }

        if (candidateDAO.hasAnyActiveApplication(studentId)) {
            lastMessage = "You already have an active application.";
            return false;
        }

        Integer normalizedCoalitionId = normalizeCoalitionId(coalitionId);
        boolean success = candidateDAO.applyForCandidacy(studentId, positionId, manifesto != null ? manifesto.trim() : "", normalizedCoalitionId);
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
            Candidate app = candidateDAO.findById(applicationId);
            Integer electionScopeFacultyId = null;
            if (app != null) {
                PositionRules.PositionCategory category = PositionRules.classify(app.getPositionName());
                electionScopeFacultyId = category == PositionRules.PositionCategory.FACULTY_CHAIRMAN
                        ? app.getFacultyId()
                        : null;
            }
            int targetElectionId = (app != null)
                    ? candidateDAO.getTargetElectionId(electionScopeFacultyId, app.getPositionId())
                    : candidateDAO.getTargetElectionId(facultyId);
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

    public String updateCandidateApplication(int applicationId, int positionId, String manifesto, Integer coalitionId, int adminId) {
        Candidate current = candidateDAO.findById(applicationId);
        if (current == null) {
            return "Candidate application not found.";
        }

        Integer normalizedCoalitionId = normalizeCoalitionId(coalitionId);
        boolean updated = candidateDAO.updateApplicationDetails(
                applicationId,
                positionId,
                manifesto != null ? manifesto.trim() : "",
                normalizedCoalitionId
        );

        if (!updated) {
            return "Failed to update candidate details.";
        }

        Candidate refreshed = candidateDAO.findById(applicationId);
        if (refreshed != null
                && Constants.APP_APPROVED.equalsIgnoreCase(refreshed.getStatus())
                && current.getPositionId() != positionId) {
            candidateDAO.removeApplicationFromAllElections(applicationId);
            int targetElectionId = candidateDAO.getTargetElectionId(refreshed.getFacultyId(), positionId);
            if (targetElectionId != -1) {
                candidateDAO.addCandidateToElection(targetElectionId, applicationId);
            }
        }

        auditService.log(null, "CANDIDATE_UPDATED",
                "Admin " + adminId + " updated candidate application " + applicationId + " (position " + positionId + ")");
        return "Candidate details updated successfully.";
    }

    public String getLastMessage() {
        return lastMessage;
    }

    private Integer normalizeCoalitionId(Integer coalitionId) {
        if (coalitionId == null || coalitionId <= 0) {
            return null;
        }
        return coalitionDAO.existsById(coalitionId) ? coalitionId : null;
    }

    private boolean validateStudentBaselineEligibility(Student student) {
        if (!student.isActive()) {
            lastMessage = "Your account is inactive. Please contact an administrator.";
            return false;
        }
        if (student.getYearOfStudy() < Constants.MIN_YEAR_OF_STUDY) {
            lastMessage = "You cannot apply yet. Minimum year of study is " + Constants.MIN_YEAR_OF_STUDY + ".";
            return false;
        }
        if (student.getGpa() < Constants.MIN_GPA) {
            lastMessage = "You cannot apply yet. Minimum GPA is " + Constants.MIN_GPA
                    + ", but your current GPA is " + String.format("%.2f", student.getGpa()) + ".";
            return false;
        }
        if (student.isHasDisciplineCase()) {
            lastMessage = "You cannot apply while a discipline case is active on your profile.";
            return false;
        }
        return true;
    }
}