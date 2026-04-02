package main.ui.admin;

import main.dao.DBConnection;
import main.models.Admin;
import main.models.Candidate;
import main.models.Faculty;
import main.models.Position;
import main.services.CandidateService;
import main.utils.Constants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class ReviewApplicationsPanel extends JPanel {

    private final Admin admin;
    private final CandidateService candidateService;
    private JTable table;
    private DefaultTableModel tableModel;
    private JTextField searchField;

    public ReviewApplicationsPanel(Admin admin) {
        this.admin = admin;
        this.candidateService = new CandidateService();
        setBackground(Constants.COLOR_BG);
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(10, 10, 10, 10));
        buildUI();
    }

    private void buildUI() {
        JPanel topBar = new JPanel(new GridBagLayout());
        topBar.setBackground(Constants.COLOR_BG);
        topBar.setBorder(new EmptyBorder(10, 15, 25, 15));

        GridBagConstraints gbcTop = new GridBagConstraints();
        gbcTop.fill = GridBagConstraints.HORIZONTAL;
        gbcTop.weightx = 1.0;
        gbcTop.gridx = 0;

        // Row 1: Title
        JLabel heading = new JLabel("📝 Manage Candidates");
        heading.setFont(Constants.FONT_HEADING);
        heading.setForeground(Constants.COLOR_PRIMARY);
        gbcTop.gridy = 0;
        gbcTop.insets = new Insets(0, 0, 15, 0);
        topBar.add(heading, gbcTop);

        // Row 2: Action Bar (Buttons & Search)
        JPanel searchBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        searchBar.setBackground(Constants.COLOR_BG);

        JButton importBtn = new JButton("📤 Bulk Import");
        importBtn.setFont(Constants.FONT_BUTTON);
        importBtn.setBackground(Constants.COLOR_SUCCESS);
        importBtn.setForeground(Color.WHITE);
        importBtn.addActionListener(e -> importCandidatesFromCSV());

        searchField = new JTextField(15);
        searchField.setFont(Constants.FONT_BODY);
        
        JButton searchBtn = new JButton("🔍 Search");
        searchBtn.setFont(Constants.FONT_BUTTON);
        searchBtn.setBackground(Constants.COLOR_SECONDARY);
        searchBtn.setForeground(Color.WHITE);
        searchBtn.addActionListener(e -> loadCandidates(searchField.getText().trim()));

        JButton refreshBtn = new JButton("↺ Refresh");
        refreshBtn.setFont(Constants.FONT_BUTTON);
        refreshBtn.setBackground(Constants.COLOR_PRIMARY);
        refreshBtn.setForeground(Color.WHITE);
        refreshBtn.addActionListener(e -> {
            searchField.setText("");
            loadCandidates("");
        });

        JButton addBtn = new JButton("Direct Add");
        addBtn.setFont(Constants.FONT_BUTTON);
        addBtn.addActionListener(e -> showAddCandidateDialog());

        searchBar.add(importBtn);
        searchBar.add(new JLabel(" Search: "));
        searchBar.add(searchField);
        searchBar.add(searchBtn);
        searchBar.add(refreshBtn);
        searchBar.add(addBtn);

        gbcTop.gridy = 1;
        gbcTop.insets = new Insets(0, 0, 0, 0);
        topBar.add(searchBar, gbcTop);

        String[] cols = {"ID", "Name", "Reg Number", "Faculty", "Position", "Status", "Action"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 6; // Only the action column is interactive
            }
        };

        table = new JTable(tableModel);
        table.setFont(Constants.FONT_BODY);
        table.setRowHeight(35);
        table.getTableHeader().setFont(Constants.FONT_BUTTON);
        table.getTableHeader().setBackground(Constants.COLOR_PRIMARY);
        table.getTableHeader().setForeground(Color.WHITE);

        table.getTableHeader().setForeground(Color.WHITE);

        loadCandidates();
        add(topBar, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
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
                        c.getFacultyName(), c.getPositionName(), c.getStatus(), "APPROVE"
                });
            }
        }
        
        List<Candidate> approved = candidateService.getAllApprovedCandidates();
        for (Candidate c : approved) {
            if (s.isEmpty() || c.getStudentName().toLowerCase().contains(s) || c.getRegNumber().toLowerCase().contains(s)) {
                tableModel.addRow(new Object[]{
                        c.getApplicationId(), c.getStudentName(), c.getRegNumber(),
                        c.getFacultyName(), c.getPositionName(), c.getStatus(), "REMOVE"
                });
            }
        }
        
        table.getColumn("Action").setCellRenderer(new ButtonRenderer());
        table.getColumn("Action").setCellEditor(new ButtonEditor(new JCheckBox()));
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
                    String result = candidateService.addCandidateDirectly(reg, posId, manifesto, admin.getAdminId());
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
                boolean isFaculty = selected.name.toLowerCase().contains("faculty");
                facultyCombo.setEnabled(isFaculty);
                if (!isFaculty) {
                    facultyCombo.setSelectedIndex(-1);
                }
            }
        });

        if (positionCombo.getItemCount() > 0) {
            positionCombo.setSelectedIndex(0);
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

            String msg = candidateService.addCandidateDirectly(reg, pos.id, manifestoArea.getText(), admin.getAdminId());
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
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM positions WHERE faculty_id IS NULL ORDER BY position_id DESC")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                combo.addItem(new PositionItem(rs.getInt("position_id"), rs.getString("position_name")));
            }
        } catch (SQLException e) {}
    }

    private class PositionItem {
        int id;
        String name;
        PositionItem(int id, String name) { this.id = id; this.name = name; }
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