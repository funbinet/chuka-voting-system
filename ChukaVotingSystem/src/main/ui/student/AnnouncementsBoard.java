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
        List<Announcement> list = announcementDAO.getAllActive();
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm");

        if (list.isEmpty()) {
            listPanel.add(new JLabel("No active announcements."));
        } else {
            for (Announcement a : list) {
                listPanel.add(createCard(a, sdf));
                listPanel.add(Box.createVerticalStrut(15));
            }
        }
        listPanel.revalidate();
        listPanel.repaint();
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
