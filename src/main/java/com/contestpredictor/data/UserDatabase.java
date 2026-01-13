package com.contestpredictor.data;

import com.contestpredictor.model.User;
import com.contestpredictor.util.FirebaseAuthService;
import com.contestpredictor.util.FirebaseAuthService.FirebaseAuthResult;
import com.contestpredictor.util.SessionManager;

import java.util.HashMap;
import java.util.Map;

public class UserDatabase {
    private static UserDatabase instance;
    private Map<String, User> users;
    private User currentUser;
    private DatabaseManager dbManager;
    private FirebaseAuthService firebaseAuth;
    private SessionManager sessionManager;

    private UserDatabase() {
        users = new HashMap<>();
        dbManager = DatabaseManager.getInstance();
        firebaseAuth = FirebaseAuthService.getInstance();
        sessionManager = SessionManager.getInstance();
        initializeUsers();
    }

    public static UserDatabase getInstance() {
        if (instance == null) {
            instance = new UserDatabase();
        }
        return instance;
    }

    private void initializeUsers() {
        // Creating 30 predefined user accounts
        // Format: username, password, currentRating, contestsParticipated, fullName
        
        // Try to load from database first, if not exist create new
        createOrLoadUser("user001", "pass001", 1200, 15, "Alex Johnson");
        createOrLoadUser("user002", "pass002", 1450, 22, "Sarah Williams");
        createOrLoadUser("user003", "pass003", 980, 8, "Michael Chen");
        createOrLoadUser("user004", "pass004", 1650, 35, "Emily Davis");
        createOrLoadUser("user005", "pass005", 1100, 12, "David Martinez");
        
        createOrLoadUser("user006", "pass006", 1820, 48, "Jessica Brown");
        createOrLoadUser("user007", "pass007", 750, 5, "Christopher Lee");
        createOrLoadUser("user008", "pass008", 1550, 28, "Amanda Taylor");
        createOrLoadUser("user009", "pass009", 1380, 19, "Daniel Garcia");
        createOrLoadUser("user010", "pass010", 1920, 55, "Jennifer Wilson");
        
        createOrLoadUser("user011", "pass011", 1050, 10, "Matthew Anderson");
        createOrLoadUser("user012", "pass012", 1700, 40, "Lisa Thomas");
        createOrLoadUser("user013", "pass013", 890, 7, "Ryan Moore");
        createOrLoadUser("user014", "pass014", 1490, 25, "Nicole Jackson");
        createOrLoadUser("user015", "pass015", 1250, 17, "Kevin White");
        
        createOrLoadUser("user016", "pass016", 1600, 32, "Rachel Harris");
        createOrLoadUser("user017", "pass017", 1150, 14, "Brandon Martin");
        createOrLoadUser("user018", "pass018", 1780, 45, "Michelle Thompson");
        createOrLoadUser("user019", "pass019", 950, 9, "Jason Garcia");
        createOrLoadUser("user020", "pass020", 2050, 60, "Lauren Martinez");
        
        createOrLoadUser("user021", "pass021", 1320, 20, "Andrew Robinson");
        createOrLoadUser("user022", "pass022", 1470, 26, "Stephanie Clark");
        createOrLoadUser("user023", "pass023", 820, 6, "Justin Rodriguez");
        createOrLoadUser("user024", "pass024", 1590, 30, "Ashley Lewis");
        createOrLoadUser("user025", "pass025", 1210, 16, "Tyler Lee");
        
        createOrLoadUser("user026", "pass026", 1850, 50, "Megan Walker");
        createOrLoadUser("user027", "pass027", 1080, 11, "Eric Hall");
        createOrLoadUser("user028", "pass028", 1680, 38, "Brittany Allen");
        createOrLoadUser("user029", "pass029", 1410, 23, "Jonathan Young");
        createOrLoadUser("user030", "pass030", 1950, 58, "Samantha King");
    }
    
