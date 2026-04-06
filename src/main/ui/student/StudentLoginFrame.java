package main.ui.student;

import main.models.Admin;
import main.models.Student;
import main.services.AuthService;
import main.ui.ForgotPasswordDialog;
import main.ui.OTPDialog;
import main.ui.RoleSelectionFrame;
import main.ui.student.StudentDashboard;
import main.utils.Constants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class StudentLoginFrame extends JFrame {

    private final AuthService authService;
    private JTextField studentRegField;
    private JPasswordField studentPassField;

    public StudentLoginFrame() {
        this.authService = new AuthService();
        initUI();
    }

    private void initUI() {
        setTitle(Constants.APP_NAME + " — Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(520, 620);
        setMinimumSize(Constants.MIN_STUDENT_LOGIN_SIZE);
        setLocationRelativeTo(null);
        setResizable(true);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(Constants.COLOR_BG);
        mainPanel.add(buildHeader(), BorderLayout.NORTH);
        mainPanel.add(buildStudentLoginPanel(), BorderLayout.CENTER);
        mainPanel.add(buildFooter(), BorderLayout.SOUTH);

        add(mainPanel);
        setVisible(true);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel();
        header.setBackground(Constants.COLOR_PRIMARY);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBorder(new EmptyBorder(25, 20, 25, 20));

        JLabel logoLabel = new JLabel("\uD83C\uDF13", JLabel.CENTER);
        logoLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 65));
        logoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel titleLabel = new JLabel("Chuka University");
        titleLabel.setFont(Constants.FONT_TITLE);
        titleLabel.setForeground(Constants.COLOR_WHITE);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subLabel = new JLabel("Student Portal");
        subLabel.setFont(Constants.FONT_BODY);
        subLabel.setForeground(Constants.COLOR_ACCENT);
        subLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        header.add(logoLabel);
        header.add(Box.createVerticalStrut(8));
        header.add(titleLabel);
        header.add(Box.createVerticalStrut(4));
        header.add(subLabel);
        return header;
    }

    private JPanel buildStudentLoginPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Constants.COLOR_BG);
        panel.setBorder(new EmptyBorder(20, 40, 20, 40));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(15, 0, 8, 0);
        panel.add(makeLabel("Registration Number (XXN/NNNNN/YY)"), gbc);
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 15, 0);
        studentRegField = makeTextField();
        panel.add(studentRegField, gbc);

        gbc.gridy = 2;
        gbc.insets = new Insets(5, 0, 8, 0);
        panel.add(makeLabel("Password"), gbc);
        gbc.gridy = 3;
        gbc.insets = new Insets(0, 0, 20, 0);
        studentPassField = new JPasswordField();
        studentPassField.setFont(Constants.FONT_BODY);
        studentPassField.setPreferredSize(new Dimension(0, 40));
        panel.add(studentPassField, gbc);

        gbc.gridy = 4;
        gbc.insets = new Insets(10, 0, 15, 0);
        JButton loginBtn = makePrimaryButton("LOGIN");
        loginBtn.addActionListener(e -> handleStudentLogin());
        panel.add(loginBtn, gbc);

        gbc.gridy = 5;
        gbc.insets = new Insets(0, 0, 5, 0);
        JButton forgotPassBtn = makeLinkButton("Forgot Password?");
        forgotPassBtn.addActionListener(e -> new ForgotPasswordDialog(this).setVisible(true));
        panel.add(forgotPassBtn, gbc);

        gbc.gridy = 6;
        gbc.insets = new Insets(20, 0, 0, 0);
        JButton backBtn = makeLinkButton("← Back to Role Selection");
        backBtn.addActionListener(e -> {
            dispose();
            new RoleSelectionFrame();
        });
        panel.add(backBtn, gbc);

        return panel;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel();
        footer.setBackground(Constants.COLOR_PRIMARY);
        footer.setBorder(new EmptyBorder(8, 0, 8, 0));
        JLabel label = new JLabel("© 2026 Chuka University");
        label.setFont(Constants.FONT_SMALL);
        label.setForeground(new Color(200, 200, 200));
        footer.add(label);
        return footer;
    }

    private void handleStudentLogin() {
        String regNumber = studentRegField.getText().trim().toUpperCase();
        String password = new String(studentPassField.getPassword());

        if (regNumber.isEmpty() || password.isEmpty()) {
            showError("Please enter both your registration number and password.");
            return;
        }

        if (!main.utils.Validator.isValidRegNumber(regNumber)) {
            showError("Please enter a valid registration number in the format XXN/NNNNN/YY.");
            return;
        }

        // Step 1: Verify password FIRST
        Student student = authService.loginStudent(regNumber, password);
        if (student == null) {
            showError(authService.getLastMessage());
            return;
        }

        // Step 2: Only prompt for OTP if the student hasn't changed their password
        // (first login)
        if (!student.isPasswordChanged()) {
            if (!authService.sendOTPToStudent(student)) {
                showError(authService.getLastMessage());
                return;
            }

            // OTP Verification
            OTPDialog otpDialog = new OTPDialog(this, student, authService);
            otpDialog.setVisible(true);
            if (!otpDialog.isVerified()) {
                return; // Stay on login if OTP fails
            }

            // Step 3: Force Password Change if first login
            showForcePasswordChangeDialog(student);
        } else {
            // Initialize session if OTP was bypassed
            AuthService.setCurrentStudent(student);
        }

        dispose();
        new StudentDashboard(AuthService.getCurrentStudent());
    }

    private void showForcePasswordChangeDialog(Student student) {
        JPasswordField newPass = new JPasswordField();
        JPasswordField confirmPass = new JPasswordField();
        Object[] message = {
                "First time login detected. Please create a new password.",
                "New Password:", newPass,
                "Confirm Password:", confirmPass
        };

        while (true) {
            int option = JOptionPane.showConfirmDialog(this, message, "Create New Password",
                    JOptionPane.OK_CANCEL_OPTION);
            if (option == JOptionPane.OK_OPTION) {
                String pass = new String(newPass.getPassword());
                String confirm = new String(confirmPass.getPassword());
                if (!main.utils.Validator.isValidPassword(pass)) {
                    JOptionPane.showMessageDialog(this,
                            "Password must be at least 8 chars, with uppercase, digit, and special char.", "Error",
                            JOptionPane.ERROR_MESSAGE);
                    continue;
                }
                if (!pass.equals(confirm)) {
                    JOptionPane.showMessageDialog(this, "Passwords do not match.", "Error", JOptionPane.ERROR_MESSAGE);
                    continue;
                }
                if (authService.updateStudentPassword(student.getStudentId(), pass)) {
                    JOptionPane.showMessageDialog(this, "Password updated successfully. You can now proceed.",
                            "Success", JOptionPane.INFORMATION_MESSAGE);
                    // Refresh student data
                    AuthService.setCurrentStudent(authService.getStudentByRegNumber(student.getRegNumber()));
                    break;
                } else {
                    JOptionPane.showMessageDialog(this, "Could not update password. Try again.", "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            } else {
                AuthService.logout();
                System.exit(0);
            }
        }
    }

    private JLabel makeLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Constants.FONT_SMALL);
        label.setForeground(Constants.COLOR_TEXT);
        return label;
    }

    private JTextField makeTextField() {
        JTextField field = new JTextField();
        field.setFont(Constants.FONT_BODY);
        field.setPreferredSize(new Dimension(0, 40));
        return field;
    }

    private JButton makePrimaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(Constants.FONT_BUTTON);
        btn.setBackground(Constants.COLOR_PRIMARY);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setPreferredSize(new Dimension(0, 45));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JButton makeLinkButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(Constants.FONT_SMALL);
        btn.setForeground(Constants.COLOR_SECONDARY);
        btn.setBackground(Constants.COLOR_BG);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Login Error", JOptionPane.ERROR_MESSAGE);
    }
}