package main.ui.student;

import main.dao.DBConnection;
import main.models.Position;
import main.models.Student;
import main.services.CandidateService;
import main.dao.StudentDAO;
import main.utils.Constants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ApplyForCandidacyDialog extends JDialog {

    private final Student student;
    private final CandidateService candidateService;
    private final StudentDAO studentDAO;
    
    private JComboBox<Position> positionCombo;
    private JTextArea manifestoArea;
    private JComboBox<String> genderCombo;
    private JCheckBox residentCheck;

    public ApplyForCandidacyDialog(Frame parent, Student student) {
        super(parent, "Apply for Candidacy", true);
        this.student = student;
        this.candidateService = new CandidateService();
        this.studentDAO = new StudentDAO();
        setSize(480, 520);
        setLocationRelativeTo(parent);
        buildUI();
    }

    private void buildUI() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Constants.COLOR_BG);
        panel.setBorder(new EmptyBorder(20, 30, 20, 30));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.insets = new Insets(10, 0, 10, 0);

        int row = 0;
        
        // 1. Profile confirmation/update section
        JLabel verifyLabel = new JLabel("<html><b>Verify Profile Data</b><br>Certain roles require specific gender or residency.</html>");
        verifyLabel.setFont(Constants.FONT_SMALL);
        gbc.gridy = row++; panel.add(verifyLabel, gbc);

        genderCombo = new JComboBox<>(new String[]{"Select Gender", "MALE", "FEMALE", "OTHER"});
        genderCombo.setFont(Constants.FONT_BODY);
        genderCombo.setPreferredSize(new Dimension(0, 38));
        if (student.getGender() != null) {
            genderCombo.setSelectedItem(student.getGender());
        }
        gbc.gridy = row++; panel.add(makeLabel("Gender:"), gbc);
        gbc.gridy = row++; panel.add(genderCombo, gbc);

        gbc.insets = new Insets(8, 0, 8, 0);
        residentCheck = new JCheckBox("I am a Resident Student");
        residentCheck.setFont(Constants.FONT_BODY);
        residentCheck.setBackground(Constants.COLOR_BG);
        residentCheck.setSelected(student.isResident());
        gbc.gridy = row++; panel.add(residentCheck, gbc);

        gbc.gridy = row++; panel.add(new JSeparator(), gbc);
        gbc.gridy = row++; panel.add(Box.createVerticalStrut(5), gbc);

        // 2. Position selection
        positionCombo = new JComboBox<>();
        positionCombo.setFont(Constants.FONT_BODY);
        positionCombo.setPreferredSize(new Dimension(0, 38));
        loadPositions();
        
        gbc.gridy = row++; panel.add(makeLabel("Select Position:"), gbc);
        gbc.gridy = row++; panel.add(positionCombo, gbc);

        gbc.gridy = row++; panel.add(Box.createVerticalStrut(5), gbc);
        // 3. Manifesto
        gbc.gridy = row++; panel.add(makeLabel("Manifesto:"), gbc);
        manifestoArea = new JTextArea(4, 20);
        manifestoArea.setLineWrap(true);
        manifestoArea.setWrapStyleWord(true);
        JScrollPane scroll = new JScrollPane(manifestoArea);
        gbc.gridy = row++; panel.add(scroll, gbc);

        // 4. Submit
        JButton applyBtn = new JButton("SUBMIT APPLICATION");
        applyBtn.setFont(Constants.FONT_BUTTON);
        applyBtn.setBackground(Constants.COLOR_PRIMARY);
        applyBtn.setForeground(Color.WHITE);
        applyBtn.setPreferredSize(new Dimension(0, 42));
        applyBtn.addActionListener(e -> handleSubmit());

        gbc.gridy = row++;
        gbc.insets = new Insets(15, 0, 0, 0);
        panel.add(applyBtn, gbc);

        add(panel);
    }

    private void loadPositions() {
        try {
            Connection conn = DBConnection.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement(
                "SELECT p.*, f.faculty_name FROM positions p LEFT JOIN faculties f ON p.faculty_id = f.faculty_id ORDER BY p.position_name"
            );
            ResultSet rs = ps.executeQuery();

            // Custom renderer to format the items
            positionCombo.setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    if (value instanceof Position) {
                        Position p = (Position) value;
                        // Use a client property or we just need the faculty ID?
                        // Actually since Position has limited fields, we will extract faculty from local scope by fetching the ResultSet here.
                        // Or simply change the text
                    }
                    return this;
                }
            });

            while (rs.next()) {
                Position p = new Position(rs.getInt("position_id"), rs.getString("position_name"), rs.getInt("faculty_id"));
                String facName = rs.getString("faculty_name");
                if (facName != null && !facName.isEmpty()) {
                    // Create an inline override for toString returning the combination
                    p = new Position(p.getPositionId(), p.getPositionName(), p.getFacultyId()) {
                        @Override
                        public String toString() {
                            return super.getPositionName() + " (" + facName + ")";
                        }
                    };
                } else {
                    p = new Position(p.getPositionId(), p.getPositionName(), p.getFacultyId()) {
                        @Override
                        public String toString() {
                            return super.getPositionName() + " (ALL FACULTIES)";
                        }
                    };
                }
                positionCombo.addItem(p);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void handleSubmit() {
        String gender = (String) genderCombo.getSelectedItem();
        if (gender == null || gender.equals("Select Gender")) {
            JOptionPane.showMessageDialog(this, "Please select your gender before applying.");
            return;
        }

        boolean isResident = residentCheck.isSelected();

        // Update profile if changes made
        if (!gender.equals(student.getGender()) || isResident != student.isResident()) {
            boolean updated = studentDAO.updateStudentProfile(student.getStudentId(), gender, isResident);
            if (!updated) {
                JOptionPane.showMessageDialog(this, "Failed to update profile details.");
                return;
            }
            // Update local student model
            student.setGender(gender);
            student.setResident(isResident);
        }

        Position pos = (Position) positionCombo.getSelectedItem();
        if (pos == null) {
            JOptionPane.showMessageDialog(this, "Please select a position.");
            return;
        }

        boolean success = candidateService.applyForCandidacyStudent(student.getStudentId(), pos.getPositionId(), manifestoArea.getText(), pos);
        if (success) {
            JOptionPane.showMessageDialog(this, candidateService.getLastMessage(), "Success", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } else {
            JOptionPane.showMessageDialog(this, candidateService.getLastMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JLabel makeLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Constants.FONT_SMALL);
        label.setForeground(Constants.COLOR_TEXT);
        return label;
    }
}
