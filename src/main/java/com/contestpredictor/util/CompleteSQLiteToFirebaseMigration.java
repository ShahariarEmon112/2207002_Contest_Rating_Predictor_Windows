package com.contestpredictor.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.*;
import java.sql.*;
import java.util.*;
import com.contestpredictor.data.DatabaseManager;

/**
 * Complete utility to export ALL SQLite data to JSON for Firebase migration
 * Maintains exact SQLite structure for all 10 tables
 * Exports: users, admins, contests, registrations, participants, rating_history,
 *          leaderboard_contests, leaderboard_registrations, leaderboard_entries, 
 *          combined_leaderboard, and sessions
 */
public class CompleteSQLiteToFirebaseMigration {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final String EXPORT_DIR = "temp/migration_export/";
    
    public static void main(String[] args) {
        try {
            exportAllData();
            System.out.println("\n✅ Complete export finished successfully!");
            System.out.println("📁 All files exported to: " + EXPORT_DIR);
            System.out.println("\nNext steps:");
            System.out.println("1. Run: npm install");
            System.out.println("2. Run: node firebase_upload_complete.js");
        } catch (Exception e) {
            System.err.println("❌ Export failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public static void exportAllData() throws Exception {
        File dir = new File(EXPORT_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        
        System.out.println("📤 Starting COMPLETE SQLite to Firebase export...\n");
        System.out.println("Exporting all 10 database tables:\n");
        
        // Export all tables maintaining exact SQLite structure
        exportUsersToJson();
        exportAdminsToJson();
        exportContestsToJson();
        exportContestRegistrationsToJson();
        exportParticipantsToJson();
        exportRatingHistoryToJson();
        exportLeaderboardContestsToJson();
        exportLeaderboardRegistrationsToJson();
        exportLeaderboardEntriesToJson();
        exportCombinedLeaderboardToJson();
        exportSessionsToJson();
    }
    
    /**
     * Export users table with all fields
     */
    private static void exportUsersToJson() throws Exception {
        System.out.println("1. Exporting users...");
        
        String sql = "SELECT username, password, full_name, current_rating, " +
                    "contests_participated, rating_history, email, firebase_uid FROM users";
        
        List<Map<String, Object>> users = new ArrayList<>();
        
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                Map<String, Object> user = new LinkedHashMap<>();
                user.put("username", rs.getString("username"));
                user.put("password", rs.getString("password"));
                user.put("full_name", rs.getString("full_name"));
                user.put("current_rating", rs.getInt("current_rating"));
                user.put("contests_participated", rs.getInt("contests_participated"));
                user.put("rating_history", rs.getString("rating_history"));
                user.put("email", rs.getString("email"));
                user.put("firebase_uid", rs.getString("firebase_uid"));
                
                users.add(user);
            }
        }
        
        saveToJson(users, EXPORT_DIR + "users.json");
        System.out.println("   ✓ Exported " + users.size() + " users");
    }
    
    /**
     * Export admins table
     */
    private static void exportAdminsToJson() throws Exception {
        System.out.println("2. Exporting admins...");
        
        String sql = "SELECT admin_id, username, password, email, full_name, created_at, is_active FROM admins";
        
        List<Map<String, Object>> admins = new ArrayList<>();
        
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                Map<String, Object> admin = new LinkedHashMap<>();
                admin.put("admin_id", rs.getInt("admin_id"));
                admin.put("username", rs.getString("username"));
                admin.put("password", rs.getString("password"));
                admin.put("email", rs.getString("email"));
                admin.put("full_name", rs.getString("full_name"));
                admin.put("created_at", rs.getString("created_at"));
                admin.put("is_active", rs.getInt("is_active"));
                
                admins.add(admin);
            }
        }
        
