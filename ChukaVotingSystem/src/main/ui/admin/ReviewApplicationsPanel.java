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

    public ReviewApplicationsPanel(Admin admin) {
        this.admin = admin;
        this.candidateService = new CandidateService();
        setBackground(Constants.COLOR_BG);
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(10, 10, 10, 10));
        buildUI();
    }

    private void buildUI() {
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(Constants.COLOR_BG);
        topBar.setBorder(new EmptyBorder(0, 0, 15, 0));

        JLabel heading = new JLabel("Manage Candidates");
        heading.setFont(Constants.FONT_HEADING);
        heading.setForeground(Constants.COLOR_PRIMARY);

        JButton addBtn = new JButton("Add Candidate");
        addBtn.setFont(Constants.FONT_BUTTON);
        addBtn.setBackground(Constants.COLOR_SUCCESS);
        addBtn.setForeground(Color.WHITE);
        addBtn.setFocusPainted(false);
        addBtn.setBorderPainted(false);
        addBtn.addActionListener(e -> showAddCandidateDialog());

        topBar.add(heading, BorderLayout.WEST);
        topBar.add(addBtn, BorderLayout.EAST);

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

        loadApprovedCandidates();
        add(topBar, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    private void loadApprovedCandidates() {
        tableModel.setRowCount(0);
        List<Candidate> candidates = candidateService.getAllApprovedCandidates();
        for (Candidate c : candidates) {
            tableModel.addRow(new Object[]{
                    c.getApplicationId(),
                    c.getStudentName(),
                    c.getRegNumber(),
                    c.getFacultyName(),
                    c.getPositionName(),
                    c.getStatus(),
                    "REMOVE"
            });
        }
        
        // Add button renderer and editor for "REMOVE" action
        table.getColumn("Action").setCellRenderer(new ButtonRenderer());
        table.getColumn("Action").setCellEditor(new ButtonEditor(new JCheckBox()));
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

        JComboBox<Faculty> facultyCombo = new JComboBox<>();
        facultyCombo.setFont(Constants.FONT_BODY);
        loadFacultiesIntoCombo(facultyCombo);

        JComboBox<Position> positionCombo = new JComboBox<>();
        positionCombo.setFont(Constants.FONT_BODY);

        facultyCombo.addActionListener(e -> {
            Faculty f = (Faculty) facultyCombo.getSelectedItem();
            if (f != null) loadPositionsForFaculty(positionCombo, f.getFacultyId());
        });

        // Trigger initial position load
        if (facultyCombo.getItemCount() > 0) {
            facultyCombo.setSelectedIndex(0);
        }

        JTextArea manifestoArea = new JTextArea(4, 20);
        manifestoArea.setLineWrap(true);
        manifestoArea.setWrapStyleWord(true);
        JScrollPane scroll = new JScrollPane(manifestoArea);

        int row = 0;
        gbc.gridy = row++; panel.add(makeLabel("Student Registration Number:"), gbc);
        gbc.gridy = row++; panel.add(regField, gbc);
        gbc.gridy = row++; panel.add(makeLabel("Select Faculty:"), gbc);
        gbc.gridy = row++; panel.add(facultyCombo, gbc);
        gbc.gridy = row++; panel.add(makeLabel("Select Position:"), gbc);
        gbc.gridy = row++; panel.add(positionCombo, gbc);
        gbc.gridy = row++; panel.add(makeLabel("Manifesto (Optional):"), gbc);
        gbc.gridy = row++; panel.add(scroll, gbc);

        JButton saveBtn = new JButton("ADD AS CANDIDATE");
        saveBtn.setFont(Constants.FONT_BUTTON);
        saveBtn.setBackground(Constants.COLOR_PRIMARY);
        saveBtn.setForeground(Color.WHITE);
        saveBtn.setPreferredSize(new Dimension(0, 42));
        saveBtn.addActionListener(e -> {
            String reg = regField.getText().trim().toUpperCase();
            Position pos = (Position) positionCombo.getSelectedItem();
            if (reg.isEmpty() || pos == null) {
                JOptionPane.showMessageDialog(dialog, "Registration number and position are required.");
                return;
            }
            String msg = candidateService.addCandidateDirectly(reg, pos.getPositionId(), manifestoArea.getText(), admin.getAdminId());
            JOptionPane.showMessageDialog(dialog, msg);
            if (msg.contains("successfully")) {
                loadApprovedCandidates();
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

    private void loadPositionsForFaculty(JComboBox<Position> combo, int facultyId) {
        combo.removeAllItems();
        try {
            Connection conn = DBConnection.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement("SELECT * FROM positions WHERE faculty_id = ? ORDER BY position_name");
            ps.setInt(1, facultyId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                combo.addItem(new Position(rs.getInt("position_id"), rs.getString("position_name"), rs.getInt("faculty_id")));
            }
        } catch (SQLException e) {}
    }

    private JLabel makeLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Constants.FONT_SMALL);
        return label;
    }

    // Button Renderer for the "REMOVE" action
    class ButtonRenderer extends JButton implements javax.swing.table.TableCellRenderer {
        public ButtonRenderer() {
            setOpaque(true);
            setFont(Constants.FONT_SMALL);
            setBackground(Constants.COLOR_DANGER);
            setForeground(Color.WHITE);
        }
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setText((value == null) ? "" : value.toString());
            return this;
        }
    }

    // Button Editor for the "REMOVE" action
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
                int confirm = JOptionPane.showConfirmDialog(button, "Are you sure you want to remove " + name + " from candidacy?");
                if (confirm == JOptionPane.YES_OPTION) {
                    candidateService.removeCandidate(appId, admin.getAdminId());
                    loadApprovedCandidates();
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