package main.ui.admin;

import main.dao.DBConnection;
import main.models.Admin;
import main.models.Election;
import main.models.Faculty;
import main.services.ElectionService;
import main.utils.Constants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

public class ManageElectionsPanel extends JPanel {

    private final Admin admin;
    private final ElectionService electionService;
    private JTable table;
    private DefaultTableModel tableModel;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public ManageElectionsPanel(Admin admin) {
        this.admin = admin;
        this.electionService = new ElectionService();
        setBackground(Constants.COLOR_BG);
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(10, 10, 10, 10));
        buildUI();
    }

    private void buildUI() {
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(Constants.COLOR_BG);
        topBar.setBorder(new EmptyBorder(0, 0, 10, 0));

        JLabel heading = new JLabel("Manage Elections");
        heading.setFont(Constants.FONT_HEADING);
        heading.setForeground(Constants.COLOR_PRIMARY);

        JButton createBtn = new JButton("Create Election");
        createBtn.setFont(Constants.FONT_BUTTON);
        createBtn.setBackground(Constants.COLOR_SUCCESS);
        createBtn.setForeground(Color.WHITE);
        createBtn.setFocusPainted(false);
        createBtn.setBorderPainted(false);
        createBtn.addActionListener(e -> showCreateDialog());

        topBar.add(heading, BorderLayout.WEST);
        topBar.add(createBtn, BorderLayout.EAST);

        String[] cols = {"ID", "Title", "Faculty", "Start Date", "End Date", "Status"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(tableModel);
        table.setFont(Constants.FONT_BODY);
        table.setRowHeight(35);
        table.getTableHeader().setFont(Constants.FONT_BUTTON);
        table.getTableHeader().setBackground(Constants.COLOR_PRIMARY);
        table.getTableHeader().setForeground(Color.WHITE);

        loadElections();
        add(topBar, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    private void loadElections() {
        tableModel.setRowCount(0);
        List<Election> elections = electionService.getAllElections();
        for (Election election : elections) {
            tableModel.addRow(new Object[]{
                    election.getElectionId(),
                    election.getTitle(),
                    election.getFacultyName(),
                    election.getStartDate(),
                    election.getEndDate(),
                    election.getStatus()
            });
        }
    }

    private void showCreateDialog() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Create Election", true);
        dialog.setSize(450, 420);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Constants.COLOR_BG);
        panel.setBorder(new EmptyBorder(20, 30, 20, 30));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.insets = new Insets(7, 0, 7, 0);

        JTextField titleField = new JTextField();
        titleField.setFont(Constants.FONT_BODY);
        titleField.setPreferredSize(new Dimension(0, 38));

        JComboBox<Faculty> facultyCombo = new JComboBox<>();
        facultyCombo.setFont(Constants.FONT_BODY);
        loadFacultiesIntoCombo(facultyCombo);

        LocalDateTime tomorrow = LocalDateTime.now().plusDays(1).withHour(8).withMinute(0).withSecond(0);
        LocalDateTime dayAfter = tomorrow.plusDays(1).withHour(18).withMinute(0).withSecond(0);

        JTextField startField = new JTextField(tomorrow.format(DATE_FORMATTER));
        startField.setFont(Constants.FONT_BODY);
        startField.setPreferredSize(new Dimension(0, 38));

        JTextField endField = new JTextField(dayAfter.format(DATE_FORMATTER));
        endField.setFont(Constants.FONT_BODY);
        endField.setPreferredSize(new Dimension(0, 38));

        gbc.gridy = 0;
        panel.add(makeLabel("Election Title:"), gbc);
        gbc.gridy = 1;
        panel.add(titleField, gbc);
        gbc.gridy = 2;
        panel.add(makeLabel("Faculty:"), gbc);
        gbc.gridy = 3;
        panel.add(facultyCombo, gbc);
        gbc.gridy = 4;
        panel.add(makeLabel("Start Date (yyyy-MM-dd HH:mm:ss):"), gbc);
        gbc.gridy = 5;
        panel.add(startField, gbc);
        gbc.gridy = 6;
        panel.add(makeLabel("End Date (yyyy-MM-dd HH:mm:ss):"), gbc);
        gbc.gridy = 7;
        panel.add(endField, gbc);

        gbc.gridy = 8;
        gbc.insets = new Insets(15, 0, 0, 0);
        JButton createBtn = new JButton("CREATE ELECTION");
        createBtn.setFont(Constants.FONT_BUTTON);
        createBtn.setBackground(Constants.COLOR_PRIMARY);
        createBtn.setForeground(Color.WHITE);
        createBtn.setFocusPainted(false);
        createBtn.setBorderPainted(false);
        createBtn.setPreferredSize(new Dimension(0, 42));
        createBtn.addActionListener(e -> {
            Faculty faculty = (Faculty) facultyCombo.getSelectedItem();
            if (faculty == null) {
                JOptionPane.showMessageDialog(dialog, "Please select a faculty.", "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            Timestamp startDate;
            Timestamp endDate;
            try {
                startDate = Timestamp.valueOf(LocalDateTime.parse(startField.getText().trim(), DATE_FORMATTER));
                endDate = Timestamp.valueOf(LocalDateTime.parse(endField.getText().trim(), DATE_FORMATTER));
            } catch (DateTimeParseException ex) {
                JOptionPane.showMessageDialog(dialog,
                        "Please enter dates using the format yyyy-MM-dd HH:mm:ss.",
                        "Invalid Date Format", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String result = electionService.createElection(
                    titleField.getText().trim(),
                    faculty.getFacultyId(),
                    startDate,
                    endDate,
                    admin.getAdminId()
            );
            JOptionPane.showMessageDialog(dialog, result,
                    result.toLowerCase().contains("successfully") ? "Election Created" : "Election Error",
                    result.toLowerCase().contains("successfully") ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE);
            if (result.toLowerCase().contains("successfully")) {
                loadElections();
                dialog.dispose();
            }
        });
        panel.add(createBtn, gbc);

        dialog.add(panel);
        dialog.setVisible(true);
    }

    private void loadFacultiesIntoCombo(JComboBox<Faculty> combo) {
        try {
            Connection conn = DBConnection.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement("SELECT * FROM faculties ORDER BY faculty_name");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                combo.addItem(new Faculty(
                        rs.getInt("faculty_id"),
                        rs.getString("faculty_code"),
                        rs.getString("faculty_name")
                ));
            }
        } catch (SQLException e) {
            System.err.println("❌ Load faculties error: " + e.getMessage());
        }
    }

    private JLabel makeLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Constants.FONT_SMALL);
        return label;
    }
}