package main.models;

import java.sql.Timestamp;

public class Admin {
    private int       adminId;
    private String    fullName;
    private String    email;
    private String    phoneNumber;
    private String    passwordHash;
    private String    passwordSalt;
    private boolean   isActive;
    private Timestamp createdAt;

    public Admin() {}

    // Getters
    public int       getAdminId()     { return adminId; }
    public String    getFullName()    { return fullName; }
    public String    getEmail()       { return email; }
    public String    getPhoneNumber() { return phoneNumber; }
    public String    getPasswordHash() { return passwordHash; }
    public String    getPasswordSalt() { return passwordSalt; }
    public boolean   isActive()       { return isActive; }
    public Timestamp getCreatedAt()   { return createdAt; }

    // Setters
    public void setAdminId(int adminId)         { this.adminId      = adminId; }
    public void setFullName(String fullName)      { this.fullName     = fullName; }
    public void setEmail(String email)            { this.email        = email; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public void setPasswordHash(String hash)      { this.passwordHash = hash; }
    public void setPasswordSalt(String salt)      { this.passwordSalt = salt; }
    public void setActive(boolean active)         { this.isActive     = active; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt    = createdAt; }

    public String toString() { return fullName + " (Admin)"; }
}
