package com.contestpredictor.controller;

import com.contestpredictor.data.UserDatabase;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class RegistrationController {

    @FXML
    private TextField fullNameField;

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private Label messageLabel;

    @FXML
    private void handleRegister() {
        String fullName = fullNameField.getText().trim();
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        // Validate input fields
        if (fullName.isEmpty() || username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            showError("Please fill in all fields");
            return;
        }

        // Validate full name (at least 2 characters)
        if (fullName.length() < 2) {
            showError("Full name must be at least 2 characters");
            return;
        }

        // Validate username (at least 4 characters, alphanumeric)
        if (username.length() < 4) {
            showError("Username must be at least 4 characters");
            return;
        }

        if (!username.matches("^[a-zA-Z0-9_]+$")) {
            showError("Username can only contain letters, numbers, and underscores");
            return;
        }

        // Validate password (at least 6 characters)
        if (password.length() < 6) {
            showError("Password must be at least 6 characters");
            return;
        }

        // Check if passwords match
        if (!password.equals(confirmPassword)) {
            showError("Passwords do not match");
            return;
        }

        // Attempt to register the user
        UserDatabase userDB = UserDatabase.getInstance();
        boolean success = userDB.registerUser(username, password, fullName);

        if (success) {
            showSuccess("Account created successfully! Redirecting to profile...");
            
            // Navigate to profile after a short delay
            new Thread(() -> {
                try {
                    Thread.sleep(1500);
                    javafx.application.Platform.runLater(() -> navigateToProfile());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        } else {
            showError("Username already exists. Please choose another one.");
        }
    }

    @FXML
    private void handleBackToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
            Parent root = loader.load();
            
            Scene scene = new Scene(root, 1000, 650);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Login - Contest Rating Predictor");
        } catch (Exception e) {
            e.printStackTrace();
            showError("Error loading login page: " + e.getMessage());
        }
    }

    private void navigateToProfile() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Profile.fxml"));
            Parent root = loader.load();
            
            Scene scene = new Scene(root, 1200, 800);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Profile - Contest Rating Predictor");
        } catch (Exception e) {
            e.printStackTrace();
            showError("Error loading profile: " + e.getMessage());
        }
    }

    @FXML
    private void initialize() {
        // Add enter key handlers for smooth navigation
        fullNameField.setOnAction(event -> usernameField.requestFocus());
        usernameField.setOnAction(event -> passwordField.requestFocus());
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
