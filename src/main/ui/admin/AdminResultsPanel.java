package main.ui.admin;

import main.models.Candidate;
import main.models.Election;
import main.services.ElectionService;
import main.services.VotingService;
import main.utils.Constants;
import main.utils.ResultsPDFExporter;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AdminResultsPanel extends JPanel {

    private ElectionService electionService;
    private VotingService   votingService;

    private JComboBox<Election> electionCombo;
    private JPanel chartsContainer;
    private JPanel summaryPanel;

    public AdminResultsPanel() {
        this.electionService = new ElectionService();
        this.votingService   = new VotingService();
        
        setBackground(Constants.COLOR_BG);
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(20, 20, 20, 20));
        
        initTopPanel();
        
        chartsContainer = new JPanel();
        chartsContainer.setLayout(new BoxLayout(chartsContainer, BoxLayout.Y_AXIS));
        chartsContainer.setBackground(Constants.COLOR_BG);
        
        JScrollPane scrollPane = new JScrollPane(chartsContainer);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        
        add(scrollPane, BorderLayout.CENTER);
        
        refreshElectionList();
    }

    private void initTopPanel() {
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.setBackground(Constants.COLOR_BG);
        topPanel.setBorder(new EmptyBorder(0, 0, 20, 0));

        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        titlePanel.setBackground(Constants.COLOR_BG);
        JLabel title = new JLabel("📊 Election Visual Analytics");
        title.setFont(Constants.FONT_TITLE);
        titlePanel.add(title);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        controls.setBackground(Constants.COLOR_BG);

        electionCombo = new JComboBox<>();
        electionCombo.setPreferredSize(new Dimension(300, 35));
        electionCombo.addActionListener(e -> loadElectionData());

        JButton exportPdfBtn = new JButton("📄 Export PDF");
        exportPdfBtn.setFont(Constants.FONT_BUTTON);
        exportPdfBtn.setBackground(Constants.COLOR_SECONDARY);
        exportPdfBtn.setForeground(Color.WHITE);
        exportPdfBtn.addActionListener(e -> handleExportPDF());

        JButton refreshBtn = new JButton("🔄 Refresh Data");
        refreshBtn.setFont(Constants.FONT_BUTTON);
        refreshBtn.addActionListener(e -> refreshElectionList());

        controls.add(new JLabel("Select Election: "));
        controls.add(electionCombo);
        controls.add(exportPdfBtn);
        controls.add(refreshBtn);

        topPanel.add(titlePanel);
        topPanel.add(Box.createVerticalStrut(10));
        topPanel.add(controls);
        
        add(topPanel, BorderLayout.NORTH);
    }

    private void handleExportPDF() {
        Election election = (Election) electionCombo.getSelectedItem();
        if (election == null) return;

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Election Results PDF");
        fileChooser.setSelectedFile(new File(election.getTitle().replace(" ", "_") + "_Results.pdf"));

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                List<Candidate> candidates = votingService.getCandidatesForElection(election.getElectionId());
                Map<Integer, Integer> voteData = new HashMap<>();
                
                // Aggregate all results for the PDF
                for (Candidate c : candidates) {
                    Map<Integer, Integer> posResults = votingService.getResults(election.getElectionId(), c.getPositionId());
                    voteData.putAll(posResults);
                }

                int totalEligible = votingService.getEligibleVoterCount(election);
                int totalVotesCast = votingService.getTurnout(election.getElectionId());

                ResultsPDFExporter.exportElectionResults(election, candidates, voteData, totalEligible, totalVotesCast, file);
                
                JOptionPane.showMessageDialog(this, "✅ PDF Exported Successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "❌ Failed to export PDF: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        }
    }

    private void refreshElectionList() {
        electionService.syncElectionStatuses();
        List<Election> elections = electionService.getAllElections();
        
        electionCombo.removeAllItems();
        for (Election e : elections) {
            if (!Constants.STATUS_UPCOMING.equals(e.getStatus())) {
                electionCombo.addItem(e);
            }
        }
        
        if (electionCombo.getItemCount() > 0) {
            loadElectionData();
        } else {
            chartsContainer.removeAll();
            chartsContainer.add(new JLabel("No active or closed elections found."));
            chartsContainer.revalidate();
            chartsContainer.repaint();
        }
    }

    private void loadElectionData() {
        Election election = (Election) electionCombo.getSelectedItem();
        if (election == null) return;

        chartsContainer.removeAll();
        
        // 1. Voter Turnout Summary (Pie Chart)
        chartsContainer.add(createTurnoutPanel(election));
        chartsContainer.add(Box.createVerticalStrut(20));

        // 2. Position-wise Results (Bar Charts)
        List<Candidate> candidates = votingService.getCandidatesForElection(election.getElectionId());
        Map<String, List<Candidate>> byPosition = new LinkedHashMap<>();
        for (Candidate c : candidates) {
            byPosition.computeIfAbsent(c.getPositionName(), k -> new ArrayList<>()).add(c);
        }

        for (Map.Entry<String, List<Candidate>> entry : byPosition.entrySet()) {
            chartsContainer.add(createPositionChart(election, entry.getKey(), entry.getValue()));
            chartsContainer.add(Box.createVerticalStrut(20));
        }

        chartsContainer.revalidate();
        chartsContainer.repaint();
    }

    private JPanel createTurnoutPanel(Election election) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createTitledBorder("Voter Turnout Analysis"));

        int votedCount = votingService.getTurnout(election.getElectionId());
        int totalEligible = votingService.getEligibleVoterCount(election);
        int nonVoters = Math.max(0, totalEligible - votedCount);

        DefaultPieDataset dataset = new DefaultPieDataset();
        dataset.setValue("Voted (" + votedCount + ")", votedCount);
        dataset.setValue("Did Not Vote (" + nonVoters + ")", nonVoters);

        JFreeChart chart = ChartFactory.createPieChart(
                "Overall Turnout for " + election.getTitle(),
                dataset, true, true, false);

        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(0, 300));
        panel.add(chartPanel, BorderLayout.CENTER);

        double turnoutPercent = totalEligible > 0 ? (votedCount * 100.0 / totalEligible) : 0;
        JLabel stats = new JLabel(String.format("<html><b>Total Eligible:</b> %d | <b>Voted:</b> %d | <b>Turnout:</b> %.1f%%</html>", 
            totalEligible, votedCount, turnoutPercent), JLabel.CENTER);
        stats.setFont(Constants.FONT_BODY);
        stats.setForeground(Constants.COLOR_PRIMARY);
        stats.setBorder(new EmptyBorder(10, 0, 15, 0));
        panel.add(stats, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createPositionChart(Election election, String positionName, List<Candidate> candidates) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createTitledBorder("Results for: " + positionName));

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        Map<Integer, Integer> results = votingService.getResults(election.getElectionId(), candidates.get(0).getPositionId());
        
        int totalVotesForPosition = results.values().stream().mapToInt(Integer::intValue).sum();

        for (Candidate c : candidates) {
            int votes = results.getOrDefault(c.getApplicationId(), 0);
            String label = c.getStudentName() + " (" + c.getCoalitionName() + ")";
            dataset.addValue(votes, "Votes", label);
        }

        JFreeChart barChart = ChartFactory.createBarChart(
                positionName + " Statistics",
                "Candidate",
                "Number of Votes",
                dataset,
                PlotOrientation.HORIZONTAL,
                false, true, false);

        // Styling and Labels
        BarRenderer renderer = (BarRenderer) barChart.getCategoryPlot().getRenderer();
        renderer.setBaseItemLabelGenerator(new StandardCategoryItemLabelGenerator());
        renderer.setBaseItemLabelsVisible(true);
        renderer.setSeriesPaint(0, Constants.COLOR_PRIMARY);

        ChartPanel chartPanel = new ChartPanel(barChart);
        chartPanel.setPreferredSize(new Dimension(0, Math.max(250, candidates.size() * 50)));
        panel.add(chartPanel, BorderLayout.CENTER);

        return panel;
    }
}
