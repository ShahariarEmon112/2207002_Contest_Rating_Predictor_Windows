package com.contestpredictor.controller;

import com.contestpredictor.data.AdminDatabase;
import com.contestpredictor.data.UserDatabase;
import com.contestpredictor.data.UserDatabase.AuthResult;
import com.contestpredictor.data.UserDatabase.PasswordResetResult;
import com.contestpredictor.model.Admin;
import com.contestpredictor.model.User;
import com.contestpredictor.util.FirebaseAuthService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.security.SecureRandom;

public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label errorLabel;
    
    @FXML
    private CheckBox rememberMeCheckbox;
    
    @FXML
    private ProgressIndicator loadingIndicator;
    
    private UserDatabase userDB;

    @FXML
    private void initialize() {
        userDB = UserDatabase.getInstance();
        
        // Add enter key handler for password field
        passwordField.setOnAction(event -> handleLogin());
        usernameField.setOnAction(event -> passwordField.requestFocus());
        
        // Hide loading indicator initially
        if (loadingIndicator != null) {
            loadingIndicator.setVisible(false);
        }
        
        // Set remember me checked by default for one-time login
        if (rememberMeCheckbox != null) {
            rememberMeCheckbox.setSelected(true);
        }
        
        // Try auto-login if there's a saved session
        Platform.runLater(() -> {
            User autoLoginUser = userDB.tryAutoLogin();
            if (autoLoginUser != null) {
                navigateToProfile();
            }
        });
    }

    @FXML
    private void handleLogin() {
        String usernameOrEmail = usernameField.getText().trim();
        String password = passwordField.getText();

        if (usernameOrEmail.isEmpty() || password.isEmpty()) {
            showError("Please enter both username/email and password");
            return;
        }
        
        // Show loading indicator
        if (loadingIndicator != null) {
            loadingIndicator.setVisible(true);
        }
        errorLabel.setVisible(false);

        // Check if admin login first
        AdminDatabase adminDB = AdminDatabase.getInstance();
        Admin admin = adminDB.authenticate(usernameOrEmail, password);
        
        if (admin != null) {
            // Admin login successful - navigate to admin dashboard
            if (loadingIndicator != null) {
                loadingIndicator.setVisible(false);
            }
            navigateToAdminDashboard(admin);
            return;
        }
        
        // Try Firebase authentication (with fallback to local)
        boolean rememberMe = rememberMeCheckbox != null && rememberMeCheckbox.isSelected();
        
        // Run authentication in background thread to not block UI
        new Thread(() -> {
            AuthResult result = userDB.authenticateWithFirebase(usernameOrEmail, password, rememberMe);
            
            Platform.runLater(() -> {
                if (loadingIndicator != null) {
                    loadingIndicator.setVisible(false);
                }
                
                if (result.isSuccess()) {
                    navigateToProfile();
                } else {
                    showError(result.getMessage());
                }
            });
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
    
    private void navigateToAdminDashboard(Admin admin) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AdminDashboard.fxml"));
            Parent root = loader.load();
            
            AdminDashboardController controller = loader.getController();
            controller.setAdmin(admin);
            
            Stage stage = (Stage) usernameField.getScene().getWindow();
            
            // Preserve window state
            boolean wasFullScreen = stage.isFullScreen();
            boolean wasMaximized = stage.isMaximized();
            double currentWidth = stage.getWidth();
            double currentHeight = stage.getHeight();
            
            Scene scene = new Scene(root, currentWidth, currentHeight);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            
            stage.setScene(scene);
            stage.setTitle("Admin Dashboard - Contest Rating Predictor");
            
            // Restore window state
            if (wasMaximized) {
                stage.setMaximized(true);
            }
            if (wasFullScreen) {
                stage.setFullScreen(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showError("Error loading admin dashboard: " + e.getMessage());
        }
    }

    @FXML
    private void handleGoToAdminLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AdminLogin.fxml"));
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
            stage.setTitle("Admin Login - Contest Rating Predictor");
            
            // Restore window state
            if (wasMaximized) {
                stage.setMaximized(true);
            }
            if (wasFullScreen) {
                stage.setFullScreen(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showError("Error loading admin login: " + e.getMessage());
        }
    }

    @FXML
    private void handleGoToRegister() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Register.fxml"));
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
            stage.setTitle("Register - Contest Rating Predictor");
            
            // Restore window state
            if (wasMaximized) {
                stage.setMaximized(true);
            }
            if (wasFullScreen) {
                stage.setFullScreen(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showError("Error loading registration page: " + e.getMessage());
        }
    }
    
    /**
     * Handle forgot password - shows OTP dialog and updates password in Firebase
     */
    @FXML
    private void handleForgotPassword() {
        // Create the custom dialog
        Stage dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.initOwner(usernameField.getScene().getWindow());
        dialogStage.setTitle("Reset Password");
        
        VBox dialogRoot = new VBox(20);
        dialogRoot.setPadding(new Insets(30));
        dialogRoot.setAlignment(Pos.CENTER);
        dialogRoot.setStyle("-fx-background-color: #2b2b2b;");
        
        // Title
        Label titleLabel = new Label("🔐 Password Reset");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #4fc3f7;");
        
        // Step indicator
        Label stepLabel = new Label("Step 1: Enter your email address");
        stepLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #b0b0b0;");
        
        // Email input
        VBox emailBox = new VBox(8);
        Label emailLabel = new Label("Email Address");
        emailLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: white;");
        TextField emailInput = new TextField();
        emailInput.setPromptText("Enter your registered email");
        emailInput.setStyle("-fx-font-size: 14px; -fx-pref-height: 40px; -fx-background-color: #3c3f41; -fx-text-fill: white;");
        emailInput.setPrefWidth(300);
        emailBox.getChildren().addAll(emailLabel, emailInput);
        
        // OTP display area (initially hidden)
        VBox otpDisplayBox = new VBox(10);
        otpDisplayBox.setAlignment(Pos.CENTER);
        otpDisplayBox.setVisible(false);
        otpDisplayBox.setManaged(false);
        
        Label otpInfoLabel = new Label("Your One-Time Password (OTP):");
        otpInfoLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #b0b0b0;");
        
        Label otpLabel = new Label();
        otpLabel.setStyle("-fx-font-size: 36px; -fx-font-weight: bold; -fx-text-fill: #4caf50; " +
                "-fx-background-color: #1e1e1e; -fx-padding: 15 30; -fx-background-radius: 10;");
        
        Label otpHintLabel = new Label("⚠️ Note this OTP - you'll need to enter it below");
        otpHintLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #ff9800;");
        
        otpDisplayBox.getChildren().addAll(otpInfoLabel, otpLabel, otpHintLabel);
        
        // OTP verification input (initially hidden)
        VBox otpVerifyBox = new VBox(8);
        otpVerifyBox.setVisible(false);
        otpVerifyBox.setManaged(false);
        
        Label otpVerifyLabel = new Label("Enter OTP");
        otpVerifyLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: white;");
        TextField otpInput = new TextField();
        otpInput.setPromptText("Enter the 6-digit OTP shown above");
        otpInput.setStyle("-fx-font-size: 16px; -fx-pref-height: 40px; -fx-background-color: #3c3f41; " +
                "-fx-text-fill: white; -fx-alignment: center;");
        otpInput.setPrefWidth(300);
        otpVerifyBox.getChildren().addAll(otpVerifyLabel, otpInput);
        
        // New password fields (initially hidden)
        VBox passwordBox = new VBox(15);
        passwordBox.setVisible(false);
        passwordBox.setManaged(false);
        
        // Old password field (optional, for Firebase sync)
        VBox oldPassBox = new VBox(8);
        Label oldPassLabel = new Label("Current Password (optional, for Firebase sync)");
        oldPassLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: white;");
        PasswordField oldPasswordField = new PasswordField();
        oldPasswordField.setPromptText("Enter current password if known");
        oldPasswordField.setStyle("-fx-font-size: 14px; -fx-pref-height: 40px; -fx-background-color: #3c3f41; -fx-text-fill: white;");
        Label oldPassHint = new Label("(Helps sync your account with Firebase)");
        oldPassHint.setStyle("-fx-font-size: 11px; -fx-text-fill: #888;");
        oldPassBox.getChildren().addAll(oldPassLabel, oldPasswordField, oldPassHint);
        
        VBox newPassBox = new VBox(8);
        Label newPassLabel = new Label("New Password");
        newPassLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: white;");
        PasswordField newPasswordField = new PasswordField();
        newPasswordField.setPromptText("Enter new password (min 6 characters)");
        newPasswordField.setStyle("-fx-font-size: 14px; -fx-pref-height: 40px; -fx-background-color: #3c3f41; -fx-text-fill: white;");
        newPassBox.getChildren().addAll(newPassLabel, newPasswordField);
        
        VBox confirmPassBox = new VBox(8);
        Label confirmPassLabel = new Label("Confirm New Password");
        confirmPassLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: white;");
        PasswordField confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText("Confirm your new password");
        confirmPasswordField.setStyle("-fx-font-size: 14px; -fx-pref-height: 40px; -fx-background-color: #3c3f41; -fx-text-fill: white;");
        confirmPassBox.getChildren().addAll(confirmPassLabel, confirmPasswordField);
        
        passwordBox.getChildren().addAll(oldPassBox, newPassBox, confirmPassBox);
        
        // Message label
        Label messageLabel = new Label();
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(300);
        messageLabel.setStyle("-fx-font-size: 12px;");
        
        // Loading indicator
        ProgressIndicator dialogLoading = new ProgressIndicator();
        dialogLoading.setMaxSize(30, 30);
        dialogLoading.setVisible(false);
        
        // Buttons
        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER);
        
        Button generateOtpButton = new Button("Generate OTP");
        generateOtpButton.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-background-color: #4fc3f7; " +
                "-fx-text-fill: white; -fx-padding: 10 25; -fx-cursor: hand;");
        
        Button verifyOtpButton = new Button("Verify OTP");
        verifyOtpButton.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-background-color: #ff9800; " +
                "-fx-text-fill: white; -fx-padding: 10 25; -fx-cursor: hand;");
        verifyOtpButton.setVisible(false);
        verifyOtpButton.setManaged(false);
        
        Button resetPasswordButton = new Button("Reset Password");
        resetPasswordButton.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-background-color: #4caf50; " +
                "-fx-text-fill: white; -fx-padding: 10 25; -fx-cursor: hand;");
        resetPasswordButton.setVisible(false);
        resetPasswordButton.setManaged(false);
        
        Button cancelButton = new Button("Cancel");
        cancelButton.setStyle("-fx-font-size: 14px; -fx-background-color: #666; -fx-text-fill: white; " +
                "-fx-padding: 10 25; -fx-cursor: hand;");
        
        buttonBox.getChildren().addAll(generateOtpButton, verifyOtpButton, resetPasswordButton, cancelButton);
        
        // Store the generated OTP
        final String[] generatedOtp = {null};
        final String[] userEmail = {null};
        
        // Generate OTP button action
        generateOtpButton.setOnAction(e -> {
            String email = emailInput.getText().trim();
            
            if (email.isEmpty()) {
                messageLabel.setText("Please enter your email address");
                messageLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #f44336;");
                return;
            }
            
            if (!email.contains("@") || !email.contains(".")) {
                messageLabel.setText("Please enter a valid email address");
                messageLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #f44336;");
                return;
            }
            
            // Show loading
            dialogLoading.setVisible(true);
            generateOtpButton.setDisable(true);
            
            new Thread(() -> {
                // Generate 6-digit OTP
                SecureRandom random = new SecureRandom();
                int otp = 100000 + random.nextInt(900000);
                generatedOtp[0] = String.valueOf(otp);
                userEmail[0] = email;
                
                // Store OTP in Firebase Realtime Database for admin verification
                FirebaseAuthService firebaseAuth = FirebaseAuthService.getInstance();
                boolean storedInFirebase = firebaseAuth.storeOtpInFirebase(email, generatedOtp[0]);
                
                Platform.runLater(() -> {
                    dialogLoading.setVisible(false);
                    
                    // Show OTP
                    otpLabel.setText(generatedOtp[0]);
                    
                    // Update UI - show OTP and verification
                    stepLabel.setText("Step 2: Note the OTP and enter it below");
                    emailBox.setDisable(true);
                    
                    otpDisplayBox.setVisible(true);
                    otpDisplayBox.setManaged(true);
                    
                    otpVerifyBox.setVisible(true);
                    otpVerifyBox.setManaged(true);
                    
                    generateOtpButton.setVisible(false);
                    generateOtpButton.setManaged(false);
                    
                    verifyOtpButton.setVisible(true);
                    verifyOtpButton.setManaged(true);
                    
                    String msg = "OTP generated! Enter it below to continue.";
                    if (storedInFirebase) {
                        msg += "\n✓ OTP saved to Firebase (visible to admin)";
                    }
                    messageLabel.setText(msg);
                    messageLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #4caf50;");
                });
            }).start();
        });
        
        // Verify OTP button action
        verifyOtpButton.setOnAction(e -> {
            String enteredOtp = otpInput.getText().trim();
            
            if (enteredOtp.isEmpty()) {
                messageLabel.setText("Please enter the OTP");
                messageLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #f44336;");
                return;
            }
            
            if (!enteredOtp.equals(generatedOtp[0])) {
                messageLabel.setText("Invalid OTP! Please check and try again.");
                messageLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #f44336;");
                return;
            }
            
            // OTP verified - show password fields
            stepLabel.setText("Step 3: Set your new password");
            
            otpDisplayBox.setVisible(false);
            otpDisplayBox.setManaged(false);
            
            otpVerifyBox.setDisable(true);
            
            passwordBox.setVisible(true);
            passwordBox.setManaged(true);
            
            verifyOtpButton.setVisible(false);
            verifyOtpButton.setManaged(false);
            
            resetPasswordButton.setVisible(true);
            resetPasswordButton.setManaged(true);
            
            messageLabel.setText("OTP verified! Enter your new password.");
            messageLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #4caf50;");
        });
        
        // Reset password button action
        resetPasswordButton.setOnAction(e -> {
            String newPassword = newPasswordField.getText();
            String confirmPassword = confirmPasswordField.getText();
            
            if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
                messageLabel.setText("Please enter and confirm your new password");
                messageLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #f44336;");
                return;
            }
            
            if (newPassword.length() < 6) {
                messageLabel.setText("Password must be at least 6 characters");
                messageLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #f44336;");
                return;
            }
            
            if (!newPassword.equals(confirmPassword)) {
                messageLabel.setText("Passwords do not match!");
                messageLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #f44336;");
                return;
            }
            
            // Show loading
            dialogLoading.setVisible(true);
            resetPasswordButton.setDisable(true);
            messageLabel.setText("Syncing with Firebase and updating password...");
            messageLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #4fc3f7;");
            
            // Get old password if provided (for Firebase sync)
            String oldPassword = oldPasswordField.getText();
            if (oldPassword.isEmpty()) {
                oldPassword = null;
            }
            final String oldPwd = oldPassword;
            
            // Sync SQLite user to Firebase and update password
            new Thread(() -> {
                // Use the new sync and update method
                PasswordResetResult result = userDB.syncAndUpdatePassword(userEmail[0], newPassword, oldPwd);
                
                // Delete OTP from Firebase after successful password reset
                if (result.isSuccess()) {
                    FirebaseAuthService firebaseAuth = FirebaseAuthService.getInstance();
                    firebaseAuth.deleteOtpFromFirebase(userEmail[0]);
                }
                
                Platform.runLater(() -> {
                    dialogLoading.setVisible(false);
                    
                    if (result.isSuccess()) {
                        String successMsg = "✓ Password updated successfully!";
                        if (result.isFirebaseSynced()) {
                            successMsg += "\n✓ Synced with Firebase!";
                        } else {
                            successMsg += "\n⚠ " + result.getMessage();
                        }
                        messageLabel.setText(successMsg);
                        messageLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #4caf50;");
                        
                        resetPasswordButton.setVisible(false);
                        resetPasswordButton.setManaged(false);
                        
                        // Change cancel button to close
                        cancelButton.setText("Close");
                        cancelButton.setStyle("-fx-font-size: 14px; -fx-background-color: #4caf50; " +
                                "-fx-text-fill: white; -fx-padding: 10 25; -fx-cursor: hand;");
                    } else {
                        messageLabel.setText("Failed: " + result.getMessage());
                        messageLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #f44336;");
                        resetPasswordButton.setDisable(false);
                    }
                });
            }).start();
        });
        
        // Cancel button action
        cancelButton.setOnAction(e -> dialogStage.close());
        
        // Assemble dialog
        dialogRoot.getChildren().addAll(
                titleLabel, stepLabel, emailBox, otpDisplayBox, 
                otpVerifyBox, passwordBox, messageLabel, dialogLoading, buttonBox
        );
        
        Scene dialogScene = new Scene(dialogRoot, 400, 550);
        dialogStage.setScene(dialogScene);
        dialogStage.setResizable(false);
        dialogStage.showAndWait();
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
}
