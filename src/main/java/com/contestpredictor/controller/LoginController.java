package com.contestpredictor.controller;

import com.contestpredictor.data.AdminDatabase;
import com.contestpredictor.data.UserDatabase;
import com.contestpredictor.model.Admin;
import com.contestpredictor.model.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label errorLabel;

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Please enter both username and password");
            return;
        }

        UserDatabase userDB = UserDatabase.getInstance();
        User user = userDB.authenticate(username, password);

        if (user != null) {
            // Login successful - navigate to profile
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
        } else {
            showError("Invalid username or password");
        }
    }
    
    @FXML
    private void handleAdminLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Please enter both username and password");
            return;
        }

        AdminDatabase adminDB = AdminDatabase.getInstance();
        Admin admin = adminDB.authenticate(username, password);

        if (admin != null) {
            // Admin login successful - navigate to admin dashboard
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AdminDashboard.fxml"));
                Parent root = loader.load();
                
                // Pass admin data to controller
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
        } else {
            showError("Invalid admin credentials");
        }
    }

    @FXML
    private void initialize() {
        // Add enter key handler for password field
        passwordField.setOnAction(event -> handleLogin());
        usernameField.setOnAction(event -> passwordField.requestFocus());
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

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
}
