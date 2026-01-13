package com.contestpredictor.controller;

import com.contestpredictor.data.LeaderboardDatabase;
import com.contestpredictor.model.LeaderboardContest;
import com.contestpredictor.model.LeaderboardEntry;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.ButtonType;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.stage.Stage;
import javafx.util.converter.IntegerStringConverter;

import java.util.List;

public class AdminManageLeaderboardController{
    
    @FXML
    private ListView<LeaderboardContest> contestsListView;
    
    @FXML
    private TableView<LeaderboardEntry> standingsTable;
    
    @FXML
    private TableColumn<LeaderboardEntry, Integer> rankColumn;
    @FXML
    private TableColumn<LeaderboardEntry, String> usernameColumn;
    @FXML
    private TableColumn<LeaderboardEntry, Integer> solveCountColumn;
    @FXML
    private TableColumn<LeaderboardEntry, Integer> penaltyColumn;
    
    @FXML
    private Label contestDetailLabel;
    @FXML
    private Label selectedContestLabel;
    
    @FXML
    private TextField usernameField;
    @FXML
    private TextField solveCountField;
    @FXML
    private TextField penaltyField;
    @FXML
    private TextField timeField;
    
    @FXML
    private Button addEntryButton;
    @FXML
    private Button updateEntryButton;
    @FXML
    private Button deleteEntryButton;
    @FXML
    private Button finalizeButton;
    @FXML
    private Button createContestButton;
    @FXML
    private Button backButton;
    @FXML
    private Button viewCombinedButton;
    @FXML
    private Button refreshContestsButton;
    
    @FXML
    private ListView<String> registeredUsersListView;
    
    @FXML
    private Label registeredUsersCountLabel;
    
    private LeaderboardDatabase leaderboardDB;
    private LeaderboardContest selectedContest;
    private String adminUsername;

    @FXML
    public void initialize() {
        leaderboardDB = LeaderboardDatabase.getInstance();
        setupTableColumns();
        setupListViewCellFactory();
        
        // Set up contest selection
        contestsListView.setOnMouseClicked(event -> {
            selectedContest = contestsListView.getSelectionModel().getSelectedItem();
            if (selectedContest != null) {
                selectedContestLabel.setText("Selected: " + selectedContest.getContestName());
                loadContestStandings();
                loadRegisteredUsers();
            }
        });
        
        // Set up registered users selection to auto-fill username
        if (registeredUsersListView != null) {
            registeredUsersListView.setOnMouseClicked(event -> {
                String selectedUser = registeredUsersListView.getSelectionModel().getSelectedItem();
                if (selectedUser != null && usernameField != null) {
                    usernameField.setText(selectedUser);
                    // Check if user already has an entry and load their data
                    loadUserEntryIfExists(selectedUser);
                }
            });
        }
        
        // Set up standings table selection to fill fields when selecting an entry
        if (standingsTable != null) {
            standingsTable.setOnMouseClicked(event -> {
                LeaderboardEntry selectedEntry = standingsTable.getSelectionModel().getSelectedItem();
                if (selectedEntry != null) {
                    usernameField.setText(selectedEntry.getUsername());
                    solveCountField.setText(String.valueOf(selectedEntry.getSolveCount()));
                    penaltyField.setText(String.valueOf(selectedEntry.getTotalPenalty()));
                    timeField.setText(String.valueOf(selectedEntry.getTotalTime()));
                }
            });
        }
        
        // Load contests immediately on initialization
        Platform.runLater(() -> {
            System.out.println("Initialize: Loading contests on startup...");
            loadContests();
        });
    }

    private void setupListViewCellFactory() {
        contestsListView.setCellFactory(param -> new ListCell<LeaderboardContest>() {
            @Override
            protected void updateItem(LeaderboardContest contest, boolean empty) {
                super.updateItem(contest, empty);
                if (empty || contest == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    String text = contest.getContestName() + " (" + contest.getContestId() + ")";
                    if (contest.isStandings_finalized()) {
                        text += " ✓";
                    }
                    setText(text);
                }
            }
        });
    }

