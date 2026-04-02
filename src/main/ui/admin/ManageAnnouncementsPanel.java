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

    public ManageAnnouncementsPanel(Admin admin) {
        this.currentAdmin = admin;
        this.announcementDAO = new AnnouncementDAO();
        setBackground(Constants.COLOR_BG);
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(20, 20, 20, 20));
        initUI();
        refreshData();
    }

    private void initUI() {
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(Constants.COLOR_BG);
        topPanel.setBorder(new EmptyBorder(0, 0, 15, 0));

        JLabel title = new JLabel("📢 Manage Announcements");
        title.setFont(Constants.FONT_HEADING);
        title.setForeground(Constants.COLOR_PRIMARY);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.setBackground(Constants.COLOR_BG);

        JButton addBtn = createBtn("Add New", Constants.COLOR_SUCCESS);
        addBtn.addActionListener(e -> showAddDialog());

        JButton deactivateBtn = createBtn("Deactivate", Constants.COLOR_DANGER);
        deactivateBtn.addActionListener(e -> handleDeactivate());

        actions.add(addBtn);
        actions.add(deactivateBtn);

        topPanel.add(title, BorderLayout.WEST);
        topPanel.add(actions, BorderLayout.EAST);

        String[] cols = {"ID", "Title", "Posted By", "Date", "Status"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        table.setRowHeight(30);

        add(topPanel, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    private void refreshData() {
        tableModel.setRowCount(0);
        List<Announcement> list = announcementDAO.getAll();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        for (Announcement a : list) {
            tableModel.addRow(new Object[]{
                a.getId(),
                a.getTitle(),
                a.getAdminName(),
                sdf.format(a.getPostedAt()),
                a.isActive() ? "Active" : "Inactive"
            });
        }
    }

    private void showAddDialog() {
        JTextField tField = new JTextField();
        JTextArea bArea = new JTextArea(5, 20);
        Object[] message = {"Title:", tField, "Body:", new JScrollPane(bArea)};

        int option = JOptionPane.showConfirmDialog(this, message, "New Announcement", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            if (announcementDAO.createAnnouncement(currentAdmin.getAdminId(), tField.getText(), bArea.getText())) {
                refreshData();
            }
        }
    }

    private void handleDeactivate() {
        int row = table.getSelectedRow();
        if (row == -1) return;
        int id = (int) tableModel.getValueAt(row, 0);
        if (announcementDAO.deactivate(id)) {
            refreshData();
        }
    }

    private JButton createBtn(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        return btn;
    }
}
