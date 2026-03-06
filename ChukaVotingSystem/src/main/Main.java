package main;

import main.dao.DBConnection;
import main.ui.LoginFrame;
import main.utils.Constants;

import javax.swing.*;

public class Main {

    public static void main(String[] args) {

        // Set Nimbus look and feel for better UI
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            // Use default if Nimbus not available
        }

        // Test DB connection
        DBConnection db = DBConnection.getInstance();
        if (db.getConnection() == null) {
            JOptionPane.showMessageDialog(null,
                "❌ Could not connect to database!\n" +
                "Please check DBConfig.java and ensure MySQL is running.",
                "Database Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        System.out.println("🎓 Starting " + Constants.APP_NAME + " v" + Constants.APP_VERSION);

        // Launch the Login Screen on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> new LoginFrame());
    }
}
