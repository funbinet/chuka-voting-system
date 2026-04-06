package main.ui.admin;

import main.dao.AuditDAO;
import main.models.AuditLog;
import main.utils.Constants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class AuditLogsPanel extends JPanel {

    private AuditDAO auditDAO;
    private JTable table;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private JSpinner fromSpinner;
    private JSpinner toSpinner;

    public AuditLogsPanel() {
        this.auditDAO = new AuditDAO();
        initUI();
        refreshData();
    }

    private void initUI() {
        setLayout(new BorderLayout());
        setBackground(Constants.COLOR_BG);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        // --- Top Control Panel ---
        JPanel topPanel = new JPanel(new BorderLayout(15, 0));
        topPanel.setBackground(Constants.COLOR_BG);
        topPanel.setBorder(new EmptyBorder(0, 0, 15, 0));

        // Filter Bar (Search + Dates)
        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        filterBar.setBackground(Constants.COLOR_BG);

        searchField = new JTextField(15);
        searchField.setPreferredSize(new Dimension(0, 30));
        searchField.setToolTipText("Search action or description...");
        
        // Date Pickers
        Calendar cal = Calendar.getInstance();
        Date now = cal.getTime();
        cal.add(Calendar.MONTH, -1);
        Date monthAgo = cal.getTime();

        fromSpinner = new JSpinner(new SpinnerDateModel(monthAgo, null, null, Calendar.DAY_OF_MONTH));
        toSpinner = new JSpinner(new SpinnerDateModel(now, null, null, Calendar.DAY_OF_MONTH));
        JSpinner.DateEditor fromEditor = new JSpinner.DateEditor(fromSpinner, "yyyy-MM-dd");
        JSpinner.DateEditor toEditor = new JSpinner.DateEditor(toSpinner, "yyyy-MM-dd");
        fromSpinner.setEditor(fromEditor);
        toSpinner.setEditor(toEditor);

        JButton filterBtn = new JButton("Filter");
        filterBtn.addActionListener(e -> refreshData());

        filterBar.add(new JLabel("Search:"));
        filterBar.add(searchField);
        filterBar.add(new JLabel("From:"));
        filterBar.add(fromSpinner);
        filterBar.add(new JLabel("To:"));
        filterBar.add(toSpinner);
        filterBar.add(filterBtn);

        // Export Button
        JButton exportBtn = new JButton("Export CSV");
        exportBtn.setBackground(Constants.COLOR_SECONDARY);
        exportBtn.setForeground(Color.WHITE);
        exportBtn.addActionListener(e -> handleExportCSV());

        topPanel.add(filterBar, BorderLayout.WEST);
        topPanel.add(exportBtn, BorderLayout.EAST);

        // --- Table ---
        String[] columns = {"No.", "ID", "Student/User", "Action", "Description", "Timestamp"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        table = new JTable(tableModel);
        table.setRowHeight(30);
        table.getTableHeader().setFont(Constants.FONT_BUTTON);
        table.setFont(Constants.FONT_BODY);
        hideInternalIdColumn();

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220)));

        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }

    private void refreshData() {
        String search = searchField.getText().trim();
        Date from = (Date) fromSpinner.getValue();
        Date to = (Date) toSpinner.getValue();

        // Adjust 'to' date to the end of the day (23:59:59)
        Calendar cal = Calendar.getInstance();
        cal.setTime(to);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        to = cal.getTime();

        List<AuditLog> logs = auditDAO.getAuditLogs(search, from, to);
        tableModel.setRowCount(0);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        int displayNo = 1;

        for (AuditLog log : logs) {
            tableModel.addRow(new Object[]{
                displayNo++,
                log.getLogId(),
                log.getStudentName() != null ? log.getStudentName() : "System",
                log.getAction(),
                log.getDescription(),
                sdf.format(log.getLoggedAt())
            });
        }
    }

    private void hideInternalIdColumn() {
        table.getColumnModel().getColumn(1).setMinWidth(0);
        table.getColumnModel().getColumn(1).setMaxWidth(0);
        table.getColumnModel().getColumn(1).setPreferredWidth(0);
    }

    private void handleExportCSV() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Audit Logs as CSV");
        fileChooser.setSelectedFile(new File("audit_logs_export.csv"));

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try (FileWriter writer = new FileWriter(file)) {
                // Headers
                for (int i = 0; i < tableModel.getColumnCount(); i++) {
                    writer.write(tableModel.getColumnName(i) + (i == tableModel.getColumnCount() - 1 ? "" : ","));
                }
                writer.write("\n");

                // Rows
                for (int i = 0; i < tableModel.getRowCount(); i++) {
                    for (int j = 0; j < tableModel.getColumnCount(); j++) {
                        String val = String.valueOf(tableModel.getValueAt(i, j)).replace(",", ";"); // Escape commas
                        writer.write(val + (j == tableModel.getColumnCount() - 1 ? "" : ","));
                    }
                    writer.write("\n");
                }

                JOptionPane.showMessageDialog(this, "✅ Export successful!", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "❌ Export failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
