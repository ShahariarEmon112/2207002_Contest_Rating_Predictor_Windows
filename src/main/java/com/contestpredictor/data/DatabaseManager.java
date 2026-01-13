package com.contestpredictor.data;

import com.contestpredictor.model.Contest;
import com.contestpredictor.model.Participant;
import com.contestpredictor.model.User;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * SQLite Database Manager for persisting contest, user, and participant data
 */
public class DatabaseManager {
    private static DatabaseManager instance;
    private static final String DB_URL = "jdbc:sqlite:contest_predictor.db";
    private Connection connection;

    private DatabaseManager() {
        try {
            connection = DriverManager.getConnection(DB_URL);
            initializeTables();
        } catch (SQLException e) {
            System.err.println("Failed to connect to database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    /**
     * Get the database connection
     */
    public Connection getConnection() {
        return connection;
    }

    /**
     * Initialize database tables
     */
    private void initializeTables() {
        try {
            Statement stmt = connection.createStatement();

            // Users table (with Firebase fields)
            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                    "username TEXT PRIMARY KEY," +
                    "password TEXT NOT NULL," +
                    "full_name TEXT NOT NULL," +
                    "current_rating INTEGER NOT NULL," +
                    "contests_participated INTEGER NOT NULL," +
                    "rating_history TEXT," + // Stored as comma-separated values
                    "email TEXT," +
                    "firebase_uid TEXT" +
                    ")");
            
            // Add email and firebase_uid columns if they don't exist (for existing databases)
            try {
                stmt.execute("ALTER TABLE users ADD COLUMN email TEXT");
            } catch (SQLException e) {
                // Column already exists, ignore
            }
            try {
                stmt.execute("ALTER TABLE users ADD COLUMN firebase_uid TEXT");
            } catch (SQLException e) {
                // Column already exists, ignore
            }

            // Admins table
            stmt.execute("CREATE TABLE IF NOT EXISTS admins (" +
                    "admin_id TEXT PRIMARY KEY," +
                    "username TEXT UNIQUE NOT NULL," +
                    "password TEXT NOT NULL," +
                    "email TEXT," +
                    "full_name TEXT NOT NULL," +
                    "created_at TEXT NOT NULL," +
                    "is_active INTEGER DEFAULT 1" + // 0 = false, 1 = true
                    ")");

            // Contests table (enhanced with admin support)
            stmt.execute("CREATE TABLE IF NOT EXISTS contests (" +
                    "contest_id TEXT PRIMARY KEY," +
                    "contest_name TEXT NOT NULL," +
                    "date_time TEXT NOT NULL," +
                    "duration INTEGER NOT NULL," +
                    "is_past INTEGER NOT NULL," + // 0 = false, 1 = true
                    "created_by_admin TEXT," +
                    "max_participants INTEGER DEFAULT 1000," +
                    "registration_open INTEGER DEFAULT 1" + // 0 = false, 1 = true
                    ")");

            // Contest registrations table
            stmt.execute("CREATE TABLE IF NOT EXISTS contest_registrations (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "contest_id TEXT NOT NULL," +
                    "username TEXT NOT NULL," +
                    "registered_at TEXT NOT NULL," +
                    "FOREIGN KEY (contest_id) REFERENCES contests(contest_id)," +
                    "UNIQUE(contest_id, username)" +
                    ")");

            // Participants table
            stmt.execute("CREATE TABLE IF NOT EXISTS participants (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "contest_id TEXT NOT NULL," +
                    "username TEXT NOT NULL," +
                    "current_rating INTEGER NOT NULL," +
                    "problems_solved INTEGER NOT NULL," +
                    "total_penalty INTEGER NOT NULL," +
                    "rank INTEGER NOT NULL," +
                    "predicted_rating INTEGER NOT NULL," +
                    "rating_change INTEGER NOT NULL," +
                    "FOREIGN KEY (contest_id) REFERENCES contests(contest_id)" +
                    ")");

            // Rating history table
            stmt.execute("CREATE TABLE IF NOT EXISTS rating_history (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "contest_id TEXT NOT NULL," +
                    "username TEXT NOT NULL," +
                    "old_rating INTEGER NOT NULL," +
                    "new_rating INTEGER NOT NULL," +
                    "delta INTEGER NOT NULL," +
                    "contest_date TEXT NOT NULL," +
                    "FOREIGN KEY (contest_id) REFERENCES contests(contest_id)" +
                    ")");
            
            // Leaderboard Contests table
            stmt.execute("CREATE TABLE IF NOT EXISTS leaderboard_contests (" +
                    "contest_id TEXT PRIMARY KEY," +
                    "contest_name TEXT NOT NULL," +
                    "description TEXT," +
                    "start_date TEXT NOT NULL," +
                    "end_date TEXT NOT NULL," +
                    "max_problems INTEGER NOT NULL," +
                    "is_active INTEGER DEFAULT 1," +
                    "standings_finalized INTEGER DEFAULT 0," +
                    "created_by_admin TEXT NOT NULL," +
                    "created_at TEXT NOT NULL" +
                    ")");
            
            // Leaderboard Registrations table
            stmt.execute("CREATE TABLE IF NOT EXISTS leaderboard_registrations (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "contest_id TEXT NOT NULL," +
                    "username TEXT NOT NULL," +
                    "registered_at TEXT NOT NULL," +
                    "FOREIGN KEY (contest_id) REFERENCES leaderboard_contests(contest_id)," +
                    "UNIQUE(contest_id, username)" +
                    ")");
            
            // Leaderboard Entries table (individual contest standings)
            stmt.execute("CREATE TABLE IF NOT EXISTS leaderboard_entries (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "contest_id TEXT NOT NULL," +
                    "username TEXT NOT NULL," +
                    "rank INTEGER NOT NULL," +
                    "solve_count INTEGER NOT NULL," +
                    "total_penalty INTEGER NOT NULL," +
                    "total_time INTEGER NOT NULL," +
                    "status TEXT DEFAULT 'Completed'," +
                    "FOREIGN KEY (contest_id) REFERENCES leaderboard_contests(contest_id)," +
                    "UNIQUE(contest_id, username)" +
                    ")");
            
            // Combined Leaderboard table (aggregate across all contests)
            stmt.execute("CREATE TABLE IF NOT EXISTS combined_leaderboard (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "username TEXT UNIQUE NOT NULL," +
                    "total_solves INTEGER NOT NULL," +
                    "total_penalty INTEGER NOT NULL," +
                    "overall_rank INTEGER NOT NULL," +
                    "contests_participated INTEGER NOT NULL," +
                    "status TEXT DEFAULT 'Active'," +
                    "last_updated TEXT" +
                    ")");
            
            // Create default admin if not exists
            createDefaultAdmin();

            stmt.close();
            System.out.println("Database tables initialized successfully");
        } catch (SQLException e) {
            System.err.println("Failed to initialize tables: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Create default admin account
     */
    private void createDefaultAdmin() {
        String checkSql = "SELECT COUNT(*) FROM admins WHERE username = 'admin'";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(checkSql)) {
            
            if (rs.next() && rs.getInt(1) == 0) {
                String insertSql = "INSERT INTO admins (admin_id, username, password, email, full_name, created_at, is_active) VALUES (?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement pstmt = connection.prepareStatement(insertSql)) {
                    pstmt.setString(1, "ADMIN_DEFAULT");
                    pstmt.setString(2, "admin");
                    pstmt.setString(3, "admin1234"); // In production, this should be hashed
                    pstmt.setString(4, "admin@contestpredictor.com");
                    pstmt.setString(5, "System Administrator");
                    pstmt.setString(6, LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                    pstmt.setInt(7, 1);
                    pstmt.executeUpdate();
                    System.out.println("Default admin account created (username: admin, password: admin1234)");
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to create default admin: " + e.getMessage());
        }
    }

    /**
     * Save a user to the database (with Firebase fields)
     */
    public void saveUser(User user) {
        String sql = "INSERT OR REPLACE INTO users (username, password, full_name, current_rating, contests_participated, rating_history, email, firebase_uid) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getPassword());
            pstmt.setString(3, user.getFullName());
            pstmt.setInt(4, user.getCurrentRating());
            pstmt.setInt(5, user.getContestsParticipated());
            
            // Convert rating history to comma-separated string
            StringBuilder history = new StringBuilder();
            for (int i = 0; i < user.getRatingHistory().size(); i++) {
                history.append(user.getRatingHistory().get(i));
                if (i < user.getRatingHistory().size() - 1) {
                    history.append(",");
                }
            }
            pstmt.setString(6, history.toString());
            pstmt.setString(7, user.getEmail());
            pstmt.setString(8, user.getFirebaseUid());
            
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Failed to save user: " + e.getMessage());
        }
    }

    /**
     * Load a user from the database (with Firebase fields)
     */
    public User loadUser(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                User user = new User(
                    rs.getString("username"),
                    rs.getString("password"),
                    rs.getInt("current_rating"),
                    rs.getInt("contests_participated"),
                    rs.getString("full_name")
                );
                
                // Parse rating history
                String historyStr = rs.getString("rating_history");
                if (historyStr != null && !historyStr.isEmpty()) {
                    user.getRatingHistory().clear();
                    String[] ratings = historyStr.split(",");
                    for (String rating : ratings) {
                        user.getRatingHistory().add(Integer.parseInt(rating.trim()));
                    }
                }
                
                // Load Firebase fields (handle null for older databases)
                try {
                    user.setEmail(rs.getString("email"));
                    user.setFirebaseUid(rs.getString("firebase_uid"));
                } catch (SQLException e) {
                    // Columns might not exist in older databases
                }
                
                return user;
            }
        } catch (SQLException e) {
            System.err.println("Failed to load user: " + e.getMessage());
        }
        return null;
    }

    /**
     * Save a contest to the database
     */
    public void saveContest(Contest contest) {
        String sql = "INSERT OR REPLACE INTO contests (contest_id, contest_name, date_time, duration, is_past) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, contest.getContestId());
            pstmt.setString(2, contest.getContestName());
            pstmt.setString(3, contest.getDateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            pstmt.setInt(4, contest.getDuration());
            pstmt.setInt(5, contest.isPast() ? 1 : 0);
            
            pstmt.executeUpdate();
            
            // Save participants
            saveParticipants(contest);
        } catch (SQLException e) {
            System.err.println("Failed to save contest: " + e.getMessage());
        }
    }
    
    /**
     * Save a contest with admin support (enhanced version)
     */
    public void saveContestWithAdmin(Contest contest) {
        String sql = "INSERT OR REPLACE INTO contests (contest_id, contest_name, date_time, duration, is_past, created_by_admin, max_participants, registration_open) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, contest.getContestId());
            pstmt.setString(2, contest.getContestName());
            pstmt.setString(3, contest.getDateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            pstmt.setInt(4, contest.getDuration());
            pstmt.setInt(5, contest.isPast() ? 1 : 0);
            pstmt.setString(6, contest.getCreatedByAdmin());
            pstmt.setInt(7, contest.getMaxParticipants());
            pstmt.setInt(8, contest.isRegistrationOpen() ? 1 : 0);
            
            pstmt.executeUpdate();
            
            // Save participants if any
            if (contest.getParticipants() != null && !contest.getParticipants().isEmpty()) {
                saveParticipants(contest);
            }
        } catch (SQLException e) {
            System.err.println("Failed to save contest with admin: " + e.getMessage());
        }
    }

    /**
     * Save participants for a contest
     */
    private void saveParticipants(Contest contest) {
        // First, delete existing participants for this contest
        String deleteSql = "DELETE FROM participants WHERE contest_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(deleteSql)) {
            pstmt.setString(1, contest.getContestId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Failed to delete old participants: " + e.getMessage());
        }
        
        // Insert new participants
        String insertSql = "INSERT INTO participants (contest_id, username, current_rating, problems_solved, total_penalty, rank, predicted_rating, rating_change) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(insertSql)) {
            for (Participant p : contest.getParticipants()) {
                pstmt.setString(1, contest.getContestId());
                pstmt.setString(2, p.getUsername());
                pstmt.setInt(3, p.getCurrentRating());
                pstmt.setInt(4, p.getProblemsSolved());
                pstmt.setInt(5, p.getTotalPenalty());
                pstmt.setInt(6, p.getRank());
                pstmt.setInt(7, p.getPredictedRating());
                pstmt.setInt(8, p.getRatingChange());
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("Failed to save participants: " + e.getMessage());
        }
    }

    /**
     * Load a contest from the database
     */
    public Contest loadContest(String contestId) {
        String sql = "SELECT * FROM contests WHERE contest_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, contestId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                Contest contest = new Contest(
                    rs.getString("contest_id"),
                    rs.getString("contest_name"),
                    LocalDateTime.parse(rs.getString("date_time"), DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    rs.getInt("duration"),
                    rs.getInt("is_past") == 1
                );
                
                // Load participants
                loadParticipantsForContest(contest);
                
                return contest;
            }
        } catch (SQLException e) {
            System.err.println("Failed to load contest: " + e.getMessage());
        }
        return null;
    }

    /**
     * Load participants for a contest
     */
    private void loadParticipantsForContest(Contest contest) {
        String sql = "SELECT * FROM participants WHERE contest_id = ? ORDER BY rank ASC";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, contest.getContestId());
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Participant p = new Participant(
                    rs.getString("username"),
                    rs.getInt("current_rating"),
                    rs.getInt("problems_solved"),
                    rs.getInt("total_penalty")
                );
                p.setRank(rs.getInt("rank"));
                p.setPredictedRating(rs.getInt("predicted_rating"));
                p.setRatingChange(rs.getInt("rating_change"));
                
                contest.addParticipant(p);
            }
        } catch (SQLException e) {
            System.err.println("Failed to load participants: " + e.getMessage());
        }
    }

    /**
     * Load all contests from the database
     */
    public List<Contest> loadAllContests() {
        List<Contest> contests = new ArrayList<>();
        String sql = "SELECT contest_id FROM contests";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                Contest contest = loadContest(rs.getString("contest_id"));
                if (contest != null) {
                    contests.add(contest);
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to load contests: " + e.getMessage());
        }
        return contests;
    }

    /**
     * Check if database has contests
     */
    public boolean hasContests() {
        String sql = "SELECT COUNT(*) FROM contests";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("Failed to check contests: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Get participants by contest ID
     */
    public List<Participant> getParticipantsByContest(String contestId) {
        List<Participant> participants = new ArrayList<>();
        String sql = "SELECT * FROM participants WHERE contest_id = ? ORDER BY rank ASC";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, contestId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Participant p = new Participant(
                    rs.getString("username"),
                    rs.getInt("current_rating"),
                    rs.getInt("problems_solved"),
                    rs.getInt("total_penalty")
                );
                p.setRank(rs.getInt("rank"));
                p.setPredictedRating(rs.getInt("predicted_rating"));
                p.setRatingChange(rs.getInt("rating_change"));
                
                participants.add(p);
            }
        } catch (SQLException e) {
            System.err.println("Failed to get participants: " + e.getMessage());
        }
        
        return participants;
    }
    
    /**
     * Save a single participant to a contest
     */
    public boolean saveParticipant(String contestId, Participant participant) {
        String sql = "INSERT INTO participants (contest_id, username, current_rating, problems_solved, total_penalty, rank, predicted_rating, rating_change) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, contestId);
            pstmt.setString(2, participant.getUsername());
            pstmt.setInt(3, participant.getCurrentRating());
            pstmt.setInt(4, participant.getProblemsSolved());
            pstmt.setInt(5, participant.getTotalPenalty());
            pstmt.setInt(6, participant.getRank());
            pstmt.setInt(7, participant.getPredictedRating());
            pstmt.setInt(8, participant.getRatingChange());
            
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("Failed to save participant: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Remove a participant from a contest
     */
    public boolean removeParticipant(String contestId, String username) {
        String sql = "DELETE FROM participants WHERE contest_id = ? AND username = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, contestId);
            pstmt.setString(2, username);
            
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Failed to remove participant: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Update participant's solve count and penalty
     */
    public boolean updateParticipantSolveCount(String contestId, String username, int problemsSolved, int totalPenalty) {
        String sql = "UPDATE participants SET problems_solved = ?, total_penalty = ? WHERE contest_id = ? AND username = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, problemsSolved);
            pstmt.setInt(2, totalPenalty);
            pstmt.setString(3, contestId);
            pstmt.setString(4, username);
            
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Failed to update participant solve count: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Register a user for a contest
     */
    public boolean registerUserForContest(String contestId, String username) {
        String sql = "INSERT OR IGNORE INTO contest_registrations (contest_id, username, registered_at) VALUES (?, ?, ?)";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, contestId);
            pstmt.setString(2, username);
            pstmt.setString(3, LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Failed to register user for contest: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Get all registered users for a contest
     */
    public List<String> getRegisteredUsers(String contestId) {
        List<String> users = new ArrayList<>();
        String sql = "SELECT username FROM contest_registrations WHERE contest_id = ? ORDER BY registered_at";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, contestId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                users.add(rs.getString("username"));
            }
        } catch (SQLException e) {
            System.err.println("Failed to get registered users: " + e.getMessage());
        }
        
        return users;
    }
    
    /**
     * Check if user is registered for contest
     */
    public boolean isUserRegisteredForContest(String contestId, String username) {
        String sql = "SELECT COUNT(*) FROM contest_registrations WHERE contest_id = ? AND username = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, contestId);
            pstmt.setString(2, username);
            
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("Failed to check user registration: " + e.getMessage());
        }
        
        return false;
    }

    /**
     * Close database connection
     */
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Database connection closed");
            }
        } catch (SQLException e) {
            System.err.println("Failed to close database: " + e.getMessage());
        }
    }
}
