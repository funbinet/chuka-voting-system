package main.models;

import java.sql.Timestamp;

public class AuditLog {
    private int logId;
    private Integer studentId;
    private String studentName; // Joined from students table
    private String action;
    private String description;
    private Timestamp loggedAt;

    public AuditLog() {}

    public int getLogId() { return logId; }
    public void setLogId(int logId) { this.logId = logId; }

    public Integer getStudentId() { return studentId; }
    public void setStudentId(Integer studentId) { this.studentId = studentId; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Timestamp getLoggedAt() { return loggedAt; }
    public void setLoggedAt(Timestamp loggedAt) { this.loggedAt = loggedAt; }
}
