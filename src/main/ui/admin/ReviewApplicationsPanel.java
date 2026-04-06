package main.ui.admin;

import main.dao.CoalitionDAO;
import main.dao.CandidateDAO;
import main.dao.DBConnection;
import main.models.Admin;
import main.models.Candidate;
import main.models.Coalition;
import main.models.Faculty;
import main.services.CandidateService;
import main.utils.Constants;
import main.utils.PositionRules;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class ReviewApplicationsPanel extends JPanel {

    private final Admin admin;
    private final CandidateService candidateService;
    private final CandidateDAO candidateDAO;
    private JTable table;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private JButton editBtn;
    private JButton deleteBtn;

    public ReviewApplicationsPanel(Admin admin) {
        this.admin = admin;
        this.candidateService = new CandidateService();
        this.candidateDAO = new CandidateDAO();
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

        JLabel heading = new JLabel("📝 Manage Candidates");
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
        searchBtn.addActionListener(e -> loadCandidates(searchField.getText().trim()));

        JButton refreshBtn = new JButton("↺ Refresh");
        refreshBtn.setFont(Constants.FONT_BUTTON);
        refreshBtn.setBackground(Constants.COLOR_PRIMARY);
        refreshBtn.setForeground(Color.WHITE);
        refreshBtn.setFocusPainted(false);
        refreshBtn.setBorderPainted(false);
        refreshBtn.addActionListener(e -> {
            searchField.setText("");
            loadCandidates("");
        });

        searchField.addActionListener(e -> loadCandidates(searchField.getText().trim()));

        JPanel searchRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        searchRow.setBackground(Constants.COLOR_BG);
        searchRow.add(new JLabel("Search:"));
        searchRow.add(searchField);
        searchRow.add(searchBtn);
        searchRow.add(refreshBtn);

        topBar.add(headingRow);
        topBar.add(Box.createVerticalStrut(8));
        topBar.add(searchRow);

        String[] cols = {"ID", "Name", "Reg Number", "Faculty", "Position", "Coalition", "Status", "Action"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 7; // Only the action column is interactive
            }
        };

        table = new JTable(tableModel);
        table.setFont(Constants.FONT_BODY);
        table.setRowHeight(35);
        table.getTableHeader().setFont(Constants.FONT_BUTTON);
        table.getTableHeader().setBackground(Constants.COLOR_PRIMARY);
        table.getTableHeader().setForeground(Color.WHITE);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JButton addBtn = new JButton("➕ Add Candidate");
        addBtn.setFont(Constants.FONT_BUTTON);
        addBtn.setBackground(Constants.COLOR_PRIMARY);
        addBtn.setForeground(Color.WHITE);
        addBtn.setFocusPainted(false);
        addBtn.setBorderPainted(false);
        addBtn.addActionListener(e -> showAddCandidateDialog());

        editBtn = new JButton("✏️ Edit Candidate");
        editBtn.setFont(Constants.FONT_BUTTON);
        editBtn.setBackground(Constants.COLOR_SECONDARY);
        editBtn.setForeground(Color.WHITE);
        editBtn.setFocusPainted(false);
        editBtn.setBorderPainted(false);
        editBtn.setEnabled(false);
        editBtn.addActionListener(e -> showEditCandidateDialog());

        deleteBtn = new JButton("🗑️ Delete Candidate");
        deleteBtn.setFont(Constants.FONT_BUTTON);
        deleteBtn.setBackground(Constants.COLOR_DANGER);
        deleteBtn.setForeground(Color.WHITE);
        deleteBtn.setFocusPainted(false);
        deleteBtn.setBorderPainted(false);
        deleteBtn.setEnabled(false);
        deleteBtn.addActionListener(e -> removeSelectedCandidate());

        JButton importBtn = new JButton("📤 Bulk Import");
        importBtn.setFont(Constants.FONT_BUTTON);
        importBtn.setBackground(Constants.COLOR_SUCCESS);
        importBtn.setForeground(Color.WHITE);
        importBtn.setFocusPainted(false);
        importBtn.setBorderPainted(false);
        importBtn.addActionListener(e -> importCandidatesFromCSV());

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

        loadCandidates("");
        add(topBar, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(actionBar, BorderLayout.SOUTH);
    }

    private void loadCandidates() {
        loadCandidates(searchField != null ? searchField.getText().trim() : "");
    }

    private void loadCandidates(String search) {
        tableModel.setRowCount(0);
        String s = search.toLowerCase();

        List<Candidate> pending = candidateService.getPendingApplications();
        for (Candidate c : pending) {
            if (s.isEmpty() || c.getStudentName().toLowerCase().contains(s) || c.getRegNumber().toLowerCase().contains(s)) {
                tableModel.addRow(new Object[]{
                        c.getApplicationId(), c.getStudentName(), c.getRegNumber(),
                        c.getFacultyName(), c.getPositionName(), c.getCoalitionName(), c.getStatus(), "APPROVE"
                });
            }
        }
        
        List<Candidate> approved = candidateService.getAllApprovedCandidates();
        for (Candidate c : approved) {
            if (s.isEmpty() || c.getStudentName().toLowerCase().contains(s) || c.getRegNumber().toLowerCase().contains(s)) {
                tableModel.addRow(new Object[]{
                        c.getApplicationId(), c.getStudentName(), c.getRegNumber(),
                        c.getFacultyName(), c.getPositionName(), c.getCoalitionName(), c.getStatus(), "REMOVE"
                });
            }
        }
        
        table.getColumn("Action").setCellRenderer(new ButtonRenderer());
        table.getColumn("Action").setCellEditor(new ButtonEditor(new JCheckBox()));
    }

    private Integer getSelectedApplicationId() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a candidate first.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return (int) tableModel.getValueAt(row, 0);
    }

    private void removeSelectedCandidate() {
        Integer appId = getSelectedApplicationId();
        if (appId == null) {
            return;
        }

        int row = table.getSelectedRow();
        String name = String.valueOf(tableModel.getValueAt(row, 1));
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Delete candidate '" + name + "' from candidacy?",
                "Confirm Deletion",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            boolean ok = candidateService.removeCandidate(appId, admin.getAdminId());
            JOptionPane.showMessageDialog(
                    this,
                    ok ? "Candidate removed successfully." : "Failed to remove candidate.",
                    ok ? "Success" : "Error",
                    ok ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE
            );
            loadCandidates();
        }
    }

    private void showEditCandidateDialog() {
        Integer appId = getSelectedApplicationId();
        if (appId == null) {
            return;
        }

        Candidate existing = candidateDAO.findById(appId);
        if (existing == null) {
            JOptionPane.showMessageDialog(this, "Could not load selected candidate application.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Edit Candidate", true);
        dialog.setSize(520, 520);
        dialog.setMinimumSize(new Dimension(480, 460));
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Constants.COLOR_BG);
        panel.setBorder(new EmptyBorder(20, 30, 20, 30));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(7, 0, 7, 0);

        JLabel studentLabel = new JLabel(existing.getStudentName() + " (" + existing.getRegNumber() + ")");
        studentLabel.setFont(Constants.FONT_BODY);

        JComboBox<PositionItem> positionCombo = new JComboBox<>();
        positionCombo.setFont(Constants.FONT_BODY);
        loadPositionsIntoCombo(positionCombo);
        selectPositionInCombo(existing.getPositionId(), positionCombo);

        JComboBox<Coalition> coalitionCombo = new JComboBox<>();
        coalitionCombo.setFont(Constants.FONT_BODY);
        coalitionCombo.addItem(new Coalition(0, "Independent (No Coalition)", ""));
        for (Coalition coalition : new CoalitionDAO().getAllCoalitions()) {
            coalitionCombo.addItem(coalition);
        }
        if (existing.getCoalitionId() != null) {
            for (int i = 0; i < coalitionCombo.getItemCount(); i++) {
                Coalition c = coalitionCombo.getItemAt(i);
                if (c.getCoalitionId() == existing.getCoalitionId()) {
                    coalitionCombo.setSelectedIndex(i);
                    break;
                }
            }
        } else {
            coalitionCombo.setSelectedIndex(0);
        }

        JTextArea manifestoArea = new JTextArea(existing.getManifesto() == null ? "" : existing.getManifesto(), 6, 20);
        manifestoArea.setLineWrap(true);
        manifestoArea.setWrapStyleWord(true);
        manifestoArea.setFont(Constants.FONT_BODY);
        JScrollPane scroll = new JScrollPane(manifestoArea);

        int row = 0;
        gbc.gridy = row++; panel.add(makeLabel("Student:"), gbc);
        gbc.gridy = row++; panel.add(studentLabel, gbc);
        gbc.gridy = row++; panel.add(makeLabel("Position:"), gbc);
        gbc.gridy = row++; panel.add(positionCombo, gbc);
        gbc.gridy = row++; panel.add(makeLabel("Coalition / Party:"), gbc);
        gbc.gridy = row++; panel.add(coalitionCombo, gbc);
        gbc.gridy = row++; panel.add(makeLabel("Manifesto:"), gbc);
        gbc.gridy = row++; panel.add(scroll, gbc);

        JButton saveBtn = new JButton("SAVE CHANGES");
        saveBtn.setFont(Constants.FONT_BUTTON);
        saveBtn.setBackground(Constants.COLOR_PRIMARY);
        saveBtn.setForeground(Color.WHITE);
        saveBtn.setFocusPainted(false);
        saveBtn.setBorderPainted(false);
        saveBtn.setPreferredSize(new Dimension(0, 42));
        saveBtn.addActionListener(e -> {
            PositionItem selectedPosition = (PositionItem) positionCombo.getSelectedItem();
            if (selectedPosition == null) {
                JOptionPane.showMessageDialog(dialog, "Please select a position.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }

            Coalition selectedCoal = (Coalition) coalitionCombo.getSelectedItem();
            Integer coalitionId = (selectedCoal != null && selectedCoal.getCoalitionId() > 0) ? selectedCoal.getCoalitionId() : null;

            String result = candidateService.updateCandidateApplication(
                    appId,
                    selectedPosition.id,
                    manifestoArea.getText(),
                    coalitionId,
                    admin.getAdminId()
            );

            JOptionPane.showMessageDialog(dialog, result);
            if (result.toLowerCase().contains("successfully")) {
                loadCandidates();
                dialog.dispose();
            }
        });

        gbc.gridy = row;
        gbc.insets = new Insets(15, 0, 0, 0);
        panel.add(saveBtn, gbc);

        dialog.add(panel);
        dialog.setVisible(true);
    }

    private void selectPositionInCombo(int positionId, JComboBox<PositionItem> combo) {
        for (int i = 0; i < combo.getItemCount(); i++) {
            PositionItem item = combo.getItemAt(i);
            if (item.id == positionId) {
                combo.setSelectedIndex(i);
                return;
            }
        }
    }

    private void importCandidatesFromCSV() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select Candidates CSV File");
        if (fileChooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        java.io.File file = fileChooser.getSelectedFile();
        int imported = 0, errors = 0;
        StringBuilder errorLog = new StringBuilder();

        try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(file))) {
            String line;
            int lineNum = 0;
            while ((line = br.readLine()) != null) {
                lineNum++;
                if (lineNum == 1 && line.toLowerCase().contains("reg")) continue;

                String[] data = line.split(",");
                if (data.length < 2) {
                    errors++;
                    errorLog.append("Line ").append(lineNum).append(": Insufficient columns.\n");
                    continue;
                }

                String reg = data[0].trim().toUpperCase();
                String posIdStr = data[1].trim();
                String manifesto = data.length > 2 ? data[2].trim() : "";

                try {
                    int posId = Integer.parseInt(posIdStr);
                    String result = candidateService.addCandidateDirectly(reg, posId, manifesto, admin.getAdminId(), null);
                    if (result.toLowerCase().contains("successfully")) {
                        imported++;
                    } else {
                        errors++;
                        errorLog.append("Line ").append(lineNum).append(": ").append(result).append("\n");
                    }
                } catch (NumberFormatException ex) {
                    errors++;
                    errorLog.append("Line ").append(lineNum).append(": Invalid Position ID.\n");
                }
            }

            JPanel panel = new JPanel(new BorderLayout(5, 5));
            panel.add(new JLabel(String.format("<html><b>Candidate Import Summary:</b><br>✅ Imported: %d<br>❌ Errors: %d</html>", imported, errors)), BorderLayout.NORTH);

            if (errors > 0 || errorLog.length() > 0) {
                JTextArea area = new JTextArea(errorLog.toString());
                area.setEditable(false);
                JScrollPane sp = new JScrollPane(area);
                sp.setPreferredSize(new Dimension(400, 200));
                panel.add(sp, BorderLayout.CENTER);
            }

            JOptionPane.showMessageDialog(this, panel, "CSV Import Results", JOptionPane.INFORMATION_MESSAGE);
            loadCandidates("");

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error reading CSV file: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showAddCandidateDialog() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Add Direct Candidate", true);
        dialog.setSize(450, 450);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Constants.COLOR_BG);
        panel.setBorder(new EmptyBorder(20, 30, 20, 30));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.insets = new Insets(7, 0, 7, 0);

        JTextField regField = new JTextField();
        regField.setFont(Constants.FONT_BODY);
        regField.setPreferredSize(new Dimension(0, 38));

        JComboBox<PositionItem> positionCombo = new JComboBox<>();
        positionCombo.setFont(Constants.FONT_BODY);
        loadPositionsIntoCombo(positionCombo);

        JComboBox<Faculty> facultyCombo = new JComboBox<>();
        facultyCombo.setFont(Constants.FONT_BODY);
        loadFacultiesIntoCombo(facultyCombo);
        facultyCombo.setEnabled(false); // Default disabled until position selected

        positionCombo.addActionListener(e -> {
            PositionItem selected = (PositionItem) positionCombo.getSelectedItem();
            if (selected != null) {
                boolean isFaculty = PositionRules.classify(selected.name) == PositionRules.PositionCategory.FACULTY_CHAIRMAN;
                facultyCombo.setEnabled(isFaculty);
                if (!isFaculty) {
                    facultyCombo.setSelectedIndex(-1);
                }
            }
        });

        if (positionCombo.getItemCount() > 0) {
            positionCombo.setSelectedIndex(0);
        }

        // Coalition ComboBox
        JComboBox<Coalition> coalitionCombo = new JComboBox<>();
        coalitionCombo.setFont(Constants.FONT_BODY);
        coalitionCombo.addItem(new Coalition(0, "Independent (No Coalition)", ""));
        for (Coalition coal : new CoalitionDAO().getAllCoalitions()) {
            coalitionCombo.addItem(coal);
        }

        JTextArea manifestoArea = new JTextArea(4, 20);
        manifestoArea.setLineWrap(true);
        manifestoArea.setWrapStyleWord(true);
        JScrollPane scroll = new JScrollPane(manifestoArea);

        int row = 0;
        gbc.gridy = row++; panel.add(makeLabel("Student Registration Number:"), gbc);
        gbc.gridy = row++; panel.add(regField, gbc);
        gbc.gridy = row++; panel.add(makeLabel("Select Position:"), gbc);
        gbc.gridy = row++; panel.add(positionCombo, gbc);
        gbc.gridy = row++; panel.add(makeLabel("Select Faculty (If Faculty Role):"), gbc);
        gbc.gridy = row++; panel.add(facultyCombo, gbc);
        gbc.gridy = row++; panel.add(makeLabel("Coalition / Party:"), gbc);
        gbc.gridy = row++; panel.add(coalitionCombo, gbc);
        gbc.gridy = row++; panel.add(makeLabel("Manifesto (Optional):"), gbc);
        gbc.gridy = row++; panel.add(scroll, gbc);

        JButton saveBtn = new JButton("ADD AS CANDIDATE");
        saveBtn.setFont(Constants.FONT_BUTTON);
        saveBtn.setBackground(Constants.COLOR_PRIMARY);
        saveBtn.setForeground(Color.WHITE);
        saveBtn.setPreferredSize(new Dimension(0, 42));
        saveBtn.addActionListener(e -> {
            String reg = regField.getText().trim().toUpperCase();
            PositionItem pos = (PositionItem) positionCombo.getSelectedItem();
            if (reg.isEmpty() || pos == null) {
                JOptionPane.showMessageDialog(dialog, "Registration number and position are required.");
                return;
            }
            
            Faculty fac = (Faculty) facultyCombo.getSelectedItem();
            if (facultyCombo.isEnabled() && fac == null) {
                JOptionPane.showMessageDialog(dialog, "Please select a faculty for this position.");
                return;
            }

            Coalition selectedCoal = (Coalition) coalitionCombo.getSelectedItem();
            Integer coalId = (selectedCoal != null && selectedCoal.getCoalitionId() > 0) ? selectedCoal.getCoalitionId() : null;
            String msg = candidateService.addCandidateDirectly(reg, pos.id, manifestoArea.getText(), admin.getAdminId(), coalId);
            JOptionPane.showMessageDialog(dialog, msg);
            if (msg.contains("successfully")) {
                loadCandidates();
                dialog.dispose();
            }
        });

        gbc.gridy = row++;
        gbc.insets = new Insets(15, 0, 0, 0);
        panel.add(saveBtn, gbc);

        dialog.add(panel);
        dialog.setVisible(true);
    }

    private void loadFacultiesIntoCombo(JComboBox<Faculty> combo) {
        try {
            Connection conn = DBConnection.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement("SELECT * FROM faculties ORDER BY faculty_name");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                combo.addItem(new Faculty(rs.getInt("faculty_id"), rs.getString("faculty_code"), rs.getString("faculty_name")));
            }
        } catch (SQLException e) {}
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
        } catch (SQLException e) {}
    }

    private boolean shouldPreferPosition(PositionItem candidate, PositionItem existing) {
        if (existing == null) {
            return true;
        }
        if (candidate.facultyId == 0 && existing.facultyId > 0) {
            return true;
        }
        if (candidate.facultyId > 0 && existing.facultyId == 0) {
            return false;
        }
        return candidate.id < existing.id;
    }

    private class PositionItem {
        int id;
        String name;
        int facultyId;
        PositionItem(int id, String name, int facultyId) {
            this.id = id;
            this.name = name;
            this.facultyId = facultyId;
        }
        @Override public String toString() { return name; }
    }

    private JLabel makeLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Constants.FONT_SMALL);
        label.setBorder(new EmptyBorder(5, 0, 0, 0));
        return label;
    }

    // Action button handlers
    class ButtonRenderer extends JButton implements javax.swing.table.TableCellRenderer {
        public ButtonRenderer() {
            setOpaque(true);
            setFont(Constants.FONT_SMALL);
            setBackground(Constants.COLOR_DANGER);
            setForeground(Color.WHITE);
        }
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (value != null) {
                setText(value.toString());
                if (value.toString().equals("APPROVE")) {
                    setBackground(Constants.COLOR_SUCCESS);
                } else {
                    setBackground(Constants.COLOR_DANGER);
                }
            } else {
                setText("");
            }
            return this;
        }
    }

    class ButtonEditor extends DefaultCellEditor {
        protected JButton button;
        private String label;
        private boolean isPushed;
        private int row;

        public ButtonEditor(JCheckBox checkBox) {
            super(checkBox);
            button = new JButton();
            button.setOpaque(true);
            button.addActionListener(e -> fireEditingStopped());
        }

        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            label = (value == null) ? "" : value.toString();
            button.setText(label);
            this.row = row;
            isPushed = true;
            return button;
        }

        public Object getCellEditorValue() {
            if (isPushed) {
                int appId = (int) tableModel.getValueAt(row, 0);
                String name = (String) tableModel.getValueAt(row, 1);
                
                if (label.equals("APPROVE")) {
                    int confirm = JOptionPane.showConfirmDialog(button, "Approve " + name + " for candidacy?", "Approve Candidate", JOptionPane.YES_NO_OPTION);
                    if (confirm == JOptionPane.YES_OPTION) {
                        main.dao.CandidateDAO cDAO = new main.dao.CandidateDAO();
                        Candidate c = cDAO.findById(appId);
                        if (c != null) {
                            String msg = candidateService.approveStudentApplication(appId, admin.getAdminId(), c.getFacultyId());
                            JOptionPane.showMessageDialog(button, msg);
                        }
                        loadCandidates();
                    }
                } else {
                    int confirm = JOptionPane.showConfirmDialog(button, "Are you sure you want to remove " + name + " from candidacy?", "Remove Candidate", JOptionPane.YES_NO_OPTION);
                    if (confirm == JOptionPane.YES_OPTION) {
                        candidateService.removeCandidate(appId, admin.getAdminId());
                        loadCandidates();
                    }
                }
            }
            isPushed = false;
            return label;
        }

        public boolean stopCellEditing() {
            isPushed = false;
            return super.stopCellEditing();
        }
    }
}