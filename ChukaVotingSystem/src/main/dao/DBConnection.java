package main.dao;

import main.utils.DBConfig;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    private static DBConnection instance;
    private Connection connection;

    private DBConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            this.connection = DriverManager.getConnection(
                DBConfig.URL,
                DBConfig.USERNAME,
                DBConfig.PASSWORD
            );
            System.out.println("✅ Database connected successfully!");
        } catch (ClassNotFoundException e) {
            System.err.println("❌ MySQL Driver not found: " + e.getMessage());
        } catch (SQLException e) {
            System.err.println("❌ Database connection failed: " + e.getMessage());
        }
    }

    public static DBConnection getInstance() {
        if (instance == null || isConnectionClosed()) {
            instance = new DBConnection();
        }
        return instance;
    }

    public Connection getConnection() {
        return connection;
    }

    private static boolean isConnectionClosed() {
        try {
            return instance.connection == null || instance.connection.isClosed();
        } catch (SQLException e) {
            return true;
        }
    }

    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("🔌 Connection closed.");
            }
        } catch (SQLException e) {
            System.err.println("❌ Error closing connection: " + e.getMessage());
        }
    }
}
