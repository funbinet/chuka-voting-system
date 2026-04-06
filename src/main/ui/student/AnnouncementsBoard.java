package main.ui.student;

import main.dao.AnnouncementDAO;
import main.models.Announcement;
import main.utils.Constants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.List;

public class AnnouncementsBoard extends JPanel {

    private AnnouncementDAO announcementDAO;
    private JPanel          listPanel;

    public AnnouncementsBoard() {
        this.announcementDAO = new AnnouncementDAO();
        setBackground(Constants.COLOR_BG);
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(10, 10, 10, 10));
        initUI();
        refreshData();
    }

    private void initUI() {
        JLabel heading = new JLabel("📢 Announcements & News");
        heading.setFont(Constants.FONT_HEADING);
        heading.setForeground(Constants.COLOR_PRIMARY);
        heading.setBorder(new EmptyBorder(0, 0, 15, 0));
        add(heading, BorderLayout.NORTH);

        listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(Constants.COLOR_BG);

        JScrollPane scroll = new JScrollPane(listPanel);
        scroll.setBorder(null);
        add(scroll, BorderLayout.CENTER);
    }

    public void refreshData() {
        listPanel.removeAll();
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm");

        boolean hasItems = false;

        // Render Live Election Results First
        if (main.services.AuthService.isLoggedIn() && Constants.ROLE_STUDENT.equals(main.services.AuthService.getCurrentRole())) {
            main.services.ElectionService elecService = new main.services.ElectionService();
            main.models.Student stu = main.services.AuthService.getCurrentStudent();
            if (stu != null) {
                // Fetch active elections from all faculties globally
                List<main.models.Election> activeElections = elecService.getAllActiveElections();
                for (main.models.Election e : activeElections) {
                    listPanel.add(buildLiveResultsCard(e));
                    listPanel.add(Box.createVerticalStrut(15));
                    hasItems = true;
                }
            }
        }

        // Render Static Announcements
        List<Announcement> list = announcementDAO.getAllActive();
        if (!list.isEmpty()) {
            for (Announcement a : list) {
                listPanel.add(createCard(a, sdf));
                listPanel.add(Box.createVerticalStrut(15));
                hasItems = true;
            }
        }

        if (!hasItems) {
            JLabel emptyLbl = new JLabel("No active announcements or live elections currently available.");
            emptyLbl.setFont(Constants.FONT_BODY);
            emptyLbl.setBorder(new EmptyBorder(10, 10, 10, 10));
            listPanel.add(emptyLbl);
        }

        listPanel.revalidate();
        listPanel.repaint();
    }

    private JPanel buildLiveResultsCard(main.models.Election e) {
        JPanel card = new JPanel(new BorderLayout(10, 10));
        card.setBackground(new Color(240, 250, 255));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(150, 200, 255), 2),
            new EmptyBorder(15, 15, 15, 15)
        ));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 250));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(240, 250, 255));
        JLabel title = new JLabel("🔴 LIVE UPDATES: " + e.getTitle() + " (" + e.getFacultyName() + ")");
        title.setFont(new Font("Segoe UI", Font.BOLD, 15));
        title.setForeground(Constants.COLOR_PRIMARY);
        header.add(title, BorderLayout.WEST);

        main.services.VotingService vs = new main.services.VotingService();
        List<main.models.Candidate> candidates = vs.getCandidatesForElection(e.getElectionId());
        
        StringBuilder sb = new StringBuilder("<html><div style='padding:5px;'>");
        if (candidates.isEmpty()) {
            sb.append("<i>No candidates available.</i>");
        } else {
            java.util.Map<String, java.util.List<main.models.Candidate>> byPosition = new java.util.LinkedHashMap<>();
            for (main.models.Candidate c : candidates) {
                byPosition.computeIfAbsent(c.getPositionName(), k -> new java.util.ArrayList<>()).add(c);
            }
            
            for (java.util.Map.Entry<String, java.util.List<main.models.Candidate>> entry : byPosition.entrySet()) {
                sb.append("<b>").append(entry.getKey()).append("</b><ul style='margin-top:2px; margin-bottom:8px;'>");
                java.util.List<main.models.Candidate> positionCands = entry.getValue();
                java.util.Map<Integer, Integer> votes = vs.getResults(e.getElectionId(), positionCands.get(0).getPositionId());
                
                positionCands.sort((c1, c2) -> votes.getOrDefault(c2.getApplicationId(), 0) - votes.getOrDefault(c1.getApplicationId(), 0));
                
                for (main.models.Candidate c : positionCands) {
                    int vCount = votes.getOrDefault(c.getApplicationId(), 0);
                    sb.append("<li>")
                      .append(c.getStudentName())
                      .append(" (<i>").append(c.getCoalitionName()).append("</i>)")
                      .append(" — <b style='color:#d9534f'>").append(vCount).append(" votes</b></li>");
                }
                sb.append("</ul>");
            }
        }
        sb.append("</div></html>");

        JLabel body = new JLabel(sb.toString());
        body.setFont(Constants.FONT_BODY);

        card.add(header, BorderLayout.NORTH);
        card.add(body, BorderLayout.CENTER);
        return card;
    }

    private JPanel createCard(Announcement a, SimpleDateFormat sdf) {
        JPanel card = new JPanel(new BorderLayout(10, 10));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(230, 230, 230), 1),
            new EmptyBorder(15, 15, 15, 15)
        ));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);

        JLabel title = new JLabel(a.getTitle());
        title.setFont(new Font("Segoe UI", Font.BOLD, 15));
        title.setForeground(Constants.COLOR_PRIMARY);

        JLabel date = new JLabel(sdf.format(a.getPostedAt()));
        date.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        date.setForeground(Color.GRAY);

        header.add(title, BorderLayout.WEST);
        header.add(date, BorderLayout.EAST);

        String preview = a.getBody().length() > 100 ? a.getBody().substring(0, 97) + "..." : a.getBody();
        JLabel body = new JLabel("<html>" + preview + "</html>");
        body.setFont(Constants.FONT_BODY);

        JButton viewBtn = new JButton("View Full Message");
        viewBtn.setFont(Constants.FONT_SMALL);
        viewBtn.addActionListener(e -> {
            JTextArea area = new JTextArea(a.getBody());
            area.setWrapStyleWord(true);
            area.setLineWrap(true);
            area.setEditable(false);
            area.setRows(10);
            area.setColumns(30);
            JOptionPane.showMessageDialog(this, new JScrollPane(area), a.getTitle(), JOptionPane.PLAIN_MESSAGE);
        });

        card.add(header, BorderLayout.NORTH);
        card.add(body, BorderLayout.CENTER);
        card.add(viewBtn, BorderLayout.SOUTH);

        return card;
    }
}
