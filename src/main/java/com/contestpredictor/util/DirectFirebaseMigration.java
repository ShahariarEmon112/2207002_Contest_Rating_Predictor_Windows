package com.contestpredictor.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.StringEntity;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.*;
import java.util.*;

/**
 * Direct SQLite to Firestore Migration Utility
 * Fetches all data from SQLite using rs.next() and uploads directly to Firestore
 * Run this as a standalone Java application: java DirectFirebaseMigration
 */
public class DirectFirebaseMigration {
    
    private static final String SQLITE_DB = "contest_predictor.db";
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static String projectId;
    private static String firestoreBaseUrl;
    
    public static void main(String[] args) {
        System.out.println("=== Direct SQLite to Firestore Migration ===\n");
        
        try {
            // Load Firebase configuration
            loadFirebaseConfig();
            
            // Establish database connection
            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + SQLITE_DB)) {
                System.out.println("✓ Connected to SQLite database\n");
                
                // Migrate all tables (matching actual database schema)
                migrateUsers(conn);
                migrateUserSessions(conn);  // contest_registrations
                migrateAdmins(conn);
                migrateContests(conn);
                migrateContestParticipants(conn);  // participants
                migrateLeaderboardContests(conn);
                migrateLeaderboardParticipants(conn);  // leaderboard_registrations
                migrateLeaderboardRankings(conn);  // leaderboard_entries + combined_leaderboard
                migrateRatingPredictions(conn);  // rating_history
                migratePasswordResetOtps(conn);  // if exists
                
                System.out.println("\n✓✓✓ Migration completed successfully! ✓✓✓");
                
            } catch (SQLException e) {
                System.err.println("Database error: " + e.getMessage());
                e.printStackTrace();
            }
            
        } catch (Exception e) {
            System.err.println("Migration failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void loadFirebaseConfig() throws IOException {
        Properties props = new Properties();
        
        // Try to load from classpath first (resources folder)
        try (var inputStream = DirectFirebaseMigration.class.getClassLoader()
                .getResourceAsStream("firebase.properties")) {
            if (inputStream != null) {
                props.load(inputStream);
            } else {
                // Fallback: try loading from current directory
                props.load(new FileInputStream("firebase.properties"));
            }
        } catch (IOException e) {
            // Last attempt: try from current directory
            props.load(new FileInputStream("firebase.properties"));
        }
        
        // Get API key first (needed for authentication)
        String apiKey = props.getProperty("FIREBASE_WEB_API_KEY");
        if (apiKey == null || apiKey.trim().isEmpty() || apiKey.equals("YOUR_WEB_API_KEY_HERE")) {
            throw new RuntimeException("FIREBASE_WEB_API_KEY not configured in firebase.properties");
        }
        
        projectId = props.getProperty("firebase.project.id");
        if (projectId == null || projectId.trim().isEmpty()) {
            throw new RuntimeException("firebase.project.id not found in firebase.properties");
        }
        
        firestoreBaseUrl = "https://firestore.googleapis.com/v1/projects/" + projectId + "/databases/(default)/documents";
        
        System.out.println("✓ Firebase Project ID: " + projectId);
        System.out.println("✓ Web API Key: " + apiKey.substring(0, 10) + "..." + " (configured)");
        System.out.println("✓ Firestore URL: " + firestoreBaseUrl + "\n");
    }
    
    private static void migrateUsers(Connection conn) throws SQLException, IOException {
        System.out.println("Migrating users table...");
        String query = "SELECT * FROM users";
        int count = 0;
        
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            while (rs.next()) {
                Map<String, Object> fields = new LinkedHashMap<>();
                fields.put("username", createStringValue(rs.getString("username")));
                fields.put("password", createStringValue(rs.getString("password")));
                fields.put("fullName", createStringValue(rs.getString("full_name")));
                fields.put("currentRating", createIntegerValue(rs.getInt("current_rating")));
                fields.put("contestsParticipated", createIntegerValue(rs.getInt("contests_participated")));
                fields.put("ratingHistory", createStringValue(rs.getString("rating_history")));
                
                String email = rs.getString("email");
                if (email != null) fields.put("email", createStringValue(email));
                
                String firebaseUid = rs.getString("firebase_uid");
                if (firebaseUid != null) fields.put("firebaseUid", createStringValue(firebaseUid));
                
                String docId = sanitizeDocumentId(rs.getString("username"));
                uploadToFirestore("users", docId, fields);
                count++;
            }
        }
        
        System.out.println("✓ Migrated " + count + " users\n");
    }
    
    private static void migrateUserSessions(Connection conn) throws SQLException, IOException {
        System.out.println("Migrating contest_registrations table...");
        String query = "SELECT * FROM contest_registrations";
        int count = 0;
        
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            while (rs.next()) {
                Map<String, Object> fields = new LinkedHashMap<>();
                fields.put("id", createIntegerValue(rs.getInt("id")));
                fields.put("contestId", createStringValue(rs.getString("contest_id")));
                fields.put("username", createStringValue(rs.getString("username")));
                fields.put("registeredAt", createStringValue(rs.getString("registered_at")));
                
                String docId = "reg_" + rs.getInt("id");
                uploadToFirestore("contest_registrations", docId, fields);
                count++;
            }
        }
        
        System.out.println("✓ Migrated " + count + " contest registrations\n");
    }
    
    private static void migrateAdmins(Connection conn) throws SQLException, IOException {
        System.out.println("Migrating admins table...");
        String query = "SELECT * FROM admins";
        int count = 0;
        
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            while (rs.next()) {
                Map<String, Object> fields = new LinkedHashMap<>();
                fields.put("adminId", createStringValue(rs.getString("admin_id")));
                fields.put("username", createStringValue(rs.getString("username")));
                fields.put("password", createStringValue(rs.getString("password")));
                
                String email = rs.getString("email");
                if (email != null) fields.put("email", createStringValue(email));
                
                fields.put("fullName", createStringValue(rs.getString("full_name")));
                fields.put("createdAt", createStringValue(rs.getString("created_at")));
                fields.put("isActive", createIntegerValue(rs.getInt("is_active")));
                
                String docId = sanitizeDocumentId(rs.getString("username"));
                uploadToFirestore("admins", docId, fields);
                count++;
            }
        }
        
        System.out.println("✓ Migrated " + count + " admins\n");
    }
    
    private static void migrateContests(Connection conn) throws SQLException, IOException {
        System.out.println("Migrating contests table...");
        String query = "SELECT * FROM contests";
        int count = 0;
        
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            while (rs.next()) {
                Map<String, Object> fields = new LinkedHashMap<>();
                fields.put("contestId", createStringValue(rs.getString("contest_id")));
                fields.put("contestName", createStringValue(rs.getString("contest_name")));
                fields.put("dateTime", createStringValue(rs.getString("date_time")));
                fields.put("duration", createIntegerValue(rs.getInt("duration")));
                fields.put("isPast", createIntegerValue(rs.getInt("is_past")));
                
                String createdBy = rs.getString("created_by_admin");
                if (createdBy != null) fields.put("createdByAdmin", createStringValue(createdBy));
                
                fields.put("maxParticipants", createIntegerValue(rs.getInt("max_participants")));
                fields.put("registrationOpen", createIntegerValue(rs.getInt("registration_open")));
                
                String docId = sanitizeDocumentId(rs.getString("contest_id"));
                uploadToFirestore("contests", docId, fields);
                count++;
            }
        }
        
        System.out.println("✓ Migrated " + count + " contests\n");
    }
    
    private static void migrateContestParticipants(Connection conn) throws SQLException, IOException {
        System.out.println("Migrating participants table...");
        String query = "SELECT * FROM participants";
        int count = 0;
        
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            while (rs.next()) {
                Map<String, Object> fields = new LinkedHashMap<>();
                fields.put("id", createIntegerValue(rs.getInt("id")));
                fields.put("contestId", createStringValue(rs.getString("contest_id")));
                fields.put("username", createStringValue(rs.getString("username")));
                fields.put("currentRating", createIntegerValue(rs.getInt("current_rating")));
                fields.put("problemsSolved", createIntegerValue(rs.getInt("problems_solved")));
                fields.put("totalPenalty", createIntegerValue(rs.getInt("total_penalty")));
                fields.put("rank", createIntegerValue(rs.getInt("rank")));
                fields.put("predictedRating", createIntegerValue(rs.getInt("predicted_rating")));
                fields.put("ratingChange", createIntegerValue(rs.getInt("rating_change")));
                
                String docId = "part_" + rs.getInt("id");
                uploadToFirestore("participants", docId, fields);
                count++;
            }
        }
        
        System.out.println("✓ Migrated " + count + " participants\n");
    }
    
    private static void migrateLeaderboardContests(Connection conn) throws SQLException, IOException {
        System.out.println("Migrating leaderboard_contests table...");
        String query = "SELECT * FROM leaderboard_contests";
        int count = 0;
        
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            while (rs.next()) {
                Map<String, Object> fields = new LinkedHashMap<>();
                fields.put("contestId", createStringValue(rs.getString("contest_id")));
                fields.put("contestName", createStringValue(rs.getString("contest_name")));
                
                String desc = rs.getString("description");
                if (desc != null) fields.put("description", createStringValue(desc));
                
                fields.put("startDate", createStringValue(rs.getString("start_date")));
                fields.put("endDate", createStringValue(rs.getString("end_date")));
                fields.put("maxProblems", createIntegerValue(rs.getInt("max_problems")));
                fields.put("isActive", createIntegerValue(rs.getInt("is_active")));
                fields.put("standingsFinalized", createIntegerValue(rs.getInt("standings_finalized")));
                fields.put("createdByAdmin", createStringValue(rs.getString("created_by_admin")));
                fields.put("createdAt", createStringValue(rs.getString("created_at")));
                
                String docId = sanitizeDocumentId(rs.getString("contest_id"));
                uploadToFirestore("leaderboard_contests", docId, fields);
                count++;
            }
        }
        
        System.out.println("✓ Migrated " + count + " leaderboard contests\n");
    }
    
    private static void migrateLeaderboardParticipants(Connection conn) throws SQLException, IOException {
        System.out.println("Migrating leaderboard_registrations table...");
        String query = "SELECT * FROM leaderboard_registrations";
        int count = 0;
        
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            while (rs.next()) {
                Map<String, Object> fields = new LinkedHashMap<>();
                fields.put("id", createIntegerValue(rs.getInt("id")));
                fields.put("contestId", createStringValue(rs.getString("contest_id")));
                fields.put("username", createStringValue(rs.getString("username")));
                fields.put("registeredAt", createStringValue(rs.getString("registered_at")));
                
                String docId = "lbreg_" + rs.getInt("id");
                uploadToFirestore("leaderboard_registrations", docId, fields);
                count++;
            }
        }
        
        System.out.println("✓ Migrated " + count + " leaderboard registrations\n");
    }
    
    private static void migrateLeaderboardRankings(Connection conn) throws SQLException, IOException {
        System.out.println("Migrating leaderboard_entries table...");
        String query = "SELECT * FROM leaderboard_entries";
        int count = 0;
        
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            while (rs.next()) {
                Map<String, Object> fields = new LinkedHashMap<>();
                fields.put("id", createIntegerValue(rs.getInt("id")));
                fields.put("contestId", createStringValue(rs.getString("contest_id")));
                fields.put("username", createStringValue(rs.getString("username")));
                fields.put("rank", createIntegerValue(rs.getInt("rank")));
                fields.put("solveCount", createIntegerValue(rs.getInt("solve_count")));
                fields.put("totalPenalty", createIntegerValue(rs.getInt("total_penalty")));
                fields.put("totalTime", createIntegerValue(rs.getInt("total_time")));
                fields.put("status", createStringValue(rs.getString("status")));
                
                String docId = "lbentry_" + rs.getInt("id");
                uploadToFirestore("leaderboard_entries", docId, fields);
                count++;
            }
        }
        
        System.out.println("✓ Migrated " + count + " leaderboard entries\n");
        
        // Also migrate combined leaderboard
        migrateCombinedLeaderboard(conn);
    }
    
    private static void migrateCombinedLeaderboard(Connection conn) throws SQLException, IOException {
        System.out.println("Migrating combined_leaderboard table...");
        String query = "SELECT * FROM combined_leaderboard";
        int count = 0;
        
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            while (rs.next()) {
                Map<String, Object> fields = new LinkedHashMap<>();
                fields.put("id", createIntegerValue(rs.getInt("id")));
                fields.put("username", createStringValue(rs.getString("username")));
                fields.put("totalSolves", createIntegerValue(rs.getInt("total_solves")));
                fields.put("totalPenalty", createIntegerValue(rs.getInt("total_penalty")));
                fields.put("overallRank", createIntegerValue(rs.getInt("overall_rank")));
                fields.put("contestsParticipated", createIntegerValue(rs.getInt("contests_participated")));
                fields.put("status", createStringValue(rs.getString("status")));
                
                String lastUpdated = rs.getString("last_updated");
                if (lastUpdated != null) fields.put("lastUpdated", createStringValue(lastUpdated));
                
                String docId = sanitizeDocumentId(rs.getString("username"));
                uploadToFirestore("combined_leaderboard", docId, fields);
                count++;
            }
        }
        
        System.out.println("✓ Migrated " + count + " combined leaderboard entries\n");
    }
    
    private static void migrateRatingPredictions(Connection conn) throws SQLException, IOException {
        System.out.println("Migrating rating_history table...");
        String query = "SELECT * FROM rating_history";
        int count = 0;
        
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            while (rs.next()) {
                Map<String, Object> fields = new LinkedHashMap<>();
                fields.put("id", createIntegerValue(rs.getInt("id")));
                fields.put("contestId", createStringValue(rs.getString("contest_id")));
                fields.put("username", createStringValue(rs.getString("username")));
                fields.put("oldRating", createIntegerValue(rs.getInt("old_rating")));
                fields.put("newRating", createIntegerValue(rs.getInt("new_rating")));
                fields.put("delta", createIntegerValue(rs.getInt("delta")));
                fields.put("contestDate", createStringValue(rs.getString("contest_date")));
                
                String docId = "history_" + rs.getInt("id");
                uploadToFirestore("rating_history", docId, fields);
                count++;
            }
        }
        
        System.out.println("✓ Migrated " + count + " rating history records\n");
    }
    
    private static void migratePasswordResetOtps(Connection conn) throws SQLException, IOException {
        System.out.println("Checking for password_reset_otps table...");
        
        // Check if table exists
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='password_reset_otps'")) {
            
            if (!rs.next()) {
                System.out.println("  (Table doesn't exist - skipping)\n");
                return;
            }
        }
        
        String query = "SELECT * FROM password_reset_otps";
        int count = 0;
        
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            while (rs.next()) {
                Map<String, Object> fields = new LinkedHashMap<>();
                fields.put("id", createIntegerValue(rs.getInt("id")));
                fields.put("email", createStringValue(rs.getString("email")));
                fields.put("otp", createStringValue(rs.getString("otp")));
                fields.put("createdAt", createStringValue(rs.getString("created_at")));
                fields.put("expiresAt", createStringValue(rs.getString("expires_at")));
                fields.put("used", createBooleanValue(rs.getBoolean("used")));
                
                String docId = sanitizeDocumentId(rs.getString("email"));
                uploadToFirestore("password_reset_otps", docId, fields);
                count++;
            }
        }
        
        System.out.println("✓ Migrated " + count + " password reset OTPs\n");
    }
    
    /**
     * Upload data to Firestore using REST API
     */
    private static void uploadToFirestore(String collection, String documentId, Map<String, Object> fields) throws IOException {
        String url = firestoreBaseUrl + "/" + collection + "/" + documentId;
        
        Map<String, Object> document = new LinkedHashMap<>();
        document.put("fields", fields);
        
        String jsonData = gson.toJson(document);
        
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost request = new HttpPost(url);
            request.setEntity(new StringEntity(jsonData, "UTF-8"));
            request.setHeader("Content-Type", "application/json");
            
            try (CloseableHttpResponse response = client.execute(request)) {
                int statusCode = response.getCode();
                if (statusCode >= 200 && statusCode < 300) {
                    // Success - document created
                } else {
                    System.err.println("  ✗ Firestore upload failed for " + collection + "/" + documentId + ": " + statusCode);
                }
            }
        }
    }
    
    /**
     * Create Firestore string value
     */
    private static Map<String, Object> createStringValue(String value) {
        Map<String, Object> field = new LinkedHashMap<>();
        field.put("stringValue", value == null ? "" : value);
        return field;
    }
    
    /**
     * Create Firestore integer value
     */
    private static Map<String, Object> createIntegerValue(long value) {
        Map<String, Object> field = new LinkedHashMap<>();
        field.put("integerValue", String.valueOf(value));
        return field;
    }
    
    /**
     * Create Firestore boolean value
     */
    private static Map<String, Object> createBooleanValue(boolean value) {
        Map<String, Object> field = new LinkedHashMap<>();
        field.put("booleanValue", value);
        return field;
    }
    
    /**
     * Sanitize document ID for Firestore (replace invalid characters)
     */
    private static String sanitizeDocumentId(String id) {
        if (id == null) return "null";
        // Firestore document IDs must not contain: / (but allows most other characters)
        return id.replaceAll("/", "_");
    }
    
    /**
     * Get nullable integer from ResultSet
     */
    private static Integer getNullableInt(ResultSet rs, String columnName) throws SQLException {
        int value = rs.getInt(columnName);
        return rs.wasNull() ? null : value;
    }
    
    /**
     * Get nullable double from ResultSet
     */
    private static Double getNullableDouble(ResultSet rs, String columnName) throws SQLException {
        double value = rs.getDouble(columnName);
        return rs.wasNull() ? null : value;
    }
}
