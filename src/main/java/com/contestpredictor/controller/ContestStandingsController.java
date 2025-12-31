package com.contestpredictor.controller;

import com.contestpredictor.data.ContestDatabase;
import com.contestpredictor.data.DatabaseManager;
import com.contestpredictor.data.UserDatabase;
import com.contestpredictor.model.Contest;
import com.contestpredictor.model.Participant;
import com.contestpredictor.model.User;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.stage.Stage;
import javafx.util.converter.IntegerStringConverter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Controller for managing contest standings
 * Allows viewing and updating participant solve counts and generating contest results
 */
public class ContestStandingsController {
    
    @FXML private Label contestTitleLabel;
    @FXML private Label contestInfoLabel;
    @FXML private ComboBox<String> contestSelector;
    @FXML private Button generateContestButton;
    @FXML private Button refreshButton;
    @FXML private Button registerButton;
    @FXML private Button backButton;
    @FXML private TableView<Participant> standingsTable;
    @FXML private TableColumn<Participant, Integer> rankColumn;
    @FXML private TableColumn<Participant, String> usernameColumn;
    @FXML private TableColumn<Participant, Integer> currentRatingColumn;
    @FXML private TableColumn<Participant, Integer> solvedColumn;
    @FXML private TableColumn<Participant, Integer> penaltyColumn;
    @FXML private TableColumn<Participant, Integer> predictedRatingColumn;
    @FXML private TableColumn<Participant, Integer> ratingChangeColumn;
    @FXML private TableColumn<Participant, Void> actionsColumn;
    
    private String currentUsername;
    private boolean isAdmin = false;
    
    @FXML
    public void initialize() {
        setupTable();
        loadContests();
        
        // Hide register button if admin
        if (registerButton != null) {
            registerButton.setVisible(!isAdmin);
        }
    }
    
