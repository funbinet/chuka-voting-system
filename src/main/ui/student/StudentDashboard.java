package main.ui.student;

import main.models.Election;
import main.models.Student;
import main.services.AuthService;
import main.services.ElectionService;
import main.ui.RoleSelectionFrame;
import main.utils.Constants;
import main.utils.SessionManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class StudentDashboard extends JFrame {

    private Student         student;
    private JPanel          contentPanel;
    private ElectionService electionService;
    private final List<Timer> countdownTimers = new ArrayList<>();

    public StudentDashboard(Student student) {
        this.student         = student;
        this.electionService = new ElectionService();
        initUI();
        SessionManager.getInstance().startSession(this);
    }

    private void initUI() {
        setTitle("Student Dashboard — " + student.getFullName());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(950, 650);
        setMinimumSize(Constants.MIN_STUDENT_DASHBOARD_SIZE);
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

        JLabel title = new JLabel("\uD83C\uDF13 " + Constants.APP_NAME);
        title.setFont(Constants.FONT_HEADING);
        title.setForeground(Color.WHITE);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rightPanel.setBackground(Constants.COLOR_PRIMARY);

        JLabel userLabel = new JLabel("\uD83D\uDC64 " + student.getFullName() + " | " + student.getFacultyName());
        userLabel.setFont(Constants.FONT_SMALL);
        userLabel.setForeground(Constants.COLOR_ACCENT);

        JButton logoutBtn = new JButton("Logout");
        logoutBtn.setFont(Constants.FONT_SMALL);
        logoutBtn.setBackground(Constants.COLOR_DANGER);
        logoutBtn.setForeground(Color.WHITE);
        logoutBtn.setFocusPainted(false);
        logoutBtn.setBorderPainted(false);
        logoutBtn.addActionListener(e -> {
            stopCountdownTimers();
            SessionManager.getInstance().stopSession();
            AuthService.logout();
            dispose();
            new RoleSelectionFrame();
        });

        JButton refreshBtn = new JButton("🔄 Refresh");
        refreshBtn.setFont(Constants.FONT_SMALL);
        refreshBtn.setBackground(Constants.COLOR_SUCCESS);
        refreshBtn.setForeground(Color.WHITE);
        refreshBtn.setFocusPainted(false);
        refreshBtn.setBorderPainted(false);
        refreshBtn.addActionListener(e -> {
            stopCountdownTimers();
            SessionManager.getInstance().stopSession();
            dispose();
            new StudentDashboard(student);
        });

        rightPanel.add(userLabel);
        rightPanel.add(Box.createHorizontalStrut(10));
        rightPanel.add(refreshBtn);
        rightPanel.add(Box.createHorizontalStrut(5));
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

        String[] menus = {"🏠 Home", "📢 Announcements", "🗳️ Vote Now", "📊 Results", "🎓 Apply for Candidacy"};

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
        stopCountdownTimers();
        contentPanel.removeAll();
        if (menu.contains("Home"))                showHome();
        else if (menu.contains("Announcements"))  showAnnouncementsBoard();
        else if (menu.contains("Vote"))           showVotePanel();
        else if (menu.contains("Results"))        showResultsPanel();
        else if (menu.contains("Apply"))          showApplyDialog();
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private void showApplyDialog() {
        ApplyForCandidacyDialog dialog = new ApplyForCandidacyDialog(this, student);
        dialog.setVisible(true);
        showHome(); // return to home when closed
    }

    private void showHome() {
        stopCountdownTimers();

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Constants.COLOR_BG);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel welcome = new JLabel("<html><center>" +
            "<h2 style='color:#1a5276'>Welcome, " + student.getFullName() + "!</h2>" +
            "<p>Reg No: <b>" + student.getRegNumber() + "</b></p>" +
            "<p>Faculty: <b>" + student.getFacultyName() + "</b></p>" +
            "<p>Year: <b>" + student.getYearOfStudy() + "</b> | GPA: <b>" + student.getGpa() + "</b></p>" +
            "</center></html>");
        welcome.setFont(Constants.FONT_BODY);
        welcome.setHorizontalAlignment(JLabel.CENTER);

        // Countdown Timer Panel for Active Elections
        JPanel countdowns = new JPanel();
        countdowns.setLayout(new BoxLayout(countdowns, BoxLayout.Y_AXIS));
        countdowns.setBackground(Constants.COLOR_BG);
        
        List<Election> activeElections = electionService.getActiveElectionsForFaculty(student.getFacultyId());
        int eligibleCount = 0;
        for (Election e : activeElections) {
            if (!isEligibleForElection(e)) {
                continue;
            }
            eligibleCount++;
            countdowns.add(createCountdownPanel(e));
            countdowns.add(Box.createVerticalStrut(10));
        }

        if (eligibleCount == 0) {
            JLabel none = new JLabel("No active elections matching your profile right now.");
            none.setFont(Constants.FONT_BODY);
            none.setForeground(Color.GRAY);
            none.setHorizontalAlignment(JLabel.CENTER);
            countdowns.add(none);
        }

        // Stats cards
        JPanel cards = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 15));
        cards.setBackground(Constants.COLOR_BG);
        cards.add(makeStatCard("🗳️", "Vote Now", "Cast your faculty vote", Constants.COLOR_PRIMARY));
        cards.add(makeStatCard("📊", "Results", "View election results", Constants.COLOR_SECONDARY));

        gbc.gridy = 0; panel.add(welcome, gbc);
        if (eligibleCount > 0) {
            gbc.gridy = 1; panel.add(countdowns, gbc);
        }
        gbc.gridy = 2; panel.add(cards, gbc);

        contentPanel.add(new JScrollPane(panel), BorderLayout.CENTER);
    }

    private boolean isEligibleForElection(Election election) {
        if (election == null) {
            return false;
        }

        if (election.getFacultyId() > 0 && election.getFacultyId() != student.getFacultyId()) {
            return false;
        }

        String position = election.getPositionName() == null ? "" : election.getPositionName().toLowerCase();

        if (position.contains("female") && !"FEMALE".equalsIgnoreCase(student.getGender())) {
            return false;
        }
        if (position.contains("male") && !position.contains("female") && !"MALE".equalsIgnoreCase(student.getGender())) {
            return false;
        }

        if ((position.contains("non-resident") || position.contains("non resident")) && student.isResident()) {
            return false;
        }
        if (position.contains("resident") && !(position.contains("non-resident") || position.contains("non resident")) && !student.isResident()) {
            return false;
        }

        return true;
    }

    private JPanel createCountdownPanel(Election election) {
        JPanel p = new JPanel(new BorderLayout(10, 0));
        p.setBackground(new Color(255, 245, 230));
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(255, 165, 0), 1),
            new EmptyBorder(10, 15, 10, 15)
        ));

        JLabel titleLbl = new JLabel("⏳ Live Election: " + election.getTitle());
        titleLbl.setFont(Constants.FONT_BUTTON);
        titleLbl.setForeground(new Color(150, 75, 0));

        JLabel timerLbl = new JLabel("Loading timer...");
        timerLbl.setFont(new Font("Monospaced", Font.BOLD, 14));
        timerLbl.setForeground(Constants.COLOR_DANGER);

        Timer timer = new Timer(1000, e -> {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime end = election.getEndDate().toLocalDateTime();
            
            if (now.isAfter(end)) {
                timerLbl.setText("Voting has ended — View Results");
                ((Timer)e.getSource()).stop();
            } else {
                Duration diff = Duration.between(now, end);
                long h = diff.toHours();
                long m = diff.toMinutesPart();
                long s = diff.toSecondsPart();
                timerLbl.setText(String.format("Voting closes in: %dh %dm %ds", h, m, s));
            }
        });
        timer.start();

        countdownTimers.add(timer);

        p.add(titleLbl, BorderLayout.WEST);
        p.add(timerLbl, BorderLayout.EAST);
        
        return p;
    }

    private void stopCountdownTimers() {
        for (Timer timer : countdownTimers) {
            if (timer != null && timer.isRunning()) {
                timer.stop();
            }
        }
        countdownTimers.clear();
    }

    private void showAnnouncementsBoard() {
        contentPanel.add(new AnnouncementsBoard(), BorderLayout.CENTER);
    }

    private void showVotePanel() {
        contentPanel.add(new VotePanel(student), BorderLayout.CENTER);
    }

    private void showResultsPanel() {
        contentPanel.add(new ResultsViewPanel(student), BorderLayout.CENTER);
    }

    private JPanel makeStatCard(String icon, String title, String desc, Color color) {
        JPanel card = new JPanel(new GridLayout(3, 1));
        card.setBackground(color);
        card.setPreferredSize(new Dimension(160, 120));
        card.setBorder(new EmptyBorder(15, 15, 15, 15));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

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

        card.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                handleSidebarClick(title);
            }
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                card.setBackground(color.brighter());
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                card.setBackground(color);
            }
        });

        return card;
    }
}