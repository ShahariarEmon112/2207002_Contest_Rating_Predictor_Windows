package com.contestpredictor.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Contest {
    private String contestId;
    private String contestName;
    private LocalDateTime dateTime;
    private int duration; // in minutes
    private boolean isPast;
    private List<Participant> participants;
    private List<String> registeredUsers; // List of usernames registered for this contest
    private String createdByAdmin; // Admin who created this contest
    private int maxParticipants; // Maximum number of participants allowed
    private boolean registrationOpen; // Whether registration is open

    public Contest(String contestId, String contestName, LocalDateTime dateTime, int duration, boolean isPast) {
        this.contestId = contestId;
        this.contestName = contestName;
        this.dateTime = dateTime;
        this.duration = duration;
        this.isPast = isPast;
        this.participants = new ArrayList<>();
        this.registeredUsers = new ArrayList<>();
        this.maxParticipants = 1000; // Default max participants
        this.registrationOpen = true;
    }
    
    // Constructor with admin support
    public Contest(String contestId, String contestName, LocalDateTime dateTime, int duration, 
                   boolean isPast, String createdByAdmin, int maxParticipants) {
        this(contestId, contestName, dateTime, duration, isPast);
        this.createdByAdmin = createdByAdmin;
        this.maxParticipants = maxParticipants;
    }

    public void addParticipant(Participant participant) {
        this.participants.add(participant);
    }
    
    // Registration methods
    public boolean registerUser(String username) {
        if (!registrationOpen) {
            return false;
        }
        if (registeredUsers.size() >= maxParticipants) {
            return false;
        }
        if (!registeredUsers.contains(username)) {
            registeredUsers.add(username);
            return true;
        }
        return false;
    }
    
    public boolean unregisterUser(String username) {
        return registeredUsers.remove(username);
    }
    
    public boolean isUserRegistered(String username) {
        return registeredUsers.contains(username);
    }
    
    public int getRegisteredCount() {
        return registeredUsers.size();
    }

    // Getters and Setters
    public String getContestId() {
        return contestId;
    }

    public void setContestId(String contestId) {
        this.contestId = contestId;
    }

    public String getContestName() {
        return contestName;
    }
    
    // Alias method for getName()
    public String getName() {
        return contestName;
    }

    public void setContestName(String contestName) {
        this.contestName = contestName;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public boolean isPast() {
        return isPast;
    }

    public void setPast(boolean past) {
        isPast = past;
    }

    public List<Participant> getParticipants() {
        return participants;
    }

    public void setParticipants(List<Participant> participants) {
        this.participants = participants;
    }
    
    public List<String> getRegisteredUsers() {
        return registeredUsers;
    }
    
    public void setRegisteredUsers(List<String> registeredUsers) {
        this.registeredUsers = registeredUsers;
    }
    
    public String getCreatedByAdmin() {
        return createdByAdmin;
    }
    
    public void setCreatedByAdmin(String createdByAdmin) {
        this.createdByAdmin = createdByAdmin;
    }
    
    public int getMaxParticipants() {
        return maxParticipants;
    }
    
    public void setMaxParticipants(int maxParticipants) {
        this.maxParticipants = maxParticipants;
    }
    
    public boolean isRegistrationOpen() {
        return registrationOpen;
    }
    
    public void setRegistrationOpen(boolean registrationOpen) {
        this.registrationOpen = registrationOpen;
    }
}
