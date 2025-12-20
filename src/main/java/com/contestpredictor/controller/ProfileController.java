package com.contestpredictor.controller;

import com.contestpredictor.data.UserDatabase;
import com.contestpredictor.model.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class ProfileController {

    @FXML
    private Label fullNameLabel;

    @FXML
    private Label usernameLabel;

    @FXML
    private Label ratingLabel;

    @FXML
    private Label ratingColorLabel;

    @FXML
    private Label contestsLabel;

    @FXML
    private Label maxRatingLabel;

    @FXML
    private void initialize() {
        loadUserProfile();
    }

    private void loadUserProfile() {
        UserDatabase userDB = UserDatabase.getInstance();
        User currentUser = userDB.getCurrentUser();

        if (currentUser != null) {
            fullNameLabel.setText(currentUser.getFullName());
            usernameLabel.setText("@" + currentUser.getUsername());
            
            int rating = currentUser.getCurrentRating();
            ratingLabel.setText(String.valueOf(rating));
            ratingColorLabel.setText(getRatingColor(rating));
            
            contestsLabel.setText(String.valueOf(currentUser.getContestsParticipated()));
            
            // Calculate max rating from history
            int maxRating = currentUser.getRatingHistory().stream()
                    .mapToInt(Integer::intValue)
                    .max()
                    .orElse(rating);
            maxRatingLabel.setText(String.valueOf(maxRating));
        }
    }

    private String getRatingColor(int rating) {
        if (rating < 400) return "Gray";
        else if (rating < 800) return "Brown";
        else if (rating < 1200) return "Green";
        else if (rating < 1600) return "Cyan";
        else if (rating < 2000) return "Blue";
        else if (rating < 2400) return "Yellow";
        else if (rating < 2800) return "Orange";
        else return "Red";
    }

    @FXML
    private void handlePredictor() {
        navigateTo("/fxml/Predictor.fxml", "Rating Predictor");
    }

    @FXML
    private void handleSearchContest() {
        navigateTo("/fxml/SearchContest.fxml", "Search Contests");
    }

    @FXML
    private void handleLogout() {
        UserDatabase.getInstance().logout();
        navigateTo("/fxml/Login.fxml", "Login");
    }
    
    private void navigateTo(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            
            int width = fxmlPath.contains("Login") ? 1000 : 1200;
            int height = fxmlPath.contains("Login") ? 650 : 800;
            Scene scene = new Scene(root, width, height);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            
            Stage stage = (Stage) fullNameLabel.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle(title + " - Contest Rating Predictor");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
