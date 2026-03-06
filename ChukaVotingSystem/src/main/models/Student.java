package main.models;

import java.sql.Timestamp;

public class Student {
    private int       studentId;
    private String    regNumber;
    private String    fullName;
    private String    email;
    private String    phoneNumber;
    private String    passwordHash;
    private int       facultyId;
    private String    facultyName;
    private int       yearOfStudy;
    private double    gpa;
    private boolean   hasDisciplineCase;
    private boolean   isVerified;
    private boolean   isActive;
    private Timestamp createdAt;

    public Student() {}

    // Getters
    public int       getStudentId()        { return studentId; }
    public String    getRegNumber()        { return regNumber; }
    public String    getFullName()         { return fullName; }
    public String    getEmail()            { return email; }
    public String    getPhoneNumber()      { return phoneNumber; }
    public String    getPasswordHash()     { return passwordHash; }
    public int       getFacultyId()        { return facultyId; }
    public String    getFacultyName()      { return facultyName; }
    public int       getYearOfStudy()      { return yearOfStudy; }
    public double    getGpa()              { return gpa; }
    public boolean   isHasDisciplineCase() { return hasDisciplineCase; }
    public boolean   isVerified()          { return isVerified; }
    public boolean   isActive()            { return isActive; }
    public Timestamp getCreatedAt()        { return createdAt; }

    // Setters
    public void setStudentId(int studentId)              { this.studentId        = studentId; }
    public void setRegNumber(String regNumber)            { this.regNumber        = regNumber; }
    public void setFullName(String fullName)              { this.fullName         = fullName; }
    public void setEmail(String email)                    { this.email            = email; }
    public void setPhoneNumber(String phoneNumber)        { this.phoneNumber      = phoneNumber; }
    public void setPasswordHash(String passwordHash)      { this.passwordHash     = passwordHash; }
    public void setFacultyId(int facultyId)               { this.facultyId        = facultyId; }
    public void setFacultyName(String facultyName)        { this.facultyName      = facultyName; }
    public void setYearOfStudy(int yearOfStudy)           { this.yearOfStudy      = yearOfStudy; }
    public void setGpa(double gpa)                        { this.gpa              = gpa; }
    public void setHasDisciplineCase(boolean v)           { this.hasDisciplineCase = v; }
    public void setVerified(boolean verified)             { this.isVerified       = verified; }
    public void setActive(boolean active)                 { this.isActive         = active; }
    public void setCreatedAt(Timestamp createdAt)         { this.createdAt        = createdAt; }

    // Eligibility check for candidacy
    public boolean isEligibleForCandidacy() {
        return !hasDisciplineCase &&
               gpa >= main.utils.Constants.MIN_GPA &&
               yearOfStudy >= main.utils.Constants.MIN_YEAR_OF_STUDY &&
               isVerified &&
               isActive;
    }

    @Override
    public String toString() {
        return fullName + " (" + regNumber + ")";
    }
}
