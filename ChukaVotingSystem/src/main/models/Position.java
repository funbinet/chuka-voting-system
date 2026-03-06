package main.models;

public class Position {
    private int    positionId;
    private String positionName;
    private int    facultyId;

    public Position() {}

    public Position(int positionId, String positionName, int facultyId) {
        this.positionId   = positionId;
        this.positionName = positionName;
        this.facultyId    = facultyId;
    }

    public int    getPositionId()   { return positionId; }
    public String getPositionName() { return positionName; }
    public int    getFacultyId()    { return facultyId; }

    public void setPositionId(int positionId)         { this.positionId   = positionId; }
    public void setPositionName(String positionName)   { this.positionName = positionName; }
    public void setFacultyId(int facultyId)            { this.facultyId    = facultyId; }

    @Override
    public String toString() { return positionName; }
}