    private void setupTableColumns() {
        // Make table editable
        standingsTable.setEditable(true);
        
        rankColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getRank()));
        usernameColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getUsername()));
        
        // Editable solve count column
        solveCountColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getSolveCount()));
        solveCountColumn.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        solveCountColumn.setOnEditCommit(event -> {
            LeaderboardEntry entry = event.getRowValue();
            entry.setSolveCount(event.getNewValue());
            // Update in database
            leaderboardDB.addLeaderboardEntry(entry);
            // Recalculate rankings
            recalculateRankings();
        });
        
        // Editable penalty column
        penaltyColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getTotalPenalty()));
        penaltyColumn.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        penaltyColumn.setOnEditCommit(event -> {
            LeaderboardEntry entry = event.getRowValue();
            entry.setTotalPenalty(event.getNewValue());
            // Update in database
            leaderboardDB.addLeaderboardEntry(entry);
            // Recalculate rankings
            recalculateRankings();
        });
    }

    private void loadContests() {
        System.out.println("=== Loading contests ===");
        List<LeaderboardContest> contests = leaderboardDB.getAllLeaderboardContests();
        System.out.println("Found " + contests.size() + " contests in database");
        
        for (int i = 0; i < contests.size(); i++) {
            LeaderboardContest c = contests.get(i);
            System.out.println((i+1) + ". " + c.getContestId() + " - " + c.getContestName() + 
                             " (Active: " + c.isActive() + ", Finalized: " + c.isStandings_finalized() + ")");
        }
        
        ObservableList<LeaderboardContest> observableContests = FXCollections.observableArrayList(contests);
        contestsListView.setItems(observableContests);
        contestsListView.refresh();
        
        System.out.println("Contest ListView now has " + contestsListView.getItems().size() + " items");
        System.out.println("=== Load complete ===");
    }

    private void loadContestStandings() {
        if (selectedContest != null) {
            List<LeaderboardEntry> standings = leaderboardDB.getContestStandings(selectedContest.getContestId());
            
            // Also get registered users and add them if they don't have entries yet
            List<String> registeredUsers = leaderboardDB.getRegisteredUsersForContest(selectedContest.getContestId());
            for (String username : registeredUsers) {
                boolean hasEntry = standings.stream()
                    .anyMatch(entry -> entry.getUsername().equals(username));
                
                if (!hasEntry) {
                    // Create a placeholder entry for registered user with no scores yet
                    LeaderboardEntry newEntry = new LeaderboardEntry(
                        username, 
                        selectedContest.getContestId(), 
                        standings.size() + 1, 
                        0, 
                        0, 
                        0
                    );
                    standings.add(newEntry);
                }
            }
            
            ObservableList<LeaderboardEntry> observableStandings = FXCollections.observableArrayList(standings);
            standingsTable.setItems(observableStandings);
        }
    }
    
    private void loadRegisteredUsers() {
        if (selectedContest == null || registeredUsersListView == null) {
            System.out.println("DEBUG: loadRegisteredUsers - selectedContest or registeredUsersListView is null");
            return;
        }
        
        System.out.println("DEBUG: Loading registered users for contest: " + selectedContest.getContestId());
        List<String> registeredUsers = leaderboardDB.getRegisteredUsersForContest(selectedContest.getContestId());
        System.out.println("DEBUG: Found " + registeredUsers.size() + " registered users");
        for (String user : registeredUsers) {
            System.out.println("  - User: " + user);
        }
        ObservableList<String> observableUsers = FXCollections.observableArrayList(registeredUsers);
        registeredUsersListView.setItems(observableUsers);
        
        if (registeredUsersCountLabel != null) {
            registeredUsersCountLabel.setText("Registered Users (" + registeredUsers.size() + ")");
        }
    }
    
    private void loadUserEntryIfExists(String username) {
        if (selectedContest == null || username == null || username.isEmpty()) return;
        
        List<LeaderboardEntry> standings = leaderboardDB.getContestStandings(selectedContest.getContestId());
        for (LeaderboardEntry entry : standings) {
            if (entry.getUsername().equals(username)) {
                // User already has an entry, load their data
                solveCountField.setText(String.valueOf(entry.getSolveCount()));
                penaltyField.setText(String.valueOf(entry.getTotalPenalty()));
                timeField.setText(String.valueOf(entry.getTotalTime()));
                return;
            }
        }
        // User doesn't have an entry yet, clear the fields
        solveCountField.clear();
        penaltyField.clear();
        timeField.clear();
    }

    @FXML
    private void handleAddEntry() {
        if (selectedContest == null) {
            showError("Please select a contest first");
            return;
        }

        try {
            String username = usernameField.getText().trim();
            int solveCount = Integer.parseInt(solveCountField.getText().trim());
            int penalty = Integer.parseInt(penaltyField.getText().trim());
            long time = Long.parseLong(timeField.getText().trim());

            if (username.isEmpty()) {
                showError("Please enter username");
                return;
            }

            // Create entry with rank 0 (will be calculated dynamically)
            LeaderboardEntry entry = new LeaderboardEntry(username, selectedContest.getContestId(),
                    0, solveCount, penalty, time);

            // Check if user is registered for the contest
            if (!leaderboardDB.isUserRegisteredForContest(selectedContest.getContestId(), username)) {
                showError("User '" + username + "' is not registered for this contest.\nPlease ask them to register first.");
                return;
            }
            
            if (leaderboardDB.addLeaderboardEntry(entry)) {
                showSuccess("Entry added successfully for " + username);
                clearFields();
                resetSelections(); // Reset contest and user selection
                recalculateRankings(); // Recalculate rankings dynamically
            } else {
                showError("Failed to add entry");
            }
        } catch (NumberFormatException e) {
            showError("Please enter valid numbers for solve count, penalty, and time");
        }
    }

    @FXML
    private void handleUpdateEntry() {
        if (selectedContest == null) {
            showError("Please select a contest first");
            return;
        }

        LeaderboardEntry selected = standingsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Please select an entry to update");
            return;
        }

        try {
            int solveCount = Integer.parseInt(solveCountField.getText().trim());
            int penalty = Integer.parseInt(penaltyField.getText().trim());
            long time = Long.parseLong(timeField.getText().trim());

            selected.setSolveCount(solveCount);
            selected.setTotalPenalty(penalty);
            selected.setTotalTime(time);

            if (leaderboardDB.addLeaderboardEntry(selected)) {
                showSuccess("Entry updated successfully for " + selected.getUsername());
                resetSelections(); // Reset selections after update
                recalculateRankings(); // Recalculate rankings dynamically
            } else {
                showError("Failed to update entry");
            }
        } catch (NumberFormatException e) {
            showError("Please enter valid numbers");
        }
    }

    @FXML
    private void handleDeleteEntry() {
        if (selectedContest == null) {
            showError("Please select a contest first");
            return;
        }
        
        LeaderboardEntry selected = standingsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Please select an entry to delete from the standings table");
            return;
        }

        // Confirm deletion
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Deletion");
        confirm.setHeaderText("Delete Entry");
        confirm.setContentText("Are you sure you want to delete the entry for '" + selected.getUsername() + "'?");
        
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            if (leaderboardDB.deleteLeaderboardEntry(selectedContest.getContestId(), selected.getUsername())) {
                showSuccess("Entry for '" + selected.getUsername() + "' deleted successfully");
                clearFields();
                recalculateRankings();
            } else {
                showError("Failed to delete entry");
            }
        }
    }

    @FXML
    private void handleFinalizeStandings() {
        if (selectedContest == null) {
            showError("Please select a contest first");
            return;
        }
        
        // Check if contest is already finalized
        if (selectedContest.isStandings_finalized()) {
            showError("This contest's standings have already been finalized.");
            return;
        }
        
        // Check if there are any entries to finalize
        List<LeaderboardEntry> standings = leaderboardDB.getContestStandings(selectedContest.getContestId());
        if (standings.isEmpty()) {
            showError("No entries to finalize. Please add at least one entry first.");
            return;
        }
        
        // Confirm finalization
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Finalization");
        confirm.setHeaderText("Finalize Contest Standings");
        confirm.setContentText("Are you sure you want to finalize standings for '" + selectedContest.getContestName() + "'?\n\n" +
                "This will:\n" +
                "• Lock the standings for this contest\n" +
                "• Calculate final rankings for " + standings.size() + " participant(s)\n" +
                "• Update the combined overall leaderboard\n\n" +
                "This action cannot be undone.");
        
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            // Recalculate rankings before finalizing
            recalculateRankings();
            
            if (leaderboardDB.finalizeContestStandings(selectedContest.getContestId())) {
                showSuccess("Standings finalized successfully for '" + selectedContest.getContestName() + "'!\n\n" +
                        "✓ " + standings.size() + " participant(s) processed\n" +
                        "✓ Combined leaderboard updated");
                loadContests();
                loadContestStandings();
            } else {
                showError("Failed to finalize standings");
            }
        }
    }

    @FXML
    private void handleViewCombinedLeaderboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/CombinedLeaderboard.fxml"));
            if (loader.getLocation() == null) {
                showError("Cannot find CombinedLeaderboard.fxml file");
                return;
            }
            
            Parent root = loader.load();
            
            CombinedLeaderboardController controller = loader.getController();
            controller.loadCombinedLeaderboard();
            
            Stage stage = new Stage();
            Scene scene = new Scene(root, 900, 600);
            
            String stylesheet = getClass().getResource("/css/styles.css").toExternalForm();
            scene.getStylesheets().add(stylesheet);
            
            stage.setTitle("Overall Leaderboard - All Contests");
            stage.setScene(scene);
            stage.showAndWait();
        } catch (Exception e) {
            System.err.println("Error opening combined leaderboard: " + e.getMessage());
            e.printStackTrace();
            showError("Error opening combined leaderboard: " + e.getMessage());
        }
    }

    @FXML
    private void handleCreateContest() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/CreateLeaderboardContest.fxml"));
            if (loader.getLocation() == null) {
                showError("Cannot find CreateLeaderboardContest.fxml file");
                return;
            }
            
            Parent root = loader.load();
            
            CreateLeaderboardContestController controller = loader.getController();
            controller.setAdminUsername(adminUsername);
            controller.setParentController(this); // Pass this controller as parent
            
            Stage stage = new Stage();
            Scene scene = new Scene(root, 600, 500);
            
            String stylesheet = getClass().getResource("/css/styles.css").toExternalForm();
            scene.getStylesheets().add(stylesheet);
            
            stage.setTitle("Create New Leaderboard Contest");
            stage.setScene(scene);
            stage.showAndWait();
            
            // Reload contests after dialog closes
            loadContests();
        } catch (Exception e) {
            System.err.println("Error opening create contest dialog: " + e.getMessage());
            e.printStackTrace();
            showError("Error opening create contest dialog: " + e.getMessage());
        }
    }

    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AdminDashboard.fxml"));
            if (loader.getLocation() == null) {
                showError("Cannot find AdminDashboard.fxml file");
                return;
            }
            
            Parent root = loader.load();
            
            AdminDashboardController controller = loader.getController();
            controller.setAdmin(new com.contestpredictor.model.Admin(adminUsername, "", adminUsername));
            
            Stage stage = (Stage) backButton.getScene().getWindow();
            Scene scene = new Scene(root, stage.getWidth(), stage.getHeight());
            
            String stylesheet = getClass().getResource("/css/styles.css").toExternalForm();
            scene.getStylesheets().add(stylesheet);
            
            stage.setTitle("Admin Dashboard");
            stage.setScene(scene);
        } catch (Exception e) {
            System.err.println("Error navigating back: " + e.getMessage());
            e.printStackTrace();
            showError("Error navigating back: " + e.getMessage());
        }
    }

    @FXML
    private void handleRefreshContests() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("REFRESH BUTTON CLICKED");
        System.out.println("=".repeat(60));
        
        // Clear current selection
        selectedContest = null;
        selectedContestLabel.setText("No contest selected");
        standingsTable.getItems().clear();
        if (registeredUsersListView != null) {
            registeredUsersListView.getItems().clear();
            if (registeredUsersCountLabel != null) {
                registeredUsersCountLabel.setText("Registered Users (0)");
            }
        }
        
        // Reload contests
        loadContests();
        
        // Show success message
        int contestCount = contestsListView.getItems().size();
        showSuccess("Contests refreshed successfully!\n\nTotal contests loaded: " + contestCount);
    }
    
    /**
     * Public method to refresh contests - can be called by other controllers
     */
    public void refreshContests() {
        Platform.runLater(() -> {
            loadContests();
        });
    }

    private void clearFields() {
        usernameField.clear();
        solveCountField.clear();
        penaltyField.clear();
        timeField.clear();
    }
    
    private void resetSelections() {
        // Clear registered users selection
        if (registeredUsersListView != null) {
            registeredUsersListView.getSelectionModel().clearSelection();
        }
        
        // Clear standings table selection
        if (standingsTable != null) {
            standingsTable.getSelectionModel().clearSelection();
        }
        
        // Clear input fields
        clearFields();
    }

    private void recalculateRankings() {
        if (selectedContest == null) return;
        
        List<LeaderboardEntry> standings = leaderboardDB.getContestStandings(selectedContest.getContestId());
        
        // Sort by solves (descending) then by penalty (ascending)
        standings.sort((a, b) -> {
            int compareSolves = Integer.compare(b.getSolveCount(), a.getSolveCount());
            if (compareSolves != 0) return compareSolves;
            return Integer.compare(a.getTotalPenalty(), b.getTotalPenalty());
        });
        
        // Update ranks and save
        for (int i = 0; i < standings.size(); i++) {
            standings.get(i).setRank(i + 1);
            leaderboardDB.addLeaderboardEntry(standings.get(i));
        }
        
        loadContestStandings();
    }

    public void setAdminUsername(String username) {
        this.adminUsername = username;
        // Load contests after admin username is set and scene is ready
        Platform.runLater(() -> {
            System.out.println("Admin username set to: " + username);
            loadContests();
        });
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
