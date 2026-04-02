package main.ui.admin;

import main.dao.DBConnection;
import main.dao.StudentDAO;
import main.models.Student;
import main.utils.Constants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ManageStudentsPanel extends JPanel {

    private JTable            table;
    private DefaultTableModel tableModel;
    private JTextField        searchField;
    private StudentDAO        studentDAO;

    public ManageStudentsPanel() {
        this.studentDAO = new StudentDAO();
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
        
        JButton importBtn = new JButton("📤 Bulk Import");
        importBtn.setFont(Constants.FONT_BUTTON);
        importBtn.setBackground(Constants.COLOR_SUCCESS);
        importBtn.setForeground(Color.WHITE);
        importBtn.addActionListener(e -> importStudentsFromCSV());
        
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

        searchBar.add(importBtn);
        searchBar.add(new JLabel("  Search: "));
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

    private void importStudentsFromCSV() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select Students CSV File");
        if (fileChooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File file = fileChooser.getSelectedFile();
        Map<String, Integer> facultyMap = getFacultyMap();
        
        int imported = 0, skipped = 0, errors = 0;
        StringBuilder errorLog = new StringBuilder();

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            int lineNum = 0;
            while ((line = br.readLine()) != null) {
                lineNum++;
                if (lineNum == 1 && line.toLowerCase().contains("reg")) continue; // Skip header

                String[] data = line.split(",");
                if (data.length < 7) {
                    errors++;
                    errorLog.append("Line ").append(lineNum).append(": Insufficient columns.\n");
                    continue;
                }

                String reg = data[0].trim().toUpperCase();
                String name = data[1].trim();
                String email = data[2].trim();
                String phone = data[3].trim();
                String facultyCode = data[4].trim().toUpperCase();
                String yearStr = data[5].trim();
                String gpaStr = data[6].trim();

                // Validations
                if (!reg.matches("^CU/[0-9]{3}/[0-9]{4}/[2][0][0-9]{2}$") && !reg.matches("^CU/[A-Z0-9/]+$")) {
                    //CU/XXX/XXXX/YYYY pattern check. The CU/ pattern is specific, 
                    //but university patterns vary. Adjusted regex to be slightly more flexible if needed.
                    if (!reg.matches("^CU/[A-Z0-9/]+$")) {
                        errors++;
                        errorLog.append("Line ").append(lineNum).append(": Invalid Reg format (").append(reg).append(").\n");
                        continue;
                    }
                }
                if (!email.contains("@")) {
                    errors++;
                    errorLog.append("Line ").append(lineNum).append(": Invalid email.\n");
                    continue;
                }
                
                int year;
                double gpa;
                try {
                    year = Integer.parseInt(yearStr);
                    gpa = Double.parseDouble(gpaStr);
                    if (year < 1 || year > 6 || gpa < 0.0 || gpa > 4.0) throw new Exception();
                } catch (Exception e) {
                    errors++;
                    errorLog.append("Line ").append(lineNum).append(": Invalid Year (1-6) or GPA (0-4.0).\n");
                    continue;
                }

                if (!facultyMap.containsKey(facultyCode)) {
                    errors++;
                    errorLog.append("Line ").append(lineNum).append(": Unknown Faculty Code (").append(facultyCode).append(").\n");
                    continue;
                }

                if (studentDAO.regNumberExists(reg) || studentDAO.phoneExists(phone)) {
                    skipped++;
                    continue;
                }

                // Import
                Student s = new Student();
                s.setRegNumber(reg);
                s.setFullName(name);
                s.setEmail(email);
                s.setPhoneNumber(phone);
                s.setFacultyId(facultyMap.get(facultyCode));
                s.setYearOfStudy(year);
                s.setGpa(gpa);

                String randomPass = UUID.randomUUID().toString().substring(0, 8);
                if (studentDAO.createStudent(s, randomPass)) {
                    imported++;
                } else {
                    errors++;
                    errorLog.append("Line ").append(lineNum).append(": Database insertion failed.\n");
                }
            }

            // Results Dialog
            showImportResults(imported, skipped, errors, errorLog.toString());
            loadStudents("");

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error reading file: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private Map<String, Integer> getFacultyMap() {
        Map<String, Integer> map = new HashMap<>();
        try {
            Connection conn = DBConnection.getInstance().getConnection();
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery("SELECT faculty_id, faculty_code FROM faculties");
            while (rs.next()) {
                map.put(rs.getString("faculty_code").toUpperCase(), rs.getInt("faculty_id"));
            }
            rs.close();
            st.close();
        } catch (SQLException e) { e.printStackTrace(); }
        return map;
    }

    private void showImportResults(int imp, int skip, int err, String logs) {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.add(new JLabel(String.format("<html><b>Import Summary:</b><br>" +
                "✅ Imported: %d<br>⏭️ Skipped: %d<br>❌ Errors: %d</html>", imp, skip, err)), BorderLayout.NORTH);

        if (err > 0) {
            JTextArea area = new JTextArea(logs);
            area.setEditable(false);
            area.setFont(new Font("Monospaced", Font.PLAIN, 12));
            JScrollPane sp = new JScrollPane(area);
            sp.setPreferredSize(new Dimension(400, 200));
            panel.add(sp, BorderLayout.CENTER);
        }

        JOptionPane.showMessageDialog(this, panel, "CSV Import Results", JOptionPane.INFORMATION_MESSAGE);
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
