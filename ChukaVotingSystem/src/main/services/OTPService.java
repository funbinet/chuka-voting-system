package main.services;

import main.dao.DBConnection;
import main.utils.Constants;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.Random;

public class OTPService {

    private Connection conn;

    public OTPService() {
        this.conn = DBConnection.getInstance().getConnection();
    }

    // Generate a 6-digit OTP
    public String generateOTP() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }

    // Save OTP to database and return the code
    public String createOTP(String phoneNumber) {
        // Invalidate old OTPs for this number
        invalidateOldOTPs(phoneNumber);

        String otp = generateOTP();
        Timestamp expiresAt = Timestamp.valueOf(
            LocalDateTime.now().plusMinutes(Constants.OTP_EXPIRY_MINS)
        );

        String sql = "INSERT INTO otp_logs (phone_number, otp_code, expires_at) VALUES (?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, phoneNumber);
            ps.setString(2, otp);
            ps.setTimestamp(3, expiresAt);
            ps.executeUpdate();
            System.out.println("📱 OTP for " + phoneNumber + ": " + otp +
                               " (expires in " + Constants.OTP_EXPIRY_MINS + " mins)");
            return otp;
        } catch (SQLException e) {
            System.err.println("❌ Create OTP error: " + e.getMessage());
            return null;
        }
    }

    // Verify submitted OTP
    public boolean verifyOTP(String phoneNumber, String submittedOTP) {
        String sql = "SELECT * FROM otp_logs WHERE phone_number=? AND otp_code=? " +
                     "AND is_used=FALSE ORDER BY created_at DESC LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, phoneNumber);
            ps.setString(2, submittedOTP);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int otpId = rs.getInt("otp_id");
                markOTPUsed(otpId);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("❌ Verify OTP error: " + e.getMessage());
        }
        return false;
    }

    // Send OTP via SMS (Simulation — replace with Twilio or Africa's Talking in production)
    public void sendOTP(String phoneNumber, String otp) {
        // =====================================================
        // PRODUCTION: Replace with Africa's Talking SMS API
        // or Twilio API call here
        // =====================================================
        System.out.println("========================================");
        System.out.println("📲 SMS SIMULATION");
        System.out.println("To: " + phoneNumber);
        System.out.println("Message: Your Chuka University Voting OTP is: " + otp);
        System.out.println("Valid for " + Constants.OTP_EXPIRY_MINS + " minutes.");
        System.out.println("========================================");
        // In the UI, a dialog will display this for simulation purposes
    }

    private void markOTPUsed(int otpId) {
        String sql = "UPDATE otp_logs SET is_used=TRUE WHERE otp_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, otpId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ Mark OTP used error: " + e.getMessage());
        }
    }

    private void invalidateOldOTPs(String phoneNumber) {
        String sql = "UPDATE otp_logs SET is_used=TRUE WHERE phone_number=? AND is_used=FALSE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, phoneNumber);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ Invalidate OTP error: " + e.getMessage());
        }
    }
}