        saveToJson(admins, EXPORT_DIR + "admins.json");
        System.out.println("   ✓ Exported " + admins.size() + " admins");
    }
    
    /**
     * Export contests table
     */
    private static void exportContestsToJson() throws Exception {
        System.out.println("3. Exporting contests...");
        
        String sql = "SELECT contest_id, contest_name, date_time, duration, is_past, " +
                    "created_by_admin, max_participants, registration_open FROM contests";
        
        List<Map<String, Object>> contests = new ArrayList<>();
        
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                Map<String, Object> contest = new LinkedHashMap<>();
                contest.put("contest_id", rs.getString("contest_id"));
                contest.put("contest_name", rs.getString("contest_name"));
                contest.put("date_time", rs.getString("date_time"));
                contest.put("duration", rs.getInt("duration"));
                contest.put("is_past", rs.getInt("is_past"));
                contest.put("created_by_admin", rs.getString("created_by_admin"));
                contest.put("max_participants", rs.getInt("max_participants"));
                contest.put("registration_open", rs.getInt("registration_open"));
                
                contests.add(contest);
            }
        }
        
        saveToJson(contests, EXPORT_DIR + "contests.json");
        System.out.println("   ✓ Exported " + contests.size() + " contests");
    }
    
    /**
     * Export contest_registrations table
     */
    private static void exportContestRegistrationsToJson() throws Exception {
        System.out.println("4. Exporting contest registrations...");
        
        String sql = "SELECT id, contest_id, username, registered_at FROM contest_registrations";
        
        List<Map<String, Object>> registrations = new ArrayList<>();
        
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                Map<String, Object> registration = new LinkedHashMap<>();
                registration.put("id", rs.getInt("id"));
                registration.put("contest_id", rs.getString("contest_id"));
                registration.put("username", rs.getString("username"));
                registration.put("registered_at", rs.getString("registered_at"));
                
                registrations.add(registration);
            }
        }
        
        saveToJson(registrations, EXPORT_DIR + "contest_registrations.json");
        System.out.println("   ✓ Exported " + registrations.size() + " contest registrations");
    }
    
    /**
     * Export participants table
     */
    private static void exportParticipantsToJson() throws Exception {
        System.out.println("5. Exporting participants...");
        
        String sql = "SELECT id, contest_id, username, current_rating, problems_solved, " +
                    "total_penalty, rank, predicted_rating, rating_change FROM participants";
        
        List<Map<String, Object>> participants = new ArrayList<>();
        
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                Map<String, Object> participant = new LinkedHashMap<>();
                participant.put("id", rs.getInt("id"));
                participant.put("contest_id", rs.getString("contest_id"));
                participant.put("username", rs.getString("username"));
                participant.put("current_rating", rs.getInt("current_rating"));
                participant.put("problems_solved", rs.getInt("problems_solved"));
                participant.put("total_penalty", rs.getInt("total_penalty"));
                participant.put("rank", rs.getInt("rank"));
                participant.put("predicted_rating", rs.getInt("predicted_rating"));
                participant.put("rating_change", rs.getInt("rating_change"));
                
                participants.add(participant);
            }
        }
        
        saveToJson(participants, EXPORT_DIR + "participants.json");
        System.out.println("   ✓ Exported " + participants.size() + " participants");
    }
    
    /**
     * Export rating_history table
     */
    private static void exportRatingHistoryToJson() throws Exception {
        System.out.println("6. Exporting rating history...");
        
        String sql = "SELECT id, contest_id, username, old_rating, new_rating, delta, contest_date FROM rating_history";
        
        List<Map<String, Object>> history = new ArrayList<>();
        
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                Map<String, Object> record = new LinkedHashMap<>();
                record.put("id", rs.getInt("id"));
                record.put("contest_id", rs.getString("contest_id"));
                record.put("username", rs.getString("username"));
                record.put("old_rating", rs.getInt("old_rating"));
                record.put("new_rating", rs.getInt("new_rating"));
                record.put("delta", rs.getInt("delta"));
                record.put("contest_date", rs.getString("contest_date"));
                
                history.add(record);
            }
        }
        
        saveToJson(history, EXPORT_DIR + "rating_history.json");
        System.out.println("   ✓ Exported " + history.size() + " rating history records");
    }
    
    /**
     * Export leaderboard_contests table
     */
    private static void exportLeaderboardContestsToJson() throws Exception {
        System.out.println("7. Exporting leaderboard contests...");
        
        String sql = "SELECT contest_id, contest_name, description, start_date, end_date, " +
                    "max_problems, is_active, standings_finalized, created_by_admin, created_at FROM leaderboard_contests";
        
        List<Map<String, Object>> contests = new ArrayList<>();
        
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                Map<String, Object> contest = new LinkedHashMap<>();
                contest.put("contest_id", rs.getString("contest_id"));
                contest.put("contest_name", rs.getString("contest_name"));
                contest.put("description", rs.getString("description"));
                contest.put("start_date", rs.getString("start_date"));
                contest.put("end_date", rs.getString("end_date"));
                contest.put("max_problems", rs.getInt("max_problems"));
                contest.put("is_active", rs.getInt("is_active"));
                contest.put("standings_finalized", rs.getInt("standings_finalized"));
                contest.put("created_by_admin", rs.getString("created_by_admin"));
                contest.put("created_at", rs.getString("created_at"));
                
                contests.add(contest);
            }
        }
        
        saveToJson(contests, EXPORT_DIR + "leaderboard_contests.json");
        System.out.println("   ✓ Exported " + contests.size() + " leaderboard contests");
    }
    
    /**
     * Export leaderboard_registrations table
     */
    private static void exportLeaderboardRegistrationsToJson() throws Exception {
        System.out.println("8. Exporting leaderboard registrations...");
        
        String sql = "SELECT id, contest_id, username, registered_at FROM leaderboard_registrations";
        
        List<Map<String, Object>> registrations = new ArrayList<>();
        
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                Map<String, Object> registration = new LinkedHashMap<>();
                registration.put("id", rs.getInt("id"));
                registration.put("contest_id", rs.getString("contest_id"));
                registration.put("username", rs.getString("username"));
                registration.put("registered_at", rs.getString("registered_at"));
                
                registrations.add(registration);
            }
        }
        
        saveToJson(registrations, EXPORT_DIR + "leaderboard_registrations.json");
        System.out.println("   ✓ Exported " + registrations.size() + " leaderboard registrations");
    }
    
    /**
     * Export leaderboard_entries table
     */
    private static void exportLeaderboardEntriesToJson() throws Exception {
        System.out.println("9. Exporting leaderboard entries...");
        
        String sql = "SELECT id, contest_id, username, rank, solve_count, total_penalty, " +
                    "total_time, status FROM leaderboard_entries";
        
        List<Map<String, Object>> entries = new ArrayList<>();
        
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("id", rs.getInt("id"));
                entry.put("contest_id", rs.getString("contest_id"));
                entry.put("username", rs.getString("username"));
                entry.put("rank", rs.getInt("rank"));
                entry.put("solve_count", rs.getInt("solve_count"));
                entry.put("total_penalty", rs.getInt("total_penalty"));
                entry.put("total_time", rs.getInt("total_time"));
                entry.put("status", rs.getString("status"));
                
                entries.add(entry);
            }
        }
        
        saveToJson(entries, EXPORT_DIR + "leaderboard_entries.json");
        System.out.println("   ✓ Exported " + entries.size() + " leaderboard entries");
    }
    
    /**
     * Export combined_leaderboard table
     */
    private static void exportCombinedLeaderboardToJson() throws Exception {
        System.out.println("10. Exporting combined leaderboard...");
        
        String sql = "SELECT id, username, total_solves, total_penalty, overall_rank, " +
                    "contests_participated, status, last_updated FROM combined_leaderboard";
        
        List<Map<String, Object>> leaderboard = new ArrayList<>();
        
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("id", rs.getInt("id"));
                entry.put("username", rs.getString("username"));
                entry.put("total_solves", rs.getInt("total_solves"));
                entry.put("total_penalty", rs.getInt("total_penalty"));
                entry.put("overall_rank", rs.getInt("overall_rank"));
                entry.put("contests_participated", rs.getInt("contests_participated"));
                entry.put("status", rs.getString("status"));
                entry.put("last_updated", rs.getString("last_updated"));
                
                leaderboard.add(entry);
            }
        }
        
        saveToJson(leaderboard, EXPORT_DIR + "combined_leaderboard.json");
        System.out.println("   ✓ Exported " + leaderboard.size() + " combined leaderboard entries");
    }
    
    /**
     * Export active sessions
     */
    private static void exportSessionsToJson() throws Exception {
        System.out.println("11. Exporting sessions...");
        
        String sql = "SELECT username, firebase_uid, email, id_token, refresh_token, " +
                    "token_expiration, last_login, remember_me FROM user_sessions";
        
        List<Map<String, Object>> sessions = new ArrayList<>();
        
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                Map<String, Object> session = new LinkedHashMap<>();
                session.put("username", rs.getString("username"));
                session.put("firebase_uid", rs.getString("firebase_uid"));
                session.put("email", rs.getString("email"));
                session.put("id_token", rs.getString("id_token"));
                session.put("refresh_token", rs.getString("refresh_token"));
                session.put("token_expiration", rs.getLong("token_expiration"));
                session.put("last_login", rs.getLong("last_login"));
                session.put("remember_me", rs.getInt("remember_me"));
                
                sessions.add(session);
            }
        }
        
        saveToJson(sessions, EXPORT_DIR + "sessions.json");
        System.out.println("   ✓ Exported " + sessions.size() + " sessions");
    }
    
    private static void saveToJson(List<Map<String, Object>> data, String filename) throws IOException {
        try (FileWriter writer = new FileWriter(filename)) {
            gson.toJson(data, writer);
        }
    }
}
