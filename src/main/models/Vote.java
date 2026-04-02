package main.models;

import java.sql.Timestamp;

public class Vote {
    private int       voteId;
    private int       studentId;
    private int       electionId;
    private int       positionId;
    private int       candidateApplicationId;
    private Timestamp votedAt;

    public Vote() {}

    public int       getVoteId()                  { return voteId; }
    public int       getStudentId()               { return studentId; }
    public int       getElectionId()              { return electionId; }
    public int       getPositionId()              { return positionId; }
    public int       getCandidateApplicationId()  { return candidateApplicationId; }
    public Timestamp getVotedAt()                 { return votedAt; }

    public void setVoteId(int voteId)                          { this.voteId                  = voteId; }
    public void setStudentId(int studentId)                    { this.studentId               = studentId; }
    public void setElectionId(int electionId)                  { this.electionId              = electionId; }
    public void setPositionId(int positionId)                  { this.positionId              = positionId; }
    public void setCandidateApplicationId(int candidateAppId)  { this.candidateApplicationId  = candidateAppId; }
    public void setVotedAt(Timestamp votedAt)                  { this.votedAt                 = votedAt; }
}
