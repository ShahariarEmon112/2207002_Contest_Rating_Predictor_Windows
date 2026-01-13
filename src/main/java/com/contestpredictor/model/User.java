package com.contestpredictor.model;

import java.util.ArrayList;
import java.util.List;

public class User {
    private String username;
    private String password;
    private int currentRating;
    private int contestsParticipated;
    private String fullName;
    private List<Integer> ratingHistory;
    
    // Firebase-related fields
    private String email;
    private String firebaseUid;

    public User(String username, String password, int currentRating, int contestsParticipated, String fullName) {
        this.username = username;
        this.password = password;
        this.currentRating = currentRating;
        this.contestsParticipated = contestsParticipated;
        this.fullName = fullName;
        this.ratingHistory = new ArrayList<>();
        this.ratingHistory.add(currentRating); // Add initial rating
    }

    // Getters and Setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getCurrentRating() {
        return currentRating;
    }

    public void setCurrentRating(int currentRating) {
        this.currentRating = currentRating;
    }

    public int getContestsParticipated() {
        return contestsParticipated;
    }

    public void setContestsParticipated(int contestsParticipated) {
        this.contestsParticipated = contestsParticipated;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public List<Integer> getRatingHistory() {
        return ratingHistory;
    }

    public void addRatingToHistory(int rating) {
        this.ratingHistory.add(rating);
        this.currentRating = rating;
    }
    
    // Firebase-related getters and setters
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getFirebaseUid() {
        return firebaseUid;
    }
    
    public void setFirebaseUid(String firebaseUid) {
        this.firebaseUid = firebaseUid;
    }
}
