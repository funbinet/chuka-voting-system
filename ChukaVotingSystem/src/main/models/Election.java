package main.models;

import java.sql.Timestamp;

public class Election {
    private int       electionId;
    private String    title;
    private int       facultyId;
    private String    facultyName;
    private Timestamp startDate;
    private Timestamp endDate;
    private String    status;
    private int       createdBy;
    private Timestamp createdAt;

    public Election() {}

    public int       getElectionId()  { return electionId; }
    public String    getTitle()       { return title; }
    public int       getFacultyId()   { return facultyId; }
    public String    getFacultyName() { return facultyName; }
    public Timestamp getStartDate()   { return startDate; }
    public Timestamp getEndDate()     { return endDate; }
    public String    getStatus()      { return status; }
    public int       getCreatedBy()   { return createdBy; }
    public Timestamp getCreatedAt()   { return createdAt; }

    public void setElectionId(int electionId)       { this.electionId  = electionId; }
    public void setTitle(String title)               { this.title       = title; }
    public void setFacultyId(int facultyId)          { this.facultyId   = facultyId; }
    public void setFacultyName(String facultyName)   { this.facultyName = facultyName; }
    public void setStartDate(Timestamp startDate)    { this.startDate   = startDate; }
    public void setEndDate(Timestamp endDate)        { this.endDate     = endDate; }
    public void setStatus(String status)             { this.status      = status; }
    public void setCreatedBy(int createdBy)          { this.createdBy   = createdBy; }
    public void setCreatedAt(Timestamp createdAt)    { this.createdAt   = createdAt; }

    @Override
    public String toString() { return title + " [" + status + "]"; }
}
