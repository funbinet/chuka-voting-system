package main.ui.admin;

import main.models.Candidate;
import main.models.Election;
import main.services.ElectionService;
import main.services.VotingService;
import main.utils.Constants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.*;
import java.util.List;

public class AdminResultsPanel extends JPanel {

    private ElectionService electionService;
    private VotingService   votingService;

    public AdminResultsPanel() {
        this.electionService = new ElectionService();
        this.votingService   = new VotingService();
        setBackground(Constants.COLOR_BG);
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(10, 10, 10, 10));
        buildUI();
    }

    private void buildUI() {
        JLabel heading = new JLabel("📊 Election Results — All Faculties");
        heading.setFont(Constants.FONT_HEADING);
        heading.setForeground(Constants.COLOR_PRIMARY);
        heading.setBorder(new EmptyBorder(0, 0, 15, 0));
        add(heading, BorderLayout.NORTH);

        electionService.syncElectionStatuses();
        List<Election> elections = electionService.getAllElections();

        if (elections.isEmpty()) {
            add(new JLabel("No elections found."), BorderLayout.CENTER);
            return;
        }

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Constants.COLOR_BG);

        for (Election election : elections) {
            panel.add(buildElectionBlock(election));
            panel.add(Box.createVerticalStrut(15));
        }

        add(new JScrollPane(panel), BorderLayout.CENTER);
    }

    private JPanel buildElectionBlock(Election election) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Constants.COLOR_PRIMARY, 2),
            new EmptyBorder(12, 12, 12, 12)
        ));

        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setBackground(Constants.COLOR_PRIMARY);
        titleBar.setBorder(new EmptyBorder(6, 10, 6, 10));

        JLabel title = new JLabel(election.getTitle() + " | " + election.getFacultyName());
        title.setFont(Constants.FONT_BUTTON);
        title.setForeground(Color.WHITE);

        JLabel statusLabel = new JLabel("[" + election.getStatus() + "]");
        statusLabel.setFont(Constants.FONT_SMALL);
        statusLabel.setForeground(Constants.COLOR_ACCENT);

        JLabel turnout = new JLabel("Voters: " + votingService.getTurnout(election.getElectionId()));
        turnout.setFont(Constants.FONT_SMALL);
        turnout.setForeground(Color.WHITE);

        titleBar.add(title, BorderLayout.WEST);
        titleBar.add(turnout, BorderLayout.CENTER);
        titleBar.add(statusLabel, BorderLayout.EAST);

        List<Candidate> candidates = votingService.getCandidatesForElection(election.getElectionId());
        Map<String, List<Candidate>> byPosition = new LinkedHashMap<>();
        for (Candidate c : candidates) {
            byPosition.computeIfAbsent(c.getPositionName(), k -> new ArrayList<>()).add(c);
        }

        JPanel resultsPanel = new JPanel(new GridLayout(0, 1, 5, 5));
        resultsPanel.setBackground(Color.WHITE);
        resultsPanel.setBorder(new EmptyBorder(10, 0, 0, 0));

        for (Map.Entry<String, List<Candidate>> entry : byPosition.entrySet()) {
            resultsPanel.add(buildPositionBlock(election, entry.getKey(), entry.getValue()));
        }

        card.add(titleBar, BorderLayout.NORTH);
        card.add(resultsPanel, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildPositionBlock(Election election, String position, List<Candidate> candidates) {
        JPanel panel = new JPanel(new GridLayout(0, 1, 3, 3));
        panel.setBackground(new Color(245, 250, 255));
        panel.setBorder(BorderFactory.createTitledBorder(position));

        Map<Integer, Integer> votes = votingService.getResults(
            election.getElectionId(), candidates.get(0).getPositionId());

        candidates.sort((a, b) ->
            votes.getOrDefault(b.getApplicationId(), 0) -
            votes.getOrDefault(a.getApplicationId(), 0));

        int total = votes.values().stream().mapToInt(Integer::intValue).sum();
        boolean first = true;

        for (Candidate c : candidates) {
            int count = votes.getOrDefault(c.getApplicationId(), 0);
            int pct   = total == 0 ? 0 : count * 100 / total;
            String prefix = first ? "🥇 " : "   ";
            JLabel lbl = new JLabel(prefix + c.getStudentName() + " — " + count + " votes (" + pct + "%)");
            lbl.setFont(first ? Constants.FONT_BUTTON : Constants.FONT_BODY);
            lbl.setForeground(first ? Constants.COLOR_SUCCESS : Constants.COLOR_TEXT);
            panel.add(lbl);
            first = false;
        }

        return panel;
    }
}
