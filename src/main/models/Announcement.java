package main.models;

import java.sql.Timestamp;

public class Announcement {
    private int       id;
    private String    title;
    private String    body;
    private int       postedBy;
    private String    adminName; // Joined for display
    private Timestamp postedAt;
    private boolean   isActive;

    public Announcement() {}

    // Getters and Setters
    public int       getId()           { return id; }
    public void      setId(int id)     { this.id = id; }
    public String    getTitle()        { return title; }
    public void      setTitle(String title) { this.title = title; }
    public String    getBody()         { return body; }
    public void      setBody(String body)   { this.body = body; }
    public int       getPostedBy()     { return postedBy; }
    public void      setPostedBy(int postedBy) { this.postedBy = postedBy; }
    public String    getAdminName()    { return adminName; }
    public void      setAdminName(String adminName) { this.adminName = adminName; }
    public Timestamp getPostedAt()     { return postedAt; }
    public void      setPostedAt(Timestamp postedAt) { this.postedAt = postedAt; }
    public boolean   isActive()        { return isActive; }
    public void      setActive(boolean active) { isActive = active; }
}
