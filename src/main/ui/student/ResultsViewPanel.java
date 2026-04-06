package main.ui.student;

import main.models.Candidate;
import main.models.Election;
import main.models.Student;
import main.services.ElectionService;
import main.services.VotingService;
import main.utils.Constants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
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
        JLabel heading = new JLabel("📊 Final Election Results — " + student.getFacultyName());
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
        JPanel card = new JPanel(new BorderLayout(0, 10));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Constants.COLOR_PRIMARY, 2),
            new EmptyBorder(15, 15, 15, 15)
        ));

        JLabel title = new JLabel("🏆 " + election.getTitle() + " — OFFICIAL STANDINGS");
        title.setFont(Constants.FONT_HEADING);
        title.setForeground(Constants.COLOR_PRIMARY);

        card.add(title, BorderLayout.NORTH);
        card.add(buildElectionTable(election), BorderLayout.CENTER);
        return card;
    }

    private JScrollPane buildElectionTable(Election election) {
        String[] columns = {"Position", "Candidate Name", "Coalition", "Total Votes", "Vote %", "Rank"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        List<Candidate> candidates = votingService.getCandidatesForElection(election.getElectionId());
        Map<String, List<Candidate>> byPosition = new LinkedHashMap<>();
        for (Candidate c : candidates) {
            byPosition.computeIfAbsent(c.getPositionName(), k -> new ArrayList<>()).add(c);
        }

        for (Map.Entry<String, List<Candidate>> entry : byPosition.entrySet()) {
            String position = entry.getKey();
            List<Candidate> sortedCands = entry.getValue();
            Map<Integer, Integer> votes = votingService.getResults(election.getElectionId(), sortedCands.get(0).getPositionId());
            sortedCands.sort((a, b) -> votes.getOrDefault(b.getApplicationId(), 0) - votes.getOrDefault(a.getApplicationId(), 0));

            int totalVotes = votes.values().stream().mapToInt(Integer::intValue).sum();
            int rank = 1;

            for (Candidate c : sortedCands) {
                int count = votes.getOrDefault(c.getApplicationId(), 0);
                int pct = totalVotes == 0 ? 0 : (count * 100 / totalVotes);
                model.addRow(new Object[]{ position, c.getStudentName(), c.getCoalitionName(), count, pct + "%", rank });
                rank++;
            }
        }

        JTable table = new JTable(model);
        table.setFont(Constants.FONT_BODY);
        table.setRowHeight(38);
        table.getTableHeader().setFont(Constants.FONT_BUTTON);
        table.getTableHeader().setBackground(Constants.COLOR_PRIMARY);
        table.getTableHeader().setForeground(Color.WHITE);
        table.setFillsViewportHeight(true);

        // Highlight Winners
        DefaultTableCellRenderer customRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, col);
                int rankVal = (int) t.getModel().getValueAt(t.convertRowIndexToModel(row), 5);
                if (rankVal == 1) {
                    c.setFont(new Font("Segoe UI", Font.BOLD, 14));
                    c.setForeground(new Color(40, 167, 69)); // Success Emerald Green
                } else {
                    c.setFont(Constants.FONT_BODY);
                    c.setForeground(Constants.COLOR_TEXT);
                }
                
                if (col == 5) {
                    ((JLabel) c).setHorizontalAlignment(JLabel.CENTER);
                } else {
                    ((JLabel) c).setHorizontalAlignment(JLabel.LEFT);
                }
                return c;
            }
        };

        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(customRenderer);
        }

        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(800, Math.min(300, table.getRowHeight() * (model.getRowCount() + 2))));
        return scroll;
    }
}
