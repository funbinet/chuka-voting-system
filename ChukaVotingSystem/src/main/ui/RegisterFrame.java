package main.ui;

import main.dao.DBConnection;
import main.models.Faculty;
import main.models.Student;
import main.services.AuthService;
import main.utils.Constants;
import main.utils.Validator;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RegisterFrame extends JFrame {

    private JTextField     regNumberField;
    private JTextField     fullNameField;
    private JTextField     emailField;
    private JTextField     phoneField;
    private JPasswordField passwordField;
    private JPasswordField confirmPassField;
    private JComboBox<Faculty> facultyCombo;
    private JComboBox<String>  yearCombo;
    private JTextField     gpaField;

    private AuthService authService;

    public RegisterFrame() {
        this.authService = new AuthService();
        initUI();
        loadFaculties();
    }

    private void initUI() {
        setTitle("Chuka University — Student Registration");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(520, 680);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(Constants.COLOR_BG);

        // Header
        JPanel header = new JPanel();
        header.setBackground(Constants.COLOR_PRIMARY);
        header.setBorder(new EmptyBorder(15, 20, 15, 20));
        JLabel title = new JLabel("🎓 Student Registration");
        title.setFont(Constants.FONT_HEADING);
        title.setForeground(Color.WHITE);
        header.add(title);

        // Scrollable form
        JPanel formPanel = buildForm();
        JScrollPane scrollPane = new JScrollPane(formPanel);
        scrollPane.setBorder(null);

        mainPanel.add(header, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        add(mainPanel);
        setVisible(true);
    }

    private JPanel buildForm() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Constants.COLOR_BG);
        panel.setBorder(new EmptyBorder(20, 40, 20, 40));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.insets = new Insets(5, 0, 5, 0);

        int row = 0;

        row = addField(panel, gbc, row, "Registration Number *", regNumberField = new JTextField());
        row = addField(panel, gbc, row, "Full Name *", fullNameField = new JTextField());
        row = addField(panel, gbc, row, "Email Address *", emailField = new JTextField());
        row = addField(panel, gbc, row, "Phone Number * (e.g. 0712345678)", phoneField = new JTextField());
        row = addField(panel, gbc, row, "Password * (min 8 chars, uppercase, digit, special)", passwordField = new JPasswordField());
        row = addField(panel, gbc, row, "Confirm Password *", confirmPassField = new JPasswordField());

        // Faculty
        gbc.gridy = row++;
        panel.add(makeLabel("Faculty *"), gbc);
        gbc.gridy = row++;
        facultyCombo = new JComboBox<>();
        facultyCombo.setFont(Constants.FONT_BODY);
        facultyCombo.setPreferredSize(new Dimension(0, 40));
        panel.add(facultyCombo, gbc);

        // Year
        gbc.gridy = row++;
        panel.add(makeLabel("Year of Study *"), gbc);
        gbc.gridy = row++;
        yearCombo = new JComboBox<>(new String[]{"1", "2", "3", "4"});
        yearCombo.setFont(Constants.FONT_BODY);
        yearCombo.setPreferredSize(new Dimension(0, 40));
        panel.add(yearCombo, gbc);

        // GPA
        row = addField(panel, gbc, row, "Current GPA (0.00 - 4.00) *", gpaField = new JTextField());

        // Register button
        gbc.gridy = row++;
        gbc.insets = new Insets(20, 0, 8, 0);
        JButton registerBtn = new JButton("REGISTER");
        registerBtn.setFont(Constants.FONT_BUTTON);
        registerBtn.setBackground(Constants.COLOR_SUCCESS);
        registerBtn.setForeground(Color.WHITE);
        registerBtn.setFocusPainted(false);
        registerBtn.setBorderPainted(false);
        registerBtn.setPreferredSize(new Dimension(0, 45));
        registerBtn.addActionListener(e -> handleRegister());
        panel.add(registerBtn, gbc);

        // Back to login
        gbc.gridy = row++;
        gbc.insets = new Insets(4, 0, 8, 0);
        JButton backBtn = new JButton("← Back to Login");
        backBtn.setFont(Constants.FONT_SMALL);
        backBtn.setForeground(Constants.COLOR_SECONDARY);
        backBtn.setBackground(Constants.COLOR_BG);
        backBtn.setBorderPainted(false);
        backBtn.setFocusPainted(false);
        backBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        backBtn.addActionListener(e -> { dispose(); new LoginFrame(); });
        panel.add(backBtn, gbc);

        return panel;
    }

    private void handleRegister() {
        String regNumber    = regNumberField.getText().trim().toUpperCase();
        String fullName     = fullNameField.getText().trim();
        String email        = emailField.getText().trim();
        String phone        = phoneField.getText().trim();
        String password     = new String(passwordField.getPassword());
        String confirmPass  = new String(confirmPassField.getPassword());
        int    yearOfStudy  = Integer.parseInt((String) yearCombo.getSelectedItem());
        double gpa;

        Faculty faculty = (Faculty) facultyCombo.getSelectedItem();

        // Validations
        if (Validator.isEmpty(regNumber) || Validator.isEmpty(fullName) ||
            Validator.isEmpty(email) || Validator.isEmpty(phone) || Validator.isEmpty(password)) {
            showError("Please fill in all required fields.");
            return;
        }
        if (!Validator.isValidRegNumber(regNumber)) {
            showError("Invalid registration number format (e.g. SCT/2021/001)");
            return;
        }
        if (!Validator.isValidPhone(phone)) {
            showError("Invalid phone number (e.g. 0712345678)");
            return;
        }
        if (!Validator.isValidEmail(email)) {
            showError("Invalid email address.");
            return;
        }
        if (!Validator.isValidPassword(password)) {
            showError("Password must be at least 8 characters with uppercase, digit, and special character.");
            return;
        }
        if (!password.equals(confirmPass)) {
            showError("Passwords do not match.");
            return;
        }
        try {
            gpa = Double.parseDouble(gpaField.getText().trim());
            if (!Validator.isValidGPA(gpa)) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            showError("Invalid GPA. Must be between 0.00 and 4.00.");
            return;
        }

        // Build student object
        Student student = new Student();
        student.setRegNumber(regNumber);
        student.setFullName(fullName);
        student.setEmail(email);
        student.setPhoneNumber(phone);
        student.setPasswordHash(password); // DAO will hash this
        student.setFacultyId(faculty.getFacultyId());
        student.setYearOfStudy(yearOfStudy);
        student.setGpa(gpa);

        main.dao.StudentDAO dao = new main.dao.StudentDAO();
        if (dao.regNumberExists(regNumber)) {
            showError("Registration number already exists.");
            return;
        }
        if (dao.phoneExists(phone)) {
            showError("Phone number already registered.");
            return;
        }

        boolean success = dao.registerStudent(student);
        if (success) {
            JOptionPane.showMessageDialog(this,
                "✅ Registration successful!\nPlease login and verify your phone number via OTP.",
                "Success", JOptionPane.INFORMATION_MESSAGE);
            dispose();
            new LoginFrame();
        } else {
            showError("Registration failed. Please try again.");
        }
    }

    private void loadFaculties() {
        try {
            Connection conn = DBConnection.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement("SELECT * FROM faculties");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                facultyCombo.addItem(new Faculty(
                    rs.getInt("faculty_id"),
                    rs.getString("faculty_code"),
                    rs.getString("faculty_name")
                ));
            }
        } catch (SQLException e) {
            System.err.println("❌ Load faculties error: " + e.getMessage());
        }
    }

    private int addField(JPanel panel, GridBagConstraints gbc, int row, String label, JComponent field) {
        gbc.gridy = row++;
        panel.add(makeLabel(label), gbc);
        gbc.gridy = row++;
        field.setFont(Constants.FONT_BODY);
        field.setPreferredSize(new Dimension(0, 40));
        panel.add(field, gbc);
        return row;
    }

    private JLabel makeLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Constants.FONT_SMALL);
        label.setForeground(Constants.COLOR_TEXT);
        return label;
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Validation Error", JOptionPane.ERROR_MESSAGE);
    }
}