    private void setupTable() {
        // Make table editable
        standingsTable.setEditable(true);
        
        // Setup columns
        rankColumn.setCellValueFactory(new PropertyValueFactory<>("rank"));
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        currentRatingColumn.setCellValueFactory(new PropertyValueFactory<>("currentRating"));
        
        // Make solved and penalty columns editable
        solvedColumn.setCellValueFactory(new PropertyValueFactory<>("problemsSolved"));
        solvedColumn.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        solvedColumn.setOnEditCommit(event -> {
            Participant participant = event.getRowValue();
            participant.setProblemsSolved(event.getNewValue());
            updateParticipantInDatabase(participant);
        });
        
        penaltyColumn.setCellValueFactory(new PropertyValueFactory<>("totalPenalty"));
        penaltyColumn.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        penaltyColumn.setOnEditCommit(event -> {
            Participant participant = event.getRowValue();
            participant.setTotalPenalty(event.getNewValue());
            updateParticipantInDatabase(participant);
        });
        
        predictedRatingColumn.setCellValueFactory(new PropertyValueFactory<>("predictedRating"));
        ratingChangeColumn.setCellValueFactory(new PropertyValueFactory<>("ratingChange"));
        
        // Setup actions column for admin
        if (isAdmin) {
            actionsColumn.setCellFactory(param -> new TableCell<>() {
                private final Button removeBtn = new Button("Remove");
                
                {
                    removeBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");
                    removeBtn.setOnAction(event -> {
                        Participant participant = getTableView().getItems().get(getIndex());
                        handleRemoveParticipant(participant);
                    });
                }
                
                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        setGraphic(removeBtn);
                    }
                }
            });
        }
    }
    
    private void loadContests() {
        ContestDatabase contestDB = ContestDatabase.getInstance();
        List<Contest> contests = contestDB.getAllContests();
        
        ObservableList<String> contestNames = FXCollections.observableArrayList();
        for (Contest contest : contests) {
            if (!contest.isPast()) {
                contestNames.add(contest.getContestId() + " - " + contest.getContestName());
            }
        }
        
        contestSelector.setItems(contestNames);
        
        // Auto-select first contest if available
        if (!contestNames.isEmpty()) {
            contestSelector.getSelectionModel().selectFirst();
            handleContestSelection();
        }
    }
    
    @FXML
    private void handleContestSelection() {
        String selected = contestSelector.getValue();
        if (selected == null) return;
        
        String contestId = selected.split(" - ")[0];
        loadContestStandings(contestId);
    }
    
    private void loadContestStandings(String contestId) {
        ContestDatabase contestDB = ContestDatabase.getInstance();
        Contest contest = contestDB.getContestById(contestId);
        
        if (contest == null) return;
        
        contestTitleLabel.setText(contest.getContestName());
        contestInfoLabel.setText(String.format("Duration: %d minutes | Registered: %d", 
            contest.getDuration(), contest.getRegisteredCount()));
        
        // Load participants from database
        DatabaseManager dbManager = DatabaseManager.getInstance();
        List<Participant> participants = dbManager.getParticipantsByContest(contestId);
        
        // If no participants exist, generate them
        if (participants.isEmpty()) {
            participants = generateInitialParticipants(contest);
        }
        
        // Sort by rank
        participants.sort(Comparator.comparingInt(Participant::getRank));
        
        ObservableList<Participant> observableParticipants = FXCollections.observableArrayList(participants);
        standingsTable.setItems(observableParticipants);
    }
    
    private List<Participant> generateInitialParticipants(Contest contest) {
        List<Participant> participants = new ArrayList<>();
        DatabaseManager dbManager = DatabaseManager.getInstance();
        UserDatabase userDB = UserDatabase.getInstance();
        
        // Get already registered users
        List<String> registeredUsers = dbManager.getRegisteredUsers(contest.getContestId());
        
        // Add default users (user001-user030) if not already registered
        for (int i = 1; i <= 30; i++) {
            String username = String.format("user%03d", i);
            
            // Register user for contest if not already registered
            if (!registeredUsers.contains(username)) {
                dbManager.registerUserForContest(contest.getContestId(), username);
                registeredUsers.add(username);
            }
            
            // Check if user exists in database
            User user = userDB.getUser(username);
            int rating = (user != null) ? user.getCurrentRating() : 1200 + (int)(Math.random() * 800);
            
            Participant p = new Participant(username, rating, 0, 0);
            p.setRank(i);
            participants.add(p);
            
            // Save to database
            dbManager.saveParticipant(contest.getContestId(), p);
        }
        
        // Add any other registered users who aren't in default list
        int rank = 31;
        for (String username : registeredUsers) {
            // Skip if already added
            boolean alreadyAdded = participants.stream()
                .anyMatch(p -> p.getUsername().equals(username));
            if (alreadyAdded) continue;
            
            User user = userDB.getUser(username);
            int rating = (user != null) ? user.getCurrentRating() : 1200;
            
            Participant p = new Participant(username, rating, 0, 0);
            p.setRank(rank++);
            participants.add(p);
            
            // Save to database
            dbManager.saveParticipant(contest.getContestId(), p);
        }
        
        return participants;
    }
    
    @FXML
    private void handleGenerateContest() {
        String selected = contestSelector.getValue();
        if (selected == null) {
            showAlert("Error", "Please select a contest first");
            return;
        }
        
        String contestId = selected.split(" - ")[0];
        
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Generate Contest");
        confirmation.setHeaderText("Generate Contest Results");
        confirmation.setContentText("This will calculate rankings and rating changes based on current solve counts. Continue?");
        
        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            generateContestResults(contestId);
        }
    }
    
    private void generateContestResults(String contestId) {
        ObservableList<Participant> participants = standingsTable.getItems();
        
        // Sort by problems solved (desc), then by penalty (asc)
        List<Participant> sortedList = new ArrayList<>(participants);
        sortedList.sort((p1, p2) -> {
            if (p1.getProblemsSolved() != p2.getProblemsSolved()) {
                return Integer.compare(p2.getProblemsSolved(), p1.getProblemsSolved());
            }
            return Integer.compare(p1.getTotalPenalty(), p2.getTotalPenalty());
        });
        
        // Assign ranks
        for (int i = 0; i < sortedList.size(); i++) {
            sortedList.get(i).setRank(i + 1);
        }
        
        // Calculate rating changes using a simple ELO-based system
        for (Participant p : sortedList) {
            int expectedRank = calculateExpectedRank(p, sortedList);
            int actualRank = p.getRank();
            
            // Rating change based on performance
            int ratingChange = (expectedRank - actualRank) * 20;
            
            // Cap rating change
            ratingChange = Math.max(-150, Math.min(150, ratingChange));
            
            p.setRatingChange(ratingChange);
            p.setPredictedRating(p.getCurrentRating() + ratingChange);
        }
        
        // Save to database
        DatabaseManager dbManager = DatabaseManager.getInstance();
        for (Participant p : sortedList) {
            dbManager.updateParticipantSolveCount(contestId, p.getUsername(), 
                p.getProblemsSolved(), p.getTotalPenalty());
        }
        
        // Refresh table
        standingsTable.setItems(FXCollections.observableArrayList(sortedList));
        showAlert("Success", "Contest results generated successfully!");
    }
    
    private int calculateExpectedRank(Participant participant, List<Participant> allParticipants) {
        double expectedRank = 1.0;
        
        for (Participant other : allParticipants) {
            if (other != participant) {
                double probability = 1.0 / (1.0 + Math.pow(10, (other.getCurrentRating() - participant.getCurrentRating()) / 400.0));
                expectedRank += (1.0 - probability);
            }
        }
        
        return (int) Math.round(expectedRank);
    }
    
    private void updateParticipantInDatabase(Participant participant) {
        String selected = contestSelector.getValue();
        if (selected == null) return;
        
        String contestId = selected.split(" - ")[0];
        DatabaseManager dbManager = DatabaseManager.getInstance();
        dbManager.updateParticipantSolveCount(contestId, participant.getUsername(), 
            participant.getProblemsSolved(), participant.getTotalPenalty());
    }
    
    private void handleRemoveParticipant(Participant participant) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Remove Participant");
        confirmation.setHeaderText("Remove " + participant.getUsername());
        confirmation.setContentText("Are you sure you want to remove this participant?");
        
        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String selected = contestSelector.getValue();
            if (selected == null) return;
            
            String contestId = selected.split(" - ")[0];
            DatabaseManager dbManager = DatabaseManager.getInstance();
            
            if (dbManager.removeParticipant(contestId, participant.getUsername())) {
                standingsTable.getItems().remove(participant);
                showAlert("Success", "Participant removed successfully");
            } else {
                showAlert("Error", "Failed to remove participant");
            }
        }
    }
    
    @FXML
    private void handleRefresh() {
        handleContestSelection();
    }
    
    @FXML
    private void handleRegisterForContest() {
        if (currentUsername == null || isAdmin) {
            showAlert("Error", "You must be logged in as a user to register");
            return;
        }
        
        String selected = contestSelector.getValue();
        if (selected == null) {
            showAlert("Error", "Please select a contest first");
            return;
        }
        
        String contestId = selected.split(" - ")[0];
        DatabaseManager dbManager = DatabaseManager.getInstance();
        
        // Check if already registered
        if (dbManager.isUserRegisteredForContest(contestId, currentUsername)) {
            showAlert("Info", "You are already registered for this contest!");
            return;
        }
        
        // Register user
        if (dbManager.registerUserForContest(contestId, currentUsername)) {
            // Add to participants if not already there
            List<Participant> participants = dbManager.getParticipantsByContest(contestId);
            boolean participantExists = participants.stream()
                .anyMatch(p -> p.getUsername().equals(currentUsername));
            
            if (!participantExists) {
                UserDatabase userDB = UserDatabase.getInstance();
                User user = userDB.getUser(currentUsername);
                int rating = (user != null) ? user.getCurrentRating() : 1200;
                
                Participant newParticipant = new Participant(currentUsername, rating, 0, 0);
                newParticipant.setRank(participants.size() + 1);
                dbManager.saveParticipant(contestId, newParticipant);
            }
            
            showAlert("Success", "You have been registered for this contest!");
            handleRefresh();
        } else {
            showAlert("Error", "Failed to register for contest");
        }
    }
    
    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader;
            if (isAdmin) {
                loader = new FXMLLoader(getClass().getResource("/fxml/AdminDashboard.fxml"));
            } else {
                loader = new FXMLLoader(getClass().getResource("/fxml/Profile.fxml"));
            }
            
            Parent root = loader.load();
            Stage stage = (Stage) backButton.getScene().getWindow();
            
            Scene scene = new Scene(root, stage.getWidth(), stage.getHeight());
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            
            stage.setScene(scene);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to go back: " + e.getMessage());
        }
    }
    
    public void setCurrentUser(String username, boolean isAdmin) {
        this.currentUsername = username;
        this.isAdmin = isAdmin;
        setupTable(); // Re-setup table with admin privileges
        
        // Update register button visibility
        if (registerButton != null) {
            registerButton.setVisible(!isAdmin);
        }
    }
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
