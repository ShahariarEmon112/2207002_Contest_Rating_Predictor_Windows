package com.contestpredictor.data;

import com.contestpredictor.model.Contest;
import com.contestpredictor.model.Participant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ContestDatabase {
    private static ContestDatabase instance;
    private List<Contest> contests;
    private DatabaseManager dbManager;

    private ContestDatabase() {
        contests = new ArrayList<>();
        dbManager = DatabaseManager.getInstance();
        
        // Try to load contests from database first
        if (dbManager.hasContests()) {
            contests = dbManager.loadAllContests();
            System.out.println("Loaded " + contests.size() + " contests from database");
        } else {
            // Initialize with default contests if database is empty
            initializeContests();
            // Save to database
            for (Contest contest : contests) {
                dbManager.saveContest(contest);
            }
            System.out.println("Initialized " + contests.size() + " contests to database");
        }
    }

    public static ContestDatabase getInstance() {
        if (instance == null) {
            instance = new ContestDatabase();
        }
        return instance;
    }

    private void initializeContests() {
        // Past Codeforces Contests (15+ past contests)
        Contest cf1 = new Contest("CF918", "Codeforces Round 918 (Div. 2)", 
            LocalDateTime.of(2024, 11, 15, 17, 35), 120, true);
        Contest cf2 = new Contest("CF920", "Codeforces Round 920 (Div. 2)", 
            LocalDateTime.of(2024, 11, 28, 17, 35), 120, true);
        Contest cf3 = new Contest("CF922", "Codeforces Round 922 (Div. 1 + Div. 2)", 
            LocalDateTime.of(2024, 12, 5, 17, 35), 150, true);
        Contest cf4 = new Contest("CF915", "Codeforces Round 915 (Div. 2)", 
            LocalDateTime.of(2024, 10, 20, 17, 35), 120, true);
        Contest cf5 = new Contest("CF912", "Codeforces Round 912 (Div. 1)", 
            LocalDateTime.of(2024, 10, 5, 17, 35), 150, true);
        
        // Past AtCoder Contests
        Contest abc1 = new Contest("ABC335", "AtCoder Beginner Contest 335", 
            LocalDateTime.of(2024, 11, 10, 21, 0), 100, true);
        Contest abc2 = new Contest("ABC336", "AtCoder Beginner Contest 336", 
            LocalDateTime.of(2024, 11, 24, 21, 0), 100, true);
        Contest abc3 = new Contest("ABC337", "AtCoder Beginner Contest 337", 
            LocalDateTime.of(2024, 12, 8, 21, 0), 100, true);
        Contest abc4 = new Contest("ABC334", "AtCoder Beginner Contest 334", 
            LocalDateTime.of(2024, 10, 28, 21, 0), 100, true);
        Contest abc5 = new Contest("ABC333", "AtCoder Beginner Contest 333", 
            LocalDateTime.of(2024, 10, 15, 21, 0), 100, true);
        Contest arc1 = new Contest("ARC169", "AtCoder Regular Contest 169", 
            LocalDateTime.of(2024, 11, 17, 21, 0), 120, true);
        Contest arc2 = new Contest("ARC168", "AtCoder Regular Contest 168", 
            LocalDateTime.of(2024, 10, 25, 21, 0), 120, true);
        Contest agc1 = new Contest("AGC068", "AtCoder Grand Contest 068", 
            LocalDateTime.of(2024, 12, 1, 21, 0), 150, true);

        // Educational Codeforces
        Contest edu1 = new Contest("EDU159", "Educational Codeforces Round 159", 
            LocalDateTime.of(2024, 11, 5, 17, 35), 135, true);
        Contest edu2 = new Contest("EDU160", "Educational Codeforces Round 160", 
            LocalDateTime.of(2024, 12, 12, 17, 35), 135, true);
        Contest edu3 = new Contest("EDU158", "Educational Codeforces Round 158", 
            LocalDateTime.of(2024, 10, 10, 17, 35), 135, true);
            
        // Past LeetCode contests
        Contest lc_past1 = new Contest("LC420", "LeetCode Weekly Contest 420", 
            LocalDateTime.of(2024, 11, 20, 8, 0), 90, true);
        Contest lc_past2 = new Contest("LC421", "LeetCode Weekly Contest 421", 
            LocalDateTime.of(2024, 11, 27, 8, 0), 90, true);

        // Add 30 realistic participants to past contests
        addRealisticParticipantsToPastContest(cf1);
        addRealisticParticipantsToPastContest(cf2);
        addRealisticParticipantsToPastContest(cf3);
        addRealisticParticipantsToPastContest(cf4);
        addRealisticParticipantsToPastContest(cf5);
        addRealisticParticipantsToPastContest(abc1);
        addRealisticParticipantsToPastContest(abc2);
        addRealisticParticipantsToPastContest(abc3);
        addRealisticParticipantsToPastContest(abc4);
        addRealisticParticipantsToPastContest(abc5);
        addRealisticParticipantsToPastContest(arc1);
        addRealisticParticipantsToPastContest(arc2);
        addRealisticParticipantsToPastContest(agc1);
        addRealisticParticipantsToPastContest(edu1);
        addRealisticParticipantsToPastContest(edu2);
        addRealisticParticipantsToPastContest(edu3);
        addRealisticParticipantsToPastContest(lc_past1);
        addRealisticParticipantsToPastContest(lc_past2);

        contests.add(cf1);
        contests.add(cf2);
        contests.add(cf3);
        contests.add(cf4);
        contests.add(cf5);
        contests.add(abc1);
        contests.add(abc2);
        contests.add(abc3);
        contests.add(abc4);
        contests.add(abc5);
        contests.add(arc1);
        contests.add(arc2);
        contests.add(agc1);
        contests.add(edu1);
        contests.add(edu2);
        contests.add(edu3);
        contests.add(lc_past1);
        contests.add(lc_past2);

        // Future Codeforces Contests (up to January 25, 2026)
        Contest cf_future1 = new Contest("CF925", "Codeforces Round 925 (Div. 2)", 
            LocalDateTime.of(2026, 1, 6, 17, 35), 120, false);
        Contest cf_future2 = new Contest("CF926", "Codeforces Round 926 (Div. 1 + Div. 2)", 
            LocalDateTime.of(2026, 1, 8, 17, 35), 150, false);
        Contest cf_future3 = new Contest("CF927", "Codeforces Round 927 (Div. 3)", 
            LocalDateTime.of(2026, 1, 10, 17, 35), 135, false);
        Contest cf_future4 = new Contest("CF928", "Codeforces Round 928 (Div. 2)", 
            LocalDateTime.of(2026, 1, 13, 17, 35), 120, false);
        Contest cf_future5 = new Contest("CF929", "Codeforces Global Round 29", 
            LocalDateTime.of(2026, 1, 18, 17, 35), 150, false);
        Contest cf_future6 = new Contest("CF930", "Codeforces Round 930 (Div. 2)", 
            LocalDateTime.of(2026, 1, 22, 17, 35), 120, false);
        Contest cf_future7 = new Contest("CF931", "Codeforces Round 931 (Div. 1)", 
            LocalDateTime.of(2026, 1, 25, 17, 35), 150, false);
        
        // Future AtCoder Contests (up to January 25, 2026)
        Contest abc_future1 = new Contest("ABC340", "AtCoder Beginner Contest 340", 
            LocalDateTime.of(2026, 1, 7, 21, 0), 100, false);
        Contest abc_future2 = new Contest("ABC341", "AtCoder Beginner Contest 341", 
            LocalDateTime.of(2026, 1, 11, 21, 0), 100, false);
        Contest abc_future3 = new Contest("ABC342", "AtCoder Beginner Contest 342", 
            LocalDateTime.of(2026, 1, 14, 21, 0), 100, false);
        Contest abc_future4 = new Contest("ABC343", "AtCoder Beginner Contest 343", 
            LocalDateTime.of(2026, 1, 18, 21, 0), 100, false);
        Contest abc_future5 = new Contest("ABC344", "AtCoder Beginner Contest 344", 
            LocalDateTime.of(2026, 1, 21, 21, 0), 100, false);
        Contest abc_future6 = new Contest("ABC345", "AtCoder Beginner Contest 345", 
            LocalDateTime.of(2026, 1, 25, 21, 0), 100, false);
        Contest arc_future1 = new Contest("ARC171", "AtCoder Regular Contest 171", 
            LocalDateTime.of(2026, 1, 12, 21, 0), 120, false);
        Contest arc_future2 = new Contest("ARC172", "AtCoder Regular Contest 172", 
            LocalDateTime.of(2026, 1, 19, 21, 0), 120, false);
        Contest agc_future1 = new Contest("AGC069", "AtCoder Grand Contest 069", 
            LocalDateTime.of(2026, 1, 24, 21, 0), 150, false);
        
        // Educational Codeforces Future (up to January 25, 2026)
        Contest edu_future1 = new Contest("EDU162", "Educational Codeforces Round 162", 
            LocalDateTime.of(2026, 1, 9, 17, 35), 135, false);
        Contest edu_future2 = new Contest("EDU163", "Educational Codeforces Round 163", 
            LocalDateTime.of(2026, 1, 16, 17, 35), 135, false);
        Contest edu_future3 = new Contest("EDU164", "Educational Codeforces Round 164", 
            LocalDateTime.of(2026, 1, 23, 17, 35), 135, false);
        
        // LeetCode Weekly Contests (up to January 25, 2026)
        Contest lc_future1 = new Contest("LC425", "LeetCode Weekly Contest 425", 
            LocalDateTime.of(2026, 1, 7, 8, 0), 90, false);
        Contest lc_future2 = new Contest("LC426", "LeetCode Weekly Contest 426", 
            LocalDateTime.of(2026, 1, 14, 8, 0), 90, false);
        Contest lc_future3 = new Contest("LC427", "LeetCode Weekly Contest 427", 
            LocalDateTime.of(2026, 1, 21, 8, 0), 90, false);
        
        // Random named contests (up to January 25, 2026)
        Contest random1 = new Contest("WC2026_01", "Winter Challenge 2026 Round 1", 
            LocalDateTime.of(2026, 1, 6, 14, 0), 120, false);
        Contest random2 = new Contest("WC2026_02", "Winter Challenge 2026 Round 2", 
            LocalDateTime.of(2026, 1, 10, 14, 0), 120, false);
        Contest random3 = new Contest("NEWYEAR26", "New Year Contest 2026", 
            LocalDateTime.of(2026, 1, 8, 10, 0), 150, false);
        Contest random4 = new Contest("ALGO_JAN", "Algorithm Masters January", 
            LocalDateTime.of(2026, 1, 15, 15, 0), 180, false);
        Contest random5 = new Contest("CP_SPRINT", "Competitive Programming Sprint", 
            LocalDateTime.of(2026, 1, 17, 12, 0), 90, false);
        Contest random6 = new Contest("ICPC_PREP", "ICPC Preparation Contest", 
            LocalDateTime.of(2026, 1, 20, 9, 0), 300, false);
        Contest random7 = new Contest("CODING_CUP", "Coding Cup Winter Edition", 
            LocalDateTime.of(2026, 1, 24, 16, 0), 120, false);

        contests.add(cf_future1);
        contests.add(cf_future2);
        contests.add(cf_future3);
        contests.add(cf_future4);
        contests.add(cf_future5);
        contests.add(cf_future6);
        contests.add(cf_future7);
        contests.add(abc_future1);
        contests.add(abc_future2);
        contests.add(abc_future3);
        contests.add(abc_future4);
        contests.add(abc_future5);
        contests.add(abc_future6);
        contests.add(arc_future1);
        contests.add(arc_future2);
        contests.add(agc_future1);
        contests.add(edu_future1);
        contests.add(edu_future2);
        contests.add(edu_future3);
        contests.add(lc_future1);
        contests.add(lc_future2);
        contests.add(lc_future3);
        contests.add(random1);
        contests.add(random2);
        contests.add(random3);
        contests.add(random4);
        contests.add(random5);
        contests.add(random6);
        contests.add(random7);
    }

    private void addRealisticParticipantsToPastContest(Contest contest) {
        // Use the 30 default usernames (user001-user030)
        java.util.Random random = new java.util.Random(contest.getContestId().hashCode());
        
        for (int i = 1; i <= 30; i++) {
            String username = String.format("user%03d", i);
            
            // Realistic rating distribution (more people in middle ranges)
            int rating;
            double r = random.nextDouble();
            if (r < 0.05) { // 5% high rated (2000+)
                rating = 2000 + random.nextInt(800);
            } else if (r < 0.20) { // 15% expert level (1600-2000)
                rating = 1600 + random.nextInt(400);
            } else if (r < 0.45) { // 25% specialist/expert (1200-1600)
                rating = 1200 + random.nextInt(400);
            } else if (r < 0.75) { // 30% pupil/specialist (800-1200)
                rating = 800 + random.nextInt(400);
            } else { // 25% newbie (400-800)
                rating = 400 + random.nextInt(400);
            }
            
            // Problems solved correlates with rating but has variance
            int maxProblems = 7; // Most contests have 5-7 problems
            int baseSolve = (int)(maxProblems * (rating / 3000.0));
            int problems = Math.max(0, Math.min(maxProblems, baseSolve + random.nextInt(3) - 1));
            
            // Penalty correlates with problems solved (more problems = more time)
            int penalty;
            if (problems == 0) {
                penalty = 0;
            } else {
                int baseTime = 15 + (problems - 1) * 20; // Base time per problem
                penalty = baseTime + random.nextInt(60); // Add random variance
            }
            
            Participant p = new Participant(username, rating, problems, penalty);
            contest.addParticipant(p);
        }
        
        // Calculate predicted ratings for this contest
        com.contestpredictor.util.RatingPredictor.calculateRatings(contest.getParticipants());
    }

    public List<Contest> getPastContests() {
        List<Contest> pastContests = new ArrayList<>();
        for (Contest contest : contests) {
            if (contest.isPast()) {
                pastContests.add(contest);
            }
        }
        return pastContests;
    }

    public List<Contest> getFutureContests() {
        List<Contest> futureContests = new ArrayList<>();
        for (Contest contest : contests) {
            if (!contest.isPast()) {
                futureContests.add(contest);
            }
        }
        return futureContests;
    }

    public List<Contest> searchContests(String query) {
        List<Contest> results = new ArrayList<>();
        String lowerQuery = query.toLowerCase();
        for (Contest contest : contests) {
            if (contest.getContestName().toLowerCase().contains(lowerQuery) ||
                contest.getContestId().toLowerCase().contains(lowerQuery)) {
                results.add(contest);
            }
        }
        return results;
    }

    public Contest getContestById(String id) {
        for (Contest contest : contests) {
            if (contest.getContestId().equals(id)) {
                return contest;
            }
        }
        return null;
    }

    public List<Contest> getAllContests() {
        return contests;
    }
    
    /**
     * Save contest created by admin
     */
    public boolean saveContestWithAdmin(Contest contest) {
        try {
            // Check if contest ID already exists
            if (getContestById(contest.getContestId()) != null) {
                return false;
            }
            
            contests.add(contest);
            dbManager.saveContestWithAdmin(contest);
            return true;
        } catch (Exception e) {
            System.err.println("Failed to save contest: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Delete a contest by ID
     */
    public boolean deleteContest(String contestId) {
        try {
            // Remove from in-memory list
            Contest toRemove = null;
            for (Contest contest : contests) {
                if (contest.getContestId().equals(contestId)) {
                    toRemove = contest;
                    break;
                }
            }
            
            if (toRemove != null) {
                contests.remove(toRemove);
                
                // Delete from database
                String sql = "DELETE FROM contests WHERE contest_id = ?";
                java.sql.Connection conn = java.sql.DriverManager.getConnection("jdbc:sqlite:contest_predictor.db");
                java.sql.PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setString(1, contestId);
                pstmt.executeUpdate();
                pstmt.close();
                
                // Also delete participants
                sql = "DELETE FROM participants WHERE contest_id = ?";
                pstmt = conn.prepareStatement(sql);
                pstmt.setString(1, contestId);
                pstmt.executeUpdate();
                pstmt.close();
                conn.close();
                
                return true;
            }
            
            return false;
        } catch (Exception e) {
            System.err.println("Failed to delete contest: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
