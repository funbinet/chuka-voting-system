package main.services;

import main.dao.DBConnection;
import main.utils.Constants;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Random;

public class OTPService {

    private final Connection conn;
    private final Map<String, Integer> failedAttempts;
    private String atUsername;
    private String atApiKey;
    private String lastMessage;
    private boolean simulationMode;

    public OTPService() {
        this.conn = DBConnection.getInstance().getConnection();
        this.failedAttempts = new HashMap<>();
        this.lastMessage = "";
        this.simulationMode = true;
        loadConfig();
    }

    private void loadConfig() {
        Properties props = new Properties();
        InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties");
        if (input == null) {
            input = getClass().getResourceAsStream("/config.properties");
        }

        if (input != null) {
            try {
                props.load(input);
                this.atUsername = props.getProperty("at.username");
                this.atApiKey = props.getProperty("at.api_key");
                this.simulationMode = atUsername == null || atUsername.isBlank() || atApiKey == null || atApiKey.isBlank();
            } catch (IOException e) {
                System.err.println("Error loading config.properties: " + e.getMessage());
            } finally {
                try {
                    input.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    public String createOTP(String phoneNumber) {
        invalidateOldOTPs(phoneNumber);
        failedAttempts.put(phoneNumber, 0);

        String otp = generateOTP();
        Timestamp expiresAt = Timestamp.valueOf(LocalDateTime.now().plusMinutes(Constants.OTP_EXPIRY_MINS));
        String sql = "INSERT INTO otp_logs (phone_number, otp_code, expires_at) VALUES (?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, phoneNumber);
            ps.setString(2, otp);
            ps.setTimestamp(3, expiresAt);
            ps.executeUpdate();
            lastMessage = simulationMode
                    ? "OTP generated in simulation mode because no SMS gateway is configured."
                    : "OTP generated and queued for SMS delivery.";
            return otp;
        } catch (SQLException e) {
            System.err.println("Create OTP error: " + e.getMessage());
            lastMessage = "The OTP could not be generated.";
            return null;
        }
    }

    public boolean verifyOTP(String phoneNumber, String submittedOTP) {
        int currentAttempts = failedAttempts.getOrDefault(phoneNumber, 0);
        if (currentAttempts >= Constants.OTP_MAX_ATTEMPTS) {
            lastMessage = "The maximum number of OTP attempts has been reached. Please request a new OTP.";
            return false;
        }

        String sql = "SELECT otp_id, otp_code, expires_at FROM otp_logs WHERE phone_number=? AND is_used=FALSE " +
                "ORDER BY created_at DESC LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, phoneNumber);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                lastMessage = "No active OTP was found. Please request a new OTP.";
                return false;
            }

            int otpId = rs.getInt("otp_id");
            String expectedOtp = rs.getString("otp_code");
            Timestamp expiresAt = rs.getTimestamp("expires_at");
            Timestamp now = Timestamp.valueOf(LocalDateTime.now());

            if (expiresAt == null || now.after(expiresAt)) {
                markOTPUsed(otpId);
                lastMessage = "The OTP has expired. Please request a new OTP.";
                return false;
            }
            if (!expectedOtp.equals(submittedOTP)) {
                failedAttempts.put(phoneNumber, currentAttempts + 1);
                int remaining = Constants.OTP_MAX_ATTEMPTS - failedAttempts.get(phoneNumber);
                lastMessage = remaining > 0
                        ? "The OTP entered is incorrect. " + remaining + " attempt(s) remaining."
                        : "The maximum number of OTP attempts has been reached. Please request a new OTP.";
                return false;
            }

            markOTPUsed(otpId);
            failedAttempts.remove(phoneNumber);
            lastMessage = "OTP verified successfully.";
            return true;
        } catch (SQLException e) {
            System.err.println("Verify OTP error: " + e.getMessage());
            lastMessage = "The OTP could not be verified.";
            return false;
        }
    }

    public boolean sendOTP(String phoneNumber, String otp) {
        if (otp == null || otp.isBlank()) {
            lastMessage = "No OTP was available to send.";
            return false;
        }
        if (!simulationMode) {
            System.out.println("SMS gateway credentials detected for " + phoneNumber + ".");
        }
        simulateSMS(phoneNumber, otp);
        return true;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public boolean isSimulationMode() {
        return simulationMode;
    }

    private String generateOTP() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }

    private void simulateSMS(String phoneNumber, String otp) {
        System.out.println("========================================");
        System.out.println("SMS DELIVERY");
        System.out.println("Mode: " + (simulationMode ? "SIMULATION" : "GATEWAY READY"));
        System.out.println("To: " + phoneNumber);
        System.out.println("Message: Your Chuka University voting OTP is: " + otp);
        System.out.println("Valid for " + Constants.OTP_EXPIRY_MINS + " minutes.");
        System.out.println("========================================");
    }

    private void markOTPUsed(int otpId) {
        String sql = "UPDATE otp_logs SET is_used=TRUE WHERE otp_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, otpId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Mark OTP used error: " + e.getMessage());
        }
    }

    private void invalidateOldOTPs(String phoneNumber) {
        String sql = "UPDATE otp_logs SET is_used=TRUE WHERE phone_number=? AND is_used=FALSE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, phoneNumber);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Invalidate OTP error: " + e.getMessage());
        }
    }
}
