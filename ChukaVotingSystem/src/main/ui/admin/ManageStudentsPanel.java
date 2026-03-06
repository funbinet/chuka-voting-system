package main.ui.admin;

import main.dao.DBConnection;
import main.utils.Constants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class ManageStudentsPanel extends JPanel {

    private JTable            table;
    private DefaultTableModel tableModel;
    private JTextField        searchField;

    public ManageStudentsPanel() {
        setBackground(Constants.COLOR_BG);
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(10, 10, 10, 10));
        buildUI();
    }

    private void buildUI() {
        // Top bar
        JPanel topBar = new JPanel(new BorderLayout(10, 0));
        topBar.setBackground(Constants.COLOR_BG);
        topBar.setBorder(new EmptyBorder(0, 0, 10, 0));

        JLabel heading = new JLabel("👥 Manage Students");
        heading.setFont(Constants.FONT_HEADING);
        heading.setForeground(Constants.COLOR_PRIMARY);

        JPanel searchBar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        searchBar.setBackground(Constants.COLOR_BG);
        searchField = new JTextField(20);
        searchField.setFont(Constants.FONT_BODY);
        JButton searchBtn = new JButton("🔍 Search");
        searchBtn.setFont(Constants.FONT_BUTTON);
        searchBtn.setBackground(Constants.COLOR_SECONDARY);
        searchBtn.setForeground(Color.WHITE);
        searchBtn.setFocusPainted(false);
        searchBtn.setBorderPainted(false);
        searchBtn.addActionListener(e -> loadStudents(searchField.getText().trim()));

        JButton refreshBtn = new JButton("↺ Refresh");
        refreshBtn.setFont(Constants.FONT_BUTTON);
        refreshBtn.setBackground(Constants.COLOR_PRIMARY);
        refreshBtn.setForeground(Color.WHITE);
        refreshBtn.setFocusPainted(false);
        refreshBtn.setBorderPainted(false);
        refreshBtn.addActionListener(e -> loadStudents(""));

        searchBar.add(new JLabel("Search: "));
        searchBar.add(searchField);
        searchBar.add(searchBtn);
        searchBar.add(refreshBtn);

        topBar.add(heading, BorderLayout.WEST);
        topBar.add(searchBar, BorderLayout.EAST);

        // Table
        String[] cols = {"ID", "Reg Number", "Full Name", "Faculty", "Year", "GPA", "Verified", "Active"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        table = new JTable(tableModel);
        table.setFont(Constants.FONT_BODY);
        table.setRowHeight(32);
        table.getTableHeader().setFont(Constants.FONT_BUTTON);
        table.getTableHeader().setBackground(Constants.COLOR_PRIMARY);
        table.getTableHeader().setForeground(Color.WHITE);
        table.setSelectionBackground(new Color(200, 220, 255));

        loadStudents("");

        add(topBar, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    private void loadStudents(String search) {
        tableModel.setRowCount(0);
        String sql = "SELECT s.student_id, s.reg_number, s.full_name, f.faculty_name, " +
                     "s.year_of_study, s.gpa, s.is_verified, s.is_active " +
                     "FROM students s JOIN faculties f ON s.faculty_id = f.faculty_id";
        if (!search.isEmpty()) {
            sql += " WHERE s.reg_number LIKE ? OR s.full_name LIKE ?";
        }
        sql += " ORDER BY s.student_id DESC";

        try {
            Connection conn = DBConnection.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            if (!search.isEmpty()) {
                ps.setString(1, "%" + search + "%");
                ps.setString(2, "%" + search + "%");
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                    rs.getInt("student_id"),
                    rs.getString("reg_number"),
                    rs.getString("full_name"),
                    rs.getString("faculty_name"),
                    "Year " + rs.getInt("year_of_study"),
                    rs.getDouble("gpa"),
                    rs.getBoolean("is_verified") ? "✅" : "❌",
                    rs.getBoolean("is_active") ? "Active" : "Inactive"
                });
            }
        } catch (SQLException e) {
            System.err.println("❌ Load students error: " + e.getMessage());
        }
    }
}
