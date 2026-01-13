package com.contestpredictor.util;

import com.contestpredictor.data.DatabaseManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Utility to export SQLite database to JSON files for Firebase migration
 * Exports: users, sessions, and other data to temporary JSON files
 */
public class SQLiteToFirebaseMigration {
    
    private final DatabaseManager dbManager;
    private final Gson gson;
    
    public SQLiteToFirebaseMigration() {
        this.dbManager = DatabaseManager.getInstance();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }
    
    /**
     * Export all users to JSON file
     * @param outputPath Path to save JSON file (e.g., "temp/users_export.json")
     * @return true if successful
     */
    public boolean exportUsersToJson(String outputPath) {
        JsonObject root = new JsonObject();
        JsonObject usersData = new JsonObject();
        
        String query = "SELECT username, password, full_name, current_rating, contests_participated, " +
                      "email, firebase_uid FROM users";
        
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            int count = 0;
            while (rs.next()) {
                String username = rs.getString("username");
                
                JsonObject userData = new JsonObject();
                userData.addProperty("username", username);
                userData.addProperty("password", rs.getString("password"));
                userData.addProperty("fullName", rs.getString("full_name"));
                userData.addProperty("currentRating", rs.getInt("current_rating"));
                userData.addProperty("contestsParticipated", rs.getInt("contests_participated"));
                userData.addProperty("email", rs.getString("email"));
                userData.addProperty("firebaseUid", rs.getString("firebase_uid"));
                userData.addProperty("exportedAt", System.currentTimeMillis());
                
                // Get rating history
                JsonArray ratingHistory = getRatingHistory(username);
                userData.add("ratingHistory", ratingHistory);
                
                usersData.add(username, userData);
                count++;
            }
            
            root.add("users", usersData);
            root.addProperty("totalUsers", count);
            root.addProperty("exportTimestamp", System.currentTimeMillis());
            root.addProperty("exportDate", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            // Write to file
            try (FileWriter writer = new FileWriter(outputPath)) {
                gson.toJson(root, writer);
                System.out.println("✓ Exported " + count + " users to: " + outputPath);
                return true;
            }
            
        } catch (SQLException | IOException e) {
            System.err.println("Error exporting users: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Export all sessions to JSON file
     * @param outputPath Path to save JSON file
     * @return true if successful
     */
    public boolean exportSessionsToJson(String outputPath) {
        JsonObject root = new JsonObject();
        JsonObject sessionsData = new JsonObject();
        
        String query = "SELECT username, firebase_uid, email, id_token, refresh_token, " +
                      "token_expiration, last_login, remember_me FROM user_sessions";
        
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            int count = 0;
            while (rs.next()) {
                String username = rs.getString("username");
                
                JsonObject sessionData = new JsonObject();
                sessionData.addProperty("username", username);
                sessionData.addProperty("firebaseUid", rs.getString("firebase_uid"));
                sessionData.addProperty("email", rs.getString("email"));
                sessionData.addProperty("idToken", rs.getString("id_token"));
                sessionData.addProperty("refreshToken", rs.getString("refresh_token"));
                sessionData.addProperty("tokenExpiration", rs.getLong("token_expiration"));
                sessionData.addProperty("lastLogin", rs.getLong("last_login"));
                sessionData.addProperty("rememberMe", rs.getBoolean("remember_me"));
                
                // Sanitize username for Firebase key
                String sanitizedUsername = username.replace(".", "_dot_").replace("@", "_at_");
                sessionsData.add(sanitizedUsername, sessionData);
                count++;
            }
            
            root.add("sessions", sessionsData);
            root.addProperty("totalSessions", count);
            root.addProperty("exportTimestamp", System.currentTimeMillis());
            
            try (FileWriter writer = new FileWriter(outputPath)) {
                gson.toJson(root, writer);
                System.out.println("✓ Exported " + count + " sessions to: " + outputPath);
                return true;
            }
            
        } catch (SQLException | IOException e) {
            System.err.println("Error exporting sessions: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Get rating history for a user
     */
    private JsonArray getRatingHistory(String username) {
        JsonArray history = new JsonArray();
        
        String query = "SELECT rating_history FROM users WHERE username = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                String historyStr = rs.getString("rating_history");
                if (historyStr != null && !historyStr.isEmpty()) {
                    String[] ratings = historyStr.split(",");
                    for (String rating : ratings) {
                        try {
                            history.add(Integer.parseInt(rating.trim()));
                        } catch (NumberFormatException e) {
                            // Skip invalid ratings
                        }
                    }
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting rating history: " + e.getMessage());
        }
        
        return history;
    }
    
    /**
     * Export all data (users + sessions)
     */
    public boolean exportAllData(String outputDirectory) {
        System.out.println("Starting SQLite to Firebase migration export...");
        System.out.println("Output directory: " + outputDirectory);
        
        // Create directory if it doesn't exist
        java.io.File dir = new java.io.File(outputDirectory);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        
        boolean usersSuccess = exportUsersToJson(outputDirectory + "/users_export.json");
        boolean sessionsSuccess = exportSessionsToJson(outputDirectory + "/sessions_export.json");
        
        if (usersSuccess && sessionsSuccess) {
            System.out.println("\n✓ Export completed successfully!");
            System.out.println("Files created:");
            System.out.println("  - " + outputDirectory + "/users_export.json");
            System.out.println("  - " + outputDirectory + "/sessions_export.json");
            return true;
        } else {
            System.err.println("\n✗ Export failed!");
            return false;
        }
    }
    
    /**
     * Main method for standalone execution
     */
    public static void main(String[] args) {
        System.out.println("===========================================");
        System.out.println("  SQLite to Firebase Migration Exporter");
        System.out.println("===========================================\n");
        
        SQLiteToFirebaseMigration exporter = new SQLiteToFirebaseMigration();
        
        String outputDir = args.length > 0 ? args[0] : "temp";
        exporter.exportAllData(outputDir);
        
        System.out.println("\nNext steps:");
        System.out.println("1. Check the exported JSON files in: " + outputDir);
        System.out.println("2. Run the upload script: node firebase_upload.js");
        System.out.println("   OR use: java -jar FirebaseUploader.jar");
    }
}