    private void createOrLoadUser(String username, String password, int rating, int contests, String fullName) {
        User user = dbManager.loadUser(username);
        if (user == null) {
            // User doesn't exist, create new user with specified credentials
            user = new User(username, password, rating, contests, fullName);
            dbManager.saveUser(user);
        } else {
            // User exists but verify/update password to match expected password
            // This ensures predefined users always have their correct passwords
            if (!user.getPassword().equals(password)) {
                // Update password in memory
                user = new User(username, password, user.getCurrentRating(), 
                               user.getContestsParticipated(), user.getFullName());
                // Preserve rating history
                User existingUser = dbManager.loadUser(username);
                if (existingUser != null && existingUser.getRatingHistory() != null) {
                    user.getRatingHistory().clear();
                    user.getRatingHistory().addAll(existingUser.getRatingHistory());
                }
                // Save with corrected password
                dbManager.saveUser(user);
            }
        }
        users.put(username, user);
    }

    public User authenticate(String username, String password) {
        User user = users.get(username);
        if (user != null && user.getPassword().equals(password)) {
            currentUser = user;
            return user;
        }
        return null;
    }
    
    /**
     * Authenticate user with Firebase Authentication
     * Falls back to local SQLite authentication if Firebase is not configured
     * @param email User's email (used as username if email format)
     * @param password User's password
     * @param rememberMe Whether to persist the session for one-time login
     * @return AuthResult with user and status
     */
    public AuthResult authenticateWithFirebase(String email, String password, boolean rememberMe) {
        // First, try Firebase authentication if enabled
        if (firebaseAuth.isFirebaseEnabled()) {
            FirebaseAuthResult result = firebaseAuth.signIn(email, password);
            
            if (result.isSuccess()) {
                // Firebase auth successful - check if user exists locally
                String username = extractUsernameFromEmail(email);
                User user = users.get(username);
                
                if (user == null) {
                    // User authenticated with Firebase but doesn't exist locally
                    // Create local user entry for first-time Firebase login
                    user = new User(username, password, 1000, 0, username);
                    user.setEmail(email);
                    user.setFirebaseUid(result.getUserId());
                    users.put(username, user);
                    dbManager.saveUser(user);
                } else {
                    // Update Firebase UID if not set
                    if (user.getFirebaseUid() == null || user.getFirebaseUid().isEmpty()) {
                        user.setFirebaseUid(result.getUserId());
                        user.setEmail(email);
                        dbManager.saveUser(user);
                    }
                }
                
                currentUser = user;
                
                // Save session for one-time login
                sessionManager.saveSession(
                        username,
                        result.getUserId(),
                        email,
                        result.getIdToken(),
                        result.getRefreshToken(),
                        System.currentTimeMillis() + (result.getExpiresIn() * 1000L),
                        rememberMe
                );
                
                return new AuthResult(true, user, "Login successful with Firebase");
            } else {
                // Firebase auth failed - could be network issue or invalid credentials
                // Try local authentication as fallback
                return tryLocalAuthentication(email, password, rememberMe, result.getMessage());
            }
        } else {
            // Firebase not enabled - use local authentication only
            return tryLocalAuthentication(email, password, rememberMe, null);
        }
    }
    
    /**
     * Try local SQLite authentication
     */
    private AuthResult tryLocalAuthentication(String usernameOrEmail, String password, 
                                              boolean rememberMe, String firebaseError) {
        // Try direct username match first
        User user = users.get(usernameOrEmail);
        
        // If not found, try to find by email
        if (user == null) {
            for (User u : users.values()) {
                if (usernameOrEmail.equals(u.getEmail())) {
                    user = u;
                    break;
                }
            }
        }
        
        // If still not found, extract username from email
        if (user == null) {
            String username = extractUsernameFromEmail(usernameOrEmail);
            user = users.get(username);
        }
        
        if (user != null && user.getPassword().equals(password)) {
            currentUser = user;
            
            // Save local session
            sessionManager.saveSession(
                    user.getUsername(),
                    null,
                    user.getEmail(),
                    null,
                    null,
                    System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000), // 30 days
                    rememberMe
            );
            
            return new AuthResult(true, user, "Login successful (local)");
        }
        
