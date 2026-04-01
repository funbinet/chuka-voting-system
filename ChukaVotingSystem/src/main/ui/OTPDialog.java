package main.ui;

import main.models.Student;
import main.services.AuthService;
import main.utils.Constants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class OTPDialog extends JDialog {

    private final Student student;
    private final AuthService authService;
    private boolean verified = false;
    private JTextField otpField;
    private JLabel statusLabel;
    private JLabel deliveryLabel;

    public OTPDialog(JFrame parent, Student student, AuthService authService) {
        super(parent, "OTP Verification", true);
        this.student = student;
        this.authService = authService;
        initUI();
    }

    private void initUI() {
        setSize(420, 380);
        setLocationRelativeTo(getParent());
        setResizable(false);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Constants.COLOR_BG);

        JPanel header = new JPanel();
        header.setBackground(Constants.COLOR_PRIMARY);
        header.setBorder(new EmptyBorder(15, 20, 15, 20));
        JLabel title = new JLabel("OTP Verification");
        title.setFont(Constants.FONT_HEADING);
        title.setForeground(Color.WHITE);
        header.add(title);

        JPanel body = new JPanel(new GridBagLayout());
        body.setBackground(Constants.COLOR_BG);
        body.setBorder(new EmptyBorder(20, 30, 20, 30));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 0, 6, 0);
        gbc.gridx = 0;

        gbc.gridy = 0;
        JLabel infoLabel = new JLabel("<html><center>An OTP has been prepared for<br><b>" + maskPhone(student.getPhoneNumber()) + "</b></center></html>");
        infoLabel.setFont(Constants.FONT_BODY);
        infoLabel.setHorizontalAlignment(JLabel.CENTER);
        body.add(infoLabel, gbc);

        gbc.gridy = 1;
        deliveryLabel = new JLabel(authService.isOtpSimulationMode()
                ? "SMS delivery is currently running in simulation mode for this build."
                : "OTP delivery is being handled through the configured SMS gateway.");
        deliveryLabel.setFont(Constants.FONT_SMALL);
        deliveryLabel.setForeground(Constants.COLOR_ACCENT);
        deliveryLabel.setHorizontalAlignment(JLabel.CENTER);
        body.add(deliveryLabel, gbc);

        gbc.gridy = 2;
        JLabel otpLabel = new JLabel("Enter the 6-digit OTP:");
        otpLabel.setFont(Constants.FONT_BODY);
        body.add(otpLabel, gbc);

        gbc.gridy = 3;
        otpField = new JTextField();
        otpField.setFont(new Font("Segoe UI", Font.BOLD, 22));
        otpField.setHorizontalAlignment(JTextField.CENTER);
        otpField.setPreferredSize(new Dimension(0, 50));
        body.add(otpField, gbc);

        gbc.gridy = 4;
        statusLabel = new JLabel(" ");
        statusLabel.setFont(Constants.FONT_SMALL);
        statusLabel.setForeground(Constants.COLOR_DANGER);
        statusLabel.setHorizontalAlignment(JLabel.CENTER);
        body.add(statusLabel, gbc);

        gbc.gridy = 5;
        gbc.insets = new Insets(12, 0, 6, 0);
        JButton verifyBtn = new JButton("VERIFY OTP");
        styleButton(verifyBtn, Constants.COLOR_SUCCESS);
        verifyBtn.addActionListener(e -> handleVerify());
        body.add(verifyBtn, gbc);

        gbc.gridy = 6;
        gbc.insets = new Insets(4, 0, 6, 0);
        JButton resendBtn = new JButton("Resend OTP");
        styleButton(resendBtn, Constants.COLOR_SECONDARY);
        resendBtn.addActionListener(e -> handleResend());
        body.add(resendBtn, gbc);

        panel.add(header, BorderLayout.NORTH);
        panel.add(body, BorderLayout.CENTER);
        add(panel);
    }

    private void handleVerify() {
        String otp = otpField.getText().trim();
        if (!otp.matches("\\d{" + Constants.OTP_LENGTH + "}")) {
            statusLabel.setText("Please enter a valid 6-digit OTP.");
            return;
        }

        if (authService.verifyStudentOTP(student, otp)) {
            verified = true;
            JOptionPane.showMessageDialog(this, "Verification completed successfully.", "OTP Verified", JOptionPane.INFORMATION_MESSAGE);
            dispose();
            return;
        }

        statusLabel.setText(authService.getLastMessage());
        otpField.setText("");
    }

    private void handleResend() {
        if (authService.sendOTPToStudent(student)) {
            statusLabel.setForeground(Constants.COLOR_SUCCESS);
            statusLabel.setText("A new OTP has been generated.");
        } else {
            statusLabel.setForeground(Constants.COLOR_DANGER);
            statusLabel.setText(authService.getLastMessage());
        }
        otpField.setText("");
    }

    private void styleButton(JButton btn, Color bg) {
        btn.setFont(Constants.FONT_BUTTON);
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setPreferredSize(new Dimension(0, 42));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 6) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 3);
    }

    public boolean isVerified() {
        return verified;
    }
}
