package main.models;

public class Faculty {
    private int    facultyId;
    private String facultyCode;
    private String facultyName;

    public Faculty() {}

    public Faculty(int facultyId, String facultyCode, String facultyName) {
        this.facultyId   = facultyId;
        this.facultyCode = facultyCode;
        this.facultyName = facultyName;
    }

    public int    getFacultyId()   { return facultyId; }
    public String getFacultyCode() { return facultyCode; }
    public String getFacultyName() { return facultyName; }

    public void setFacultyId(int facultyId)       { this.facultyId   = facultyId; }
    public void setFacultyCode(String facultyCode) { this.facultyCode = facultyCode; }
    public void setFacultyName(String facultyName) { this.facultyName = facultyName; }

    @Override
    public String toString() { return facultyName; }
}
