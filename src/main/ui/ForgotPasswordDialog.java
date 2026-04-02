package main.ui;

import main.dao.StudentDAO;
import main.models.Student;
import main.services.OTPService;
import main.utils.Constants;
import main.utils.PasswordHasher;
import main.utils.Validator;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class ForgotPasswordDialog extends JDialog {

    private CardLayout cardLayout;
    private JPanel     mainPanel;
    private StudentDAO studentDAO;
    private OTPService otpService;

    // State
    private Student targetStudent;
    private String  generatedOTP;

    // Step 1: Phone
    private JTextField phoneField;

    // Step 2: OTP
    private JTextField otpField;

    // Step 3: New Password
    private JPasswordField passField;
    private JPasswordField confirmPassField;

    public ForgotPasswordDialog(JFrame parent) {
        super(parent, "Reset Password", true);
        this.studentDAO = new StudentDAO();
        this.otpService = new OTPService();
        initUI();
    }

    private void initUI() {
        setSize(420, 450);
        setLocationRelativeTo(getParent());
        setResizable(false);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);
        mainPanel.setBackground(Constants.COLOR_BG);

        mainPanel.add(buildPhonePanel(), "PHONE");
        mainPanel.add(buildOTPPanel(),   "OTP");
        mainPanel.add(buildResetPanel(), "RESET");

        add(mainPanel);
    }

    private JPanel buildPhonePanel() {
        JPanel p = createBasePanel("📱 Forgot Password");
        GridBagConstraints gbc = createGBC();

        JLabel info = new JLabel("<html>Enter your registered phone number to receive an OTP.</html>");
        info.setFont(Constants.FONT_BODY);
        gbc.gridy = 0; p.add(info, gbc);

        phoneField = new JTextField();
        phoneField.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        phoneField.setPreferredSize(new Dimension(0, 45));
        gbc.gridy = 1; gbc.insets = new Insets(20, 0, 20, 0);
        p.add(phoneField, gbc);

        JButton nextBtn = createButton("SEND OTP", Constants.COLOR_PRIMARY);
        nextBtn.addActionListener(e -> handleSendOTP());
        gbc.gridy = 2; p.add(nextBtn, gbc);

        return p;
    }

    private JPanel buildOTPPanel() {
        JPanel p = createBasePanel("🔑 OTP Verification");
        GridBagConstraints gbc = createGBC();

        JLabel info = new JLabel("Enter the 6-digit code sent to your phone.");
        info.setFont(Constants.FONT_BODY);
        gbc.gridy = 0; p.add(info, gbc);

        otpField = new JTextField();
        otpField.setFont(new Font("Segoe UI", Font.BOLD, 22));
        otpField.setHorizontalAlignment(JTextField.CENTER);
        otpField.setPreferredSize(new Dimension(0, 50));
        gbc.gridy = 1; gbc.insets = new Insets(20, 0, 20, 0);
        p.add(otpField, gbc);

        JButton verifyBtn = createButton("VERIFY OTP", Constants.COLOR_SUCCESS);
        verifyBtn.addActionListener(e -> handleVerifyOTP());
        gbc.gridy = 2; p.add(verifyBtn, gbc);

        return p;
    }

    private JPanel buildResetPanel() {
        JPanel p = createBasePanel("🔐 Set New Password");
        GridBagConstraints gbc = createGBC();

        gbc.gridy = 0; p.add(new JLabel("New Password:"), gbc);
        passField = new JPasswordField();
        passField.setPreferredSize(new Dimension(0, 40));
        gbc.gridy = 1; p.add(passField, gbc);

        gbc.gridy = 2; p.add(new JLabel("Confirm Password:"), gbc);
        confirmPassField = new JPasswordField();
        confirmPassField.setPreferredSize(new Dimension(0, 40));
        gbc.gridy = 3; p.add(confirmPassField, gbc);

        JButton resetBtn = createButton("UPDATE PASSWORD", Constants.COLOR_SUCCESS);
        resetBtn.addActionListener(e -> handleUpdatePassword());
        gbc.gridy = 4; gbc.insets = new Insets(20, 0, 0, 0);
        p.add(resetBtn, gbc);

        return p;
    }

    // --- Handlers ---

    private void handleSendOTP() {
        String phone = phoneField.getText().trim();
        if (!Validator.isValidPhone(phone)) {
            JOptionPane.showMessageDialog(this, "Invalid phone number format.");
            return;
        }

        targetStudent = studentDAO.findByPhoneNumber(phone);
        if (targetStudent == null) {
            JOptionPane.showMessageDialog(this, "Phone number not registered.");
            return;
        }

        generatedOTP = otpService.createOTP(phone);
        otpService.sendOTP(phone, generatedOTP);
        cardLayout.show(mainPanel, "OTP");
    }

    private void handleVerifyOTP() {
        String code = otpField.getText().trim();
        if (otpService.verifyOTP(targetStudent.getPhoneNumber(), code)) {
            cardLayout.show(mainPanel, "RESET");
        } else {
            JOptionPane.showMessageDialog(this, "Invalid or expired OTP.");
        }
    }

    private void handleUpdatePassword() {
        String pass = new String(passField.getPassword());
        String confirm = new String(confirmPassField.getPassword());

        if (!Validator.isValidPassword(pass)) {
            JOptionPane.showMessageDialog(this, "Password must be 8+ chars, with uppercase, digit, and special char.");
            return;
        }
        if (!pass.equals(confirm)) {
            JOptionPane.showMessageDialog(this, "Passwords do not match.");
            return;
        }

        String salt = PasswordHasher.generateSalt();
        String hash = PasswordHasher.hash(pass, salt);

        if (studentDAO.updatePassword(targetStudent.getStudentId(), hash, salt)) {
            JOptionPane.showMessageDialog(this, "✅ Password updated successfully! Please login.");
            dispose();
        } else {
            JOptionPane.showMessageDialog(this, "❌ Failed to update password.");
        }
    }

    // --- Helpers ---

    private JPanel createBasePanel(String titleStr) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Constants.COLOR_BG);
        p.setBorder(new EmptyBorder(30, 40, 30, 40));

        // You could add a title label at top of each panel if desired
        return p;
    }

    private GridBagConstraints createGBC() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.insets = new Insets(5, 0, 5, 0);
        gbc.weightx = 1.0;
        return gbc;
    }

    private JButton createButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(Constants.FONT_BUTTON);
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setPreferredSize(new Dimension(0, 45));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }
}
