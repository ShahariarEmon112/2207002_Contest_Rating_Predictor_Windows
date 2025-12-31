package com.contestpredictor.model;

public class Participant {
    private String username;
    private int currentRating;
    private int problemsSolved;
    private int totalPenalty; // in minutes
    private int rank;
    private int predictedRating;
    private int ratingChange;

    public Participant(String username, int currentRating, int problemsSolved, int totalPenalty) {
        this.username = username;
        this.currentRating = currentRating;
        this.problemsSolved = problemsSolved;
        this.totalPenalty = totalPenalty;
        this.rank = 0;
        this.predictedRating = currentRating;
        this.ratingChange = 0;
    }
    
    // Constructor with rank parameter
    public Participant(String username, int currentRating, int problemsSolved, int totalPenalty, int rank) {
        this.username = username;
        this.currentRating = currentRating;
        this.problemsSolved = problemsSolved;
        this.totalPenalty = totalPenalty;
        this.rank = rank;
        this.predictedRating = currentRating;
        this.ratingChange = 0;
    }

    // Getters and Setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getCurrentRating() {
        return currentRating;
    }

    public void setCurrentRating(int currentRating) {
        this.currentRating = currentRating;
    }

    public int getProblemsSolved() {
        return problemsSolved;
    }

    public void setProblemsSolved(int problemsSolved) {
        this.problemsSolved = problemsSolved;
    }

    public int getTotalPenalty() {
        return totalPenalty;
    }

    public void setTotalPenalty(int totalPenalty) {
        this.totalPenalty = totalPenalty;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public int getPredictedRating() {
        return predictedRating;
    }

    public void setPredictedRating(int predictedRating) {
        this.predictedRating = predictedRating;
    }

    public int getRatingChange() {
        return ratingChange;
    }

    public void setRatingChange(int ratingChange) {
        this.ratingChange = ratingChange;
    }
}
