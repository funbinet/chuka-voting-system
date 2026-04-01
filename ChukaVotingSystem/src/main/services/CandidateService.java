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
    private final AuditService auditService;
    private String lastMessage;

    public CandidateService() {
        this.candidateDAO = new CandidateDAO();
        this.studentDAO = new StudentDAO();
        this.auditService = AuditService.getInstance();
        this.lastMessage = "";
    }

    public String addCandidateDirectly(String regNumber, int positionId, String manifesto, int adminId) {
        Student student = studentDAO.findByRegNumber(regNumber);
        if (student == null) {
            lastMessage = "Student with registration number " + regNumber + " not found.";
            return lastMessage;
        }

        Integer positionFacultyId = candidateDAO.findFacultyIdByPosition(positionId);
        if (positionFacultyId == null) {
            lastMessage = "The selected position could not be found.";
            return lastMessage;
        }

        if (positionFacultyId != student.getFacultyId()) {
            lastMessage = "The student and position must be in the same faculty.";
            return lastMessage;
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

        // Get the newly created application ID
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

        // Auto-approve since admin is adding directly
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