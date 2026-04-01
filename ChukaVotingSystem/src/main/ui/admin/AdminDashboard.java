package main.ui.admin;

import main.models.Admin;
import main.services.AuthService;
import main.ui.LoginFrame;
import main.utils.Constants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class AdminDashboard extends JFrame {

    private Admin  admin;
    private JPanel contentPanel;

    public AdminDashboard(Admin admin) {
        this.admin = admin;
        initUI();
    }

    private void initUI() {
        setTitle("Admin Dashboard — " + admin.getFullName());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 700);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(Constants.COLOR_BG);

        mainPanel.add(buildTopBar(), BorderLayout.NORTH);
        mainPanel.add(buildSidebar(), BorderLayout.WEST);

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
        topBar.setBackground(new Color(15, 52, 96));
        topBar.setBorder(new EmptyBorder(12, 20, 12, 20));

        JLabel title = new JLabel("\uD83C\uDF13 " + Constants.APP_NAME + " — ADMIN PANEL");
        title.setFont(Constants.FONT_HEADING);
        title.setForeground(Color.WHITE);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        right.setBackground(new Color(15, 52, 96));

        JLabel adminLabel = new JLabel("\uD83D\uDD11 " + admin.getFullName());
        adminLabel.setFont(Constants.FONT_SMALL);
        adminLabel.setForeground(Constants.COLOR_ACCENT);

        JButton logoutBtn = new JButton("Logout");
        logoutBtn.setFont(Constants.FONT_SMALL);
        logoutBtn.setBackground(Constants.COLOR_DANGER);
        logoutBtn.setForeground(Color.WHITE);
        logoutBtn.setFocusPainted(false);
        logoutBtn.setBorderPainted(false);
        logoutBtn.addActionListener(e -> { AuthService.logout(); dispose(); new LoginFrame(); });

        right.add(adminLabel);
        right.add(logoutBtn);

        topBar.add(title, BorderLayout.WEST);
        topBar.add(right, BorderLayout.EAST);
        return topBar;
    }

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setBackground(new Color(23, 32, 42));
        sidebar.setPreferredSize(new Dimension(220, 0));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBorder(new EmptyBorder(20, 10, 20, 10));

        String[] menus = {
            "🏠  Dashboard",
            "🗳️  Manage Elections",
            "👥  Manage Candidates",
            "📢  Announcements",
            "📊  View Results",
            "👥  Manage Students"
        };

        for (String menu : menus) {
            JButton btn = new JButton(menu);
            btn.setFont(Constants.FONT_BODY);
            btn.setBackground(new Color(23, 32, 42));
            btn.setForeground(Color.WHITE);
            btn.setFocusPainted(false);
            btn.setBorderPainted(false);
            btn.setAlignmentX(Component.CENTER_ALIGNMENT);
            btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.addActionListener(e -> handleMenu(menu.trim()));
            sidebar.add(btn);
            sidebar.add(Box.createVerticalStrut(5));
        }

        return sidebar;
    }

    private void handleMenu(String menu) {
        contentPanel.removeAll();
        if (menu.contains("Dashboard"))           showHome();
        else if (menu.contains("Elections"))      contentPanel.add(new ManageElectionsPanel(admin), BorderLayout.CENTER);
        else if (menu.contains("Candidates"))     contentPanel.add(new ReviewApplicationsPanel(admin), BorderLayout.CENTER);
        else if (menu.contains("Announcements"))  contentPanel.add(new ManageAnnouncementsPanel(admin), BorderLayout.CENTER);
        else if (menu.contains("Results"))        contentPanel.add(new AdminResultsPanel(), BorderLayout.CENTER);
        else if (menu.contains("Students"))       contentPanel.add(new ManageStudentsPanel(), BorderLayout.CENTER);
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private void showHome() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Constants.COLOR_BG);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0;

        JLabel welcome = new JLabel("<html><center>" +
            "<h2 style='color:#1a5276'>Admin Dashboard</h2>" +
            "<p>Welcome back, <b>" + admin.getFullName() + "</b></p>" +
            "</center></html>");
        welcome.setFont(Constants.FONT_BODY);
        welcome.setHorizontalAlignment(JLabel.CENTER);

        JPanel cards = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 15));
        cards.setBackground(Constants.COLOR_BG);
        cards.add(makeCard("🗳️", "Elections", "Create & manage elections", Constants.COLOR_PRIMARY));
        cards.add(makeCard("👥", "Candidates", "Add & manage candidates", Constants.COLOR_ACCENT));
        cards.add(makeCard("📊", "Results", "View live vote counts", Constants.COLOR_SUCCESS));
        cards.add(makeCard("👥", "Students", "Manage student records", Constants.COLOR_SECONDARY));

        gbc.gridy = 0; panel.add(welcome, gbc);
        gbc.gridy = 1; panel.add(cards, gbc);
        contentPanel.add(panel, BorderLayout.CENTER);
    }

    private JPanel makeCard(String icon, String title, String desc, Color color) {
        JPanel card = new JPanel(new GridLayout(3, 1));
        card.setBackground(color);
        card.setPreferredSize(new Dimension(180, 130));
        card.setBorder(new EmptyBorder(15, 15, 15, 15));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel ico = new JLabel(icon, JLabel.CENTER);
        ico.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 30));

        JLabel t = new JLabel(title, JLabel.CENTER);
        t.setFont(Constants.FONT_BUTTON);
        t.setForeground(Color.WHITE);

        JLabel d = new JLabel("<html><center>" + desc + "</center></html>", JLabel.CENTER);
        d.setFont(Constants.FONT_SMALL);
        d.setForeground(new Color(230, 230, 230));

        card.add(ico); card.add(t); card.add(d);

        card.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                handleMenu(title);
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