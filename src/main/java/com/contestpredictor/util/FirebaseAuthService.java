package com.contestpredictor.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Properties;

/**
 * Firebase Authentication Service using Firebase REST API
 * Provides methods for user sign-up, sign-in, token refresh, and password reset
 */
public class FirebaseAuthService {
    private static FirebaseAuthService instance;
    private final Properties properties;
    private final HttpClient httpClient;
    private final Gson gson;
    
    private String apiKey;
    private String projectId;
    private boolean firebaseEnabled;
    
    // Current session data
    private String currentIdToken;
    private String currentRefreshToken;
    private String currentUserId;
    private String currentEmail;
    private long tokenExpirationTime;
    
    private FirebaseAuthService() {
        this.properties = new Properties();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.gson = new Gson();
        loadConfiguration();
    }
    
    public static synchronized FirebaseAuthService getInstance() {
        if (instance == null) {
            instance = new FirebaseAuthService();
        }
        return instance;
    }
    
    /**
     * Load Firebase configuration from properties file
     */
    private void loadConfiguration() {
        try {
            // Try loading from classpath first
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("firebase.properties");
            if (inputStream != null) {
                properties.load(inputStream);
                inputStream.close();
            } else {
                // Try loading from file system
                try (FileInputStream fis = new FileInputStream("src/main/resources/firebase.properties")) {
                    properties.load(fis);
                }
            }
            
            this.apiKey = properties.getProperty("firebase.api.key", "");
            this.projectId = properties.getProperty("firebase.project.id", "");
            this.firebaseEnabled = Boolean.parseBoolean(properties.getProperty("firebase.enabled", "true"));
            
            System.out.println("Firebase configuration loaded. Enabled: " + firebaseEnabled);
            
        } catch (IOException e) {
            System.err.println("Warning: Could not load firebase.properties. Using SQLite only mode.");
            this.firebaseEnabled = false;
        }
    }
    
    /**
     * Check if Firebase is properly configured and enabled
     */
    public boolean isFirebaseEnabled() {
        return firebaseEnabled && apiKey != null && !apiKey.isEmpty() 
                && !apiKey.equals("YOUR_API_KEY_HERE");
    }
    
    /**
     * Sign up a new user with email and password
     * @param email User's email address
     * @param password User's password (min 6 characters)
     * @return FirebaseAuthResult with user data or error
     */
    public FirebaseAuthResult signUp(String email, String password) {
        if (!isFirebaseEnabled()) {
            return new FirebaseAuthResult(false, "Firebase is not configured. Using local authentication.");
        }
        
        try {
            String url = properties.getProperty("firebase.auth.signup.url") + apiKey;
            
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("email", email);
            requestBody.addProperty("password", password);
            requestBody.addProperty("returnSecureToken", true);
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            return parseAuthResponse(response.body(), response.statusCode());
            
        } catch (Exception e) {
            System.err.println("Firebase sign-up error: " + e.getMessage());
            return new FirebaseAuthResult(false, "Network error: " + e.getMessage());
        }
    }
    
    /**
     * Sign in an existing user with email and password
     * @param email User's email address
     * @param password User's password
     * @return FirebaseAuthResult with user data or error
     */
    public FirebaseAuthResult signIn(String email, String password) {
        if (!isFirebaseEnabled()) {
            return new FirebaseAuthResult(false, "Firebase is not configured. Using local authentication.");
        }
        
        try {
            String url = properties.getProperty("firebase.auth.signin.url") + apiKey;
            
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("email", email);
            requestBody.addProperty("password", password);
            requestBody.addProperty("returnSecureToken", true);
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            return parseAuthResponse(response.body(), response.statusCode());
            
        } catch (Exception e) {
            System.err.println("Firebase sign-in error: " + e.getMessage());
            return new FirebaseAuthResult(false, "Network error: " + e.getMessage());
        }
    }
    
