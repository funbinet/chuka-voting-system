package main.ui.admin;

import main.models.Admin;
import main.services.AuthService;
import main.ui.RoleSelectionFrame;
import main.utils.Constants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class AdminLoginFrame extends JFrame {

    private final AuthService authService;
    private JTextField adminEmailField;
    private JPasswordField adminPassField;

    public AdminLoginFrame() {
        this.authService = new AuthService();
        initUI();
    }

    private void initUI() {
        setTitle(Constants.APP_NAME + " — Admin Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(520, 560);
        setMinimumSize(Constants.MIN_ADMIN_LOGIN_SIZE);
        setLocationRelativeTo(null);
        setResizable(true);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(Constants.COLOR_BG);
        mainPanel.add(buildHeader(), BorderLayout.NORTH);
        mainPanel.add(buildAdminLoginPanel(), BorderLayout.CENTER);
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

        JLabel subLabel = new JLabel("Admin Portal");
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

    private JPanel buildAdminLoginPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Constants.COLOR_BG);
        panel.setBorder(new EmptyBorder(20, 40, 20, 40));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 0, 8, 0);

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(makeLabel("Admin Email"), gbc);

        gbc.gridy = 1;
        gbc.insets = new Insets(4, 0, 15, 0);
        adminEmailField = makeTextField();
        panel.add(adminEmailField, gbc);

        gbc.gridy = 2;
        gbc.insets = new Insets(8, 0, 4, 0);
        panel.add(makeLabel("Password"), gbc);

        gbc.gridy = 3;
        gbc.insets = new Insets(4, 0, 20, 0);
        adminPassField = new JPasswordField();
        adminPassField.setFont(Constants.FONT_BODY);
        adminPassField.setPreferredSize(new Dimension(0, 45));
        adminPassField.addActionListener(e -> handleAdminLogin()); // Press enter to login
        panel.add(adminPassField, gbc);

        gbc.gridy = 4;
        gbc.insets = new Insets(10, 0, 15, 0);
        JButton loginBtn = makePrimaryButton("ADMIN LOGIN");
        loginBtn.addActionListener(e -> handleAdminLogin());
        panel.add(loginBtn, gbc);

        gbc.gridy = 5;
        gbc.insets = new Insets(0, 0, 0, 0);
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
        footer.setBackground(new Color(15, 52, 96));
        footer.setBorder(new EmptyBorder(8, 0, 8, 0));
        JLabel label = new JLabel("© 2026 Chuka University");
        label.setFont(Constants.FONT_SMALL);
        label.setForeground(new Color(200, 200, 200));
        footer.add(label);
        return footer;
    }

    private void handleAdminLogin() {
        String email = adminEmailField.getText().trim();
        String password = new String(adminPassField.getPassword());

        if (email.isEmpty() || password.isEmpty()) {
            showError("Please enter both the administrator email and password.");
            return;
        }

        Admin admin = authService.loginAdmin(email, password);
        if (admin == null) {
            showError(authService.getLastMessage());
            return;
        }

        dispose();
        new AdminDashboard(admin);
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
        field.setPreferredSize(new Dimension(0, 45));
        return field;
    }

    private JButton makePrimaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(Constants.FONT_BUTTON);
        btn.setBackground(new Color(15, 52, 96));
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
        btn.setForeground(Constants.COLOR_TEXT); // dark grey
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
