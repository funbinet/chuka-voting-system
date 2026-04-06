package main.ui.admin;

import main.models.Admin;
import main.services.AuthService;
import main.ui.RoleSelectionFrame;
import main.utils.Constants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;
import main.dao.AdminNotificationDAO;
import main.models.AdminNotification;

public class AdminDashboard extends JFrame {

    private Admin  admin;
    private JPanel contentPanel;
    private JLabel adminLabel;
    private AdminNotificationDAO notificationDAO;
    private Timer notificationTimer;
    private JPanel mainPanel;

    public AdminDashboard(Admin admin) {
        this.admin = admin;
        this.notificationDAO = new AdminNotificationDAO();
        initUI();
        startNotificationPolling();
    }

    private void initUI() {
        setTitle("Admin Dashboard — " + admin.getFullName());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 700);
        setMinimumSize(Constants.MIN_ADMIN_DASHBOARD_SIZE);
        setLocationRelativeTo(null);

        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu("Menu");
        JMenuItem manageElectionsItem = new JMenuItem("Manage Elections");
        JMenuItem manageStudentsItem = new JMenuItem("Manage Students");
        JMenuItem manageAnnouncementsItem = new JMenuItem("Manage Announcements");
        JMenuItem reviewApplicationsItem = new JMenuItem("Review Applications");
        JMenuItem auditLogsItem = new JMenuItem("Audit Logs");
        JMenuItem resultsItem = new JMenuItem("View Results");
        JMenuItem manageCoalitionsItem = new JMenuItem("Manage Coalitions");
        JMenuItem logoutItem = new JMenuItem("Logout");

        menu.add(manageElectionsItem);
        menu.add(manageStudentsItem);
        menu.add(manageAnnouncementsItem);
        menu.add(reviewApplicationsItem);
        menu.add(auditLogsItem);
        menu.add(resultsItem);
        menu.add(manageCoalitionsItem);
        menu.add(logoutItem);
        menuBar.add(menu);
        setJMenuBar(menuBar);

        mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(Constants.COLOR_BG);

        manageElectionsItem.addActionListener(e -> handleMenu("Elections"));
        manageStudentsItem.addActionListener(e -> handleMenu("Students"));
        manageAnnouncementsItem.addActionListener(e -> handleMenu("Announcements"));
        reviewApplicationsItem.addActionListener(e -> handleMenu("Candidates"));
        auditLogsItem.addActionListener(e -> handleMenu("Audit"));
        resultsItem.addActionListener(e -> handleMenu("Results"));
        manageCoalitionsItem.addActionListener(e -> handleMenu("Coalitions"));
        logoutItem.addActionListener(e -> {
            AuthService.logout();
            stopNotificationPolling();
            dispose();
            new RoleSelectionFrame();
        });

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

        adminLabel = new JLabel("\uD83D\uDD11 " + admin.getFullName());
        adminLabel.setFont(Constants.FONT_SMALL);
        adminLabel.setForeground(Constants.COLOR_ACCENT);
        
        JButton notifBtn = new JButton("🔔 Notifications");
        notifBtn.setFont(Constants.FONT_SMALL);
        notifBtn.setBackground(new Color(23, 32, 42));
        notifBtn.setForeground(Color.WHITE);
        notifBtn.setFocusPainted(false);
        notifBtn.setBorderPainted(false);
        notifBtn.addActionListener(e -> showNotificationsDialog());

        JButton logoutBtn = new JButton("Logout");
        logoutBtn.setFont(Constants.FONT_SMALL);
        logoutBtn.setBackground(Constants.COLOR_DANGER);
        logoutBtn.setForeground(Color.WHITE);
        logoutBtn.setFocusPainted(false);
        logoutBtn.setBorderPainted(false);
        logoutBtn.addActionListener(e -> {
            AuthService.logout();
            stopNotificationPolling();
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
            stopNotificationPolling();
            dispose();
            new AdminDashboard(admin);
        });

