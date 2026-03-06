package main.services;

import main.dao.CandidateDAO;
import main.models.Candidate;
import main.models.Student;
import main.utils.Constants;

import java.util.List;

public class CandidateService {

    private CandidateDAO candidateDAO;

    public CandidateService() {
        this.candidateDAO = new CandidateDAO();
    }

    // Student applies — checks eligibility first
    public String applyForCandidacy(Student student, int positionId, String manifesto) {
        // Check eligibility
        if (!student.isEligibleForCandidacy()) {
            StringBuilder reason = new StringBuilder("❌ You are not eligible:\n");
            if (student.isHasDisciplineCase())
                reason.append("• You have an active discipline case.\n");
            if (student.getGpa() < Constants.MIN_GPA)
                reason.append("• Your GPA (").append(student.getGpa())
                      .append(") is below the minimum (").append(Constants.MIN_GPA).append(").\n");
            if (student.getYearOfStudy() < Constants.MIN_YEAR_OF_STUDY)
                reason.append("• First year students cannot run for positions.\n");
            if (!student.isVerified())
                reason.append("• Your account phone number is not verified.\n");
            return reason.toString();
        }

        // Check if already applied for this position
        if (candidateDAO.hasApplied(student.getStudentId(), positionId)) {
            return "❌ You have already applied for this position.";
        }

        if (manifesto == null || manifesto.trim().isEmpty()) {
            return "❌ Please provide a manifesto.";
        }

        boolean success = candidateDAO.applyForCandidacy(student.getStudentId(), positionId, manifesto);
        return success
            ? "✅ Application submitted! Awaiting admin review and peer nominations."
            : "❌ Application failed. Please try again.";
    }

    // Peer nomination
    public String nominateCandidate(int applicationId, int nominatingStudentId) {
        boolean success = candidateDAO.nominateCandidate(applicationId, nominatingStudentId);
        return success ? "✅ Nomination recorded!" : "❌ You may have already nominated this candidate.";
    }

    // Admin approve
    public boolean approveApplication(int applicationId, int adminId) {
        return candidateDAO.approveApplication(applicationId, adminId);
    }

    // Admin reject
    public boolean rejectApplication(int applicationId, int adminId, String reason) {
        return candidateDAO.rejectApplication(applicationId, adminId, reason);
    }

    public List<Candidate> getPendingApplications() {
        return candidateDAO.getPendingApplications();
    }

    public List<Candidate> getApprovedCandidatesByFaculty(int facultyId) {
        return candidateDAO.getApprovedCandidatesByFaculty(facultyId);
    }

    public List<Candidate> getCandidatesForElection(int electionId) {
        return candidateDAO.getCandidatesForElection(electionId);
    }
}
