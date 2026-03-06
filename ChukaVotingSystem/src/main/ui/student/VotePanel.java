package main.ui.student;

import main.models.Candidate;
import main.models.Election;
import main.models.Student;
import main.services.ElectionService;
import main.services.VotingService;
import main.utils.Constants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.*;
import java.util.List;

public class VotePanel extends JPanel {

    private Student         student;
    private VotingService   votingService;
    private ElectionService electionService;

    public VotePanel(Student student) {
        this.student         = student;
        this.votingService   = new VotingService();
        this.electionService = new ElectionService();
        setBackground(Constants.COLOR_BG);
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(10, 10, 10, 10));
        buildUI();
    }

    private void buildUI() {
        JLabel heading = new JLabel("🗳️ Active Elections — " + student.getFacultyName());
        heading.setFont(Constants.FONT_HEADING);
        heading.setForeground(Constants.COLOR_PRIMARY);
        heading.setBorder(new EmptyBorder(0, 0, 15, 0));
        add(heading, BorderLayout.NORTH);

        electionService.syncElectionStatuses();
        List<Election> elections = electionService.getActiveElectionsForFaculty(student.getFacultyId());

        if (elections.isEmpty()) {
            JLabel noElec = new JLabel("No active elections for your faculty at this time.");
            noElec.setFont(Constants.FONT_BODY);
            noElec.setForeground(Color.GRAY);
            noElec.setHorizontalAlignment(JLabel.CENTER);
            add(noElec, BorderLayout.CENTER);
            return;
        }

        JPanel electionsPanel = new JPanel();
        electionsPanel.setLayout(new BoxLayout(electionsPanel, BoxLayout.Y_AXIS));
        electionsPanel.setBackground(Constants.COLOR_BG);

        for (Election election : elections) {
            electionsPanel.add(buildElectionCard(election));
            electionsPanel.add(Box.createVerticalStrut(20));
        }

        add(new JScrollPane(electionsPanel), BorderLayout.CENTER);
    }

    private JPanel buildElectionCard(Election election) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Constants.COLOR_SECONDARY, 1),
            new EmptyBorder(15, 15, 15, 15)
        ));

        JLabel title = new JLabel("📋 " + election.getTitle());
        title.setFont(Constants.FONT_HEADING);
        title.setForeground(Constants.COLOR_PRIMARY);

        List<Candidate> candidates = votingService.getCandidatesForElection(election.getElectionId());

        // Group candidates by position
        Map<String, List<Candidate>> byPosition = new LinkedHashMap<>();
        for (Candidate c : candidates) {
            byPosition.computeIfAbsent(c.getPositionName(), k -> new ArrayList<>()).add(c);
        }

        JPanel positionsPanel = new JPanel();
        positionsPanel.setLayout(new BoxLayout(positionsPanel, BoxLayout.Y_AXIS));
        positionsPanel.setBackground(Color.WHITE);

        for (Map.Entry<String, List<Candidate>> entry : byPosition.entrySet()) {
            positionsPanel.add(buildPositionSection(election, entry.getKey(), entry.getValue()));
            positionsPanel.add(Box.createVerticalStrut(10));
        }

        card.add(title, BorderLayout.NORTH);
        card.add(positionsPanel, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildPositionSection(Election election, String positionName, List<Candidate> candidates) {
        JPanel section = new JPanel(new BorderLayout());
        section.setBackground(new Color(245, 248, 255));
        section.setBorder(new EmptyBorder(8, 8, 8, 8));

        boolean alreadyVoted = candidates.isEmpty() ? false :
            votingService.hasVoted(student.getStudentId(), election.getElectionId(), candidates.get(0).getPositionId());

        JLabel posLabel = new JLabel("🏷️ " + positionName + (alreadyVoted ? "  ✅ Voted" : ""));
        posLabel.setFont(Constants.FONT_BUTTON);
        posLabel.setForeground(Constants.COLOR_PRIMARY);

        JPanel candidatesPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        candidatesPanel.setBackground(new Color(245, 248, 255));

        ButtonGroup group = new ButtonGroup();
        List<JRadioButton> radioButtons = new ArrayList<>();

        for (Candidate c : candidates) {
            JRadioButton rb = new JRadioButton("<html><b>" + c.getStudentName() + "</b><br><small>" + c.getRegNumber() + "</small></html>");
            rb.setFont(Constants.FONT_BODY);
            rb.setBackground(new Color(245, 248, 255));
            rb.putClientProperty("candidate", c);
            rb.setEnabled(!alreadyVoted);
            group.add(rb);
            radioButtons.add(rb);
            candidatesPanel.add(rb);
        }

        JButton voteBtn = new JButton(alreadyVoted ? "✅ Already Voted" : "Cast Vote");
        voteBtn.setFont(Constants.FONT_BUTTON);
        voteBtn.setBackground(alreadyVoted ? Constants.COLOR_SUCCESS : Constants.COLOR_ACCENT);
        voteBtn.setForeground(Color.WHITE);
        voteBtn.setFocusPainted(false);
        voteBtn.setBorderPainted(false);
        voteBtn.setEnabled(!alreadyVoted);

        voteBtn.addActionListener(e -> {
            JRadioButton selected = radioButtons.stream().filter(AbstractButton::isSelected).findFirst().orElse(null);
            if (selected == null) {
                JOptionPane.showMessageDialog(this, "Please select a candidate.", "Select Candidate", JOptionPane.WARNING_MESSAGE);
                return;
            }
            Candidate chosen = (Candidate) selected.getClientProperty("candidate");
            int confirm = JOptionPane.showConfirmDialog(this,
                "Confirm your vote for " + chosen.getStudentName() + " as " + positionName + "?",
                "Confirm Vote", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                String result = votingService.castVote(
                    student.getStudentId(), election.getElectionId(),
                    chosen.getPositionId(), chosen.getApplicationId()
                );
                JOptionPane.showMessageDialog(this, result);
                if (result.startsWith("✅")) {
                    voteBtn.setText("✅ Voted");
                    voteBtn.setBackground(Constants.COLOR_SUCCESS);
                    voteBtn.setEnabled(false);
                    radioButtons.forEach(rb -> rb.setEnabled(false));
                }
            }
        });

        section.add(posLabel, BorderLayout.NORTH);
        section.add(candidatesPanel, BorderLayout.CENTER);
        section.add(voteBtn, BorderLayout.EAST);
        return section;
    }
}
