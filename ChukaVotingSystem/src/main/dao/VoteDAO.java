package main.dao;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class VoteDAO {

    private final Connection conn;

    public VoteDAO() {
        this.conn = DBConnection.getInstance().getConnection();
    }

    public boolean castVote(int studentId, int electionId, int positionId, int candidateApplicationId) {
        String sql = "INSERT INTO votes (student_id, election_id, position_id, candidate_application_id) VALUES (?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setInt(2, electionId);
            ps.setInt(3, positionId);
            ps.setInt(4, candidateApplicationId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("âŒ Cast vote error: " + e.getMessage());
            return false;
        }
    }

    public boolean hasVoted(int studentId, int electionId, int positionId) {
        String sql = "SELECT COUNT(*) FROM votes WHERE student_id=? AND election_id=? AND position_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setInt(2, electionId);
            ps.setInt(3, positionId);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
            System.err.println("âŒ Has voted check error: " + e.getMessage());
            return false;
        }
    }

    public boolean isCandidateInElectionForPosition(int electionId, int positionId, int candidateApplicationId) {
        String sql = "SELECT COUNT(*) FROM election_candidates ec " +
                "JOIN candidate_applications ca ON ec.application_id = ca.application_id " +
                "WHERE ec.election_id=? AND ca.application_id=? AND ca.position_id=? AND ca.status='APPROVED'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, electionId);
            ps.setInt(2, candidateApplicationId);
            ps.setInt(3, positionId);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
            System.err.println("âŒ Candidate ballot lookup error: " + e.getMessage());
            return false;
        }
    }

    public Map<Integer, Integer> getVoteCountsByPosition(int electionId, int positionId) {
        Map<Integer, Integer> counts = new HashMap<>();
        String sql = "SELECT candidate_application_id, COUNT(*) as vote_count FROM votes " +
                "WHERE election_id=? AND position_id=? GROUP BY candidate_application_id";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, electionId);
            ps.setInt(2, positionId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                counts.put(rs.getInt("candidate_application_id"), rs.getInt("vote_count"));
            }
        } catch (SQLException e) {
            System.err.println("âŒ Get vote counts error: " + e.getMessage());
        }
        return counts;
    }

    public int getTotalVotes(int electionId) {
        String sql = "SELECT COUNT(DISTINCT student_id) FROM votes WHERE election_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, electionId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            System.err.println("âŒ Get total votes error: " + e.getMessage());
            return 0;
        }
    }
}
