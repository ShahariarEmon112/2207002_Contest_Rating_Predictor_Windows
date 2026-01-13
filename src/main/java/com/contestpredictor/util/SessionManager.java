package com.contestpredictor.util;

import com.contestpredictor.data.DatabaseManager;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Session Manager for persistent login functionality
 * Manages user sessions with Firebase tokens and SQLite storage
 * Implements "one-time login" - user stays logged in until they explicitly log out
 */
public class SessionManager {
    private static SessionManager instance;
    private final DatabaseManager dbManager;
    
    private SessionData currentSession;
    
    private SessionManager() {
        this.dbManager = DatabaseManager.getInstance();
        initializeSessionTable();
    }
    
    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }
    
    /**
     * Initialize session table in SQLite
     */
    private void initializeSessionTable() {
        try {
            Connection conn = dbManager.getConnection();
            Statement stmt = conn.createStatement();
            
            // Sessions table for persistent login
            stmt.execute("CREATE TABLE IF NOT EXISTS user_sessions (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "username TEXT UNIQUE NOT NULL," +
                    "firebase_uid TEXT," +
                    "email TEXT," +
                    "id_token TEXT," +
                    "refresh_token TEXT," +
                    "token_expiration INTEGER," +
                    "remember_me INTEGER DEFAULT 1," +
                    "last_login TEXT NOT NULL," +
                    "created_at TEXT NOT NULL" +
                    ")");
            
            stmt.close();
            System.out.println("Session table initialized");
            
        } catch (SQLException e) {
            System.err.println("Failed to initialize session table: " + e.getMessage());
        }
    }
    
    /**
     * Save session to SQLite (for one-time login)
     * @param username Local username
     * @param firebaseUid Firebase user ID (can be null if using local auth)
     * @param email User email
     * @param idToken Firebase ID token
     * @param refreshToken Firebase refresh token
     * @param tokenExpiration Token expiration timestamp
     * @param rememberMe Whether to persist the session
     */
    public void saveSession(String username, String firebaseUid, String email, 
                           String idToken, String refreshToken, long tokenExpiration, 
                           boolean rememberMe) {
        try {
            Connection conn = dbManager.getConnection();
            String now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            
            // Delete any existing session for this user
            String deleteSql = "DELETE FROM user_sessions WHERE username = ?";
            try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
                deleteStmt.setString(1, username);
                deleteStmt.executeUpdate();
            }
            
            // Insert new session
            String insertSql = "INSERT INTO user_sessions (username, firebase_uid, email, id_token, " +
                    "refresh_token, token_expiration, remember_me, last_login, created_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            
            try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                pstmt.setString(1, username);
                pstmt.setString(2, firebaseUid);
                pstmt.setString(3, email);
                pstmt.setString(4, idToken);
                pstmt.setString(5, refreshToken);
                pstmt.setLong(6, tokenExpiration);
                pstmt.setInt(7, rememberMe ? 1 : 0);
                pstmt.setString(8, now);
                pstmt.setString(9, now);
                pstmt.executeUpdate();
            }
            
            // Update current session
            this.currentSession = new SessionData(username, firebaseUid, email, 
                    idToken, refreshToken, tokenExpiration, rememberMe);
            
            System.out.println("Session saved for user: " + username);
            
        } catch (SQLException e) {
            System.err.println("Failed to save session: " + e.getMessage());
        }
    }
    
    /**
     * Load saved session from SQLite
     * @return SessionData if valid session exists, null otherwise
     */
    public SessionData loadSavedSession() {
        try {
            Connection conn = dbManager.getConnection();
            
            // Get the most recent session with remember_me enabled
            String sql = "SELECT * FROM user_sessions WHERE remember_me = 1 ORDER BY last_login DESC LIMIT 1";
            
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                
                if (rs.next()) {
                    String username = rs.getString("username");
                    String firebaseUid = rs.getString("firebase_uid");
                    String email = rs.getString("email");
                    String idToken = rs.getString("id_token");
                    String refreshToken = rs.getString("refresh_token");
                    long tokenExpiration = rs.getLong("token_expiration");
                    boolean rememberMe = rs.getInt("remember_me") == 1;
                    
                    this.currentSession = new SessionData(username, firebaseUid, email,
                            idToken, refreshToken, tokenExpiration, rememberMe);
                    
                    System.out.println("Session loaded for user: " + username);
                    return currentSession;
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Failed to load session: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Check if there's a valid saved session
     * @return true if valid session exists
     */
    public boolean hasValidSession() {
        SessionData session = loadSavedSession();
        if (session == null) {
            return false;
        }
        
        // If using Firebase, check if token needs refresh
        if (session.getRefreshToken() != null && !session.getRefreshToken().isEmpty()) {
            FirebaseAuthService firebaseAuth = FirebaseAuthService.getInstance();
            
            // If token is expired but we have refresh token, try to refresh
            if (System.currentTimeMillis() >= session.getTokenExpiration()) {
                firebaseAuth.setSessionData(
                        session.getIdToken(),
                        session.getRefreshToken(),
                        session.getFirebaseUid(),
                        session.getEmail(),
                        session.getTokenExpiration()
                );
                
                FirebaseAuthService.FirebaseAuthResult result = firebaseAuth.refreshToken();
                if (result.isSuccess()) {
                    // Update session with new tokens
                    saveSession(session.getUsername(), session.getFirebaseUid(), session.getEmail(),
                            firebaseAuth.getCurrentIdToken(), firebaseAuth.getCurrentRefreshToken(),
                            System.currentTimeMillis() + (3600 * 1000), true);
                    return true;
                } else {
                    // Token refresh failed, session is invalid
                    clearSession();
                    return false;
                }
            }
        }
        
        return true;
    }
    
    /**
     * Get the current session username
     * @return username of logged-in user, or null if no session
     */
    public String getCurrentUsername() {
        if (currentSession != null) {
            return currentSession.getUsername();
        }
        SessionData saved = loadSavedSession();
        return saved != null ? saved.getUsername() : null;
    }
    
    /**
     * Get the current session data
     */
    public SessionData getCurrentSession() {
        return currentSession;
    }
    
    /**
     * Clear the current session (logout)
     */
    public void clearSession() {
        if (currentSession != null) {
            try {
                Connection conn = dbManager.getConnection();
                String sql = "DELETE FROM user_sessions WHERE username = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, currentSession.getUsername());
                    pstmt.executeUpdate();
                }
                System.out.println("Session cleared for user: " + currentSession.getUsername());
            } catch (SQLException e) {
                System.err.println("Failed to clear session: " + e.getMessage());
            }
        }
        
        // Also clear Firebase session
        FirebaseAuthService.getInstance().signOut();
        
        this.currentSession = null;
    }
    
    /**
     * Clear all sessions (used for complete logout)
     */
    public void clearAllSessions() {
        try {
            Connection conn = dbManager.getConnection();
            String sql = "DELETE FROM user_sessions";
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(sql);
            }
            System.out.println("All sessions cleared");
        } catch (SQLException e) {
            System.err.println("Failed to clear all sessions: " + e.getMessage());
        }
        
        FirebaseAuthService.getInstance().signOut();
        this.currentSession = null;
    }
    
    /**
     * Update the last login time for the current session
     */
    public void updateLastLogin() {
        if (currentSession == null) return;
        
        try {
            Connection conn = dbManager.getConnection();
            String sql = "UPDATE user_sessions SET last_login = ? WHERE username = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                pstmt.setString(2, currentSession.getUsername());
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("Failed to update last login: " + e.getMessage());
        }
    }
    
    /**
     * Session data class
     */
    public static class SessionData {
        private final String username;
        private final String firebaseUid;
        private final String email;
        private final String idToken;
        private final String refreshToken;
        private final long tokenExpiration;
        private final boolean rememberMe;
        
        public SessionData(String username, String firebaseUid, String email,
                          String idToken, String refreshToken, long tokenExpiration,
                          boolean rememberMe) {
            this.username = username;
            this.firebaseUid = firebaseUid;
            this.email = email;
            this.idToken = idToken;
            this.refreshToken = refreshToken;
            this.tokenExpiration = tokenExpiration;
            this.rememberMe = rememberMe;
        }
        
        public String getUsername() { return username; }
        public String getFirebaseUid() { return firebaseUid; }
        public String getEmail() { return email; }
        public String getIdToken() { return idToken; }
        public String getRefreshToken() { return refreshToken; }
        public long getTokenExpiration() { return tokenExpiration; }
        public boolean isRememberMe() { return rememberMe; }
    }
}
