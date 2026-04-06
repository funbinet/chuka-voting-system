package main.services;

import main.dao.ElectionDAO;
import main.dao.StudentDAO;
import main.dao.VoteDAO;
import main.models.Candidate;
import main.models.Election;
import main.models.Student;
import main.utils.Constants;
import main.utils.PositionRules;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class VotingService {

    private final VoteDAO voteDAO;
    private final ElectionDAO electionDAO;
    private final CandidateService candidateService;
    private final StudentDAO studentDAO;
    private final AuditService auditService;
    private final EmailService emailService;
    private String lastMessage;

    public VotingService() {
        this.voteDAO = new VoteDAO();
        this.electionDAO = new ElectionDAO();
        this.candidateService = new CandidateService();
        this.studentDAO = new StudentDAO();
        this.auditService = AuditService.getInstance();
        this.emailService = EmailService.getInstance();
        this.lastMessage = "";
    }

    public String castVote(int studentId, int electionId, int positionId, int candidateApplicationId) {
        ElectionService electionService = new ElectionService();
        electionService.syncElectionStatuses();

        Student student = studentDAO.findById(studentId);
        Election election = electionDAO.findById(electionId);
        if (student == null) {
            lastMessage = "Student account could not be validated.";
            return lastMessage;
        }
        if (election == null) {
            lastMessage = "The selected election could not be found.";
            auditService.log(studentId, "VOTE_FAILED", lastMessage);
            return lastMessage;
        }
        if (!Constants.STATUS_ACTIVE.equals(election.getStatus())) {
            lastMessage = "Voting is not available because this election is not active.";
            auditService.log(studentId, "VOTE_FAILED", lastMessage);
            return lastMessage;
        }
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        if (now.before(election.getStartDate()) || now.after(election.getEndDate())) {
            lastMessage = "Voting is not available because the election window has closed.";
            auditService.log(studentId, "VOTE_FAILED", lastMessage);
            return lastMessage;
        }
        if (election.getFacultyId() > 0 && student.getFacultyId() != election.getFacultyId()) {
            lastMessage = "You may only vote in elections for your faculty.";
            auditService.log(studentId, "VOTE_FAILED", lastMessage);
            return lastMessage;
        }

        String eligibilityError = validatePositionEligibility(student, election);
        if (eligibilityError != null) {
            lastMessage = eligibilityError;
            auditService.log(studentId, "VOTE_FAILED", lastMessage);
            return lastMessage;
        }

        if (voteDAO.hasVoted(studentId, electionId, positionId)) {
            lastMessage = "You have already voted for this position.";
            auditService.log(studentId, "VOTE_FAILED", lastMessage);
            return lastMessage;
        }
        if (!voteDAO.isCandidateInElectionForPosition(electionId, positionId, candidateApplicationId)) {
            lastMessage = "The selected candidate is not on the ballot for this position.";
            auditService.log(studentId, "VOTE_FAILED", lastMessage);
            return lastMessage;
        }

        boolean success = voteDAO.castVote(studentId, electionId, positionId, candidateApplicationId);
        lastMessage = success ? "Your vote has been recorded successfully." : "The vote could not be recorded. Please try again.";
        auditService.log(studentId, success ? "VOTE_CAST" : "VOTE_FAILED",
                "Election ID " + electionId + ", position ID " + positionId + ". " + lastMessage);
        if (success) {
            String positionLabel = (election.getPositionName() == null || election.getPositionName().isBlank())
                    ? ("Position " + positionId)
                    : election.getPositionName();
            emailService.sendVoteConfirmationEmail(student, election.getTitle(), positionLabel);
        }
        return lastMessage;
    }

    public Map<Integer, Integer> getResults(int electionId, int positionId) {
        return voteDAO.getVoteCountsByPosition(electionId, positionId);
    }

    public int getTurnout(int electionId) {
        return voteDAO.getTotalVotes(electionId);
    }

    public int getEligibleVoterCount(Election election) {
        if (election == null) {
            return 0;
        }
        return studentDAO.getEligibleVoterCount(election.getFacultyId(), election.getPositionName());
    }

    public List<Candidate> getCandidatesForElection(int electionId) {
        return candidateService.getCandidatesForElection(electionId);
    }

    public boolean hasVoted(int studentId, int electionId, int positionId) {
        return voteDAO.hasVoted(studentId, electionId, positionId);
    }

    public String getLastMessage() {
        return lastMessage;
    }

    private String validatePositionEligibility(Student student, Election election) {
        PositionRules.PositionCategory category = PositionRules.classify(election.getPositionName());
        if (!PositionRules.isCanonical(category)) {
            return null;
        }

        String roleName = PositionRules.canonicalLabel(category);

        if (!PositionRules.isGenderEligible(student.getGender(), category)) {
            return "Only " + PositionRules.requiredGender(category) + " students can vote for "
                    + roleName + ". Your profile gender is "
                    + PositionRules.displayGender(student.getGender()) + ".";
        }

        if (!PositionRules.isResidencyEligible(student.isResident(), category)) {
            return "Only " + PositionRules.requiredResidencyLabel(category) + " students can vote for "
                    + roleName + ". Your profile residency is "
                    + PositionRules.profileResidencyLabel(student.isResident()) + ".";
        }

        return null;
    }
}
