package main.utils;

public class DBConfig {
    public static final String HOST     = "localhost";
    public static final String PORT     = "3306";
    public static final String DATABASE = "chuka_voting_db";
    public static final String USERNAME = "root";
    public static final String PASSWORD = "@Kanyira432"; // ← Change this

    public static final String URL =
            "jdbc:mysql://" + HOST + ":" + PORT + "/" + DATABASE +
                    "?useSSL=false&serverTimezone=Africa/Nairobi&allowPublicKeyRetrieval=true";
}
