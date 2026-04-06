package main.models;

import java.sql.Timestamp;

public class Candidate {
    private int applicationId;
    private int studentId;
    private String studentName;
    private String regNumber;
    private int yearOfStudy;
    private double gpa;
    private int positionId;
    private String positionName;
    private String manifesto;
    private int nominationCount;
    private String status;
    private String rejectionReason;
    private Timestamp appliedAt;
    private Timestamp reviewedAt;
    private int reviewedBy;
    private int facultyId;
    private String facultyName;
    private Integer coalitionId;
    private String coalitionName;
    private Coalition coalition;

    public Candidate() {
    }

    public Candidate(int applicationId, int studentId, int positionId, String manifesto, String status, Coalition coalition) {
        this.applicationId = applicationId;
        this.studentId = studentId;
        this.positionId = positionId;
        this.manifesto = manifesto;
        this.status = status;
        setCoalition(coalition);
    }

    public int getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(int applicationId) {
        this.applicationId = applicationId;
    }

    public int getId() {
        return applicationId;
    }

    public void setId(int id) {
        this.applicationId = id;
    }

    public int getStudentId() {
        return studentId;
    }

    public void setStudentId(int studentId) {
        this.studentId = studentId;
    }

    public int getPositionId() {
        return positionId;
    }

    public void setPositionId(int positionId) {
        this.positionId = positionId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getRegNumber() {
        return regNumber;
    }

    public void setRegNumber(String regNumber) {
        this.regNumber = regNumber;
    }

    public int getYearOfStudy() {
        return yearOfStudy;
    }

    public void setYearOfStudy(int yearOfStudy) {
        this.yearOfStudy = yearOfStudy;
    }

    public double getGpa() {
        return gpa;
    }

    public void setGpa(double gpa) {
        this.gpa = gpa;
    }

    public String getPositionName() {
        return positionName;
    }

    public void setPositionName(String positionName) {
        this.positionName = positionName;
    }

    public String getManifesto() {
        return manifesto;
    }

    public void setManifesto(String manifesto) {
        this.manifesto = manifesto;
    }

    public String getBio() {
        return manifesto;
    }

    public void setBio(String bio) {
        this.manifesto = bio;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getNominationCount() {
        return nominationCount;
    }

    public void setNominationCount(int nominationCount) {
        this.nominationCount = nominationCount;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public Timestamp getAppliedAt() {
        return appliedAt;
    }

    public void setAppliedAt(Timestamp appliedAt) {
        this.appliedAt = appliedAt;
    }

    public Timestamp getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(Timestamp reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public int getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(int reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public int getFacultyId() {
        return facultyId;
    }

    public void setFacultyId(int facultyId) {
        this.facultyId = facultyId;
    }

    public String getFacultyName() {
        return facultyName;
    }

    public void setFacultyName(String facultyName) {
        this.facultyName = facultyName;
    }

    public Integer getCoalitionId() {
        return coalitionId;
    }

    public void setCoalitionId(Integer coalitionId) {
        this.coalitionId = coalitionId;
    }

    public String getCoalitionName() {
        if (coalitionName != null && !coalitionName.isBlank()) {
            return coalitionName;
        }
        if (coalition != null && coalition.getName() != null && !coalition.getName().isBlank()) {
            return coalition.getName();
        }
        return "Independent";
    }

    public void setCoalitionName(String coalitionName) {
        this.coalitionName = coalitionName;
    }

    public Coalition getCoalition() {
        return coalition;
    }

    public void setCoalition(Coalition coalition) {
        this.coalition = coalition;
        if (coalition != null) {
            this.coalitionId = coalition.getCoalitionId();
            this.coalitionName = coalition.getName();
        }
    }

    @Override
    public String toString() {
        return (studentName != null ? studentName : studentId) + " - " +
                (positionName != null ? positionName : positionId);
    }
}
