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

public class ResultsViewPanel extends JPanel {

    private Student         student;
    private ElectionService electionService;
    private VotingService   votingService;

    public ResultsViewPanel(Student student) {
        this.student         = student;
        this.electionService = new ElectionService();
        this.votingService   = new VotingService();
        setBackground(Constants.COLOR_BG);
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(10, 10, 10, 10));
        buildUI();
    }

    private void buildUI() {
        JLabel heading = new JLabel("📊 Election Results — " + student.getFacultyName());
        heading.setFont(Constants.FONT_HEADING);
        heading.setForeground(Constants.COLOR_PRIMARY);
        heading.setBorder(new EmptyBorder(0, 0, 15, 0));
        add(heading, BorderLayout.NORTH);

        List<Election> elections = electionService.getElectionsByFaculty(student.getFacultyId());
        elections.removeIf(e -> !"CLOSED".equals(e.getStatus()));

        if (elections.isEmpty()) {
            JLabel msg = new JLabel("No closed elections with results yet.");
            msg.setFont(Constants.FONT_BODY);
            msg.setForeground(Color.GRAY);
            msg.setHorizontalAlignment(JLabel.CENTER);
            add(msg, BorderLayout.CENTER);
            return;
        }

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Constants.COLOR_BG);

        for (Election election : elections) {
            panel.add(buildElectionResult(election));
            panel.add(Box.createVerticalStrut(20));
        }

        add(new JScrollPane(panel), BorderLayout.CENTER);
    }

    private JPanel buildElectionResult(Election election) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Constants.COLOR_PRIMARY),
            new EmptyBorder(15, 15, 15, 15)
        ));

        JLabel title = new JLabel("🏆 " + election.getTitle() + " — RESULTS");
        title.setFont(Constants.FONT_HEADING);
        title.setForeground(Constants.COLOR_PRIMARY);

        List<Candidate> candidates = votingService.getCandidatesForElection(election.getElectionId());
        Map<String, List<Candidate>> byPosition = new LinkedHashMap<>();
        for (Candidate c : candidates) {
            byPosition.computeIfAbsent(c.getPositionName(), k -> new ArrayList<>()).add(c);
        }

        JPanel resultsPanel = new JPanel();
        resultsPanel.setLayout(new BoxLayout(resultsPanel, BoxLayout.Y_AXIS));
        resultsPanel.setBackground(Color.WHITE);

        for (Map.Entry<String, List<Candidate>> entry : byPosition.entrySet()) {
            resultsPanel.add(buildPositionResult(election, entry.getKey(), entry.getValue()));
            resultsPanel.add(Box.createVerticalStrut(8));
        }

        card.add(title, BorderLayout.NORTH);
        card.add(resultsPanel, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildPositionResult(Election election, String position, List<Candidate> candidates) {
        JPanel panel = new JPanel(new GridLayout(0, 1, 4, 4));
        panel.setBackground(new Color(248, 252, 255));
        panel.setBorder(BorderFactory.createTitledBorder(position));

        if (candidates.isEmpty()) return panel;

        Map<Integer, Integer> votes = votingService.getResults(
            election.getElectionId(), candidates.get(0).getPositionId());

        candidates.sort((a, b) ->
            votes.getOrDefault(b.getApplicationId(), 0) -
            votes.getOrDefault(a.getApplicationId(), 0));

        int totalVotes = votes.values().stream().mapToInt(Integer::intValue).sum();
        boolean first = true;

        for (Candidate c : candidates) {
            int count = votes.getOrDefault(c.getApplicationId(), 0);
            int pct   = totalVotes == 0 ? 0 : (count * 100 / totalVotes);
            String prefix = first ? "🥇 WINNER: " : "   ";
            JLabel label = new JLabel(prefix + c.getStudentName() + " — " + count + " votes (" + pct + "%)");
            label.setFont(first ? Constants.FONT_BUTTON : Constants.FONT_BODY);
            label.setForeground(first ? Constants.COLOR_SUCCESS : Constants.COLOR_TEXT);
            panel.add(label);
            first = false;
        }

        return panel;
    }
}