    /**
     * Refresh the ID token using the refresh token
     * @return FirebaseAuthResult with new token or error
     */
    public FirebaseAuthResult refreshToken() {
        if (!isFirebaseEnabled() || currentRefreshToken == null) {
            return new FirebaseAuthResult(false, "No refresh token available");
        }
        
        try {
            String url = properties.getProperty("firebase.auth.refresh.url") + apiKey;
            
            String requestBody = "grant_type=refresh_token&refresh_token=" + currentRefreshToken;
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                JsonObject jsonResponse = gson.fromJson(response.body(), JsonObject.class);
                this.currentIdToken = jsonResponse.get("id_token").getAsString();
                this.currentRefreshToken = jsonResponse.get("refresh_token").getAsString();
                int expiresIn = jsonResponse.get("expires_in").getAsInt();
                this.tokenExpirationTime = System.currentTimeMillis() + (expiresIn * 1000L);
                
                return new FirebaseAuthResult(true, "Token refreshed successfully");
            } else {
                return new FirebaseAuthResult(false, "Failed to refresh token");
            }
            
        } catch (Exception e) {
            System.err.println("Token refresh error: " + e.getMessage());
            return new FirebaseAuthResult(false, "Network error: " + e.getMessage());
        }
    }
    
    /**
     * Send password reset email
     * @param email User's email address
     * @return FirebaseAuthResult with success status
     */
    public FirebaseAuthResult sendPasswordResetEmail(String email) {
        if (!isFirebaseEnabled()) {
            return new FirebaseAuthResult(false, "Firebase is not configured");
        }
        
        try {
            String url = properties.getProperty("firebase.auth.resetpassword.url") + apiKey;
            
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("requestType", "PASSWORD_RESET");
            requestBody.addProperty("email", email);
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                return new FirebaseAuthResult(true, "Password reset email sent");
            } else {
                JsonObject errorResponse = gson.fromJson(response.body(), JsonObject.class);
                String errorMessage = parseFirebaseError(errorResponse);
                return new FirebaseAuthResult(false, errorMessage);
            }
            
        } catch (Exception e) {
            System.err.println("Password reset error: " + e.getMessage());
            return new FirebaseAuthResult(false, "Network error: " + e.getMessage());
        }
    }
    
    /**
     * Parse the authentication response from Firebase
     */
    private FirebaseAuthResult parseAuthResponse(String responseBody, int statusCode) {
        try {
            JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
            
            if (statusCode == 200) {
                // Success
                this.currentIdToken = jsonResponse.get("idToken").getAsString();
                this.currentRefreshToken = jsonResponse.get("refreshToken").getAsString();
                this.currentUserId = jsonResponse.get("localId").getAsString();
                this.currentEmail = jsonResponse.get("email").getAsString();
                
                int expiresIn = jsonResponse.get("expiresIn").getAsInt();
                this.tokenExpirationTime = System.currentTimeMillis() + (expiresIn * 1000L);
                
                FirebaseAuthResult result = new FirebaseAuthResult(true, "Authentication successful");
                result.setUserId(currentUserId);
                result.setEmail(currentEmail);
                result.setIdToken(currentIdToken);
                result.setRefreshToken(currentRefreshToken);
                result.setExpiresIn(expiresIn);
                
                return result;
                
            } else {
                // Error
                String errorMessage = parseFirebaseError(jsonResponse);
                return new FirebaseAuthResult(false, errorMessage);
            }
            
        } catch (Exception e) {
            return new FirebaseAuthResult(false, "Failed to parse response: " + e.getMessage());
        }
    }
    
    /**
     * Parse Firebase error messages to user-friendly messages
     */
    private String parseFirebaseError(JsonObject errorResponse) {
        try {
            JsonObject error = errorResponse.getAsJsonObject("error");
            String errorCode = error.get("message").getAsString();
            
            switch (errorCode) {
                case "EMAIL_EXISTS":
                    return "This email is already registered. Please sign in or use a different email.";
                case "INVALID_EMAIL":
                    return "Invalid email format. Please enter a valid email address.";
                case "WEAK_PASSWORD":
                    return "Password is too weak. Please use at least 6 characters.";
                case "EMAIL_NOT_FOUND":
                    return "No account found with this email. Please register first.";
                case "INVALID_PASSWORD":
                    return "Incorrect password. Please try again.";
                case "INVALID_LOGIN_CREDENTIALS":
                    return "Invalid email or password. Please check your credentials.";
                case "USER_DISABLED":
                    return "This account has been disabled. Please contact support.";
                case "TOO_MANY_ATTEMPTS_TRY_LATER":
                    return "Too many failed attempts. Please try again later.";
                case "OPERATION_NOT_ALLOWED":
                    return "Email/password authentication is not enabled. Please contact admin.";
                default:
                    return "Authentication error: " + errorCode;
            }
        } catch (Exception e) {
            return "Unknown error occurred";
        }
    }
    
    /**
     * Update password for an authenticated user
     * Uses the setAccountInfo endpoint to change password
     * @param idToken The user's current ID token
     * @param newPassword The new password (min 6 characters)
     * @return FirebaseAuthResult with success status
     */
    public FirebaseAuthResult updatePassword(String idToken, String newPassword) {
        if (!isFirebaseEnabled()) {
            return new FirebaseAuthResult(false, "Firebase is not configured");
        }
        
        if (newPassword == null || newPassword.length() < 6) {
            return new FirebaseAuthResult(false, "Password must be at least 6 characters");
        }
        
        try {
            String url = "https://identitytoolkit.googleapis.com/v1/accounts:update?key=" + apiKey;
            
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("idToken", idToken);
            requestBody.addProperty("password", newPassword);
            requestBody.addProperty("returnSecureToken", true);
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                // Parse the response to get new tokens
                JsonObject jsonResponse = gson.fromJson(response.body(), JsonObject.class);
                
                // Update current session with new tokens
                this.currentIdToken = jsonResponse.get("idToken").getAsString();
                if (jsonResponse.has("refreshToken")) {
                    this.currentRefreshToken = jsonResponse.get("refreshToken").getAsString();
                }
                
                FirebaseAuthResult result = new FirebaseAuthResult(true, "Password updated successfully");
                result.setIdToken(this.currentIdToken);
                result.setRefreshToken(this.currentRefreshToken);
                return result;
            } else {
                JsonObject errorResponse = gson.fromJson(response.body(), JsonObject.class);
                String errorMessage = parseFirebaseError(errorResponse);
                return new FirebaseAuthResult(false, errorMessage);
            }
            
        } catch (Exception e) {
            System.err.println("Password update error: " + e.getMessage());
            return new FirebaseAuthResult(false, "Network error: " + e.getMessage());
        }
    }
    
    /**
     * Verify email and password then update to new password
     * This is used for password reset when user knows their current password
     * @param email User's email
     * @param currentPassword Current password for verification
     * @param newPassword New password to set
     * @return FirebaseAuthResult with success status
     */
    public FirebaseAuthResult resetPasswordWithVerification(String email, String currentPassword, String newPassword) {
        // First sign in to get the ID token
        FirebaseAuthResult signInResult = signIn(email, currentPassword);
        
        if (!signInResult.isSuccess()) {
            return new FirebaseAuthResult(false, "Current password verification failed: " + signInResult.getMessage());
        }
        
        // Now update the password using the obtained ID token
        return updatePassword(signInResult.getIdToken(), newPassword);
    }
    
    /**
     * Sign out the current user
     */
    public void signOut() {
        this.currentIdToken = null;
        this.currentRefreshToken = null;
        this.currentUserId = null;
        this.currentEmail = null;
        this.tokenExpirationTime = 0;
    }
    
    /**
     * Check if the current token is valid and not expired
     */
    public boolean isTokenValid() {
        return currentIdToken != null && System.currentTimeMillis() < tokenExpirationTime;
    }
    
    /**
     * Get current user's email
     */
    public String getCurrentEmail() {
        return currentEmail;
    }
    
    /**
     * Get current user's Firebase UID
     */
    public String getCurrentUserId() {
        return currentUserId;
    }
    
    /**
     * Get current ID token (for API calls)
     */
    public String getCurrentIdToken() {
        return currentIdToken;
    }
    
    /**
     * Get current refresh token
     */
    public String getCurrentRefreshToken() {
        return currentRefreshToken;
    }
    
    /**
     * Set session data (used when restoring from saved session)
     */
    public void setSessionData(String idToken, String refreshToken, String userId, String email, long expirationTime) {
        this.currentIdToken = idToken;
        this.currentRefreshToken = refreshToken;
        this.currentUserId = userId;
        this.currentEmail = email;
        this.tokenExpirationTime = expirationTime;
    }
    
    /**
     * Store OTP in Firebase Realtime Database
     * @param email User's email (sanitized for Firebase key)
     * @param otp The 6-digit OTP
     * @return true if stored successfully
     */
    public boolean storeOtpInFirebase(String email, String otp) {
        if (!isFirebaseEnabled()) {
            return false;
        }
        
        try {
            String databaseUrl = properties.getProperty("firebase.database.url", "");
            if (databaseUrl.isEmpty() || databaseUrl.equals("https://YOUR_PROJECT_ID.firebaseio.com")) {
                System.out.println("Firebase Database URL not configured");
                return false;
            }
            
            // Sanitize email for Firebase key (replace @ and . with _)
            String sanitizedEmail = email.replace("@", "_at_").replace(".", "_dot_");
            
            // Create OTP data with timestamp
            JsonObject otpData = new JsonObject();
            otpData.addProperty("otp", otp);
            otpData.addProperty("email", email);
            otpData.addProperty("timestamp", System.currentTimeMillis());
            otpData.addProperty("expiresAt", System.currentTimeMillis() + (10 * 60 * 1000)); // 10 minutes expiry
            
            // Store in Firebase Realtime Database
            String url = databaseUrl + "/password_reset_otps/" + sanitizedEmail + ".json?auth=" + apiKey;
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(gson.toJson(otpData)))
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                System.out.println("OTP stored in Firebase for: " + email);
                return true;
            } else {
                System.err.println("Failed to store OTP in Firebase: " + response.body());
                return false;
            }
            
        } catch (Exception e) {
            System.err.println("Error storing OTP in Firebase: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Retrieve OTP from Firebase Realtime Database
     * Admin can check this to verify user's OTP
     * @param email User's email
     * @return OTP if exists and not expired, null otherwise
     */
    public String getOtpFromFirebase(String email) {
        if (!isFirebaseEnabled()) {
            return null;
        }
        
        try {
            String databaseUrl = properties.getProperty("firebase.database.url", "");
            if (databaseUrl.isEmpty() || databaseUrl.equals("https://YOUR_PROJECT_ID.firebaseio.com")) {
                return null;
            }
            
            // Sanitize email for Firebase key
            String sanitizedEmail = email.replace("@", "_at_").replace(".", "_dot_");
            
            String url = databaseUrl + "/password_reset_otps/" + sanitizedEmail + ".json?auth=" + apiKey;
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200 && !response.body().equals("null")) {
                JsonObject otpData = gson.fromJson(response.body(), JsonObject.class);
                
                // Check if OTP has expired
                long expiresAt = otpData.get("expiresAt").getAsLong();
                if (System.currentTimeMillis() < expiresAt) {
                    return otpData.get("otp").getAsString();
                } else {
                    System.out.println("OTP expired for: " + email);
                    deleteOtpFromFirebase(email); // Clean up expired OTP
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error retrieving OTP from Firebase: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Delete OTP from Firebase after use or expiry
     * @param email User's email
     */
    public void deleteOtpFromFirebase(String email) {
        if (!isFirebaseEnabled()) {
            return;
        }
        
        try {
            String databaseUrl = properties.getProperty("firebase.database.url", "");
            if (databaseUrl.isEmpty()) {
                return;
            }
            
            String sanitizedEmail = email.replace("@", "_at_").replace(".", "_dot_");
            String url = databaseUrl + "/password_reset_otps/" + sanitizedEmail + ".json?auth=" + apiKey;
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .DELETE()
                    .build();
            
            httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("OTP deleted from Firebase for: " + email);
            
        } catch (Exception e) {
            System.err.println("Error deleting OTP from Firebase: " + e.getMessage());
        }
    }
    
    /**
     * Inner class to hold authentication result
     */
    public static class FirebaseAuthResult {
        private final boolean success;
        private final String message;
        private String userId;
        private String email;
        private String idToken;
        private String refreshToken;
        private int expiresIn;
        
        public FirebaseAuthResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getUserId() { return userId; }
        public String getEmail() { return email; }
        public String getIdToken() { return idToken; }
        public String getRefreshToken() { return refreshToken; }
        public int getExpiresIn() { return expiresIn; }
        
        public void setUserId(String userId) { this.userId = userId; }
        public void setEmail(String email) { this.email = email; }
        public void setIdToken(String idToken) { this.idToken = idToken; }
        public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
        public void setExpiresIn(int expiresIn) { this.expiresIn = expiresIn; }
    }
}
