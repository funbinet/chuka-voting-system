package main.ui;

import main.ui.admin.AdminLoginFrame;
import main.ui.student.StudentLoginFrame;
import main.utils.Constants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class RoleSelectionFrame extends JFrame {

    public RoleSelectionFrame() {
        setTitle(Constants.APP_NAME + " — Welcome");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(480, 500);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(Constants.COLOR_BG);

        mainPanel.add(buildHeader(), BorderLayout.NORTH);
        mainPanel.add(buildSelectionPanel(), BorderLayout.CENTER);
        mainPanel.add(buildFooter(), BorderLayout.SOUTH);

        add(mainPanel);
        setVisible(true);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel();
        header.setBackground(Constants.COLOR_PRIMARY);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBorder(new EmptyBorder(25, 20, 25, 20));

        JLabel logoLabel = new JLabel("\uD83C\uDF13", JLabel.CENTER);
        logoLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 65));
        logoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel titleLabel = new JLabel("Chuka University");
        titleLabel.setFont(Constants.FONT_TITLE);
        titleLabel.setForeground(Constants.COLOR_WHITE);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subLabel = new JLabel("Student Voting System");
        subLabel.setFont(Constants.FONT_BODY);
        subLabel.setForeground(Constants.COLOR_ACCENT);
        subLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        header.add(logoLabel);
        header.add(Box.createVerticalStrut(8));
        header.add(titleLabel);
        header.add(Box.createVerticalStrut(4));
        header.add(subLabel);
        return header;
    }

    private JPanel buildSelectionPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Constants.COLOR_BG);
        panel.setBorder(new EmptyBorder(20, 40, 20, 40));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 0, 10, 0);
        gbc.gridx = 0;

        JLabel instruct = new JLabel("Please select your role to login:");
        instruct.setFont(Constants.FONT_BODY);
        instruct.setHorizontalAlignment(SwingConstants.CENTER);

        JButton studentBtn = makePrimaryButton("\uD83C\uDF13 Student Portal");
        studentBtn.addActionListener(e -> {
            dispose();
            new StudentLoginFrame();
        });

        JButton adminBtn = makeSecondaryButton("\uD83C\uDF13 Administrator Portal");
        adminBtn.addActionListener(e -> {
            dispose();
            new AdminLoginFrame();
        });

        gbc.gridy = 0;
        panel.add(instruct, gbc);
        gbc.gridy = 1;
        panel.add(Box.createVerticalStrut(10), gbc);
        gbc.gridy = 2;
        panel.add(studentBtn, gbc);
        gbc.gridy = 3;
        panel.add(adminBtn, gbc);

        return panel;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel();
        footer.setBackground(Constants.COLOR_PRIMARY);
        footer.setBorder(new EmptyBorder(8, 0, 8, 0));
        JLabel label = new JLabel("© 2026 Chuka University");
        label.setFont(Constants.FONT_SMALL);
        label.setForeground(new Color(200, 200, 200));
        footer.add(label);
        return footer;
    }

    private JButton makePrimaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(Constants.FONT_BUTTON);
        btn.setBackground(Constants.COLOR_PRIMARY);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setPreferredSize(new Dimension(0, 50));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JButton makeSecondaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(Constants.FONT_BUTTON);
        btn.setBackground(new Color(23, 32, 42)); // Dark theme button
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setPreferredSize(new Dimension(0, 50));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }
}
