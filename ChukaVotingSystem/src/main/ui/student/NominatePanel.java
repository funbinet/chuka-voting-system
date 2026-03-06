package main.ui.student;

import main.models.Candidate;
import main.models.Student;
import main.services.CandidateService;
import main.utils.Constants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

public class NominatePanel extends JPanel {

    private Student          student;
    private CandidateService candidateService;

    public NominatePanel(Student student) {
        this.student          = student;
        this.candidateService = new CandidateService();
        setBackground(Constants.COLOR_BG);
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(10, 10, 10, 10));
        buildUI();
    }

    private void buildUI() {
        JLabel heading = new JLabel("✍️ Nominate a Candidate");
        heading.setFont(Constants.FONT_HEADING);
        heading.setForeground(Constants.COLOR_PRIMARY);
        heading.setBorder(new EmptyBorder(0, 0, 15, 0));
        add(heading, BorderLayout.NORTH);

        List<Candidate> pending = candidateService.getPendingApplications();
        // Filter to same faculty
        pending.removeIf(c -> c.getFacultyId() != student.getFacultyId());

        if (pending.isEmpty()) {
            JLabel msg = new JLabel("No pending candidate applications to nominate in your faculty.");
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
            listPanel.add(buildCandidateCard(c));
            listPanel.add(Box.createVerticalStrut(10));
        }

        add(new JScrollPane(listPanel), BorderLayout.CENTER);
    }

    private JPanel buildCandidateCard(Candidate c) {
        JPanel card = new JPanel(new BorderLayout(10, 0));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220)),
            new EmptyBorder(12, 15, 12, 15)
        ));

        JPanel info = new JPanel(new GridLayout(3, 1));
        info.setBackground(Color.WHITE);
        info.add(makeLabel("<html><b>" + c.getStudentName() + "</b> — " + c.getPositionName() + "</html>", Constants.FONT_BODY));
        info.add(makeLabel("Reg: " + c.getRegNumber(), Constants.FONT_SMALL));
        info.add(makeLabel("Nominations: " + c.getNominationCount() + " / " + Constants.MIN_NOMINATIONS, Constants.FONT_SMALL));

        JButton nominateBtn = new JButton("✍️ Nominate");
        nominateBtn.setFont(Constants.FONT_BUTTON);
        nominateBtn.setBackground(Constants.COLOR_ACCENT);
        nominateBtn.setForeground(Color.WHITE);
        nominateBtn.setFocusPainted(false);
        nominateBtn.setBorderPainted(false);
        nominateBtn.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                "Nominate " + c.getStudentName() + " for " + c.getPositionName() + "?",
                "Confirm Nomination", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                String result = candidateService.nominateCandidate(c.getApplicationId(), student.getStudentId());
                JOptionPane.showMessageDialog(this, result);
                nominateBtn.setEnabled(false);
                nominateBtn.setText("✅ Nominated");
            }
        });

        card.add(info, BorderLayout.CENTER);
        card.add(nominateBtn, BorderLayout.EAST);
        return card;
    }

    private JLabel makeLabel(String text, Font font) {
        JLabel label = new JLabel(text);
        label.setFont(font);
        return label;
    }
}
