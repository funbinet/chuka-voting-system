package main.dao;

import main.utils.DBConfig;
import main.utils.PositionRules;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.EnumMap;
import java.util.Map;

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
            runSchemaMigrations();
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

    private void runSchemaMigrations() {
        if (connection == null) {
            return;
        }

        try {
            ensurePositionsTable();
            Map<PositionRules.PositionCategory, Integer> canonicalIds = ensureCanonicalPositions();
            ensureElectionsPositionColumn(canonicalIds);
        } catch (SQLException e) {
            System.err.println("⚠ Schema migration warning: " + e.getMessage());
        }
    }

    private void ensurePositionsTable() throws SQLException {
        if (tableExists("positions")) {
            return;
        }

        try (Statement st = connection.createStatement()) {
            st.executeUpdate("CREATE TABLE positions (" +
                    "position_id INT PRIMARY KEY AUTO_INCREMENT," +
                    "position_name VARCHAR(100) NOT NULL," +
                    "faculty_id INT NULL)");
        }

        if (tableExists("faculties")) {
            try (Statement st = connection.createStatement()) {
                st.executeUpdate("ALTER TABLE positions ADD CONSTRAINT fk_positions_faculty " +
                        "FOREIGN KEY (faculty_id) REFERENCES faculties(faculty_id)");
            } catch (SQLException ignored) {
                // Constraint may already exist in another environment.
            }
        }
    }

    private Map<PositionRules.PositionCategory, Integer> ensureCanonicalPositions() throws SQLException {
        Map<PositionRules.PositionCategory, Integer> ids = new EnumMap<>(PositionRules.PositionCategory.class);

        if (!tableExists("positions")) {
            return ids;
        }

        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("SELECT position_id, position_name FROM positions ORDER BY position_id ASC")) {
            while (rs.next()) {
                PositionRules.PositionCategory category = PositionRules.classify(rs.getString("position_name"));
                if (PositionRules.isCanonical(category) && !ids.containsKey(category)) {
                    ids.put(category, rs.getInt("position_id"));
                }
            }
        }

        String insertSql = "INSERT INTO positions (position_name, faculty_id) VALUES (?, ?)";
        for (PositionRules.PositionCategory category : PositionRules.CANONICAL_ORDER) {
            if (ids.containsKey(category)) {
                continue;
            }

            try (PreparedStatement ps = connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, PositionRules.canonicalLabel(category));
                ps.setNull(2, Types.INTEGER);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        ids.put(category, keys.getInt(1));
                    }
                }
            }
        }

        return ids;
    }

    private void ensureElectionsPositionColumn(Map<PositionRules.PositionCategory, Integer> canonicalIds) throws SQLException {
        if (!tableExists("elections")) {
            return;
        }

        if (!columnExists("elections", "position_id")) {
            try (Statement st = connection.createStatement()) {
                st.executeUpdate("ALTER TABLE elections ADD COLUMN position_id INT NULL AFTER faculty_id");
            }
        }

        if (!columnExists("elections", "position_id")) {
            return;
        }

        try (Statement st = connection.createStatement()) {
            st.executeUpdate("UPDATE elections e " +
                    "JOIN (" +
                    "  SELECT ec.election_id, MIN(ca.position_id) AS inferred_position_id " +
                    "  FROM election_candidates ec " +
                    "  JOIN candidate_applications ca ON ca.application_id = ec.application_id " +
                    "  GROUP BY ec.election_id" +
                    ") ep ON ep.election_id = e.election_id " +
                    "SET e.position_id = ep.inferred_position_id " +
                    "WHERE e.position_id IS NULL");
        } catch (SQLException ignored) {
            // Legacy environments may not have all related tables populated; title-based fallback handles remaining rows.
        }

        String selectNullSql = "SELECT election_id, title FROM elections WHERE position_id IS NULL";
        String updateSql = "UPDATE elections SET position_id = ? WHERE election_id = ?";
        try (PreparedStatement select = connection.prepareStatement(selectNullSql);
             PreparedStatement update = connection.prepareStatement(updateSql);
             ResultSet rs = select.executeQuery()) {
            while (rs.next()) {
                PositionRules.PositionCategory category = PositionRules.classify(rs.getString("title"));
                Integer positionId = canonicalIds.get(category);
                if (positionId == null) {
                    continue;
                }
                update.setInt(1, positionId);
                update.setInt(2, rs.getInt("election_id"));
                update.addBatch();
            }
            update.executeBatch();
        }

        Integer defaultPositionId = canonicalIds.get(PositionRules.PositionCategory.FACULTY_CHAIRMAN);
        if (defaultPositionId == null && !canonicalIds.isEmpty()) {
            defaultPositionId = canonicalIds.values().iterator().next();
        }

        if (defaultPositionId != null) {
            try (PreparedStatement ps = connection.prepareStatement(
                    "UPDATE elections SET position_id = ? WHERE position_id IS NULL")) {
                ps.setInt(1, defaultPositionId);
                ps.executeUpdate();
            }
        }

        try (Statement st = connection.createStatement()) {
            st.executeUpdate("CREATE INDEX idx_elections_position_id ON elections(position_id)");
        } catch (SQLException ignored) {
            // Index may already exist.
        }
    }

    private boolean tableExists(String tableName) throws SQLException {
        String sql = "SELECT COUNT(*) FROM information_schema.tables " +
                "WHERE table_schema = DATABASE() AND table_name = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, tableName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private boolean columnExists(String tableName, String columnName) throws SQLException {
        String sql = "SELECT COUNT(*) FROM information_schema.columns " +
                "WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, tableName);
            ps.setString(2, columnName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
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
