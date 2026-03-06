package main.services;

import main.dao.ElectionDAO;
import main.dao.VoteDAO;
import main.models.Candidate;
import main.models.Election;

import java.util.List;
import java.util.Map;

public class VotingService {

    private VoteDAO     voteDAO;
    private ElectionDAO electionDAO;
    private CandidateService candidateService;

    public VotingService() {
        this.voteDAO          = new VoteDAO();
        this.electionDAO      = new ElectionDAO();
        this.candidateService = new CandidateService();
    }

    // Cast a vote — validates before inserting
    public String castVote(int studentId, int electionId, int positionId, int candidateApplicationId) {
        // 1. Check election is still ACTIVE
        Election election = electionDAO.findById(electionId);
        if (election == null)                    return "❌ Election not found.";
        if (!"ACTIVE".equals(election.getStatus())) return "❌ This election is not currently active.";

        // 2. Check student hasn't already voted for this position
        if (voteDAO.hasVoted(studentId, electionId, positionId)) {
            return "❌ You have already voted for this position.";
        }

        // 3. Cast the vote
        boolean success = voteDAO.castVote(studentId, electionId, positionId, candidateApplicationId);
        return success ? "✅ Vote cast successfully!" : "❌ Voting failed. Please try again.";
    }

    // Get results for a specific election (admin/post-election)
    public Map<Integer, Integer> getResults(int electionId, int positionId) {
        return voteDAO.getVoteCountsByPosition(electionId, positionId);
    }

    // Get total voter turnout
    public int getTurnout(int electionId) {
        return voteDAO.getTotalVotes(electionId);
    }

    // Get candidates for an election by faculty
    public List<Candidate> getCandidatesForElection(int electionId) {
        return candidateService.getCandidatesForElection(electionId);
    }

    // Check if student has voted for a position
    public boolean hasVoted(int studentId, int electionId, int positionId) {
        return voteDAO.hasVoted(studentId, electionId, positionId);
    }
}