        right.add(adminLabel);
        right.add(refreshBtn);
        right.add(notifBtn);
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
            "🤝  Manage Coalitions",
            "📢  Announcements",
            "📊  View Results",
            "👥  Manage Students",
            "📋  Audit Logs"
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
        else if (menu.contains("Coalitions"))     contentPanel.add(new ManageCoalitionsPanel(admin), BorderLayout.CENTER);
        else if (menu.contains("Announcements"))  contentPanel.add(new ManageAnnouncementsPanel(admin), BorderLayout.CENTER);
        else if (menu.contains("Results"))        contentPanel.add(new AdminResultsPanel(), BorderLayout.CENTER);
        else if (menu.contains("Students"))       contentPanel.add(new ManageStudentsPanel(), BorderLayout.CENTER);
        else if (menu.contains("Audit"))          contentPanel.add(new AuditLogsPanel(), BorderLayout.CENTER);
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private void showHome() {
        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setBackground(Constants.COLOR_BG);

        JLabel welcome = new JLabel("<html><center>" +
            "<h2 style='color:#1a5276'>Admin Dashboard</h2>" +
            "<p>Welcome back, <b>" + admin.getFullName() + "</b></p>" +
            "</center></html>");
        welcome.setFont(Constants.FONT_BODY);
        welcome.setHorizontalAlignment(JLabel.CENTER);

        JPanel welcomeRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        welcomeRow.setBackground(Constants.COLOR_BG);
        welcomeRow.add(welcome);

        GridLayout cardsLayout = new GridLayout(0, 5, 15, 15);
        JPanel cards = new JPanel(cardsLayout);
        cards.setBackground(Constants.COLOR_BG);
        cards.setBorder(new EmptyBorder(0, 8, 8, 8));
        cards.add(makeCard("🗳️", "Elections", "Create & manage elections", Constants.COLOR_PRIMARY));
        cards.add(makeCard("👥", "Candidates", "Add & manage candidates", Constants.COLOR_ACCENT));
        cards.add(makeCard("🤝", "Coalitions", "Manage parties & coalitions", new Color(142, 68, 173)));
        cards.add(makeCard("📊", "Results", "View live vote counts", Constants.COLOR_SUCCESS));
        cards.add(makeCard("👥", "Students", "Manage student records", Constants.COLOR_SECONDARY));
        cards.add(makeCard("📋", "Audit Logs", "Track system activities", new Color(52, 73, 94)));

        JPanel cardsContainer = new JPanel(new BorderLayout());
        cardsContainer.setBackground(Constants.COLOR_BG);
        cardsContainer.add(cards, BorderLayout.NORTH);

        JScrollPane cardsScroll = new JScrollPane(cardsContainer);
        cardsScroll.setBorder(BorderFactory.createEmptyBorder());
        cardsScroll.getViewport().setBackground(Constants.COLOR_BG);
        cardsScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        cardsScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        cardsScroll.getViewport().addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                adjustHomeCardsLayout(cardsLayout, cardsScroll.getViewport().getWidth(), cards);
            }
        });
        SwingUtilities.invokeLater(() -> adjustHomeCardsLayout(cardsLayout, cardsScroll.getViewport().getWidth(), cards));

        panel.add(welcomeRow, BorderLayout.NORTH);
        panel.add(cardsScroll, BorderLayout.CENTER);
        contentPanel.add(panel, BorderLayout.CENTER);
    }

    private void adjustHomeCardsLayout(GridLayout layout, int availableWidth, JPanel cardsPanel) {
        int columns;
        if (availableWidth >= 1320) {
            columns = 6;
        } else if (availableWidth >= 1020) {
            columns = 4;
        } else if (availableWidth >= 760) {
            columns = 3;
        } else if (availableWidth >= 520) {
            columns = 2;
        } else {
            columns = 1;
        }

        if (layout.getColumns() != columns) {
            layout.setColumns(columns);
            cardsPanel.revalidate();
            cardsPanel.repaint();
        }
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

    private void startNotificationPolling() {
        notificationTimer = new Timer(5000, e -> {
            List<AdminNotification> unread = notificationDAO.getUnreadNotificationsForAdmin(admin.getAdminId());
            if (!unread.isEmpty()) {
                adminLabel.setText("\uD83D\uDD11 " + admin.getFullName() + " (🔔 " + unread.size() + " New)");
            } else {
                adminLabel.setText("\uD83D\uDD11 " + admin.getFullName());
            }
        });
        notificationTimer.start();
    }

    private void stopNotificationPolling() {
        if (notificationTimer != null) {
            notificationTimer.stop();
            notificationTimer = null;
        }
    }

    private void showNotificationsDialog() {
        List<AdminNotification> unread = notificationDAO.getUnreadNotificationsForAdmin(admin.getAdminId());
        if (unread.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No new notifications.");
            return;
        }

        StringBuilder sb = new StringBuilder("New Notifications:\n\n");
        for (AdminNotification n : unread) {
            sb.append("- ").append(n.getTitle()).append(": ").append(n.getMessage()).append("\n");
            notificationDAO.markAsRead(n.getId());
        }
        
        JOptionPane.showMessageDialog(this, sb.toString(), "Notifications", JOptionPane.INFORMATION_MESSAGE);
        adminLabel.setText("\uD83D\uDD11 " + admin.getFullName());
    }
}