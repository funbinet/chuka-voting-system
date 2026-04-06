package main.ui.admin;

import main.dao.AnnouncementDAO;
import main.models.Admin;
import main.models.Announcement;
import main.utils.Constants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.List;

public class ManageAnnouncementsPanel extends JPanel {

    private AnnouncementDAO announcementDAO;
    private JTable          table;
    private DefaultTableModel tableModel;
    private Admin           currentAdmin;
    private JTextField      searchField;

    public ManageAnnouncementsPanel(Admin admin) {
        this.currentAdmin = admin;
        this.announcementDAO = new AnnouncementDAO();
        setBackground(Constants.COLOR_BG);
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(10, 10, 10, 10));
        initUI();
        refreshData("");
    }

    private void initUI() {
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.setBackground(Constants.COLOR_BG);
        topPanel.setBorder(new EmptyBorder(0, 0, 12, 0));

        JLabel title = new JLabel("📢 Manage Announcements");
        title.setFont(Constants.FONT_HEADING);
        title.setForeground(Constants.COLOR_PRIMARY);

        JPanel headingRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        headingRow.setBackground(Constants.COLOR_BG);
        headingRow.add(title);

        searchField = new JTextField(26);
        searchField.setFont(Constants.FONT_BODY);

        JButton searchBtn = createBtn("🔍 Search", Constants.COLOR_SECONDARY);
        searchBtn.addActionListener(e -> refreshData(searchField.getText().trim()));

        JButton refreshBtn = createBtn("↺ Refresh", Constants.COLOR_PRIMARY);
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

        topPanel.add(headingRow);
        topPanel.add(Box.createVerticalStrut(8));
        topPanel.add(searchRow);

        JButton addBtn = createBtn("➕ Add Announcement", Constants.COLOR_PRIMARY);
        addBtn.addActionListener(e -> showAddDialog());

        JButton deleteBtn = createBtn("🗑️ Delete Announcement", Constants.COLOR_DANGER);
        deleteBtn.addActionListener(e -> handleDelete());

        JButton clearAllBtn = createBtn("🧹 Clear All Announcements", Constants.COLOR_SUCCESS);
        clearAllBtn.addActionListener(e -> clearAllAnnouncements());

        JPanel actionBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 8));
        actionBar.setBackground(Constants.COLOR_BG);
        actionBar.setBorder(new EmptyBorder(10, 0, 0, 0));
        actionBar.add(addBtn);
        actionBar.add(deleteBtn);
        actionBar.add(clearAllBtn);

        String[] cols = {"No.", "ID", "Title", "Posted By", "Date"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        table.setRowHeight(30);
        table.setFont(Constants.FONT_BODY);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setFont(Constants.FONT_BUTTON);
        table.getTableHeader().setBackground(Constants.COLOR_PRIMARY);
        table.getTableHeader().setForeground(Color.WHITE);
        hideInternalIdColumn();

        add(topPanel, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(actionBar, BorderLayout.SOUTH);
    }

    private void refreshData() {
        refreshData(searchField != null ? searchField.getText().trim() : "");
    }

    private void refreshData(String search) {
        tableModel.setRowCount(0);
        List<Announcement> list = announcementDAO.getAll();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        String term = search == null ? "" : search.trim().toLowerCase();
        int displayNo = 1;
        for (Announcement a : list) {
            if (!term.isEmpty()) {
                String title = a.getTitle() == null ? "" : a.getTitle().toLowerCase();
                String admin = a.getAdminName() == null ? "" : a.getAdminName().toLowerCase();
                String date = a.getPostedAt() == null ? "" : sdf.format(a.getPostedAt()).toLowerCase();
                if (!(title.contains(term) || admin.contains(term) || date.contains(term))) {
                    continue;
                }
            }

            tableModel.addRow(new Object[]{
                displayNo++,
                a.getId(),
                a.getTitle(),
                a.getAdminName(),
                sdf.format(a.getPostedAt())
            });
        }
    }

    private void showAddDialog() {
        JTextField tField = new JTextField();
        JTextArea bArea = new JTextArea(5, 20);
        Object[] message = {"Title:", tField, "Body:", new JScrollPane(bArea)};

        int option = JOptionPane.showConfirmDialog(this, message, "New Announcement", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            String title = tField.getText() == null ? "" : tField.getText().trim();
            String body = bArea.getText() == null ? "" : bArea.getText().trim();

            if (title.isEmpty() || body.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Title and body are required.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (announcementDAO.createAnnouncement(currentAdmin.getAdminId(), title, body)) {
                refreshData();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to create announcement.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handleDelete() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select an announcement to delete.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int modelRow = table.convertRowIndexToModel(row);
        int id = (int) tableModel.getValueAt(modelRow, 1);
        String title = String.valueOf(tableModel.getValueAt(modelRow, 2));
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Delete announcement '" + title + "'?",
                "Confirm Deletion",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        if (announcementDAO.deleteAnnouncement(id)) {
            refreshData();
        } else {
            JOptionPane.showMessageDialog(this, "Failed to delete announcement.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearAllAnnouncements() {
        int confirm = JOptionPane.showConfirmDialog(
                this,
            "Delete ALL announcements (active and inactive)?",
                "Confirm Clear All",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        int affected = announcementDAO.deleteAllAnnouncements();
        JOptionPane.showMessageDialog(this, "Deleted " + affected + " announcements.", "Done", JOptionPane.INFORMATION_MESSAGE);
        refreshData();
    }

    private JButton createBtn(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        return btn;
    }

    private void hideInternalIdColumn() {
        table.getColumnModel().getColumn(1).setMinWidth(0);
        table.getColumnModel().getColumn(1).setMaxWidth(0);
        table.getColumnModel().getColumn(1).setPreferredWidth(0);
    }
}
