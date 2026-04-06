package main.ui.admin;

import main.dao.CoalitionDAO;
import main.models.Admin;
import main.models.Coalition;
import main.utils.Constants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class ManageCoalitionsPanel extends JPanel {

    private final Admin admin;
    private final CoalitionDAO coalitionDAO;
    private JTextField searchField;
    private JTable coalitionTable;
    private DefaultTableModel tableModel;
    private JButton editBtn;
    private JButton deleteBtn;
    private final List<Coalition> visibleCoalitions = new ArrayList<>();

    public ManageCoalitionsPanel(Admin admin) {
        this.admin = admin;
        this.coalitionDAO = new CoalitionDAO();
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

        JLabel heading = new JLabel("🤝 Manage Coalitions");
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
        searchBtn.addActionListener(e -> refreshData(searchField.getText().trim()));

        JButton refreshBtn = new JButton("↺ Refresh");
        refreshBtn.setFont(Constants.FONT_BUTTON);
        refreshBtn.setBackground(Constants.COLOR_PRIMARY);
        refreshBtn.setForeground(Color.WHITE);
        refreshBtn.setFocusPainted(false);
        refreshBtn.setBorderPainted(false);
        refreshBtn.addActionListener(e -> {
            searchField.setText("");
            refreshData("");
        });

        searchField.addActionListener(e -> refreshData(searchField.getText().trim()));

        JPanel searchRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        searchRow.setBackground(Constants.COLOR_BG);
        searchRow.add(new JLabel("Search:"));
        searchRow.add(searchField);
        searchRow.add(searchBtn);
        searchRow.add(refreshBtn);

        topBar.add(headingRow);
        topBar.add(Box.createVerticalStrut(8));
        topBar.add(searchRow);

        String[] columns = {"ID", "Name", "Motto", "Created At"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        coalitionTable = new JTable(tableModel);
        coalitionTable.setFont(Constants.FONT_BODY);
        coalitionTable.setRowHeight(35);
        coalitionTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        coalitionTable.getTableHeader().setFont(Constants.FONT_BUTTON);
        coalitionTable.getTableHeader().setBackground(Constants.COLOR_PRIMARY);
        coalitionTable.getTableHeader().setForeground(Color.WHITE);

        JButton addBtn = new JButton("➕ Add Coalition");
        addBtn.setFont(Constants.FONT_BUTTON);
        addBtn.setBackground(Constants.COLOR_PRIMARY);
        addBtn.setForeground(Color.WHITE);
        addBtn.setFocusPainted(false);
        addBtn.setBorderPainted(false);
        addBtn.addActionListener(e -> showCoalitionDialog(null));

        editBtn = new JButton("✏️ Edit Coalition");
        editBtn.setFont(Constants.FONT_BUTTON);
        editBtn.setBackground(Constants.COLOR_SECONDARY);
        editBtn.setForeground(Color.WHITE);
        editBtn.setFocusPainted(false);
        editBtn.setBorderPainted(false);
        editBtn.setEnabled(false);
        editBtn.addActionListener(e -> editSelectedCoalition());

        deleteBtn = new JButton("🗑️ Delete Coalition");
        deleteBtn.setFont(Constants.FONT_BUTTON);
        deleteBtn.setBackground(Constants.COLOR_DANGER);
        deleteBtn.setForeground(Color.WHITE);
        deleteBtn.setFocusPainted(false);
        deleteBtn.setBorderPainted(false);
        deleteBtn.setEnabled(false);
        deleteBtn.addActionListener(e -> deleteSelectedCoalition());

        JButton importBtn = new JButton("📤 Bulk Import");
        importBtn.setFont(Constants.FONT_BUTTON);
        importBtn.setBackground(Constants.COLOR_SUCCESS);
        importBtn.setForeground(Color.WHITE);
        importBtn.setFocusPainted(false);
        importBtn.setBorderPainted(false);
        importBtn.addActionListener(e -> importCoalitionsFromCSV());

        coalitionTable.getSelectionModel().addListSelectionListener(e -> {
            boolean selected = coalitionTable.getSelectedRow() != -1;
            editBtn.setEnabled(selected);
            deleteBtn.setEnabled(selected);
        });

        coalitionTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && coalitionTable.getSelectedRow() >= 0) {
                    editSelectedCoalition();
                }
            }
        });

        JScrollPane scroll = new JScrollPane(coalitionTable);
        scroll.setBorder(BorderFactory.createEmptyBorder());

        JPanel actionBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 8));
        actionBar.setBackground(Constants.COLOR_BG);
        actionBar.setBorder(new EmptyBorder(10, 0, 0, 0));
        actionBar.add(addBtn);
        actionBar.add(editBtn);
        actionBar.add(deleteBtn);
        actionBar.add(importBtn);

        add(topBar, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        add(actionBar, BorderLayout.SOUTH);

        refreshData("");
    }

    private void refreshData() {
        refreshData(searchField != null ? searchField.getText().trim() : "");
    }

    private void refreshData(String search) {
        tableModel.setRowCount(0);
        visibleCoalitions.clear();
        String term = search == null ? "" : search.trim().toLowerCase();
        List<Coalition> list = coalitionDAO.getAllCoalitions();
        int displayId = 1;
        for (Coalition c : list) {
            if (!term.isEmpty()) {
                String name = c.getName() == null ? "" : c.getName().toLowerCase();
                String motto = c.getMotto() == null ? "" : c.getMotto().toLowerCase();
                if (!name.contains(term) && !motto.contains(term)) {
                    continue;
                }
            }

            visibleCoalitions.add(c);

            tableModel.addRow(new Object[]{
                    displayId++,
                    c.getName(),
                    c.getMotto(),
                    c.getCreatedAt().toString()
            });
        }
    }

    private Coalition getSelectedCoalition() {
        int row = coalitionTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a coalition first.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return null;
        }

        int modelRow = coalitionTable.convertRowIndexToModel(row);
        if (modelRow < 0 || modelRow >= visibleCoalitions.size()) {
            JOptionPane.showMessageDialog(this, "Unable to resolve the selected coalition. Please refresh and try again.", "Selection Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }

        Coalition selected = visibleCoalitions.get(modelRow);
        Coalition coalition = coalitionDAO.findById(selected.getCoalitionId());
        return coalition != null ? coalition : selected;
    }

    private void editSelectedCoalition() {
        Coalition coalition = getSelectedCoalition();
        if (coalition != null) {
            showCoalitionDialog(coalition);
        }
    }

    private void deleteSelectedCoalition() {
        Coalition coalition = getSelectedCoalition();
        if (coalition == null) {
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Delete coalition '" + coalition.getName() + "'?",
                "Confirm Deletion",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            boolean ok = coalitionDAO.deleteCoalition(coalition.getCoalitionId());
            JOptionPane.showMessageDialog(
                    this,
                    ok ? "Coalition deleted successfully." : "Failed to delete coalition. It may still be referenced by candidate applications.",
                    ok ? "Success" : "Error",
                    ok ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE
            );
            refreshData();
        }
    }

    private void importCoalitionsFromCSV() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select Coalitions CSV File");
        if (fileChooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File file = fileChooser.getSelectedFile();
        int imported = 0;
        int skipped = 0;
        int errors = 0;
        StringBuilder errorLog = new StringBuilder();

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            int lineNum = 0;

            while ((line = br.readLine()) != null) {
                lineNum++;
                if (lineNum == 1 && line.toLowerCase().contains("name")) {
                    continue;
                }

                String[] data = line.split(",");
                if (data.length < 1) {
                    errors++;
                    errorLog.append("Line ").append(lineNum).append(": Missing coalition name.\n");
                    continue;
                }

                String name = data[0].trim();
                String motto = data.length > 1 ? data[1].trim() : "";

                if (name.isEmpty()) {
                    errors++;
                    errorLog.append("Line ").append(lineNum).append(": Coalition name is required.\n");
                    continue;
                }

                boolean ok = coalitionDAO.createCoalition(name, motto);
                if (ok) {
                    imported++;
                } else {
                    skipped++;
                    errorLog.append("Line ").append(lineNum).append(": Could not add '").append(name)
                            .append("' (possibly duplicate).\n");
                }
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error reading CSV file: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.add(new JLabel(String.format("<html><b>Coalition Import Summary:</b><br>✅ Imported: %d<br>⏭️ Skipped: %d<br>❌ Errors: %d</html>", imported, skipped, errors)), BorderLayout.NORTH);

        if (errors > 0 || skipped > 0) {
            JTextArea area = new JTextArea(errorLog.toString());
            area.setEditable(false);
            area.setFont(new Font("Monospaced", Font.PLAIN, 12));
            JScrollPane sp = new JScrollPane(area);
            sp.setPreferredSize(new Dimension(420, 220));
            panel.add(sp, BorderLayout.CENTER);
        }

        JOptionPane.showMessageDialog(this, panel, "CSV Import Results", JOptionPane.INFORMATION_MESSAGE);
        refreshData();
    }

    private void showCoalitionDialog(Coalition existing) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), 
                existing == null ? "Create Coalition" : "Edit Coalition", true);
        dialog.setSize(480, 330);
        dialog.setMinimumSize(new Dimension(420, 300));
        dialog.setLocationRelativeTo(this);

        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(new EmptyBorder(20, 24, 20, 24));
        p.setBackground(Constants.COLOR_BG);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 0, 6, 0);
        gbc.weightx = 1.0;

        JTextField nameField = new JTextField(existing != null ? existing.getName() : "");
        nameField.setFont(Constants.FONT_BODY);
        nameField.setPreferredSize(new Dimension(0, 38));
        JTextField mottoField = new JTextField(existing != null ? existing.getMotto() : "");
        mottoField.setFont(Constants.FONT_BODY);
        mottoField.setPreferredSize(new Dimension(0, 38));

        gbc.gridy = 0; p.add(makeLabel("Coalition Name:"), gbc);
        gbc.gridy = 1; p.add(nameField, gbc);
        gbc.gridy = 2; p.add(makeLabel("Motto/Slogan:"), gbc);
        gbc.gridy = 3; p.add(mottoField, gbc);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        btnPanel.setBackground(Constants.COLOR_BG);

        JButton cancelBtn = new JButton("CANCEL");
        cancelBtn.setFont(Constants.FONT_BUTTON);
        cancelBtn.setBackground(new Color(127, 140, 141));
        cancelBtn.setForeground(Color.WHITE);
        cancelBtn.setFocusPainted(false);
        cancelBtn.setBorderPainted(false);
        cancelBtn.addActionListener(e -> dialog.dispose());

        JButton saveBtn = new JButton(existing == null ? "CREATE" : "SAVE CHANGES");
        saveBtn.setFont(Constants.FONT_BUTTON);
        saveBtn.setBackground(Constants.COLOR_PRIMARY);
        saveBtn.setForeground(Color.WHITE);
        saveBtn.setFocusPainted(false);
        saveBtn.setBorderPainted(false);
        saveBtn.addActionListener(e -> {
            String name = nameField.getText().trim();
            String motto = mottoField.getText().trim();
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Name is required.");
                return;
            }
            boolean ok;
            if (existing == null) {
                ok = coalitionDAO.createCoalition(name, motto);
            } else {
                ok = coalitionDAO.updateCoalition(existing.getCoalitionId(), name, motto);
            }
            if (ok) {
                refreshData();
                dialog.dispose();
            } else {
                JOptionPane.showMessageDialog(dialog, "Failed to save coalition. Check for duplicate names.");
            }
        });

        btnPanel.add(cancelBtn);
        btnPanel.add(saveBtn);

        gbc.gridy = 4; gbc.insets = new Insets(18, 0, 0, 0);
        p.add(btnPanel, gbc);

        dialog.add(p);
        dialog.setVisible(true);
    }

    private JLabel makeLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Constants.FONT_SMALL);
        return label;
    }
}
