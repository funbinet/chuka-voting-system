package main.utils;

public class DBConfig {
    public static final String HOST = get("DB_HOST", "db.host", "localhost");
    public static final String PORT = get("DB_PORT", "db.port", "3306");
    public static final String DATABASE = get("DB_NAME", "db.name", "chuka_voting_db");
    public static final String USERNAME = get("DB_USER", "db.user", "root");
    public static final String PASSWORD = get("DB_PASSWORD", "db.password", "@Kanyira432");

    public static final String URL = "jdbc:mysql://" + HOST + ":" + PORT + "/" + DATABASE +
            "?useSSL=false&serverTimezone=Africa/Nairobi&allowPublicKeyRetrieval=true";

    private static String get(String envKey, String propertyKey, String defaultValue) {
        String value = System.getenv(envKey);
        if (value == null || value.isBlank()) {
            value = System.getProperty(propertyKey);
        }
        return (value == null || value.isBlank()) ? defaultValue : value;
    }
}
