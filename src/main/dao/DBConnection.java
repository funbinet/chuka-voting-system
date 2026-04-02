package main.dao;

import main.utils.DBConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    private static DBConnection instance;
    private Connection connection;
    private String lastError;

    private DBConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            this.connection = DriverManager.getConnection(
                    DBConfig.URL,
                    DBConfig.USERNAME,
                    DBConfig.PASSWORD
            );
            this.lastError = null;
            System.out.println("Database connected successfully.");
        } catch (ClassNotFoundException e) {
            this.lastError = "MySQL JDBC driver not found. Ensure mysql-connector-j is on the classpath.";
            System.err.println(this.lastError);
        } catch (SQLException e) {
            this.lastError = "Database connection failed: " + e.getMessage();
            System.err.println(this.lastError);
        }
    }

    public static DBConnection getInstance() {
        if (instance == null || isConnectionClosed()) {
            instance = new DBConnection();
        }
        return instance;
    }

    private static boolean isConnectionClosed() {
        if (instance == null || instance.connection == null) return true;
        try {
            return instance.connection.isClosed();
        } catch (SQLException e) {
            return true;
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public String getLastError() {
        return lastError;
    }



    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Connection closed.");
            }
        } catch (SQLException e) {
            System.err.println("Error closing connection: " + e.getMessage());
        }
    }
}
