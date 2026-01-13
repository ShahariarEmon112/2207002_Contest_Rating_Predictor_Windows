package com.contestpredictor.controller;

import com.contestpredictor.data.AdminDatabase;
import com.contestpredictor.data.UserDatabase;
import com.contestpredictor.data.UserDatabase.AuthResult;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.stage.Stage;

public class RegistrationController {

    @FXML
    private TextField fullNameField;

    @FXML
    private TextField usernameField;
    
    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private Label messageLabel;
    
    @FXML
    private RadioButton contestantRadio;
    
    @FXML
    private RadioButton adminRadio;
    
    @FXML
    private CheckBox rememberMeCheckbox;
    
    @FXML
    private ProgressIndicator loadingIndicator;
    
    private ToggleGroup accountTypeGroup;
    private UserDatabase userDB;

    @FXML
    private void handleRegister() {
        String fullName = fullNameField.getText().trim();
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        boolean isAdmin = adminRadio.isSelected();

        // Validate input fields
        if (fullName.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            showError("Please fill in all required fields");
            return;
        }
        
        // Email is now required for Firebase registration
        if (email.isEmpty()) {
            showError("Email is required for registration");
            return;
        }
        
        // Validate email format
        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            showError("Please enter a valid email address");
            return;
        }

        // Validate full name (at least 2 characters)
        if (fullName.length() < 2) {
            showError("Full name must be at least 2 characters");
            return;
        }

        // Validate password (at least 6 characters for Firebase)
        if (password.length() < 6) {
            showError("Password must be at least 6 characters");
            return;
        }

        // Check if passwords match
        if (!password.equals(confirmPassword)) {
            showError("Passwords do not match");
            return;
        }
        
        // Show loading indicator
        if (loadingIndicator != null) {
            loadingIndicator.setVisible(true);
        }
        messageLabel.setVisible(false);

        // Attempt to register the user
        if (isAdmin) {
            // Admin registration (local only)
            AdminDatabase adminDB = AdminDatabase.getInstance();
            boolean success = adminDB.registerAdmin(username.isEmpty() ? extractUsername(email) : username, password, fullName);
            if (loadingIndicator != null) {
                loadingIndicator.setVisible(false);
            }
            if (success) {
                showSuccess("Admin account created successfully! Redirecting to login...");
                navigateToLoginAfterDelay();
            } else {
                showError("Admin username already exists. Please choose another one.");
            }
        } else {
            // User registration with Firebase
            boolean rememberMe = rememberMeCheckbox != null && rememberMeCheckbox.isSelected();
            
            // Run in background thread
            new Thread(() -> {
                AuthResult result = userDB.registerWithFirebase(email, password, fullName, rememberMe);
                
                Platform.runLater(() -> {
                    if (loadingIndicator != null) {
                        loadingIndicator.setVisible(false);
                    }
                    
                    if (result.isSuccess()) {
                        showSuccess(result.getMessage() + " Logging you in...");
                        // Navigate directly to profile since user is already logged in
                        navigateToProfileAfterDelay();
                    } else {
                        showError(result.getMessage());
                    }
                });
            }).start();
        }
    }
    
    private String extractUsername(String email) {
        if (email.contains("@")) {
            return email.substring(0, email.indexOf("@"));
        }
        return email;
    }
    
    private void navigateToLoginAfterDelay() {
        new Thread(() -> {
            try {
                Thread.sleep(1500);
                Platform.runLater(() -> handleBackToLogin());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }
    
    private void navigateToProfileAfterDelay() {
        new Thread(() -> {
            try {
                Thread.sleep(1000);
                Platform.runLater(() -> navigateToProfile());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }
    
    private void navigateToProfile() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Profile.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) usernameField.getScene().getWindow();
            
            // Preserve window state
            boolean wasFullScreen = stage.isFullScreen();
            boolean wasMaximized = stage.isMaximized();
            double currentWidth = stage.getWidth();
            double currentHeight = stage.getHeight();
            
            Scene scene = new Scene(root, currentWidth, currentHeight);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            
            stage.setScene(scene);
            stage.setTitle("Profile - Contest Rating Predictor");
            
            // Restore window state
            if (wasMaximized) {
                stage.setMaximized(true);
            }
            if (wasFullScreen) {
                stage.setFullScreen(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showError("Error loading profile: " + e.getMessage());
        }
    }

    @FXML
    private void handleBackToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) usernameField.getScene().getWindow();
            
            // Preserve window state
            boolean wasFullScreen = stage.isFullScreen();
            boolean wasMaximized = stage.isMaximized();
            double currentWidth = stage.getWidth();
            double currentHeight = stage.getHeight();
            
            Scene scene = new Scene(root, currentWidth, currentHeight);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            
            stage.setScene(scene);
            stage.setTitle("Login - Contest Rating Predictor");
            
            // Restore window state
            if (wasMaximized) {
                stage.setMaximized(true);
            }
            if (wasFullScreen) {
                stage.setFullScreen(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showError("Error loading login page: " + e.getMessage());
        }
    }

    @FXML
    private void initialize() {
        userDB = UserDatabase.getInstance();
        
        // Setup account type toggle group
        accountTypeGroup = new ToggleGroup();
        contestantRadio.setToggleGroup(accountTypeGroup);
        adminRadio.setToggleGroup(accountTypeGroup);
        contestantRadio.setSelected(true); // Default to contestant
        
        // Email field is now always visible for Firebase registration
        emailField.setVisible(true);
        emailField.setManaged(true);
        
        // Hide loading indicator initially
        if (loadingIndicator != null) {
            loadingIndicator.setVisible(false);
        }
        
        // Set remember me checked by default
        if (rememberMeCheckbox != null) {
            rememberMeCheckbox.setSelected(true);
        }
        
        // Add enter key handlers for smooth navigation
        fullNameField.setOnAction(event -> emailField.requestFocus());
        emailField.setOnAction(event -> passwordField.requestFocus());
        passwordField.setOnAction(event -> confirmPasswordField.requestFocus());
        confirmPasswordField.setOnAction(event -> handleRegister());
    }

    private void showError(String message) {
        messageLabel.setText(message);
        messageLabel.setStyle("-fx-text-fill: #d32f2f; -fx-background-color: #ffebee;");
        messageLabel.setVisible(true);
    }

    private void showSuccess(String message) {
        messageLabel.setText(message);
        messageLabel.setStyle("-fx-text-fill: #2e7d32; -fx-background-color: #e8f5e9;");
        messageLabel.setVisible(true);
    }
}
