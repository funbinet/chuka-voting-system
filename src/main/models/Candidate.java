package main.models;

import java.sql.Timestamp;

public class Candidate {
    private int       applicationId;
    private int       studentId;
    private String    studentName;
    private String    regNumber;
    private int       yearOfStudy;
    private double    gpa;
    private int       positionId;
    private String    positionName;
    private String    manifesto;
    private int       nominationCount;
    private String    status;
    private String    rejectionReason;
    private Timestamp appliedAt;
    private Timestamp reviewedAt;
    private int       reviewedBy;
    private int       facultyId;
    private String    facultyName;

    public Candidate() {}

    // Getters
    public int       getApplicationId()   { return applicationId; }
    public int       getStudentId()       { return studentId; }
    public String    getStudentName()     { return studentName; }
    public String    getRegNumber()       { return regNumber; }
    public int       getYearOfStudy()     { return yearOfStudy; }
    public double    getGpa()             { return gpa; }
    public int       getPositionId()      { return positionId; }
    public String    getPositionName()    { return positionName; }
    public String    getManifesto()       { return manifesto; }
    public int       getNominationCount() { return nominationCount; }
    public String    getStatus()          { return status; }
    public String    getRejectionReason() { return rejectionReason; }
    public Timestamp getAppliedAt()       { return appliedAt; }
    public Timestamp getReviewedAt()      { return reviewedAt; }
    public int       getReviewedBy()      { return reviewedBy; }
    public int       getFacultyId()       { return facultyId; }
    public String    getFacultyName()     { return facultyName; }

    // Setters
    public void setApplicationId(int applicationId)       { this.applicationId   = applicationId; }
    public void setStudentId(int studentId)               { this.studentId       = studentId; }
    public void setStudentName(String studentName)         { this.studentName     = studentName; }
    public void setRegNumber(String regNumber)             { this.regNumber       = regNumber; }
    public void setYearOfStudy(int yearOfStudy)           { this.yearOfStudy     = yearOfStudy; }
    public void setGpa(double gpa)                        { this.gpa             = gpa; }
    public void setPositionId(int positionId)             { this.positionId      = positionId; }
    public void setPositionName(String positionName)       { this.positionName    = positionName; }
    public void setManifesto(String manifesto)             { this.manifesto       = manifesto; }
    public void setNominationCount(int nominationCount)   { this.nominationCount = nominationCount; }
    public void setStatus(String status)                   { this.status          = status; }
    public void setRejectionReason(String rejectionReason){ this.rejectionReason = rejectionReason; }
    public void setAppliedAt(Timestamp appliedAt)         { this.appliedAt       = appliedAt; }
    public void setReviewedAt(Timestamp reviewedAt)       { this.reviewedAt      = reviewedAt; }
    public void setReviewedBy(int reviewedBy)             { this.reviewedBy      = reviewedBy; }
    public void setFacultyId(int facultyId)               { this.facultyId       = facultyId; }
    public void setFacultyName(String facultyName)        { this.facultyName     = facultyName; }

    @Override
    public String toString() { return studentName + " - " + positionName; }
}
