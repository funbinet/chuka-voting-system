package main.services;

import main.dao.AdminDAO;
import main.dao.StudentDAO;
import main.models.Admin;
import main.models.Student;
import main.utils.Constants;
import main.utils.PasswordHasher;

public class AuthService {

    private final StudentDAO studentDAO;
    private final AdminDAO adminDAO;
    private final OTPService otpService;
    private final AuditService auditService;
    private final ElectionService electionService;
    private String lastMessage;

    private static Student currentStudent;
    private static Admin currentAdmin;
    private static String currentRole;

    public AuthService() {
        this.studentDAO = new StudentDAO();
        this.adminDAO = new AdminDAO();
        this.otpService = new OTPService();
        this.auditService = AuditService.getInstance();
        this.electionService = new ElectionService();
        this.lastMessage = "";
    }

    public Student loginStudent(String regNumber, String password) {
        electionService.syncElectionStatuses();
        Student student = studentDAO.findByRegNumber(regNumber);
        if (student == null) {
            lastMessage = "Invalid registration number or password.";
            auditService.log(null, "LOGIN_FAILED", "Unknown registration number attempted: " + regNumber);
            return null;
        }
        if (!student.isActive()) {
            lastMessage = "This student account has been deactivated. Please contact the election administrator.";
            auditService.log(student.getStudentId(), "LOGIN_BLOCKED", lastMessage);
            return null;
        }
        if (!PasswordHasher.verify(password, student.getPasswordSalt(), student.getPasswordHash())) {
            lastMessage = "Invalid registration number or password.";
            auditService.log(student.getStudentId(), "LOGIN_FAILED", "Invalid password attempt for " + regNumber);
            return null;
        }
        lastMessage = student.isVerified()
                ? "Credentials accepted. OTP verification is required for sign-in."
                : "Credentials accepted. Complete OTP verification to verify your account and sign in.";
        return student;
    }

    public Admin loginAdmin(String email, String password) {
        electionService.syncElectionStatuses();
        Admin admin = adminDAO.findByEmail(email);
        if (admin == null || !PasswordHasher.verify(password, admin.getPasswordSalt(), admin.getPasswordHash())) {
            lastMessage = "Invalid administrator credentials.";
            auditService.log(null, "ADMIN_LOGIN_FAILED", "Failed admin login for email: " + email);
            return null;
        }
        if (!admin.isActive()) {
            lastMessage = "This administrator account is inactive.";
            auditService.log(null, "ADMIN_LOGIN_BLOCKED", "Inactive admin login attempted for email: " + email);
            return null;
        }
        setCurrentAdmin(admin);
        lastMessage = "Administrator login successful.";
        auditService.log(null, "ADMIN_LOGIN", "Admin logged in: " + email);
        return admin;
    }

    public boolean sendOTPToStudent(Student student) {
        String otp = otpService.createOTP(student.getPhoneNumber());
        boolean sent = otpService.sendOTP(student.getPhoneNumber(), otp);
        lastMessage = otpService.getLastMessage();
        auditService.log(student.getStudentId(), sent ? "OTP_SENT" : "OTP_SEND_FAILED",
                "OTP requested for phone number " + student.getPhoneNumber() + ". " + lastMessage);
        return sent;
    }

    public boolean verifyStudentOTP(Student student, String submittedOTP) {
        boolean verified = otpService.verifyOTP(student.getPhoneNumber(), submittedOTP);
        lastMessage = otpService.getLastMessage();
        if (!verified) {
            auditService.log(student.getStudentId(), "OTP_VERIFY_FAILED", lastMessage);
            return false;
        }

        boolean firstVerification = !student.isVerified();
        if (!student.isVerified()) {
            studentDAO.markVerified(student.getStudentId());
            student.setVerified(true);
        }
        Student refreshedStudent = studentDAO.findById(student.getStudentId());
        setCurrentStudent(refreshedStudent != null ? refreshedStudent : student);
        auditService.log(student.getStudentId(), "LOGIN_SUCCESS", "Student logged in successfully after OTP verification.");
        lastMessage = firstVerification
                ? "OTP verified and account marked as verified."
                : "OTP verified successfully.";
        return true;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public Student getStudentByRegNumber(String regNumber) {
        return studentDAO.findByRegNumber(regNumber);
    }

    public boolean updateStudentPassword(int studentId, String newPassword) {
        String salt = PasswordHasher.generateSalt();
        String hash = PasswordHasher.hash(newPassword, salt);
        boolean ok = studentDAO.updatePassword(studentId, hash, salt);
        if (ok) {
            auditService.log(studentId, "PASSWORD_CHANGED", "Student changed their password.");
        }
        return ok;
    }

    public boolean isOtpSimulationMode() {
        return otpService.isSimulationMode();
    }

    public static void setCurrentStudent(Student student) {
        currentStudent = student;
        currentAdmin = null;
        currentRole = Constants.ROLE_STUDENT;
    }

    public static void setCurrentAdmin(Admin admin) {
        currentAdmin = admin;
        currentStudent = null;
        currentRole = Constants.ROLE_ADMIN;
    }

    public static Student getCurrentStudent() {
        return currentStudent;
    }

    public static Admin getCurrentAdmin() {
        return currentAdmin;
    }

    public static String getCurrentRole() {
        return currentRole;
    }

    public static boolean isLoggedIn() {
        return currentStudent != null || currentAdmin != null;
    }

    public static void logout() {
        currentStudent = null;
        currentAdmin = null;
        currentRole = null;
    }
}
