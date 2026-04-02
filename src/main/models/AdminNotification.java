package main.models;

import java.sql.Timestamp;

public class AdminNotification {
    private int id;
    private int adminId; // Added for the case where it belongs to a specific admin. -1 or 0 for global.
    private String title;
    private String message;
    private boolean isRead;
    private Timestamp createdAt;

    public AdminNotification() {}

    public AdminNotification(int adminId, String title, String message) {
        this.adminId = adminId;
        this.title = title;
        this.message = message;
        this.isRead = false;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getAdminId() { return adminId; }
    public void setAdminId(int adminId) { this.adminId = adminId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
