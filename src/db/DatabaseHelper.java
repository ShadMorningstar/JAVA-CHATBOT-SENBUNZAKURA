package db;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class to handle database persistence layer operations for the chat application.
 * Manages database connections, entity initializations, and CRUD queries for user sessions.
 */
public class DatabaseHelper {

    private static final String URL = "jdbc:mysql://localhost:3306/senbunzakura_db?useSSL=false&allowPublicKeyRetrieval=true";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    private static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (Exception e) {
            // Driver class configuration fallthrough
        }
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    /**
     * Assures that all transactional database schemas exist on runtime startup.
     * Generates persistent structure tables dynamically if they are missing.
     */
    private static void checkAndCreateTable() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {

            // Core dictionary layer for key-value memory mapping
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS important_memory (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    info_key VARCHAR(250) UNIQUE,
                    info_val TEXT
                );
                """);

            // Multi-Session transaction log mapping
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS chat_sessions (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    session_id VARCHAR(50),
                    sender VARCHAR(50),
                    message TEXT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                );
                """);
        } catch (SQLException e) {
            System.out.println("Schema Initialization Exception: Failure to check/create structures. Details: " + e.getMessage());
        }
    }

    public static void saveMemory(String key, String val) {
        checkAndCreateTable();
        String query = "INSERT INTO important_memory (info_key, info_val) VALUES (?, ?) ON DUPLICATE KEY UPDATE info_val = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, key);
            pstmt.setString(2, val);
            pstmt.setString(3, val);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static Map<String, String> loadMemory() {
        checkAndCreateTable();
        Map<String, String> memories = new HashMap<>();
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT info_key, info_val FROM important_memory")) {
            while (rs.next()) {
                memories.put(rs.getString("info_key"), rs.getString("info_val"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return memories;
    }

    /**
     * Appends an individual message instance associated with a targeted session token into persistence.
     */
    public static void saveChatMessage(String sessionId, String sender, String message) {
        checkAndCreateTable();
        String query = "INSERT INTO chat_sessions (session_id, sender, message) VALUES (?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, sessionId);
            pstmt.setString(2, sender);
            pstmt.setString(3, message);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Fetches all registered conversation tokens grouped internally to prevent redundant index traversal.
     * Orders records dynamically based on the latest activity timestamp.
     */
    public static List<String> getAllSessions() {
        checkAndCreateTable();
        List<String> sessions = new ArrayList<>();

        // Group aggregation query utilized to structure sequencing constraints cleanly
        String query = "SELECT session_id FROM chat_sessions GROUP BY session_id ORDER BY MAX(id) DESC";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                sessions.add(rs.getString("session_id"));
            }
        } catch (SQLException e) {
            System.out.println("Data Pipeline Alert: Failed to compile current active session history. Details: " + e.getMessage());
            e.printStackTrace();
        }
        return sessions;
    }

    /**
     * Extracts sequentially arranged message data packages filtering exclusively by the session unique identifier.
     */
    public static List<String[]> getSessionHistory(String sessionId) {
        checkAndCreateTable();
        List<String[]> history = new ArrayList<>();
        String query = "SELECT sender, message FROM chat_sessions WHERE session_id = ? ORDER BY id ASC";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, sessionId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    history.add(new String[]{rs.getString("sender"), rs.getString("message")});
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return history;
    }

    /**
     * Purges records linked to a targeted context thread identifier cleanly from storage tracking.
     */
    public static void deleteSession(String sessionId) {
        checkAndCreateTable();
        String query = "DELETE FROM chat_sessions WHERE session_id = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, sessionId);
            pstmt.executeUpdate();
            System.out.println("Data Pipeline Sync: Successfully removed session target entry from storage framework: " + sessionId);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Executes a complete structural cleanup of database indexes resetting session metrics tracking.
     */
    public static void clearAllSessions() {
        checkAndCreateTable();
        String query = "TRUNCATE TABLE chat_sessions";
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(query);
            System.out.println("Data Pipeline Sync: Master system reset processed. Transaction logs flushed cleanly.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}