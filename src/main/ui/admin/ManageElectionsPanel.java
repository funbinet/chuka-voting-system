package main.ui.admin;

import main.dao.DBConnection;
import main.models.Admin;
import main.models.Election;
import main.models.Faculty;
import main.services.ElectionService;
import main.utils.Constants;
import main.utils.PositionRules;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ManageElectionsPanel extends JPanel {

    private final Admin admin;
    private final ElectionService electionService;
    private JTable table;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private JButton editBtn;
    private JButton deleteBtn;
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
        JPanel topBar = new JPanel();
        topBar.setLayout(new BoxLayout(topBar, BoxLayout.Y_AXIS));
        topBar.setBackground(Constants.COLOR_BG);
        topBar.setBorder(new EmptyBorder(0, 0, 12, 0));

        JLabel heading = new JLabel("Manage Elections");
        heading.setFont(Constants.FONT_HEADING);
        heading.setForeground(Constants.COLOR_PRIMARY);

        JPanel headingRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        headingRow.setBackground(Constants.COLOR_BG);
        headingRow.add(heading);

        searchField = new JTextField(26);
        searchField.setFont(Constants.FONT_BODY);

        JButton searchBtn = new JButton("🔍 Search");
        searchBtn.setFont(Constants.FONT_BUTTON);
        searchBtn.setBackground(Constants.COLOR_SECONDARY);
        searchBtn.setForeground(Color.WHITE);
        searchBtn.setFocusPainted(false);
        searchBtn.setBorderPainted(false);
        searchBtn.addActionListener(e -> loadElections(searchField.getText().trim()));

        JButton refreshBtn = new JButton("↺ Refresh");
        refreshBtn.setFont(Constants.FONT_BUTTON);
        refreshBtn.setBackground(Constants.COLOR_PRIMARY);
        refreshBtn.setForeground(Color.WHITE);
        refreshBtn.setFocusPainted(false);
        refreshBtn.setBorderPainted(false);
        refreshBtn.addActionListener(e -> {
            searchField.setText("");
            loadElections("");
        });

        searchField.addActionListener(e -> loadElections(searchField.getText().trim()));

        JPanel searchRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        searchRow.setBackground(Constants.COLOR_BG);
        searchRow.add(new JLabel("Search:"));
        searchRow.add(searchField);
        searchRow.add(searchBtn);
        searchRow.add(refreshBtn);

        topBar.add(headingRow);
        topBar.add(Box.createVerticalStrut(8));
        topBar.add(searchRow);

        String[] cols = {"No.", "ID", "Title", "Faculty", "Position", "Start Date", "End Date", "Status"};
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
        hideInternalIdColumn();

        editBtn = new JButton("✏️ Edit Election");
        editBtn.setFont(Constants.FONT_BUTTON);
        editBtn.setBackground(Constants.COLOR_SECONDARY);
        editBtn.setForeground(Color.WHITE);
        editBtn.setFocusPainted(false);
        editBtn.setBorderPainted(false);
        editBtn.setEnabled(false);
        editBtn.addActionListener(e -> {
            Integer electionId = getSelectedElectionId();
            if (electionId != null) {
                showEditDialog(electionId);
            }
        });

        deleteBtn = new JButton("🗑️ Delete Election");
        deleteBtn.setFont(Constants.FONT_BUTTON);
        deleteBtn.setBackground(Constants.COLOR_DANGER);
        deleteBtn.setForeground(Color.WHITE);
        deleteBtn.setFocusPainted(false);
        deleteBtn.setBorderPainted(false);
        deleteBtn.setEnabled(false);
        deleteBtn.addActionListener(e -> deleteSelectedElection());

        JButton addBtn = new JButton("➕ Add Election");
        addBtn.setFont(Constants.FONT_BUTTON);
        addBtn.setBackground(Constants.COLOR_PRIMARY);
        addBtn.setForeground(Color.WHITE);
        addBtn.setFocusPainted(false);
        addBtn.setBorderPainted(false);
        addBtn.addActionListener(e -> showCreateDialog());

        JButton importBtn = new JButton("📤 Bulk Import");
        importBtn.setFont(Constants.FONT_BUTTON);
        importBtn.setBackground(Constants.COLOR_SUCCESS);
        importBtn.setForeground(Color.WHITE);
        importBtn.setFocusPainted(false);
        importBtn.setBorderPainted(false);
        importBtn.addActionListener(e -> importElectionsFromCSV());

        JButton addAllBtn = new JButton("🚀 Add All Elections (9)");
        addAllBtn.setFont(Constants.FONT_BUTTON);
        addAllBtn.setBackground(Constants.COLOR_PRIMARY);
        addAllBtn.setForeground(Color.WHITE);
        addAllBtn.setFocusPainted(false);
        addAllBtn.setBorderPainted(false);
        addAllBtn.addActionListener(e -> showBulkCreateDialog());

        table.getSelectionModel().addListSelectionListener(e -> {
            boolean selected = table.getSelectedRow() != -1;
            editBtn.setEnabled(selected);
            deleteBtn.setEnabled(selected);
        });

        JPanel actionBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 8));
        actionBar.setBackground(Constants.COLOR_BG);
        actionBar.setBorder(new EmptyBorder(10, 0, 0, 0));
        actionBar.add(addBtn);
        actionBar.add(editBtn);
        actionBar.add(deleteBtn);
        actionBar.add(importBtn);
        actionBar.add(addAllBtn);

        loadElections("");
        add(topBar, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(actionBar, BorderLayout.SOUTH);
    }

    private void loadElections() {
        loadElections(searchField != null ? searchField.getText().trim() : "");
    }

    private void loadElections(String search) {
        tableModel.setRowCount(0);
        List<Election> elections = electionService.getAllElections();
        String term = search == null ? "" : search.trim().toLowerCase();
        int displayNo = 1;
        for (Election election : elections) {
            if (!term.isEmpty()) {
                String title = election.getTitle() == null ? "" : election.getTitle().toLowerCase();
                String faculty = election.getFacultyName() == null ? "" : election.getFacultyName().toLowerCase();
                String position = election.getPositionName() == null ? "" : election.getPositionName().toLowerCase();
                String status = election.getStatus() == null ? "" : election.getStatus().toLowerCase();
                String id = String.valueOf(election.getElectionId());
                if (!(title.contains(term) || faculty.contains(term) || position.contains(term) || status.contains(term) || id.contains(term))) {
                    continue;
                }
            }

            tableModel.addRow(new Object[]{
            displayNo++,
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

    private Integer getSelectedElectionId() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select an election first.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        int modelRow = table.convertRowIndexToModel(row);
        return (int) tableModel.getValueAt(modelRow, 1);
    }

    private void deleteSelectedElection() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select an election to delete.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int modelRow = table.convertRowIndexToModel(row);
        int electionId = (int) tableModel.getValueAt(modelRow, 1);
        String title = String.valueOf(tableModel.getValueAt(modelRow, 2));

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Delete election '" + title + "'? This will also remove linked candidates and votes.",
                "Confirm Deletion",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            String result = electionService.deleteElection(electionId, admin.getAdminId());
            JOptionPane.showMessageDialog(
                    this,
                    result,
                    result.toLowerCase().contains("failed") ? "Error" : "Success",
                    result.toLowerCase().contains("failed") ? JOptionPane.ERROR_MESSAGE : JOptionPane.INFORMATION_MESSAGE
            );
            loadElections();
        }
    }

    private void importElectionsFromCSV() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select Elections CSV File");
        if (fileChooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File file = fileChooser.getSelectedFile();
        Map<String, Integer> facultyMap = getFacultyCodeMap();
        int imported = 0;
        int errors = 0;
        StringBuilder errorLog = new StringBuilder();

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            int lineNum = 0;
            while ((line = br.readLine()) != null) {
                lineNum++;
                if (lineNum == 1 && line.toLowerCase().contains("title")) {
                    continue;
                }

                String[] data = line.split(",");
                if (data.length < 5) {
                    errors++;
                    errorLog.append("Line ").append(lineNum)
                            .append(": Expected columns title,position_id,faculty_code,start_date,end_date.\n");
                    continue;
                }

                String title = data[0].trim();
                String positionStr = data[1].trim();
                String facultyCode = data[2].trim().toUpperCase();
                String startStr = data[3].trim();
                String endStr = data[4].trim();

                if (title.isEmpty()) {
                    errors++;
                    errorLog.append("Line ").append(lineNum).append(": Title is required.\n");
                    continue;
                }

                int positionId;
                try {
                    positionId = Integer.parseInt(positionStr);
                } catch (NumberFormatException ex) {
                    errors++;
                    errorLog.append("Line ").append(lineNum).append(": Invalid position_id.\n");
                    continue;
                }

                Timestamp startDate;
                Timestamp endDate;
                try {
                    startDate = Timestamp.valueOf(LocalDateTime.parse(startStr, DATE_FORMATTER));
                    endDate = Timestamp.valueOf(LocalDateTime.parse(endStr, DATE_FORMATTER));
                } catch (DateTimeParseException ex) {
                    errors++;
                    errorLog.append("Line ").append(lineNum).append(": Invalid date format (yyyy-MM-dd HH:mm:ss).\n");
                    continue;
                }

                if (!endDate.after(startDate)) {
                    errors++;
                    errorLog.append("Line ").append(lineNum).append(": End date must be after start date.\n");
                    continue;
                }

                Integer facultyId = null;
                if (!facultyCode.isEmpty() && !"GLOBAL".equals(facultyCode) && !"ALL".equals(facultyCode)) {
                    facultyId = facultyMap.get(facultyCode);
                    if (facultyId == null) {
                        errors++;
                        errorLog.append("Line ").append(lineNum).append(": Unknown faculty code '").append(facultyCode).append("'.\n");
                        continue;
                    }
                }

                String result = electionService.createElection(title, facultyId, positionId, startDate, endDate, admin.getAdminId());
                if (result.toLowerCase().contains("successfully")) {
                    imported++;
                } else {
                    errors++;
                    errorLog.append("Line ").append(lineNum).append(": ").append(result).append("\n");
                }
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error reading CSV file: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.add(new JLabel(String.format("<html><b>Election Import Summary:</b><br>✅ Imported: %d<br>❌ Errors: %d</html>", imported, errors)), BorderLayout.NORTH);
        if (errors > 0) {
            JTextArea area = new JTextArea(errorLog.toString());
            area.setEditable(false);
            JScrollPane sp = new JScrollPane(area);
            sp.setPreferredSize(new Dimension(420, 220));
            panel.add(sp, BorderLayout.CENTER);
        }

        JOptionPane.showMessageDialog(this, panel, "CSV Import Results", JOptionPane.INFORMATION_MESSAGE);
        loadElections();
    }

    private Map<String, Integer> getFacultyCodeMap() {
        Map<String, Integer> map = new HashMap<>();
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT faculty_id, faculty_code FROM faculties")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                map.put(rs.getString("faculty_code").toUpperCase(), rs.getInt("faculty_id"));
            }
        } catch (SQLException e) {
            System.err.println("❌ Faculty map load error: " + e.getMessage());
        }
        return map;
    }

    private void hideInternalIdColumn() {
        table.getColumnModel().getColumn(1).setMinWidth(0);
        table.getColumnModel().getColumn(1).setMaxWidth(0);
        table.getColumnModel().getColumn(1).setPreferredWidth(0);
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

        JLabel infoLabel = new JLabel("<html><b>Ready to create all 9 standard elections.</b><br>Please set the universal start and end date:</html>");
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
        JButton createBtn = new JButton("INITIATE BULK CREATION (9 ELECTIONS)");
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
                "This will generate 9 elections spanning faculty and university-wide roles.\nProceed?", 
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
        combo.removeAllItems();
        Map<PositionRules.PositionCategory, PositionItem> canonicalPositions =
                new EnumMap<>(PositionRules.PositionCategory.class);

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT position_id, position_name, faculty_id FROM positions ORDER BY position_id ASC")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                PositionRules.PositionCategory category = PositionRules.classify(rs.getString("position_name"));
                if (!PositionRules.isCanonical(category)) {
                    continue;
                }

                int facultyId = rs.getObject("faculty_id") == null ? 0 : rs.getInt("faculty_id");
                PositionItem candidate = new PositionItem(
                    rs.getInt("position_id"),
                    PositionRules.canonicalLabel(category),
                    facultyId
                );

                PositionItem existing = canonicalPositions.get(category);
                if (shouldPreferPosition(candidate, existing)) {
                    canonicalPositions.put(category, candidate);
                }
            }

            for (PositionRules.PositionCategory category : PositionRules.CANONICAL_ORDER) {
                PositionItem option = canonicalPositions.get(category);
                if (option != null) {
                    combo.addItem(option);
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private boolean shouldPreferPosition(PositionItem candidate, PositionItem existing) {
        if (existing == null) {
            return true;
        }
        if (candidate.getFacultyId() == 0 && existing.getFacultyId() > 0) {
            return true;
        }
        if (candidate.getFacultyId() > 0 && existing.getFacultyId() == 0) {
            return false;
        }
        return candidate.getId() < existing.getId();
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