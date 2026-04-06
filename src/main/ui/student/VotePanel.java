package main.ui.student;

import main.models.Candidate;
import main.models.Election;
import main.models.Student;
import main.services.ElectionService;
import main.services.VotingService;
import main.utils.Constants;
import main.utils.PositionRules;

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
        removeAll();

        JLabel heading = new JLabel("🗳️ Active Elections — " + student.getFacultyName());
        heading.setFont(Constants.FONT_HEADING);
        heading.setForeground(Constants.COLOR_PRIMARY);
        heading.setBorder(new EmptyBorder(0, 0, 15, 0));
        add(heading, BorderLayout.NORTH);

        electionService.syncElectionStatuses();
        List<Election> activeElections = electionService.getActiveElectionsForFaculty(student.getFacultyId());
        List<Election> elections = new ArrayList<>();
        for (Election election : activeElections) {
            if (isEligibleForElection(election)) {
                elections.add(election);
            }
        }

        if (elections.isEmpty()) {
            JLabel noElec = new JLabel("No active elections you are eligible to vote in at this time.");
            noElec.setFont(Constants.FONT_BODY);
            noElec.setForeground(Color.GRAY);
            noElec.setHorizontalAlignment(JLabel.CENTER);
            add(noElec, BorderLayout.CENTER);
            revalidate();
            repaint();
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
        revalidate();
        repaint();
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

        int eligiblePositions = 0;

        for (Map.Entry<String, List<Candidate>> entry : byPosition.entrySet()) {
            String positionName = entry.getKey();
            if (!isEligibleForPosition(positionName)) {
                continue;
            }
            eligiblePositions++;
            positionsPanel.add(buildPositionSection(election, positionName, entry.getValue()));
            positionsPanel.add(Box.createVerticalStrut(10));
        }

        if (eligiblePositions == 0) {
            JLabel empty = new JLabel("No eligible positions in this election for your profile.");
            empty.setFont(Constants.FONT_BODY);
            empty.setForeground(Color.GRAY);
            positionsPanel.add(empty);
        }

        card.add(title, BorderLayout.NORTH);
        card.add(positionsPanel, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildPositionSection(Election election, String positionName, List<Candidate> candidates) {
        JPanel section = new JPanel(new BorderLayout());
        section.setBackground(Color.WHITE);
        section.setBorder(new EmptyBorder(15, 0, 15, 0));

        boolean alreadyVoted = candidates.isEmpty() ? false :
            votingService.hasVoted(student.getStudentId(), election.getElectionId(), candidates.get(0).getPositionId());

        JLabel posLabel = new JLabel("🏷️ " + positionName + (alreadyVoted ? "  ✅ Voted" : ""));
        posLabel.setFont(Constants.FONT_BUTTON);
        posLabel.setForeground(Constants.COLOR_PRIMARY);
        posLabel.setBorder(new EmptyBorder(0, 0, 10, 0));

        JPanel cardsContainer = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        cardsContainer.setBackground(Color.WHITE);

        final Candidate[] selectedCandidate = {null};
        List<JPanel> cardPanels = new ArrayList<>();

        for (Candidate c : candidates) {
            JPanel card = createCandidateCard(c, alreadyVoted);
            cardPanels.add(card);
            
            if (!alreadyVoted) {
                card.addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override
                    public void mousePressed(java.awt.event.MouseEvent e) {
                        // Reset all cards
                        for (JPanel p : cardPanels) {
                            p.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1));
                            p.setBackground(new Color(252, 252, 252));
                        }
                        // Highlight selected
                        card.setBorder(BorderFactory.createLineBorder(Constants.COLOR_ACCENT, 3));
                        card.setBackground(new Color(240, 245, 255));
                        selectedCandidate[0] = c;
                    }
                });
            }
            cardsContainer.add(card);
        }

        JButton voteBtn = new JButton(alreadyVoted ? "✅ Vote Recorded" : "Cast Vote for " + positionName);
        voteBtn.setFont(Constants.FONT_BUTTON);
        voteBtn.setBackground(alreadyVoted ? Constants.COLOR_SUCCESS : Constants.COLOR_ACCENT);
        voteBtn.setForeground(Color.WHITE);
        voteBtn.setPreferredSize(new Dimension(250, 45));
        voteBtn.setEnabled(!alreadyVoted);

        voteBtn.addActionListener(e -> {
            if (selectedCandidate[0] == null) {
                JOptionPane.showMessageDialog(this, "Please select a candidate card first.", "Selection Required", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int confirm = JOptionPane.showConfirmDialog(this,
                "Confirm your vote for " + selectedCandidate[0].getStudentName() + "?",
                "Confirm Vote", JOptionPane.YES_NO_OPTION);
            
            if (confirm == JOptionPane.YES_OPTION) {
                String result = votingService.castVote(
                    student.getStudentId(), election.getElectionId(),
                    selectedCandidate[0].getPositionId(), selectedCandidate[0].getApplicationId()
                );
                JOptionPane.showMessageDialog(this, result);
                if (result.toLowerCase().contains("successfully")) {
                    // Reload all election cards so the student can continue with other eligible elections.
                    buildUI();
                }
            }
        });

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottomPanel.setBackground(Color.WHITE);
        bottomPanel.add(voteBtn);

        section.add(posLabel, BorderLayout.NORTH);
        section.add(cardsContainer, BorderLayout.CENTER);
        section.add(bottomPanel, BorderLayout.SOUTH);
        
        return section;
    }

    private JPanel createCandidateCard(Candidate c, boolean alreadyVoted) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setPreferredSize(new Dimension(200, 210));
        card.setBackground(new Color(252, 252, 252));
        card.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1));
        card.setCursor(alreadyVoted ? Cursor.getDefaultCursor() : Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel nameLbl = new JLabel(c.getStudentName());
        nameLbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
        nameLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel regLbl = new JLabel(c.getRegNumber());
        regLbl.setFont(Constants.FONT_SMALL);
        regLbl.setForeground(Color.GRAY);
        regLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel coalLbl = new JLabel(c.getCoalitionName());
        coalLbl.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        coalLbl.setForeground(new Color(142, 68, 173));
        coalLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel yearLbl = new JLabel("Year " + c.getYearOfStudy());
        yearLbl.setFont(Constants.FONT_SMALL);
        yearLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton viewBtn = new JButton("View Manifesto");
        viewBtn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        viewBtn.setMargin(new Insets(2, 5, 2, 5));
        viewBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        viewBtn.addActionListener(e -> {
            JTextArea textArea = new JTextArea(c.getManifesto());
            textArea.setLineWrap(true);
            textArea.setWrapStyleWord(true);
            textArea.setEditable(false);
            textArea.setRows(15);
            textArea.setColumns(30);
            
            JScrollPane scrollPane = new JScrollPane(textArea);
            JOptionPane.showMessageDialog(this, scrollPane, c.getStudentName() + "'s Manifesto", JOptionPane.PLAIN_MESSAGE);
        });

        card.add(Box.createVerticalGlue());
        card.add(new JLabel("👤") {{ setFont(new Font("Serif", Font.PLAIN, 40)); setAlignmentX(0.5f); }});
        card.add(Box.createVerticalStrut(10));
        card.add(nameLbl);
        card.add(regLbl);
        card.add(coalLbl);
        card.add(yearLbl);
        card.add(Box.createVerticalStrut(10));
        card.add(viewBtn);
        card.add(Box.createVerticalGlue());

        return card;
    }

    private boolean isEligibleForPosition(String positionName) {
        PositionRules.PositionCategory category = PositionRules.classify(positionName);
        if (!PositionRules.isCanonical(category)) {
            return true;
        }
        return PositionRules.isGenderEligible(student.getGender(), category)
                && PositionRules.isResidencyEligible(student.isResident(), category);
    }

    private boolean isEligibleForElection(Election election) {
        if (election == null) {
            return false;
        }

        if (election.getFacultyId() > 0 && student.getFacultyId() != election.getFacultyId()) {
            return false;
        }

        return isEligibleForPosition(election.getPositionName());
    }
}
