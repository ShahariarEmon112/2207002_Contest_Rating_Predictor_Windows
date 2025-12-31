package com.contestpredictor.util;

import com.contestpredictor.model.Participant;
import java.util.List;
import java.util.Collections;
import java.util.Comparator;

/**
 * Rating prediction algorithm based on AtCoder-style rating system
 * Formula considers: rank, problems solved, current rating, and performance
 */
public class RatingPredictor {

    /**
     * Calculate predicted ratings for all participants
     * @param participants List of participants with their performance data
     */
    public static void calculateRatings(List<Participant> participants) {
        // Sort participants by performance (problems solved desc, then penalty asc)
        Collections.sort(participants, new Comparator<Participant>() {
            @Override
            public int compare(Participant p1, Participant p2) {
                if (p1.getProblemsSolved() != p2.getProblemsSolved()) {
                    return p2.getProblemsSolved() - p1.getProblemsSolved();
                }
                return p1.getTotalPenalty() - p2.getTotalPenalty();
            }
        });

        // Assign ranks
        for (int i = 0; i < participants.size(); i++) {
            participants.get(i).setRank(i + 1);
        }

        // Calculate average rating
        double avgRating = 0;
        for (Participant p : participants) {
            avgRating += p.getCurrentRating();
        }
        avgRating /= participants.size();

        // Calculate predicted ratings
        for (Participant p : participants) {
            int predictedRating = calculateIndividualRating(p, participants.size(), avgRating);
            p.setPredictedRating(predictedRating);
            p.setRatingChange(predictedRating - p.getCurrentRating());
        }
    }

    /**
     * Calculate individual rating using improved Codeforces/AtCoder-inspired formula
     * @param participant The participant
     * @param totalParticipants Total number of participants
     * @param avgRating Average rating of all participants
     * @return Predicted new rating
     */
    private static int calculateIndividualRating(Participant participant, int totalParticipants, double avgRating) {
        int currentRating = participant.getCurrentRating();
        int rank = participant.getRank();
        int problemsSolved = participant.getProblemsSolved();

        // Calculate performance rating from actual rank
        double performance = calculatePerformance(rank, totalParticipants, avgRating);

        // Calculate rating change using Elo-based system
        double delta = (performance - currentRating) / 2.0;

        // Apply problem-solving multiplier (better reward for solving more)
        double solvingMultiplier = 1.0 + (problemsSolved * 0.08);
        delta *= solvingMultiplier;

        // Apply K-factor based on rating tier
        double kFactor = getKFactor(currentRating);
        delta *= (kFactor / 32.0);

        // Calculate expected rank and apply bonus/penalty
        double expectedRank = calculateExpectedRank(currentRating, avgRating, totalParticipants);
        double rankDiff = expectedRank - rank;
        
        if (rankDiff > 0) {
            // Performed better than expected
            delta += Math.min(rankDiff * 1.5, 40);
        } else if (rankDiff < 0) {
            // Performed worse than expected
            delta += Math.max(rankDiff * 1.5, -40);
        }

        // Calculate new rating
        int newRating = currentRating + (int) Math.round(delta);

        // Ensure rating doesn't go below 0
        if (newRating < 0) newRating = 0;

        // Cap maximum single-contest change based on current rating
        int maxChange = getMaxChange(currentRating);
        if (newRating - currentRating > maxChange) {
            newRating = currentRating + maxChange;
        } else if (currentRating - newRating > maxChange) {
            newRating = currentRating - maxChange;
        }

        return newRating;
    }

    /**
     * Get K-factor based on rating (Elo-style)
     */
    private static double getKFactor(int rating) {
        if (rating >= 2400) return 16.0;
        if (rating >= 2000) return 20.0;
        if (rating >= 1600) return 24.0;
        if (rating >= 1200) return 28.0;
        return 32.0;
    }

    /**
     * Get maximum rating change allowed per contest
     */
    private static int getMaxChange(int rating) {
        if (rating >= 2400) return 150;
        if (rating >= 2000) return 180;
        if (rating >= 1600) return 200;
        if (rating >= 1200) return 250;
        return 300;
    }

    /**
     * Calculate seed (Codeforces-style expected rank)
     */
    @SuppressWarnings("unused")
    private static double calculateSeed(int rating, double avgRating, int totalParticipants) {
        double seed = 1.0;
        for (int i = 0; i < totalParticipants; i++) {
            // Simulate rating distribution
            double otherRating = avgRating + (Math.random() - 0.5) * 600;
            seed += 1.0 / (1.0 + Math.pow(10.0, (otherRating - rating) / 400.0));
        }
        return seed;
    }

    /**
     * Calculate the rating you "need" to achieve a specific rank
     */
    @SuppressWarnings("unused")
    private static double calculateNeedRating(int rank, int totalParticipants, double avgRating) {
        double percentile = (double)(totalParticipants - rank + 1) / (double)totalParticipants;
        
        // Convert percentile to rating using logistic function
        if (percentile > 0.995) percentile = 0.995;
        if (percentile < 0.005) percentile = 0.005;
        
        double needRating = avgRating + 400.0 * Math.log10(percentile / (1.0 - percentile));
        return needRating;
    }

    /**
     * Calculate expected rank based on rating
     */
    private static double calculateExpectedRank(int rating, double avgRating, int totalParticipants) {
        double expectedPerformance = 1.0 / (1.0 + Math.pow(10, (avgRating - rating) / 400.0));
        return totalParticipants * (1.0 - expectedPerformance) + 1;
    }

    /**
     * Calculate performance rating from rank
     */
    private static double calculatePerformance(int rank, int totalParticipants, double avgRating) {
        double percentile = (double) (totalParticipants - rank + 1) / totalParticipants;
        
        // Bound percentile to avoid infinity
        if (percentile >= 0.998) percentile = 0.998;
        if (percentile <= 0.002) percentile = 0.002;
        
        // Convert percentile to performance rating using Elo-style formula
        double performance = avgRating + 400.0 * Math.log10(percentile / (1.0 - percentile));

        return performance;
    }

    /**
     * Get rating color based on rating value (AtCoder style)
     */
    public static String getRatingColor(int rating) {
        if (rating >= 2800) return "#FF0000"; // Red
        if (rating >= 2400) return "#FF8C00"; // Orange
        if (rating >= 2000) return "#FFFF00"; // Yellow
        if (rating >= 1600) return "#0000FF"; // Blue
        if (rating >= 1200) return "#00C0C0"; // Cyan
        if (rating >= 800) return "#008000";  // Green
        if (rating >= 400) return "#804000";  // Brown
        return "#808080"; // Gray
    }

    /**
     * Get rating title based on rating value
     */
    public static String getRatingTitle(int rating) {
        if (rating >= 2800) return "Legendary Grandmaster";
        if (rating >= 2400) return "International Grandmaster";
        if (rating >= 2000) return "Grandmaster";
        if (rating >= 1600) return "Master";
        if (rating >= 1200) return "Expert";
        if (rating >= 800) return "Specialist";
        if (rating >= 400) return "Apprentice";
        return "Novice";
    }
}
