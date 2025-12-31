package com.contestpredictor.util;

import com.contestpredictor.model.Contestant;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Dynamic rating predictor using AtCoder-style ranking algorithm
 * Fetches contestants from Codeforces API and calculates rating changes based on rank
 */
public class ContestantRatingPredictor {
    
    private static final String CODEFORCES_API_URL = "https://codeforces.com/api/user.ratedList?activeOnly=true";
    
    /**
     * Fetches contestants dynamically from Codeforces API
     * @param limit Number of contestants to fetch
     * @return List of contestants with their current ratings
     */
    public static List<Contestant> fetchContestants(int limit) throws Exception {
        URL url = new java.net.URI(CODEFORCES_API_URL).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        
        BufferedReader br = new BufferedReader(
            new InputStreamReader(conn.getInputStream())
        );
        
        StringBuilder json = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            json.append(line);
        }
        br.close();
        
        Gson gson = new Gson();
        JsonObject root = gson.fromJson(json.toString(), JsonObject.class);
        
        // Check API response status
        String status = root.get("status").getAsString();
        if (!"OK".equals(status)) {
            throw new Exception("API returned error: " + root.get("comment").getAsString());
        }
        
        JsonArray users = root.getAsJsonArray("result");
        List<Contestant> list = new ArrayList<>();
        
        int count = Math.min(limit, users.size());
        for (int i = 0; i < count; i++) {
            JsonObject u = users.get(i).getAsJsonObject();
            String handle = u.get("handle").getAsString();
            int rating = u.get("rating").getAsInt();
            
            // Rank will be assigned later
            list.add(new Contestant(handle, rating));
        }
        
        return list;
    }
    
    /**
     * Assigns contest ranks to contestants based on problems solved and penalty
     * @param contestants List of contestants
     */
    public static void assignRanks(List<Contestant> contestants) {
        // Sort by problems solved (descending), then by penalty (ascending)
        contestants.sort((a, b) -> {
            if (a.getProblemsSolved() != b.getProblemsSolved()) {
                return b.getProblemsSolved() - a.getProblemsSolved();
            }
            return a.getPenalty() - b.getPenalty();
        });
        
        for (int i = 0; i < contestants.size(); i++) {
            contestants.get(i).setRank(i + 1);
        }
    }
    
    /**
     * Calculates performance rating for a given rank using AtCoder formula
     * Performance = avgRating + 400 * log2(totalParticipants / rank)
     * @param rank Contestant's rank
     * @param totalParticipants Total number of participants
     * @param avgRating Average rating of all participants
     * @return Performance rating
     */
    public static int calculatePerformance(int rank, int totalParticipants, double avgRating) {
        if (rank <= 0 || totalParticipants <= 0) return (int) avgRating;
        
        double performance = avgRating + 400.0 * (Math.log(totalParticipants / (double) rank) / Math.log(2));
        return (int) Math.round(performance);
    }
    
    /**
     * Calculates rating change based on AtCoder algorithm
     * Delta = (Performance - OldRating) * adjustmentFactor
     * Adjustment factor depends on number of contests participated
     * @param oldRating Current rating
     * @param performance Performance in this contest
     * @param contestCount Number of contests participated
     * @return Rating change
     */
    public static int calculateRatingChange(int oldRating, int performance, int contestCount) {
        // Adjustment factor: higher for newer accounts (like AtCoder)
        double adjustmentFactor;
        if (contestCount == 0) {
            adjustmentFactor = 1.0; // First contest
        } else if (contestCount <= 10) {
            adjustmentFactor = 0.9 - (contestCount * 0.05); // Decreasing from 0.9 to 0.4
        } else if (contestCount <= 50) {
            adjustmentFactor = 0.4 - ((contestCount - 10) * 0.005); // Slow decrease
        } else {
            adjustmentFactor = 0.2; // Stable for experienced users
        }
        
        int delta = (int) Math.round((performance - oldRating) * adjustmentFactor);
        return delta;
    }
    
    /**
     * Computes rating changes for all contestants using rank-based AtCoder algorithm
     * No expected rank calculation - purely based on actual rank and average rating
     * @param contestants List of contestants
     */
    public static void computeRatingChanges(List<Contestant> contestants) {
        if (contestants.isEmpty()) return;
        
        int totalParticipants = contestants.size();
        
        // Calculate average rating
        double avgRating = contestants.stream()
            .mapToInt(Contestant::getOldRating)
            .average()
            .orElse(1500.0);
        
        // Calculate performance and delta for each contestant
        for (Contestant contestant : contestants) {
            if (contestant.getRank() == 0) {
                contestant.setDelta(0);
                continue;
            }
            
            int performance = calculatePerformance(
                contestant.getRank(), 
                totalParticipants, 
                avgRating
            );
            
            // Assume average contest participation of 10 (can be customized)
            int contestCount = 10;
            int delta = calculateRatingChange(
                contestant.getOldRating(), 
                performance, 
                contestCount
            );
            
            contestant.setDelta(delta);
        }
    }
    
    /**
     * Updates ratings for all contestants based on their deltas
     * @param contestants List of contestants
     */
    public static void updateRatings(List<Contestant> contestants) {
        for (Contestant contestant : contestants) {
            int newRating = contestant.getOldRating() + contestant.getDelta();
            contestant.setNewRating(newRating);
        }
    }
    
    /**
     * Complete rating prediction pipeline
     * Fetches contestants, assigns ranks, calculates rating changes using AtCoder formula
     * @param limit Number of contestants to fetch
     * @return List of contestants with predicted ratings
     */
    public static List<Contestant> runRatingPredictor(int limit) throws Exception {
        List<Contestant> contestants = fetchContestants(limit);
        predictRatings(contestants);
        return contestants;
    }
    
    /**
     * Predict ratings for existing list of contestants using rank-based AtCoder algorithm
     * @param contestants List of contestants with assigned ranks
     */
    public static void predictRatings(List<Contestant> contestants) {
        assignRanks(contestants);
        computeRatingChanges(contestants);
        updateRatings(contestants);
    }
}