        String errorMessage = firebaseError != null ? firebaseError : "Invalid username or password";
        return new AuthResult(false, null, errorMessage);
    }
    
    /**
     * Check if there's a valid saved session and auto-login
     * @return User if valid session exists, null otherwise
     */
    public User tryAutoLogin() {
        if (sessionManager.hasValidSession()) {
            String username = sessionManager.getCurrentUsername();
            if (username != null) {
                User user = users.get(username);
                if (user != null) {
                    currentUser = user;
                    sessionManager.updateLastLogin();
                    System.out.println("Auto-login successful for: " + username);
                    return user;
                }
            }
        }
        return null;
    }
    
    /**
     * Extract username from email (before @)
     */
    private String extractUsernameFromEmail(String email) {
        if (email.contains("@")) {
            return email.substring(0, email.indexOf("@"));
        }
        return email;
    }

    public boolean registerUser(String username, String password, String fullName) {
        // Check if username already exists
        if (users.containsKey(username)) {
            return false;
        }
        
        // Create new user with initial rating of 1000 and 0 contests
        User newUser = new User(username, password, 1000, 0, fullName);
        users.put(username, newUser);
        currentUser = newUser;
        
        // Save to database
        dbManager.saveUser(newUser);
        
        return true;
    }
    
    /**
     * Register a new user with Firebase Authentication
     * Creates both Firebase account and local SQLite entry
     * @param email User's email
     * @param password User's password (min 6 characters for Firebase)
     * @param fullName User's full name
     * @param rememberMe Whether to persist the session
     * @return AuthResult with registration status
     */
    public AuthResult registerWithFirebase(String email, String password, String fullName, boolean rememberMe) {
        String username = extractUsernameFromEmail(email);
        
        // Check if username already exists locally
        if (users.containsKey(username)) {
            return new AuthResult(false, null, "Username already exists. Please use a different email.");
        }
        
        // Try Firebase registration if enabled
        if (firebaseAuth.isFirebaseEnabled()) {
            FirebaseAuthResult result = firebaseAuth.signUp(email, password);
            
            if (result.isSuccess()) {
                // Firebase registration successful - create local user
                User newUser = new User(username, password, 1000, 0, fullName);
                newUser.setEmail(email);
                newUser.setFirebaseUid(result.getUserId());
                
                users.put(username, newUser);
                currentUser = newUser;
                dbManager.saveUser(newUser);
                
                // Save session for one-time login
                sessionManager.saveSession(
                        username,
                        result.getUserId(),
                        email,
                        result.getIdToken(),
                        result.getRefreshToken(),
                        System.currentTimeMillis() + (result.getExpiresIn() * 1000L),
                        rememberMe
                );
                
                return new AuthResult(true, newUser, "Registration successful!");
            } else {
                return new AuthResult(false, null, result.getMessage());
            }
        } else {
            // Firebase not enabled - register locally only
            if (password.length() < 6) {
                return new AuthResult(false, null, "Password must be at least 6 characters");
            }
            
            User newUser = new User(username, password, 1000, 0, fullName);
            newUser.setEmail(email);
            
            users.put(username, newUser);
            currentUser = newUser;
            dbManager.saveUser(newUser);
            
            // Save local session
            sessionManager.saveSession(
                    username,
                    null,
                    email,
                    null,
                    null,
                    System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000),
                    rememberMe
            );
            
            return new AuthResult(true, newUser, "Registration successful (local)!");
        }
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        // Save updated user to database
        if (user != null) {
            dbManager.saveUser(user);
        }
    }

    public Map<String, User> getAllUsers() {
        return users;
    }
    
    public User getUser(String username) {
        return users.get(username);
    }
    
    public User getUserByUsername(String username) {
        return users.get(username);
    }

    public void logout() {
        currentUser = null;
        sessionManager.clearSession();
    }
    
    /**
     * Send password reset email via Firebase
     * @param email User's email
     * @return true if email sent successfully
     */
    public boolean sendPasswordResetEmail(String email) {
        if (firebaseAuth.isFirebaseEnabled()) {
            FirebaseAuthResult result = firebaseAuth.sendPasswordResetEmail(email);
            return result.isSuccess();
        }
        return false;
    }
    
    /**
     * Update password by email address
     * Updates both local SQLite database and attempts Firebase update
     * @param email User's email
     * @param newPassword New password to set
     * @return true if password was updated successfully
     */
    public boolean updatePasswordByEmail(String email, String newPassword) {
        // Find user by email
        User foundUser = null;
        String foundUsername = null;
        
        for (Map.Entry<String, User> entry : users.entrySet()) {
            User user = entry.getValue();
            if (email.equals(user.getEmail()) || email.equals(entry.getKey())) {
                foundUser = user;
                foundUsername = entry.getKey();
                break;
            }
        }
        
        // Also check by extracting username from email
        if (foundUser == null) {
            String username = extractUsernameFromEmail(email);
            foundUser = users.get(username);
            foundUsername = username;
        }
        
        if (foundUser != null) {
            // Update password in memory
            foundUser.setPassword(newPassword);
            
            // Update email if not set
            if (foundUser.getEmail() == null || foundUser.getEmail().isEmpty()) {
                foundUser.setEmail(email);
            }
            
            // Update in database
            dbManager.saveUser(foundUser);
            
            // Update in users map
            users.put(foundUsername, foundUser);
            
            System.out.println("Password updated successfully in SQLite for: " + foundUsername);
            return true;
        }
        
        System.out.println("User not found for email: " + email);
        return false;
    }
    
    /**
     * Result class for password reset with Firebase sync
     */
    public static class PasswordResetResult {
        private final boolean success;
        private final String message;
        private final boolean firebaseSynced;
        
        public PasswordResetResult(boolean success, String message, boolean firebaseSynced) {
            this.success = success;
            this.message = message;
            this.firebaseSynced = firebaseSynced;
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public boolean isFirebaseSynced() { return firebaseSynced; }
    }
    
    /**
     * Sync SQLite user to Firebase and update password
     * This will:
     * 1. Check if user exists in Firebase
     * 2. If not, create Firebase account with the new password
     * 3. If exists, update the password in Firebase
     * 4. Update password in SQLite
     * 
     * @param email User's email
     * @param newPassword New password to set
     * @param oldPassword Optional old password for existing Firebase users (can be null)
     * @return PasswordResetResult with details
     */
    public PasswordResetResult syncAndUpdatePassword(String email, String newPassword, String oldPassword) {
        // Find user in SQLite
        User foundUser = null;
        String foundUsername = null;
        
        for (Map.Entry<String, User> entry : users.entrySet()) {
            User user = entry.getValue();
            if (email.equals(user.getEmail()) || email.equals(entry.getKey())) {
                foundUser = user;
                foundUsername = entry.getKey();
                break;
            }
        }
        
        // Also check by extracting username from email
        if (foundUser == null) {
            String username = extractUsernameFromEmail(email);
            foundUser = users.get(username);
            foundUsername = username;
        }
        
        if (foundUser == null) {
            return new PasswordResetResult(false, "User not found with email: " + email, false);
        }
        
        boolean firebaseSynced = false;
        String firebaseMessage = "";
        
        // Try Firebase operations if enabled
        if (firebaseAuth.isFirebaseEnabled()) {
            // Check if user has Firebase UID - if not, try to create account or sync
            if (foundUser.getFirebaseUid() == null || foundUser.getFirebaseUid().isEmpty()) {
                // User exists in SQLite but not in Firebase - create Firebase account
                System.out.println("User not in Firebase, creating account for: " + email);
                
                FirebaseAuthResult signUpResult = firebaseAuth.signUp(email, newPassword);
                
                if (signUpResult.isSuccess()) {
                    // Successfully created Firebase account
                    foundUser.setFirebaseUid(signUpResult.getUserId());
                    foundUser.setEmail(email);
                    firebaseSynced = true;
                    firebaseMessage = "Firebase account created and synced!";
                    System.out.println("Firebase account created for: " + email);
                } else if (signUpResult.getMessage().contains("already registered") || 
                           signUpResult.getMessage().contains("EMAIL_EXISTS")) {
                    // Email already exists in Firebase - try to sign in with old password and update
                    System.out.println("Email exists in Firebase, trying to update password...");
                    
                    // Try with old password from SQLite
                    String passwordToTry = oldPassword != null ? oldPassword : foundUser.getPassword();
                    FirebaseAuthResult signInResult = firebaseAuth.signIn(email, passwordToTry);
                    
                    if (signInResult.isSuccess()) {
                        // Signed in - now update password
                        FirebaseAuthResult updateResult = firebaseAuth.updatePassword(
                                signInResult.getIdToken(), newPassword);
                        
                        if (updateResult.isSuccess()) {
                            foundUser.setFirebaseUid(signInResult.getUserId());
                            foundUser.setEmail(email);
                            firebaseSynced = true;
                            firebaseMessage = "Firebase password updated!";
                            System.out.println("Firebase password updated for: " + email);
                        } else {
                            firebaseMessage = "Firebase update failed: " + updateResult.getMessage();
                        }
                    } else {
                        // Can't sign in with old password - send reset email
                        firebaseAuth.sendPasswordResetEmail(email);
                        firebaseMessage = "Password reset email sent to Firebase. Please also reset via email.";
                    }
                } else {
                    firebaseMessage = "Firebase sync failed: " + signUpResult.getMessage();
                }
            } else {
                // User has Firebase UID - try to update password
                System.out.println("User has Firebase UID, updating password...");
                
                // Need to sign in first to get ID token
                String passwordToTry = oldPassword != null ? oldPassword : foundUser.getPassword();
                FirebaseAuthResult signInResult = firebaseAuth.signIn(email, passwordToTry);
                
                if (signInResult.isSuccess()) {
                    FirebaseAuthResult updateResult = firebaseAuth.updatePassword(
                            signInResult.getIdToken(), newPassword);
                    
                    if (updateResult.isSuccess()) {
                        firebaseSynced = true;
                        firebaseMessage = "Firebase password updated!";
                        System.out.println("Firebase password updated for: " + email);
                    } else {
                        firebaseMessage = "Firebase update failed: " + updateResult.getMessage();
                    }
                } else {
                    // Can't sign in - try creating new account (maybe Firebase was reset)
                    FirebaseAuthResult signUpResult = firebaseAuth.signUp(email, newPassword);
                    if (signUpResult.isSuccess()) {
                        foundUser.setFirebaseUid(signUpResult.getUserId());
                        firebaseSynced = true;
                        firebaseMessage = "Firebase account recreated!";
                    } else if (signUpResult.getMessage().contains("already registered") ||
                               signUpResult.getMessage().contains("EMAIL_EXISTS")) {
                        // Send reset email as last resort
                        firebaseAuth.sendPasswordResetEmail(email);
                        firebaseMessage = "Password reset email sent. Please also reset via email link.";
                    } else {
                        firebaseMessage = "Firebase sync failed: " + signUpResult.getMessage();
                    }
                }
            }
        } else {
            firebaseMessage = "Firebase not enabled, updated locally only.";
        }
        
        // Always update SQLite
        foundUser.setPassword(newPassword);
        if (foundUser.getEmail() == null || foundUser.getEmail().isEmpty()) {
            foundUser.setEmail(email);
        }
        dbManager.saveUser(foundUser);
        users.put(foundUsername, foundUser);
        
        String finalMessage = "Password updated in SQLite. " + firebaseMessage;
        return new PasswordResetResult(true, finalMessage, firebaseSynced);
    }
    
    /**
     * Check if Firebase authentication is enabled
     */
    public boolean isFirebaseEnabled() {
        return firebaseAuth.isFirebaseEnabled();
    }
    
    /**
     * Authentication result class
     */
    public static class AuthResult {
        private final boolean success;
        private final User user;
        private final String message;
        
        public AuthResult(boolean success, User user, String message) {
            this.success = success;
            this.user = user;
            this.message = message;
        }
        
        public boolean isSuccess() { return success; }
        public User getUser() { return user; }
        public String getMessage() { return message; }
    }
}
