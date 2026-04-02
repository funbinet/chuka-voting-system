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

        JButton bulkCreateBtn = new JButton("🚀 Add All Elections");
        bulkCreateBtn.setFont(Constants.FONT_BUTTON);
        bulkCreateBtn.setBackground(Constants.COLOR_PRIMARY);
        bulkCreateBtn.setForeground(Color.WHITE);
        bulkCreateBtn.setFocusPainted(false);
        bulkCreateBtn.setBorderPainted(false);
        bulkCreateBtn.addActionListener(e -> showBulkCreateDialog());

        JButton editBtn = new JButton("Edit");
        editBtn.setFont(Constants.FONT_BUTTON);
        editBtn.setBackground(Constants.COLOR_SECONDARY);
        editBtn.setForeground(Color.WHITE);
        editBtn.setFocusPainted(false);
        editBtn.setBorderPainted(false);
        editBtn.setEnabled(false);
        editBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row != -1) {
                int electionId = (int) tableModel.getValueAt(row, 0);
                showEditDialog(electionId);
            }
        });

        JButton deleteBtn = new JButton("Delete");
        deleteBtn.setFont(Constants.FONT_BUTTON);
        deleteBtn.setBackground(Constants.COLOR_DANGER);
        deleteBtn.setForeground(Color.WHITE);
        deleteBtn.setFocusPainted(false);
        deleteBtn.setBorderPainted(false);
        deleteBtn.setEnabled(false);
        deleteBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row != -1) {
                int electionId = (int) tableModel.getValueAt(row, 0);
                String title = (String) tableModel.getValueAt(row, 1);
                int confirm = JOptionPane.showConfirmDialog(this, 
                    "Are you sure you want to permanently delete election '" + title + "'?\nAll associated candidates and votes will be cleared permanently.", 
                    "Confirm Deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (confirm == JOptionPane.YES_OPTION) {
                    String result = electionService.deleteElection(electionId, admin.getAdminId());
                    JOptionPane.showMessageDialog(this, result, result.contains("failed") ? "Error" : "Success", result.contains("failed") ? JOptionPane.ERROR_MESSAGE : JOptionPane.INFORMATION_MESSAGE);
                    loadElections();
                }
            }
        });

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.setBackground(Constants.COLOR_BG);
        btnPanel.add(editBtn);
        btnPanel.add(deleteBtn);
        btnPanel.add(createBtn);
        btnPanel.add(bulkCreateBtn);

        topBar.add(heading, BorderLayout.WEST);
        topBar.add(btnPanel, BorderLayout.EAST);

        String[] cols = {"ID", "Title", "Faculty", "Position", "Start Date", "End Date", "Status"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(tableModel);
        table.setFont(Constants.FONT_BODY);
        table.setRowHeight(35);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setFont(Constants.FONT_BUTTON);
        table.getTableHeader().setBackground(Constants.COLOR_PRIMARY);
        table.getTableHeader().setForeground(Color.WHITE);

        table.getSelectionModel().addListSelectionListener(e -> {
            boolean selected = table.getSelectedRow() != -1;
            editBtn.setEnabled(selected);
            deleteBtn.setEnabled(selected);
        });

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
                    election.getPositionName(),
                    election.getStartDate(),
                    election.getEndDate(),
                    election.getStatus()
            });
        }
    }

    private void showCreateDialog() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Create Position-Specific Election", true);
        dialog.setSize(480, 520);
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

        JComboBox<PositionItem> positionCombo = new JComboBox<>();
        positionCombo.setFont(Constants.FONT_BODY);
        loadPositionsIntoCombo(positionCombo);

        JComboBox<Faculty> facultyCombo = new JComboBox<>();
        facultyCombo.setFont(Constants.FONT_BODY);
        loadFacultiesIntoCombo(facultyCombo);

        positionCombo.addActionListener(e -> {
            PositionItem selected = (PositionItem) positionCombo.getSelectedItem();
            if (selected != null) {
                boolean requiresFaculty = selected.getName().toLowerCase().contains("faculty");
                facultyCombo.setEnabled(requiresFaculty);
                if (!requiresFaculty) {
                    facultyCombo.setSelectedIndex(-1);
                }
                if (titleField.getText().trim().isEmpty() || titleField.getText().contains("Election")) {
                    titleField.setText(selected.getName() + " Election");
                }
            }
        });

        LocalDateTime tomorrow = LocalDateTime.now().plusDays(1).withHour(8).withMinute(0).withSecond(0);
        LocalDateTime dayAfter = tomorrow.plusDays(1).withHour(18).withMinute(0).withSecond(0);

        JTextField startField = new JTextField(tomorrow.format(DATE_FORMATTER));
        startField.setFont(Constants.FONT_BODY);
        startField.setPreferredSize(new Dimension(0, 38));

        JTextField endField = new JTextField(dayAfter.format(DATE_FORMATTER));
        endField.setFont(Constants.FONT_BODY);
        endField.setPreferredSize(new Dimension(0, 38));

        gbc.gridy = 0; panel.add(makeLabel("Election Title:"), gbc);
        gbc.gridy = 1; panel.add(titleField, gbc);
        gbc.gridy = 2; panel.add(makeLabel("Select Position:"), gbc);
        gbc.gridy = 3; panel.add(positionCombo, gbc);
        gbc.gridy = 4; panel.add(makeLabel("Faculty (if applicable):"), gbc);
        gbc.gridy = 5; panel.add(facultyCombo, gbc);
        gbc.gridy = 6; panel.add(makeLabel("Start Date (yyyy-MM-dd HH:mm:ss):"), gbc);
        gbc.gridy = 7; panel.add(startField, gbc);
        gbc.gridy = 8; panel.add(makeLabel("End Date (yyyy-MM-dd HH:mm:ss):"), gbc);
        gbc.gridy = 9; panel.add(endField, gbc);

        gbc.gridy = 10;
        gbc.insets = new Insets(15, 0, 0, 0);
        JButton createBtn = new JButton("CREATE MODULAR ELECTION");
        createBtn.setFont(Constants.FONT_BUTTON);
        createBtn.setBackground(Constants.COLOR_PRIMARY);
        createBtn.setForeground(Color.WHITE);
        createBtn.setPreferredSize(new Dimension(0, 42));
        createBtn.addActionListener(e -> {
            PositionItem position = (PositionItem) positionCombo.getSelectedItem();
            Faculty faculty = (Faculty) facultyCombo.getSelectedItem();
            
            if (position == null) {
                JOptionPane.showMessageDialog(dialog, "Please select a position.", "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            Integer fId = (facultyCombo.isEnabled() && faculty != null) ? faculty.getFacultyId() : null;

            Timestamp startDate;
            Timestamp endDate;
            try {
                startDate = Timestamp.valueOf(LocalDateTime.parse(startField.getText().trim(), DATE_FORMATTER));
                endDate = Timestamp.valueOf(LocalDateTime.parse(endField.getText().trim(), DATE_FORMATTER));
            } catch (DateTimeParseException ex) {
                JOptionPane.showMessageDialog(dialog, "Invalid date format.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String result = electionService.createElection(
                    titleField.getText().trim(),
                    fId,
                    position.getId(),
                    startDate,
                    endDate,
                    admin.getAdminId()
            );
            
            JOptionPane.showMessageDialog(dialog, result);
            if (result.toLowerCase().contains("successfully")) {
                loadElections();
                dialog.dispose();
            }
        });
        panel.add(createBtn, gbc);

        dialog.add(panel);
        dialog.setVisible(true);
    }

    private void showBulkCreateDialog() {
        String validationError = electionService.validateAllPositionsFilled();
        if (validationError != null) {
            JOptionPane.showMessageDialog(this, 
                "Bulk creation blocked:\n" + validationError + "\n\nPlease ensure every position has at least one approved candidate first.", 
                "Action Blocked", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Bulk Create All Standard Elections", true);
        dialog.setSize(450, 400);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Constants.COLOR_BG);
        panel.setBorder(new EmptyBorder(20, 30, 20, 30));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.insets = new Insets(10, 0, 10, 0);

        JLabel infoLabel = new JLabel("<html><b>Ready to create all 19 standard elections.</b><br>Please set the universal start and end date:</html>");
        infoLabel.setFont(Constants.FONT_BODY);

        LocalDateTime tomorrow = LocalDateTime.now().plusDays(1).withHour(8).withMinute(0).withSecond(0);
        LocalDateTime dayAfter = tomorrow.plusDays(2).withHour(18).withMinute(0).withSecond(0);

        JTextField startField = new JTextField(tomorrow.format(DATE_FORMATTER));
        startField.setFont(Constants.FONT_BODY);
        startField.setPreferredSize(new Dimension(0, 38));

        JTextField endField = new JTextField(dayAfter.format(DATE_FORMATTER));
        endField.setFont(Constants.FONT_BODY);
        endField.setPreferredSize(new Dimension(0, 38));

        gbc.gridy = 0; panel.add(infoLabel, gbc);
        gbc.gridy = 1; panel.add(makeLabel("Universal Start Date:"), gbc);
        gbc.gridy = 2; panel.add(startField, gbc);
        gbc.gridy = 3; panel.add(makeLabel("Universal End Date:"), gbc);
        gbc.gridy = 4; panel.add(endField, gbc);

        gbc.gridy = 5;
        gbc.insets = new Insets(20, 0, 0, 0);
        JButton createBtn = new JButton("INITIATE BULK CREATION (19 ELECTIONS)");
        createBtn.setFont(Constants.FONT_BUTTON);
        createBtn.setBackground(Constants.COLOR_SUCCESS);
        createBtn.setForeground(Color.WHITE);
        createBtn.setPreferredSize(new Dimension(0, 45));
        createBtn.addActionListener(e -> {
            Timestamp startDate;
            Timestamp endDate;
            try {
                startDate = Timestamp.valueOf(LocalDateTime.parse(startField.getText().trim(), DATE_FORMATTER));
                endDate = Timestamp.valueOf(LocalDateTime.parse(endField.getText().trim(), DATE_FORMATTER));
            } catch (DateTimeParseException ex) {
                JOptionPane.showMessageDialog(dialog, "Invalid date format.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (!endDate.after(startDate)) {
                JOptionPane.showMessageDialog(dialog, "End date must be after start date.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(dialog, 
                "This will generate 19 elections spanning all faculties and generic roles.\nProceed?", 
                "Confirm Bulk Action", JOptionPane.YES_NO_OPTION);
            
            if (confirm == JOptionPane.YES_OPTION) {
                int count = electionService.createAllStandardElections(startDate, endDate, admin.getAdminId());
                JOptionPane.showMessageDialog(dialog, "Successfully created " + count + " elections.");
                loadElections();
                dialog.dispose();
            }
        });
        panel.add(createBtn, gbc);

        dialog.add(panel);
        dialog.setVisible(true);
    }

    private void loadPositionsIntoCombo(JComboBox<PositionItem> combo) {
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM positions WHERE faculty_id IS NULL ORDER BY position_id DESC")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                combo.addItem(new PositionItem(
                    rs.getInt("position_id"),
                    rs.getString("position_name"),
                    0
                ));
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private Faculty findFacultyById(int id, JComboBox<Faculty> combo) {
        for (int i = 0; i < combo.getItemCount(); i++) {
            Faculty f = combo.getItemAt(i);
            if (f.getFacultyId() == id) return f;
        }
        return null;
    }

    private static class PositionItem {
        private final int id;
        private final String name;
        private final int facultyId;
        public PositionItem(int id, String name, int facultyId) {
            this.id = id; this.name = name; this.facultyId = facultyId;
        }
        public int getId() { return id; }
        public String getName() { return name; }
        public int getFacultyId() { return facultyId; }
        public boolean isGeneral() { return facultyId == 0; }
        @Override public String toString() { return name; }
    }

    private void showEditDialog(int electionId) {
        Election election = electionService.getElectionById(electionId);
        if (election == null) return;

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Edit Election", true);
        dialog.setSize(450, 350);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Constants.COLOR_BG);
        panel.setBorder(new EmptyBorder(20, 30, 20, 30));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.insets = new Insets(7, 0, 7, 0);

        JTextField titleField = new JTextField(election.getTitle());
        titleField.setFont(Constants.FONT_BODY);
        titleField.setPreferredSize(new Dimension(0, 38));

        JComboBox<PositionItem> positionCombo = new JComboBox<>();
        positionCombo.setFont(Constants.FONT_BODY);
        loadPositionsIntoCombo(positionCombo);
        selectPositionInCombo(election.getPositionId(), positionCombo);

        JComboBox<Faculty> facultyCombo = new JComboBox<>();
        facultyCombo.setFont(Constants.FONT_BODY);
        loadFacultiesIntoCombo(facultyCombo);
        selectFacultyInCombo(election.getFacultyId(), facultyCombo);

        positionCombo.addActionListener(e -> {
            PositionItem selected = (PositionItem) positionCombo.getSelectedItem();
            if (selected != null) {
                boolean isGeneral = selected.isGeneral() || selected.getName().toLowerCase().contains("resident");
                facultyCombo.setEnabled(!isGeneral);
                if (isGeneral) facultyCombo.setSelectedIndex(-1);
            }
        });

        PositionItem initiallySelected = (PositionItem) positionCombo.getSelectedItem();
        if (initiallySelected != null) {
            boolean isGeneral = initiallySelected.isGeneral() || initiallySelected.getName().toLowerCase().contains("resident");
            facultyCombo.setEnabled(!isGeneral);
        }

        JTextField startField = new JTextField(DATE_FORMATTER.format(election.getStartDate().toLocalDateTime()));
        startField.setFont(Constants.FONT_BODY);
        startField.setPreferredSize(new Dimension(0, 38));

        JTextField endField = new JTextField(DATE_FORMATTER.format(election.getEndDate().toLocalDateTime()));
        endField.setFont(Constants.FONT_BODY);
        endField.setPreferredSize(new Dimension(0, 38));

        gbc.gridy = 0; panel.add(makeLabel("Election Title:"), gbc);
        gbc.gridy = 1; panel.add(titleField, gbc);
        gbc.gridy = 2; panel.add(makeLabel("Position:"), gbc);
        gbc.gridy = 3; panel.add(positionCombo, gbc);
        gbc.gridy = 4; panel.add(makeLabel("Faculty (if applicable):"), gbc);
        gbc.gridy = 5; panel.add(facultyCombo, gbc);
        gbc.gridy = 6; panel.add(makeLabel("Start Date:"), gbc);
        gbc.gridy = 7; panel.add(startField, gbc);
        gbc.gridy = 8; panel.add(makeLabel("End Date:"), gbc);
        gbc.gridy = 9; panel.add(endField, gbc);

        gbc.gridy = 10;
        gbc.insets = new Insets(15, 0, 0, 0);
        JButton updateBtn = new JButton("UPDATE ELECTION");
        updateBtn.setFont(Constants.FONT_BUTTON);
        updateBtn.setBackground(Constants.COLOR_PRIMARY);
        updateBtn.setForeground(Color.WHITE);
        updateBtn.setPreferredSize(new Dimension(0, 42));
        updateBtn.addActionListener(e -> {
            PositionItem pos = (PositionItem) positionCombo.getSelectedItem();
            Faculty fac = (Faculty) facultyCombo.getSelectedItem();
            
            Timestamp startDate;
            Timestamp endDate;
            try {
                startDate = Timestamp.valueOf(LocalDateTime.parse(startField.getText().trim(), DATE_FORMATTER));
                endDate = Timestamp.valueOf(LocalDateTime.parse(endField.getText().trim(), DATE_FORMATTER));
            } catch (DateTimeParseException ex) {
                JOptionPane.showMessageDialog(dialog, "Invalid date format.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            Integer fId = (facultyCombo.isEnabled() && fac != null) ? fac.getFacultyId() : null;

            String result = electionService.updateElection(electionId, titleField.getText().trim(), fId, pos.getId(), startDate, endDate, admin.getAdminId());
            JOptionPane.showMessageDialog(dialog, result);
            if (result.toLowerCase().contains("successfully")) {
                loadElections();
                dialog.dispose();
            }
        });
        panel.add(updateBtn, gbc);

        dialog.add(panel);
        dialog.setVisible(true);
    }

    private void selectPositionInCombo(int id, JComboBox<PositionItem> combo) {
        for (int i = 0; i < combo.getItemCount(); i++) {
            if (combo.getItemAt(i).getId() == id) {
                combo.setSelectedIndex(i);
                return;
            }
        }
    }

    private void selectFacultyInCombo(int id, JComboBox<Faculty> combo) {
        for (int i = 0; i < combo.getItemCount(); i++) {
            if (combo.getItemAt(i).getFacultyId() == id) {
                combo.setSelectedIndex(i);
                return;
            }
        }
    }

    private void loadFacultiesIntoCombo(JComboBox<Faculty> combo) {
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM faculties ORDER BY faculty_name")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                combo.addItem(new Faculty(
                        rs.getInt("faculty_id"),
                        rs.getString("faculty_code"),
                        rs.getString("faculty_name")
                ));
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private JLabel makeLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Constants.FONT_SMALL);
        return label;
    }
}