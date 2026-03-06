package main.ui.student;

import main.models.Student;
import main.services.AuthService;
import main.ui.LoginFrame;
import main.utils.Constants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class StudentDashboard extends JFrame {

    private Student student;
    private JPanel  contentPanel;

    public StudentDashboard(Student student) {
        this.student = student;
        initUI();
    }

    private void initUI() {
        setTitle("Student Dashboard — " + student.getFullName());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(950, 650);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(Constants.COLOR_BG);

        mainPanel.add(buildSidebar(), BorderLayout.WEST);
        mainPanel.add(buildTopBar(), BorderLayout.NORTH);

        contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(Constants.COLOR_BG);
        contentPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        showHome();

        mainPanel.add(contentPanel, BorderLayout.CENTER);
        add(mainPanel);
        setVisible(true);
    }

    private JPanel buildTopBar() {
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(Constants.COLOR_PRIMARY);
        topBar.setBorder(new EmptyBorder(10, 20, 10, 20));

        JLabel title = new JLabel("🎓 " + Constants.APP_NAME);
        title.setFont(Constants.FONT_HEADING);
        title.setForeground(Color.WHITE);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rightPanel.setBackground(Constants.COLOR_PRIMARY);

        JLabel userLabel = new JLabel("👤 " + student.getFullName() + " | " + student.getFacultyName());
        userLabel.setFont(Constants.FONT_SMALL);
        userLabel.setForeground(Constants.COLOR_ACCENT);

        JButton logoutBtn = new JButton("Logout");
        logoutBtn.setFont(Constants.FONT_SMALL);
        logoutBtn.setBackground(Constants.COLOR_DANGER);
        logoutBtn.setForeground(Color.WHITE);
        logoutBtn.setFocusPainted(false);
        logoutBtn.setBorderPainted(false);
        logoutBtn.addActionListener(e -> {
            AuthService.logout();
            dispose();
            new LoginFrame();
        });

        rightPanel.add(userLabel);
        rightPanel.add(Box.createHorizontalStrut(10));
        rightPanel.add(logoutBtn);

        topBar.add(title, BorderLayout.WEST);
        topBar.add(rightPanel, BorderLayout.EAST);
        return topBar;
    }

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setBackground(Constants.COLOR_SECONDARY);
        sidebar.setPreferredSize(new Dimension(200, 0));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBorder(new EmptyBorder(20, 10, 20, 10));

        String[] menus = {"🏠 Home", "🗳️ Vote Now", "📋 Apply as Candidate", "✍️ Nominate", "📊 Results"};

        for (String menu : menus) {
            JButton btn = new JButton(menu);
            btn.setFont(Constants.FONT_BODY);
            btn.setBackground(Constants.COLOR_SECONDARY);
            btn.setForeground(Color.WHITE);
            btn.setFocusPainted(false);
            btn.setBorderPainted(false);
            btn.setAlignmentX(Component.CENTER_ALIGNMENT);
            btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.addActionListener(e -> handleSidebarClick(menu));
            sidebar.add(btn);
            sidebar.add(Box.createVerticalStrut(5));
        }

        return sidebar;
    }

    private void handleSidebarClick(String menu) {
        contentPanel.removeAll();
        switch (menu) {
            case "🏠 Home":             showHome(); break;
            case "🗳️ Vote Now":         showVotePanel(); break;
            case "📋 Apply as Candidate": showApplyPanel(); break;
            case "✍️ Nominate":          showNominatePanel(); break;
            case "📊 Results":           showResultsPanel(); break;
        }
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private void showHome() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Constants.COLOR_BG);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.insets = new Insets(10, 10, 10, 10);

        JLabel welcome = new JLabel("<html><center>" +
            "<h2 style='color:#1a5276'>Welcome, " + student.getFullName() + "!</h2>" +
            "<p>Reg No: <b>" + student.getRegNumber() + "</b></p>" +
            "<p>Faculty: <b>" + student.getFacultyName() + "</b></p>" +
            "<p>Year: <b>" + student.getYearOfStudy() + "</b> | GPA: <b>" + student.getGpa() + "</b></p>" +
            "<p>Verified: <b>" + (student.isVerified() ? "✅ Yes" : "❌ No") + "</b></p>" +
            "</center></html>");
        welcome.setFont(Constants.FONT_BODY);
        welcome.setHorizontalAlignment(JLabel.CENTER);

        // Stats cards
        JPanel cards = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 15));
        cards.setBackground(Constants.COLOR_BG);
        cards.add(makeStatCard("🗳️", "Vote Now", "Cast your faculty vote", Constants.COLOR_PRIMARY));
        cards.add(makeStatCard("📋", "Apply", "Run for a position", Constants.COLOR_SUCCESS));
        cards.add(makeStatCard("📊", "Results", "View election results", Constants.COLOR_SECONDARY));

        gbc.gridy = 0; panel.add(welcome, gbc);
        gbc.gridy = 1; panel.add(cards, gbc);

        contentPanel.add(panel, BorderLayout.CENTER);
    }

    private void showVotePanel() {
        contentPanel.add(new VotePanel(student), BorderLayout.CENTER);
    }

    private void showApplyPanel() {
        contentPanel.add(new CandidateApplicationPanel(student), BorderLayout.CENTER);
    }

    private void showNominatePanel() {
        contentPanel.add(new NominatePanel(student), BorderLayout.CENTER);
    }

    private void showResultsPanel() {
        contentPanel.add(new ResultsViewPanel(student), BorderLayout.CENTER);
    }

    private JPanel makeStatCard(String icon, String title, String desc, Color color) {
        JPanel card = new JPanel(new GridLayout(3, 1));
        card.setBackground(color);
        card.setPreferredSize(new Dimension(160, 120));
        card.setBorder(new EmptyBorder(15, 15, 15, 15));

        JLabel iconLabel = new JLabel(icon, JLabel.CENTER);
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 28));

        JLabel titleLabel = new JLabel(title, JLabel.CENTER);
        titleLabel.setFont(Constants.FONT_BUTTON);
        titleLabel.setForeground(Color.WHITE);

        JLabel descLabel = new JLabel("<html><center>" + desc + "</center></html>", JLabel.CENTER);
        descLabel.setFont(Constants.FONT_SMALL);
        descLabel.setForeground(new Color(230, 230, 230));

        card.add(iconLabel);
        card.add(titleLabel);
        card.add(descLabel);
        return card;
    }
}
