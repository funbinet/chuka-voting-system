package main.ui.student;

import main.dao.DBConnection;
import main.models.Position;
import main.models.Student;
import main.services.CandidateService;
import main.utils.Constants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CandidateApplicationPanel extends JPanel {

    private Student          student;
    private CandidateService candidateService;
    private JComboBox<Position> positionCombo;
    private JTextArea           manifestoArea;

    public CandidateApplicationPanel(Student student) {
        this.student          = student;
        this.candidateService = new CandidateService();
        setBackground(Constants.COLOR_BG);
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(10, 10, 10, 10));
        buildUI();
    }

    private void buildUI() {
        JLabel heading = new JLabel("📋 Apply as Candidate");
        heading.setFont(Constants.FONT_HEADING);
        heading.setForeground(Constants.COLOR_PRIMARY);
        heading.setBorder(new EmptyBorder(0, 0, 15, 0));
        add(heading, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220)),
            new EmptyBorder(20, 30, 20, 30)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.insets = new Insets(8, 0, 8, 0);

        // Eligibility check
        gbc.gridy = 0;
        JPanel eligPanel = buildEligibilityPanel();
        form.add(eligPanel, gbc);

        // Position selection
        gbc.gridy = 1;
        form.add(makeLabel("Select Position:"), gbc);
        gbc.gridy = 2;
        positionCombo = new JComboBox<>();
        positionCombo.setFont(Constants.FONT_BODY);
        positionCombo.setPreferredSize(new Dimension(0, 40));
        loadPositions();
        form.add(positionCombo, gbc);

        // Manifesto
        gbc.gridy = 3;
        form.add(makeLabel("Your Manifesto (Campaign Statement):"), gbc);
        gbc.gridy = 4;
        manifestoArea = new JTextArea(6, 30);
        manifestoArea.setFont(Constants.FONT_BODY);
        manifestoArea.setLineWrap(true);
        manifestoArea.setWrapStyleWord(true);
        manifestoArea.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        form.add(new JScrollPane(manifestoArea), gbc);

        // Submit button
        gbc.gridy = 5;
        gbc.insets = new Insets(20, 0, 8, 0);
        JButton submitBtn = new JButton("SUBMIT APPLICATION");
        submitBtn.setFont(Constants.FONT_BUTTON);
        submitBtn.setBackground(Constants.COLOR_PRIMARY);
        submitBtn.setForeground(Color.WHITE);
        submitBtn.setFocusPainted(false);
        submitBtn.setBorderPainted(false);
        submitBtn.setPreferredSize(new Dimension(0, 45));
        submitBtn.setEnabled(student.isEligibleForCandidacy());
        submitBtn.addActionListener(e -> handleSubmit());
        form.add(submitBtn, gbc);

        add(new JScrollPane(form), BorderLayout.CENTER);
    }

    private JPanel buildEligibilityPanel() {
        JPanel panel = new JPanel(new GridLayout(0, 1, 4, 4));
        panel.setBackground(new Color(240, 248, 255));
        panel.setBorder(BorderFactory.createTitledBorder("Eligibility Status"));

        boolean gpaOk    = student.getGpa() >= Constants.MIN_GPA;
        boolean yearOk   = student.getYearOfStudy() >= Constants.MIN_YEAR_OF_STUDY;
        boolean discOk   = !student.isHasDisciplineCase();
        boolean verifOk  = student.isVerified();

        panel.add(makeCheckRow(gpaOk, "GPA ≥ " + Constants.MIN_GPA + " (Yours: " + student.getGpa() + ")"));
        panel.add(makeCheckRow(yearOk, "Year ≥ " + Constants.MIN_YEAR_OF_STUDY + " (Yours: Year " + student.getYearOfStudy() + ")"));
        panel.add(makeCheckRow(discOk, "No disciplinary case on record"));
        panel.add(makeCheckRow(verifOk, "Phone number verified"));

        return panel;
    }

    private JLabel makeCheckRow(boolean pass, String text) {
        JLabel label = new JLabel((pass ? "✅ " : "❌ ") + text);
        label.setFont(Constants.FONT_SMALL);
        label.setForeground(pass ? Constants.COLOR_SUCCESS : Constants.COLOR_DANGER);
        return label;
    }

    private void handleSubmit() {
        Position position = (Position) positionCombo.getSelectedItem();
        String manifesto  = manifestoArea.getText().trim();

        if (position == null) { showError("Please select a position."); return; }
        if (manifesto.isEmpty()) { showError("Please write your manifesto."); return; }
        if (manifesto.length() < 50) { showError("Manifesto should be at least 50 characters."); return; }

        int confirm = JOptionPane.showConfirmDialog(this,
            "Apply for: " + position.getPositionName() + "?\nThis cannot be changed after submission.",
            "Confirm Application", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            String result = candidateService.applyForCandidacy(student, position.getPositionId(), manifesto);
            JOptionPane.showMessageDialog(this, result);
        }
    }

    private void loadPositions() {
        try {
            Connection conn = DBConnection.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM positions WHERE faculty_id = ?");
            ps.setInt(1, student.getFacultyId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                positionCombo.addItem(new Position(
                    rs.getInt("position_id"),
                    rs.getString("position_name"),
                    rs.getInt("faculty_id")
                ));
            }
        } catch (SQLException e) {
            System.err.println("❌ Load positions error: " + e.getMessage());
        }
    }

    private JLabel makeLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Constants.FONT_BODY);
        return label;
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
