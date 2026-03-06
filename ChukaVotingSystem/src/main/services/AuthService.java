package main.services;

import main.dao.AdminDAO;
import main.dao.StudentDAO;
import main.models.Admin;
import main.models.Student;
import main.utils.Constants;
import main.utils.PasswordHasher;

public class AuthService {

    private StudentDAO studentDAO;
    private AdminDAO   adminDAO;
    private OTPService otpService;

    // Currently logged-in user (session)
    private static Student  currentStudent;
    private static Admin    currentAdmin;
    private static String   currentRole;

    public AuthService() {
        this.studentDAO = new StudentDAO();
        this.adminDAO   = new AdminDAO();
        this.otpService = new OTPService();
    }

    // ── Student Login ──────────────────────────────────────────
    public Student loginStudent(String regNumber, String password) {
        Student student = studentDAO.findByRegNumber(regNumber);
        if (student == null) return null;
        if (!PasswordHasher.verify(password, student.getPasswordHash())) return null;
        if (!student.isActive()) return null;
        return student; // OTP check happens next
    }

    // ── Admin Login ────────────────────────────────────────────
    public Admin loginAdmin(String email, String password) {
        Admin admin = adminDAO.findByEmail(email);
        if (admin == null) return null;
        if (!PasswordHasher.verify(password, admin.getPasswordHash())) return null;
        if (!admin.isActive()) return null;
        setCurrentAdmin(admin);
        return admin;
    }

    // ── OTP Flow ───────────────────────────────────────────────
    public String sendOTPToStudent(Student student) {
        String otp = otpService.createOTP(student.getPhoneNumber());
        otpService.sendOTP(student.getPhoneNumber(), otp);
        return otp; // Return for simulation display in UI
    }

    public boolean verifyStudentOTP(Student student, String submittedOTP) {
        boolean verified = otpService.verifyOTP(student.getPhoneNumber(), submittedOTP);
        if (verified) {
            if (!student.isVerified()) {
                studentDAO.markVerified(student.getStudentId());
            }
            setCurrentStudent(student);
        }
        return verified;
    }

    // ── Session Management ─────────────────────────────────────
    public static void setCurrentStudent(Student student) {
        currentStudent = student;
        currentAdmin   = null;
        currentRole    = Constants.ROLE_STUDENT;
    }

    public static void setCurrentAdmin(Admin admin) {
        currentAdmin   = admin;
        currentStudent = null;
        currentRole    = Constants.ROLE_ADMIN;
    }

    public static Student  getCurrentStudent() { return currentStudent; }
    public static Admin    getCurrentAdmin()   { return currentAdmin; }
    public static String   getCurrentRole()    { return currentRole; }

    public static boolean isLoggedIn() {
        return currentStudent != null || currentAdmin != null;
    }

    public static void logout() {
        currentStudent = null;
        currentAdmin   = null;
        currentRole    = null;
        System.out.println("👋 User logged out.");
    }
}
