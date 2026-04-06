package main.ui.student;

import main.dao.CoalitionDAO;
import main.dao.DBConnection;
import main.models.Coalition;
import main.models.Position;
import main.models.Student;
import main.services.CandidateService;
import main.utils.Constants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class ApplyForCandidacyDialog extends JDialog {

    private final Student student;
    private final CandidateService candidateService;
    private final CoalitionDAO coalitionDAO;

    private JComboBox<Position> positionCombo;
    private JTextArea manifestoArea;
    private JComboBox<Coalition> coalitionCombo;
    private JCheckBox independentCheck;

    public ApplyForCandidacyDialog(Frame parent, Student student) {
        super(parent, "Apply for Candidacy", true);
        this.student = student;
        this.candidateService = new CandidateService();
        this.coalitionDAO = new CoalitionDAO();
        setSize(500, 460);
        setLocationRelativeTo(parent);
        buildUI();
    }

    private void buildUI() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Constants.COLOR_BG);
        panel.setBorder(new EmptyBorder(20, 30, 20, 30));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.insets = new Insets(8, 0, 8, 0);

        int row = 0;
        gbc.gridy = row++;
        panel.add(makeLabel("Position:"), gbc);
        positionCombo = new JComboBox<>();
        positionCombo.setFont(Constants.FONT_BODY);
        gbc.gridy = row++;
        panel.add(positionCombo, gbc);

        gbc.gridy = row++;
        panel.add(makeLabel("Coalition / Party:"), gbc);
        coalitionCombo = new JComboBox<>();
        coalitionCombo.setFont(Constants.FONT_BODY);
        gbc.gridy = row++;
        panel.add(coalitionCombo, gbc);

        independentCheck = new JCheckBox("Run as Independent");
        independentCheck.setFont(Constants.FONT_BODY);
        independentCheck.setBackground(Constants.COLOR_BG);
        gbc.gridy = row++;
        panel.add(independentCheck, gbc);

        gbc.gridy = row++;
        panel.add(makeLabel("Manifesto:"), gbc);
        manifestoArea = new JTextArea(6, 20);
        manifestoArea.setLineWrap(true);
        manifestoArea.setWrapStyleWord(true);
        manifestoArea.setFont(Constants.FONT_BODY);
        gbc.gridy = row++;
        panel.add(new JScrollPane(manifestoArea), gbc);

        JButton applyButton = new JButton("SUBMIT APPLICATION");
        applyButton.setFont(Constants.FONT_BUTTON);
        applyButton.setBackground(Constants.COLOR_PRIMARY);
        applyButton.setForeground(Color.WHITE);
        applyButton.setFocusPainted(false);
        applyButton.setBorderPainted(false);
        applyButton.setPreferredSize(new Dimension(0, 42));
        gbc.gridy = row;
        gbc.insets = new Insets(15, 0, 0, 0);
        panel.add(applyButton, gbc);

        loadPositions();
        loadCoalitions();

        independentCheck.addActionListener(e -> {
            if (independentCheck.isSelected()) {
                coalitionCombo.setEnabled(false);
                coalitionCombo.setSelectedIndex(0);
            } else {
                coalitionCombo.setEnabled(true);
            }
        });

        applyButton.addActionListener(e -> handleSubmit());

        add(panel);
    }

    private void loadPositions() {
        try {
            Connection conn = DBConnection.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement(
                "SELECT p.*, f.faculty_name FROM positions p LEFT JOIN faculties f ON p.faculty_id = f.faculty_id ORDER BY p.position_name"
            );
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Position p = new Position(rs.getInt("position_id"), rs.getString("position_name"), rs.getInt("faculty_id"));
                positionCombo.addItem(p);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading positions: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadCoalitions() {
        coalitionCombo.removeAllItems();
        coalitionCombo.addItem(new Coalition(0, "Independent (No Coalition)", ""));
        List<Coalition> coalitions = coalitionDAO.getAllCoalitions();
        for (Coalition coalition : coalitions) {
            coalitionCombo.addItem(coalition);
        }
    }

    private void handleSubmit() {
        Position selectedPosition = (Position) positionCombo.getSelectedItem();
        String manifesto = manifestoArea.getText() == null ? "" : manifestoArea.getText().trim();

        if (selectedPosition == null) {
            JOptionPane.showMessageDialog(this, "Please select a position.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Coalition selectedCoalition = (Coalition) coalitionCombo.getSelectedItem();
        Integer coalitionId = null;
        if (!independentCheck.isSelected() && selectedCoalition != null && selectedCoalition.getCoalitionId() > 0) {
            coalitionId = selectedCoalition.getCoalitionId();
        }

        boolean success = candidateService.applyForCandidacyStudent(
                student.getStudentId(),
                selectedPosition.getPositionId(),
                manifesto,
                selectedPosition,
                coalitionId
        );

        if (success) {
            JOptionPane.showMessageDialog(this, candidateService.getLastMessage(), "Success", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } else {
            JOptionPane.showMessageDialog(this, candidateService.getLastMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JLabel makeLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Constants.FONT_SMALL);
        label.setForeground(Constants.COLOR_TEXT);
        return label;
    }
}
