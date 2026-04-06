package main.ui.admin;

import main.dao.DBConnection;
import main.dao.StudentDAO;
import main.models.Student;
import main.utils.Constants;
import main.utils.Validator;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.sql.*;
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
        JPanel topBar = new JPanel();
        topBar.setLayout(new BoxLayout(topBar, BoxLayout.Y_AXIS));
        topBar.setBackground(Constants.COLOR_BG);
        topBar.setBorder(new EmptyBorder(0, 0, 12, 0));

        JLabel heading = new JLabel("👥 Manage Students");
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
        searchBtn.addActionListener(e -> loadStudents(searchField.getText().trim()));

        JButton refreshBtn = new JButton("↺ Refresh");
        refreshBtn.setFont(Constants.FONT_BUTTON);
        refreshBtn.setBackground(Constants.COLOR_PRIMARY);
        refreshBtn.setForeground(Color.WHITE);
        refreshBtn.setFocusPainted(false);
        refreshBtn.setBorderPainted(false);
        refreshBtn.addActionListener(e -> loadStudents(""));

        JPanel searchRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        searchRow.setBackground(Constants.COLOR_BG);
        searchRow.add(new JLabel("Search:"));
        searchRow.add(searchField);
        searchRow.add(searchBtn);
        searchRow.add(refreshBtn);

        topBar.add(headingRow);
        topBar.add(Box.createVerticalStrut(8));
        topBar.add(searchRow);

        JButton addBtn = new JButton("➕ Add Student");
        addBtn.setFont(Constants.FONT_BUTTON);
        addBtn.setBackground(Constants.COLOR_PRIMARY);
        addBtn.setForeground(Color.WHITE);
        addBtn.setFocusPainted(false);
        addBtn.setBorderPainted(false);
        addBtn.addActionListener(e -> showStudentDialog(null));

        JButton editBtn = new JButton("✏️ Edit Student");
        editBtn.setFont(Constants.FONT_BUTTON);
        editBtn.setBackground(Constants.COLOR_SECONDARY);
        editBtn.setForeground(Color.WHITE);
        editBtn.setFocusPainted(false);
        editBtn.setBorderPainted(false);
        editBtn.addActionListener(e -> editSelectedStudent());

        JButton deleteBtn = new JButton("🗑️ Delete Student");
        deleteBtn.setFont(Constants.FONT_BUTTON);
        deleteBtn.setBackground(Constants.COLOR_DANGER);
        deleteBtn.setForeground(Color.WHITE);
        deleteBtn.setFocusPainted(false);
        deleteBtn.setBorderPainted(false);
        deleteBtn.addActionListener(e -> deactivateSelectedStudent());

        JButton importBtn = new JButton("📤 Bulk Import");
        importBtn.setFont(Constants.FONT_BUTTON);
        importBtn.setBackground(Constants.COLOR_SUCCESS);
        importBtn.setForeground(Color.WHITE);
        importBtn.setFocusPainted(false);
        importBtn.setBorderPainted(false);
        importBtn.addActionListener(e -> importStudentsFromCSV());

        JPanel actionBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 8));
        actionBar.setBackground(Constants.COLOR_BG);
        actionBar.setBorder(new EmptyBorder(10, 0, 0, 0));
        actionBar.add(addBtn);
        actionBar.add(editBtn);
        actionBar.add(deleteBtn);
        actionBar.add(importBtn);

        // Table
        String[] cols = {"No.", "ID", "Reg Number", "Full Name", "Faculty", "Year", "GPA", "Gender", "Resident", "Verified", "Active"};
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
        hideInternalIdColumn();

        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(BorderFactory.createEmptyBorder());

        loadStudents("");

        add(topBar, BorderLayout.NORTH);
        add(tableScroll, BorderLayout.CENTER);
        add(actionBar, BorderLayout.SOUTH);
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
                if (data.length < 9) {
                    errors++;
                    errorLog.append("Line ").append(lineNum).append(": Insufficient columns (expected: reg,name,email,phone,faculty,year,gpa,gender,resident).\n");
                    continue;
                }

                String reg = data[0].trim().toUpperCase();
                String name = data[1].trim();
                String email = data[2].trim();
                String phone = data[3].trim();
                String facultyCode = data[4].trim().toUpperCase();
                String yearStr = data[5].trim();
                String gpaStr = data[6].trim();
                String gender = data.length > 7 ? data[7].trim().toUpperCase() : "";
                String residentStr = data.length > 8 ? data[8].trim().toLowerCase() : "";

                // Validations
                if (!Validator.isValidRegNumber(reg) && !reg.matches("^CU/[A-Z0-9/]+$")) {
                    errors++;
                    errorLog.append("Line ").append(lineNum).append(": Invalid Reg format (").append(reg).append(").\n");
                    continue;
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

                if (!("MALE".equals(gender) || "FEMALE".equals(gender) || "OTHER".equals(gender))) {
                    errors++;
                    errorLog.append("Line ").append(lineNum).append(": Gender must be MALE/FEMALE/OTHER.\n");
                    continue;
                }

                Boolean isResident = parseResidentValue(residentStr);
                if (isResident == null) {
                    errors++;
                    errorLog.append("Line ").append(lineNum).append(": Resident column must be yes/no, true/false, resident/non-resident.\n");
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
                s.setGender(gender);
                s.setResident(isResident);

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
                     "s.year_of_study, s.gpa, s.gender, s.is_resident, s.is_verified, s.is_active " +
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
            int displayNo = 1;
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                    displayNo++,
                    rs.getInt("student_id"),
                    rs.getString("reg_number"),
                    rs.getString("full_name"),
                    rs.getString("faculty_name"),
                    "Year " + rs.getInt("year_of_study"),
                    rs.getDouble("gpa"),
                    rs.getString("gender"),
                    rs.getBoolean("is_resident") ? "Resident" : "Non-Resident",
                    rs.getBoolean("is_verified") ? "✅" : "❌",
                    rs.getBoolean("is_active") ? "Active" : "Inactive"
                });
            }
        } catch (SQLException e) {
            System.err.println("❌ Load students error: " + e.getMessage());
        }
    }

    private void editSelectedStudent() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a student to edit.", "Edit Student", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int modelRow = table.convertRowIndexToModel(row);
        int studentId = (int) tableModel.getValueAt(modelRow, 1);
        Student student = studentDAO.findById(studentId);
        if (student == null) {
            JOptionPane.showMessageDialog(this, "Could not load selected student.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        showStudentDialog(student);
    }

    private void deactivateSelectedStudent() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a student to delete.", "Delete Student", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int modelRow = table.convertRowIndexToModel(row);
        int studentId = (int) tableModel.getValueAt(modelRow, 1);
        String studentName = String.valueOf(tableModel.getValueAt(modelRow, 3));

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Delete student '" + studentName + "'? This action deactivates the account.",
                "Confirm Deletion",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (confirm == JOptionPane.YES_OPTION) {
            if (studentDAO.setStudentActive(studentId, false)) {
                JOptionPane.showMessageDialog(this, "Student deactivated successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                loadStudents(searchField.getText().trim());
            } else {
                JOptionPane.showMessageDialog(this, "Failed to deactivate student.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showStudentDialog(Student existing) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                existing == null ? "Add Student" : "Edit Student",
                true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setSize(700, 760);
        dialog.setMinimumSize(new Dimension(620, 680));
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(Constants.COLOR_BG);
        formPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(6, 0, 6, 0);

        JTextField regField = new JTextField(existing == null ? "" : existing.getRegNumber());
        regField.setFont(Constants.FONT_BODY);
        regField.setPreferredSize(new Dimension(0, 38));
        JTextField nameField = new JTextField(existing == null ? "" : existing.getFullName());
        nameField.setFont(Constants.FONT_BODY);
        nameField.setPreferredSize(new Dimension(0, 38));
        JTextField emailField = new JTextField(existing == null ? "" : existing.getEmail());
        emailField.setFont(Constants.FONT_BODY);
        emailField.setPreferredSize(new Dimension(0, 38));
        JTextField phoneField = new JTextField(existing == null ? "" : existing.getPhoneNumber());
        phoneField.setFont(Constants.FONT_BODY);
        phoneField.setPreferredSize(new Dimension(0, 38));

        JComboBox<FacultyItem> facultyCombo = new JComboBox<>();
        facultyCombo.setFont(Constants.FONT_BODY);
        facultyCombo.setPreferredSize(new Dimension(0, 38));
        loadFacultiesIntoCombo(facultyCombo);
        if (existing != null) {
            selectFaculty(facultyCombo, existing.getFacultyId());
        }

        JSpinner yearSpinner = new JSpinner(new SpinnerNumberModel(existing == null ? 1 : existing.getYearOfStudy(), 1, 6, 1));
        yearSpinner.setFont(Constants.FONT_BODY);
        yearSpinner.setPreferredSize(new Dimension(0, 38));
        JComponent spinnerEditor = yearSpinner.getEditor();
        if (spinnerEditor instanceof JSpinner.DefaultEditor) {
            ((JSpinner.DefaultEditor) spinnerEditor).getTextField().setFont(Constants.FONT_BODY);
        }
        JTextField gpaField = new JTextField(existing == null ? "0.00" : String.valueOf(existing.getGpa()));
        gpaField.setFont(Constants.FONT_BODY);
        gpaField.setPreferredSize(new Dimension(0, 38));

        JComboBox<String> genderCombo = new JComboBox<>(new String[]{"MALE", "FEMALE", "OTHER"});
        genderCombo.setFont(Constants.FONT_BODY);
        genderCombo.setPreferredSize(new Dimension(0, 38));
        if (existing != null && existing.getGender() != null) {
            genderCombo.setSelectedItem(existing.getGender().toUpperCase());
        }

        JCheckBox residentCheck = new JCheckBox("Resident");
        residentCheck.setBackground(Constants.COLOR_BG);
        residentCheck.setSelected(existing != null && existing.isResident());

        JCheckBox activeCheck = new JCheckBox("Active Account");
        activeCheck.setBackground(Constants.COLOR_BG);
        activeCheck.setSelected(existing == null || existing.isActive());
        activeCheck.setFont(Constants.FONT_BODY);

        JPasswordField passwordField = new JPasswordField("Student@123");
        passwordField.setFont(Constants.FONT_BODY);
        passwordField.setPreferredSize(new Dimension(0, 38));

        int row = 0;
        gbc.gridy = row++; formPanel.add(makeFormLabel("Reg Number"), gbc);
        gbc.gridy = row++; formPanel.add(regField, gbc);
        gbc.gridy = row++; formPanel.add(makeFormLabel("Full Name"), gbc);
        gbc.gridy = row++; formPanel.add(nameField, gbc);
        gbc.gridy = row++; formPanel.add(makeFormLabel("Email"), gbc);
        gbc.gridy = row++; formPanel.add(emailField, gbc);
        gbc.gridy = row++; formPanel.add(makeFormLabel("Phone"), gbc);
        gbc.gridy = row++; formPanel.add(phoneField, gbc);
        gbc.gridy = row++; formPanel.add(makeFormLabel("Faculty"), gbc);
        gbc.gridy = row++; formPanel.add(facultyCombo, gbc);
        gbc.gridy = row++; formPanel.add(makeFormLabel("Year of Study"), gbc);
        gbc.gridy = row++; formPanel.add(yearSpinner, gbc);
        gbc.gridy = row++; formPanel.add(makeFormLabel("GPA"), gbc);
        gbc.gridy = row++; formPanel.add(gpaField, gbc);
        gbc.gridy = row++; formPanel.add(makeFormLabel("Gender"), gbc);
        gbc.gridy = row++; formPanel.add(genderCombo, gbc);
        gbc.gridy = row++; formPanel.add(residentCheck, gbc);
        gbc.gridy = row++; formPanel.add(activeCheck, gbc);

        if (existing == null) {
            gbc.gridy = row++; formPanel.add(makeFormLabel("Initial Password"), gbc);
            gbc.gridy = row++; formPanel.add(passwordField, gbc);
        }

        gbc.gridy = row;
        gbc.weighty = 1.0;
        formPanel.add(Box.createVerticalGlue(), gbc);

        JScrollPane scrollPane = new JScrollPane(formPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        JButton cancelBtn = new JButton("CANCEL");
        cancelBtn.setFont(Constants.FONT_BUTTON);
        cancelBtn.setBackground(new Color(127, 140, 141));
        cancelBtn.setForeground(Color.WHITE);
        cancelBtn.setFocusPainted(false);
        cancelBtn.setBorderPainted(false);
        cancelBtn.addActionListener(e -> dialog.dispose());

        JButton saveBtn = new JButton(existing == null ? "ADD STUDENT" : "SAVE CHANGES");
        saveBtn.setFont(Constants.FONT_BUTTON);
        saveBtn.setBackground(Constants.COLOR_PRIMARY);
        saveBtn.setForeground(Color.WHITE);
        saveBtn.setFocusPainted(false);
        saveBtn.setBorderPainted(false);
        saveBtn.addActionListener(e -> {
            String reg = regField.getText().trim().toUpperCase();
            String name = nameField.getText().trim();
            String email = emailField.getText().trim().toLowerCase();
            String phone = phoneField.getText().trim();

            double gpa;
            try {
                gpa = Double.parseDouble(gpaField.getText().trim());
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "GPA must be a number.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (!Validator.isValidRegNumber(reg)) {
                JOptionPane.showMessageDialog(dialog, "Invalid registration number format.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (!Validator.isValidEmail(email)) {
                JOptionPane.showMessageDialog(dialog, "Invalid email format.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (!Validator.isValidPhone(phone)) {
                JOptionPane.showMessageDialog(dialog, "Invalid phone number format.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (!Validator.isValidGPA(gpa)) {
                JOptionPane.showMessageDialog(dialog, "GPA must be between 0.0 and 4.0.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }

            FacultyItem selectedFaculty = (FacultyItem) facultyCombo.getSelectedItem();
            if (selectedFaculty == null) {
                JOptionPane.showMessageDialog(dialog, "Please select a faculty.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }

            Student student = new Student();
            if (existing != null) {
                student.setStudentId(existing.getStudentId());
            }
            student.setRegNumber(reg);
            student.setFullName(name);
            student.setEmail(email);
            student.setPhoneNumber(phone);
            student.setFacultyId(selectedFaculty.id);
            student.setYearOfStudy((Integer) yearSpinner.getValue());
            student.setGpa(gpa);
            student.setGender(String.valueOf(genderCombo.getSelectedItem()));
            student.setResident(residentCheck.isSelected());
            student.setActive(activeCheck.isSelected());

            boolean ok;
            if (existing == null) {
                String password = new String(passwordField.getPassword()).trim();
                if (!Validator.isValidPassword(password)) {
                    JOptionPane.showMessageDialog(dialog, "Initial password must meet complexity requirements.", "Validation", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                ok = studentDAO.createStudent(student, password);
            } else {
                ok = studentDAO.updateStudentRecord(student);
            }

            if (ok) {
                JOptionPane.showMessageDialog(dialog, existing == null ? "Student added." : "Student updated.", "Success", JOptionPane.INFORMATION_MESSAGE);
                loadStudents(searchField.getText().trim());
                dialog.dispose();
            } else {
                JOptionPane.showMessageDialog(dialog, "Operation failed. Check duplicates or constraints.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        JPanel buttonBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 10));
        buttonBar.setBackground(Constants.COLOR_BG);
        buttonBar.setBorder(new EmptyBorder(6, 12, 12, 12));
        buttonBar.add(cancelBtn);
        buttonBar.add(saveBtn);

        dialog.add(scrollPane, BorderLayout.CENTER);
        dialog.add(buttonBar, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private void hideInternalIdColumn() {
        table.getColumnModel().getColumn(1).setMinWidth(0);
        table.getColumnModel().getColumn(1).setMaxWidth(0);
        table.getColumnModel().getColumn(1).setPreferredWidth(0);
    }

    private void loadFacultiesIntoCombo(JComboBox<FacultyItem> combo) {
        Map<String, Integer> map = getFacultyMap();
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            combo.addItem(new FacultyItem(entry.getValue(), entry.getKey()));
        }
    }

    private void selectFaculty(JComboBox<FacultyItem> combo, int facultyId) {
        for (int i = 0; i < combo.getItemCount(); i++) {
            FacultyItem item = combo.getItemAt(i);
            if (item.id == facultyId) {
                combo.setSelectedIndex(i);
                return;
            }
        }
    }

    private Boolean parseResidentValue(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase();
        if (normalized.equals("yes") || normalized.equals("true") || normalized.equals("resident") || normalized.equals("1")) {
            return true;
        }
        if (normalized.equals("no") || normalized.equals("false") || normalized.equals("non-resident") || normalized.equals("non resident") || normalized.equals("0")) {
            return false;
        }
        return null;
    }

    private JLabel makeFormLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Constants.FONT_SMALL);
        return label;
    }

    private static class FacultyItem {
        private final int id;
        private final String code;

        private FacultyItem(int id, String code) {
            this.id = id;
            this.code = code;
        }

        @Override
        public String toString() {
            return code;
        }
    }
}
