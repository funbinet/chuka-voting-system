package main.ui;

import main.models.Admin;
import main.models.Student;
import main.services.AuthService;
import main.ui.admin.AdminDashboard;
import main.ui.student.StudentDashboard;
import main.utils.Constants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class LoginFrame extends JFrame {

    private AuthService authService;

    private JTabbedPane tabbedPane;

    // Student login fields
    private JTextField     studentRegField;
    private JPasswordField studentPassField;

    // Admin login fields
    private JTextField     adminEmailField;
    private JPasswordField adminPassField;

    public LoginFrame() {
        this.authService = new AuthService();
        initUI();
    }

    private void initUI() {
        setTitle(Constants.APP_NAME + " — Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(480, 580);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(Constants.COLOR_BG);

        mainPanel.add(buildHeader(), BorderLayout.NORTH);
        mainPanel.add(buildLoginTabs(), BorderLayout.CENTER);
        mainPanel.add(buildFooter(), BorderLayout.SOUTH);

        add(mainPanel);
        setVisible(true);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel();
        header.setBackground(Constants.COLOR_PRIMARY);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBorder(new EmptyBorder(25, 20, 25, 20));

        JLabel logoLabel = new JLabel("🎓", JLabel.CENTER);
        logoLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 50));
        logoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel titleLabel = new JLabel("Chuka University");
        titleLabel.setFont(Constants.FONT_TITLE);
        titleLabel.setForeground(Constants.COLOR_WHITE);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subLabel = new JLabel("Student Voting System");
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

    private JTabbedPane buildLoginTabs() {
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(Constants.FONT_BODY);
        tabbedPane.setBackground(Constants.COLOR_BG);

        tabbedPane.addTab("🎓 Student Login", buildStudentLoginPanel());
        tabbedPane.addTab("🔑 Admin Login",   buildAdminLoginPanel());

        return tabbedPane;
    }

    private JPanel buildStudentLoginPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Constants.COLOR_BG);
        panel.setBorder(new EmptyBorder(20, 40, 20, 40));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 0, 8, 0);

        // Reg Number
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(makeLabel("Registration Number (e.g. SCT/2021/001)"), gbc);
        gbc.gridy = 1;
        studentRegField = makeTextField("Enter reg number");
        panel.add(studentRegField, gbc);

        // Password
        gbc.gridy = 2;
        panel.add(makeLabel("Password"), gbc);
        gbc.gridy = 3;
        studentPassField = new JPasswordField();
        studentPassField.setFont(Constants.FONT_BODY);
        studentPassField.setPreferredSize(new Dimension(0, 40));
        panel.add(studentPassField, gbc);

        // Login button
        gbc.gridy = 4;
        gbc.insets = new Insets(20, 0, 8, 0);
        JButton loginBtn = makePrimaryButton("LOGIN");
        loginBtn.addActionListener(e -> handleStudentLogin());
        panel.add(loginBtn, gbc);

        // Register link
        gbc.gridy = 5;
        gbc.insets = new Insets(0, 0, 8, 0);
        JButton registerBtn = makeLinkButton("Don't have an account? Register here");
        registerBtn.addActionListener(e -> openRegistration());
        panel.add(registerBtn, gbc);

        return panel;
    }

    private JPanel buildAdminLoginPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Constants.COLOR_BG);
        panel.setBorder(new EmptyBorder(20, 40, 20, 40));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 0, 8, 0);

        // Email
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(makeLabel("Admin Email"), gbc);
        gbc.gridy = 1;
        adminEmailField = makeTextField("admin@chuka.ac.ke");
        panel.add(adminEmailField, gbc);

        // Password
        gbc.gridy = 2;
        panel.add(makeLabel("Password"), gbc);
        gbc.gridy = 3;
        adminPassField = new JPasswordField();
        adminPassField.setFont(Constants.FONT_BODY);
        adminPassField.setPreferredSize(new Dimension(0, 40));
        panel.add(adminPassField, gbc);

        // Login button
        gbc.gridy = 4;
        gbc.insets = new Insets(20, 0, 8, 0);
        JButton loginBtn = makePrimaryButton("ADMIN LOGIN");
        loginBtn.addActionListener(e -> handleAdminLogin());
        panel.add(loginBtn, gbc);

        return panel;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel();
        footer.setBackground(Constants.COLOR_PRIMARY);
        footer.setBorder(new EmptyBorder(8, 0, 8, 0));
        JLabel label = new JLabel("© 2024 Chuka University | v" + Constants.APP_VERSION);
        label.setFont(Constants.FONT_SMALL);
        label.setForeground(new Color(200, 200, 200));
        footer.add(label);
        return footer;
    }

    // ── Handlers ───────────────────────────────────────────────

    private void handleStudentLogin() {
        String regNumber = studentRegField.getText().trim().toUpperCase();
        String password  = new String(studentPassField.getPassword());

        if (regNumber.isEmpty() || password.isEmpty()) {
            showError("Please fill in all fields.");
            return;
        }

        Student student = authService.loginStudent(regNumber, password);
        if (student == null) {
            showError("Invalid credentials or account inactive.");
            return;
        }

        // Send OTP
        authService.sendOTPToStudent(student);

        // Show OTP dialog
        OTPDialog otpDialog = new OTPDialog(this, student, authService);
        otpDialog.setVisible(true);

        if (otpDialog.isVerified()) {
            dispose();
            new StudentDashboard(student);
        }
    }

    private void handleAdminLogin() {
        String email    = adminEmailField.getText().trim();
        String password = new String(adminPassField.getPassword());

        if (email.isEmpty() || password.isEmpty()) {
            showError("Please fill in all fields.");
            return;
        }

        Admin admin = authService.loginAdmin(email, password);
        if (admin == null) {
            showError("Invalid admin credentials.");
            return;
        }

        dispose();
        new AdminDashboard(admin);
    }

    private void openRegistration() {
        dispose();
        new RegisterFrame();
    }

    // ── UI Helpers ─────────────────────────────────────────────

    private JLabel makeLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Constants.FONT_SMALL);
        label.setForeground(Constants.COLOR_TEXT);
        return label;
    }

    private JTextField makeTextField(String placeholder) {
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
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
