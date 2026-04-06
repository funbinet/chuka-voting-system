package main.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class DBConfig {
    private static final Properties properties = new Properties();

    static {
        try (InputStream input = DBConfig.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input != null) {
                properties.load(input);
            }
        } catch (IOException e) {
            System.err.println("Warning: Could not load config.properties for DB configuration.");
        }
    }

    public static final String HOST = get("DB_HOST", "db.host", "localhost");
    public static final String PORT = get("DB_PORT", "db.port", "3306");
    public static final String DATABASE = get("DB_NAME", "db.name", "chuka_voting_db");
    public static final String USERNAME = get("DB_USER", "db.user", "root");
    public static final String PASSWORD = get("DB_PASSWORD", "db.password", "");

    public static final String URL = "jdbc:mysql://" + HOST + ":" + PORT + "/" + DATABASE +
            "?useSSL=false&serverTimezone=Africa/Nairobi&allowPublicKeyRetrieval=true";

    private static String get(String envKey, String propertyKey, String defaultValue) {
        // 1. Check Environment Variables (used by Docker)
        String value = System.getenv(envKey);
        
        // 2. Check config.properties file
        if (value == null || value.isBlank()) {
            value = properties.getProperty(propertyKey);
        }

        // 3. Check JVM System Properties
        if (value == null || value.isBlank()) {
            value = System.getProperty(propertyKey);
        }

        return (value == null || value.isBlank()) ? defaultValue : value;
    }
}
