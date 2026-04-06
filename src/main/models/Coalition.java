package main.models;

import java.sql.Timestamp;

public class Coalition {
    private int       coalitionId;
    private String    name;
    private String    motto;
    private Timestamp createdAt;

    public Coalition() {}

    public Coalition(int coalitionId, String name, String motto) {
        this.coalitionId = coalitionId;
        this.name        = name;
        this.motto       = motto;
    }

    public int       getCoalitionId() { return coalitionId; }
    public String    getName()        { return name; }
    public String    getMotto()       { return motto; }
    public Timestamp getCreatedAt()   { return createdAt; }

    public void setCoalitionId(int coalitionId) { this.coalitionId = coalitionId; }
    public void setName(String name)              { this.name = name; }
    public void setMotto(String motto)            { this.motto = motto; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() { return name; }
}
