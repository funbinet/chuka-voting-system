package main.ui.admin;

import main.models.Admin;
import main.models.Candidate;
import main.services.CandidateService;
import main.utils.Constants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

public class ReviewApplicationsPanel extends JPanel {

    private Admin            admin;
    private CandidateService candidateService;

    public ReviewApplicationsPanel(Admin admin) {
        this.admin            = admin;
        this.candidateService = new CandidateService();
        setBackground(Constants.COLOR_BG);
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(10, 10, 10, 10));
        buildUI();
    }

    private void buildUI() {
        JLabel heading = new JLabel("📋 Candidate Applications — Review & Approve");
        heading.setFont(Constants.FONT_HEADING);
        heading.setForeground(Constants.COLOR_PRIMARY);
        heading.setBorder(new EmptyBorder(0, 0, 15, 0));
        add(heading, BorderLayout.NORTH);

        List<Candidate> pending = candidateService.getPendingApplications();

        if (pending.isEmpty()) {
            JLabel msg = new JLabel("No pending applications to review.");
            msg.setFont(Constants.FONT_BODY);
            msg.setForeground(Color.GRAY);
            msg.setHorizontalAlignment(JLabel.CENTER);
            add(msg, BorderLayout.CENTER);
            return;
        }

        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(Constants.COLOR_BG);

        for (Candidate c : pending) {
            listPanel.add(buildApplicationCard(c));
            listPanel.add(Box.createVerticalStrut(12));
        }

        add(new JScrollPane(listPanel), BorderLayout.CENTER);
    }

    private JPanel buildApplicationCard(Candidate c) {
        JPanel card = new JPanel(new BorderLayout(10, 0));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            new EmptyBorder(15, 15, 15, 15)
        ));

        // Info section
        JPanel info = new JPanel(new GridLayout(0, 1, 3, 3));
        info.setBackground(Color.WHITE);
        info.add(boldLabel(c.getStudentName() + " — " + c.getPositionName()));
        info.add(smallLabel("Reg No: " + c.getRegNumber()));
        info.add(smallLabel("Nominations: " + c.getNominationCount() + " / " + Constants.MIN_NOMINATIONS));

        // Manifesto
        JTextArea manifestoArea = new JTextArea(c.getManifesto(), 3, 30);
        manifestoArea.setFont(Constants.FONT_SMALL);
        manifestoArea.setEditable(false);
        manifestoArea.setBackground(new Color(248, 248, 248));
        manifestoArea.setLineWrap(true);
        manifestoArea.setWrapStyleWord(true);
        manifestoArea.setBorder(BorderFactory.createTitledBorder("Manifesto"));

        // Buttons
        JPanel btnPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        btnPanel.setBackground(Color.WHITE);

        JButton approveBtn = new JButton("✅ Approve");
        approveBtn.setFont(Constants.FONT_BUTTON);
        approveBtn.setBackground(Constants.COLOR_SUCCESS);
        approveBtn.setForeground(Color.WHITE);
        approveBtn.setFocusPainted(false);
        approveBtn.setBorderPainted(false);

        JButton rejectBtn = new JButton("❌ Reject");
        rejectBtn.setFont(Constants.FONT_BUTTON);
        rejectBtn.setBackground(Constants.COLOR_DANGER);
        rejectBtn.setForeground(Color.WHITE);
        rejectBtn.setFocusPainted(false);
        rejectBtn.setBorderPainted(false);

        approveBtn.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                "Approve " + c.getStudentName() + " for " + c.getPositionName() + "?",
                "Confirm", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                boolean ok = candidateService.approveApplication(c.getApplicationId(), admin.getAdminId());
                JOptionPane.showMessageDialog(this, ok ? "✅ Approved!" : "❌ Failed.");
                refreshPanel();
            }
        });

        rejectBtn.addActionListener(e -> {
            String reason = JOptionPane.showInputDialog(this, "Enter rejection reason:");
            if (reason != null && !reason.trim().isEmpty()) {
                boolean ok = candidateService.rejectApplication(c.getApplicationId(), admin.getAdminId(), reason);
                JOptionPane.showMessageDialog(this, ok ? "Application rejected." : "❌ Failed.");
                refreshPanel();
            }
        });

        btnPanel.add(approveBtn);
        btnPanel.add(rejectBtn);

        JPanel leftPanel = new JPanel(new BorderLayout(0, 8));
        leftPanel.setBackground(Color.WHITE);
        leftPanel.add(info, BorderLayout.NORTH);
        leftPanel.add(manifestoArea, BorderLayout.CENTER);

        card.add(leftPanel, BorderLayout.CENTER);
        card.add(btnPanel, BorderLayout.EAST);
        return card;
    }

    private void refreshPanel() {
        removeAll();
        buildUI();
        revalidate();
        repaint();
    }

    private JLabel boldLabel(String text) {
        JLabel l = new JLabel("<html><b>" + text + "</b></html>");
        l.setFont(Constants.FONT_BODY);
        return l;
    }

    private JLabel smallLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(Constants.FONT_SMALL);
        l.setForeground(Color.GRAY);
        return l;
    }
}
