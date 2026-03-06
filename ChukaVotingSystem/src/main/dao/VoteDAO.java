package main.dao;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class VoteDAO {

    private Connection conn;

    public VoteDAO() {
        this.conn = DBConnection.getInstance().getConnection();
    }

    // Cast a vote
    public boolean castVote(int studentId, int electionId, int positionId, int candidateApplicationId) {
        String sql = "INSERT INTO votes (student_id, election_id, position_id, candidate_application_id) " +
                     "VALUES (?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setInt(2, electionId);
            ps.setInt(3, positionId);
            ps.setInt(4, candidateApplicationId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ Cast vote error: " + e.getMessage());
            return false;
        }
    }

    // Check if student already voted for a position in this election
    public boolean hasVoted(int studentId, int electionId, int positionId) {
        String sql = "SELECT COUNT(*) FROM votes WHERE student_id=? AND election_id=? AND position_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setInt(2, electionId);
            ps.setInt(3, positionId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) {
            System.err.println("❌ Has voted check error: " + e.getMessage());
        }
        return false;
    }

    // Get vote counts per candidate for a specific position in an election
    // Returns: Map<candidateApplicationId, voteCount>
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
            System.err.println("❌ Get vote counts error: " + e.getMessage());
        }
        return counts;
    }

    // Total votes cast in an election
    public int getTotalVotes(int electionId) {
        String sql = "SELECT COUNT(DISTINCT student_id) FROM votes WHERE election_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, electionId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("❌ Get total votes error: " + e.getMessage());
        }
        return 0;
    }
}
