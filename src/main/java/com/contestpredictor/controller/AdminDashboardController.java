package com.contestpredictor.controller;

import com.contestpredictor.data.ContestDatabase;
import com.contestpredictor.data.DatabaseManager;
import com.contestpredictor.data.UserDatabase;
import com.contestpredictor.model.Admin;
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
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Enhanced AdminDashboardController with participant management
 */
public class AdminDashboardController {
    
    // Header Elements
    @FXML private Label adminNameLabel;
    @FXML private Button logoutButton;
    
    // Create Contest Section
    @FXML private TextField contestIdField;
    @FXML private TextField contestNameField;
    @FXML private DatePicker contestDatePicker;
    @FXML private TextField durationField;
    @FXML private TextField maxParticipantsField;
    @FXML private Button createContestButton;
    @FXML private Label contestStatusLabel;
    
    // Contests Table
    @FXML private TableView<Contest> contestsTable;
    @FXML private TableColumn<Contest, String> contestIdColumn;
    @FXML private TableColumn<Contest, String> contestNameColumn;
    @FXML private TableColumn<Contest, String> dateTimeColumn;
    @FXML private TableColumn<Contest, Integer> durationColumn;
    @FXML private TableColumn<Contest, String> statusColumn;
    @FXML private TableColumn<Contest, Void> actionsColumn;
    
    // Participants Management
    @FXML private ComboBox<String> participantContestSelector;
    @FXML private TextField addParticipantUsernameField;
    @FXML private Label addParticipantStatusLabel;
    @FXML private Label participantCountLabel;
    @FXML private TableView<Participant> participantsTable;
    @FXML private TableColumn<Participant, String> participantUsernameColumn;
    @FXML private TableColumn<Participant, Integer> participantRatingColumn;
    @FXML private TableColumn<Participant, Integer> participantSolvedColumn;
    @FXML private TableColumn<Participant, Integer> participantPenaltyColumn;
    @FXML private TableColumn<Participant, Integer> participantRankColumn;
    @FXML private TableColumn<Participant, Void> participantActionsColumn;
    
    // Statistics
    @FXML private Label totalContestsLabel;
    @FXML private Label totalUsersLabel;
    @FXML private Label futureContestsLabel;
    @FXML private Label totalParticipantsLabel;
    
    private Admin currentAdmin;
    private String selectedContestId;
    
    @FXML
    public void initialize() {
        try {
            // Set default values
            if (maxParticipantsField != null) {
                maxParticipantsField.setText("1000");
            }
            if (contestDatePicker != null) {
                contestDatePicker.setValue(LocalDate.now());
            }
            
            // Initialize table columns
            setupContestsTableColumns();
            setupParticipantsTableColumns();
            
        } catch (Exception e) {
            System.err.println("Error during initialization: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void setupContestsTableColumns() {
        if (contestIdColumn != null) {
            contestIdColumn.setCellValueFactory(new PropertyValueFactory<>("contestId"));
        }
        if (contestNameColumn != null) {
            contestNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        }
        if (dateTimeColumn != null) {
            dateTimeColumn.setCellValueFactory(cellData -> {
                Contest contest = cellData.getValue();
                String formatted = contest.getDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                return new javafx.beans.property.SimpleStringProperty(formatted);
            });
        }
        if (durationColumn != null) {
            durationColumn.setCellValueFactory(new PropertyValueFactory<>("duration"));
        }
        if (statusColumn != null) {
            statusColumn.setCellValueFactory(cellData -> {
                Contest contest = cellData.getValue();
                String status = contest.isPast() ? "Past" : "Future";
                return new javafx.beans.property.SimpleStringProperty(status);
            });
        }
        if (actionsColumn != null) {
            actionsColumn.setCellFactory(param -> new TableCell<>() {
                private final Button deleteBtn = new Button("Delete");
                
                {
                    deleteBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-cursor: hand;");
                    deleteBtn.setOnAction(event -> {
                        Contest contest = getTableView().getItems().get(getIndex());
                        handleDeleteContest(contest);
                    });
                }
                
                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        setGraphic(deleteBtn);
                    }
                }
            });
        }
    }
    
    private void setupParticipantsTableColumns() {
        if (participantUsernameColumn != null) {
            participantUsernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        }
        if (participantRatingColumn != null) {
            participantRatingColumn.setCellValueFactory(new PropertyValueFactory<>("currentRating"));
        }
        if (participantSolvedColumn != null) {
            participantSolvedColumn.setCellValueFactory(new PropertyValueFactory<>("problemsSolved"));
        }
        if (participantPenaltyColumn != null) {
            participantPenaltyColumn.setCellValueFactory(new PropertyValueFactory<>("totalPenalty"));
        }
        if (participantRankColumn != null) {
            participantRankColumn.setCellValueFactory(new PropertyValueFactory<>("rank"));
        }
        if (participantActionsColumn != null) {
            participantActionsColumn.setCellFactory(param -> new TableCell<>() {
                private final Button removeBtn = new Button("Remove");
                
                {
                    removeBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-cursor: hand;");
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
    
    public void setAdmin(Admin admin) {
        System.out.println("setAdmin called with: " + (admin != null ? admin.getFullName() : "null"));
        
        if (admin == null) {
            System.err.println("ERROR: Admin object is null!");
            return;
        }
        
        this.currentAdmin = admin;
        
        if (adminNameLabel != null) {
            adminNameLabel.setText(admin.getFullName());
            System.out.println("Admin name label set to: " + admin.getFullName());
        } else {
            System.err.println("WARNING: adminNameLabel is null!");
        }
        
        // Load initial data
        try {
            System.out.println("Loading contests...");
            loadContests();
            System.out.println("Loading contest selectors...");
            loadContestSelectors();
            System.out.println("Updating statistics...");
            updateStatistics();
            System.out.println("All data loaded successfully");
        } catch (Exception e) {
            System.err.println("Error loading data after setting admin: " + e.getMessage());
            e.printStackTrace();
            // Don't fail completely, just log the error
        }
    }
    
    @FXML
    private void handleCreateContest() {
        try {
            String contestId = contestIdField.getText().trim();
            String contestName = contestNameField.getText().trim();
            LocalDate date = contestDatePicker.getValue();
            int duration = Integer.parseInt(durationField.getText().trim());
            int maxParticipants = Integer.parseInt(maxParticipantsField.getText().trim());
            
            // Validation
            if (contestId.isEmpty() || contestName.isEmpty()) {
                showAlert("Validation Error", "Please fill in all required fields");
                return;
            }
            
            if (date == null) {
                showAlert("Validation Error", "Please select a date");
                return;
            }
            
            // Create contest with current time
            LocalDateTime dateTime = LocalDateTime.of(date, LocalTime.now());
            boolean isPast = dateTime.isBefore(LocalDateTime.now());
            
            Contest contest = new Contest(
                contestId,
                contestName,
                dateTime,
                duration,
                isPast,
                currentAdmin.getUsername(),
                maxParticipants
            );
            
            // Save to database
            ContestDatabase contestDB = ContestDatabase.getInstance();
            boolean success = contestDB.saveContestWithAdmin(contest);
            
            if (success) {
                // Auto-register the 30 default users for this contest
                autoRegisterDefaultUsers(contestId);
                
                contestStatusLabel.setText("✓ Contest created with 30 default participants!");
                contestStatusLabel.setStyle("-fx-text-fill: #4CAF50;");
                handleClearForm();
                loadContests();
                loadContestSelectors();
                updateStatistics();
            } else {
                showAlert("Error", "Failed to create contest. Contest ID may already exist.");
            }
            
        } catch (NumberFormatException e) {
            showAlert("Invalid Input", "Duration and Max Participants must be valid numbers");
        } catch (Exception e) {
            showAlert("Error", "Failed to create contest: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Auto-register the 30 default users (user001-user030) for a new contest
     */
    private void autoRegisterDefaultUsers(String contestId) {
        DatabaseManager dbManager = DatabaseManager.getInstance();
        UserDatabase userDB = UserDatabase.getInstance();
        
        for (int i = 1; i <= 30; i++) {
            String username = String.format("user%03d", i);
            
            // Check if user exists, get their rating
            User user = userDB.getUser(username);
            int rating = (user != null) ? user.getCurrentRating() : 1200;
            
            // Create participant with initial values
            Participant participant = new Participant(username, rating, 0, 0);
            participant.setRank(i);
            
            // Register user for contest and save as participant
            dbManager.registerUserForContest(contestId, username);
            dbManager.saveParticipant(contestId, participant);
        }
        
        System.out.println("Auto-registered 30 default users for contest: " + contestId);
    }
    
    @FXML
    private void handleClearForm() {
        contestIdField.clear();
        contestNameField.clear();
        contestDatePicker.setValue(LocalDate.now());
        durationField.clear();
        maxParticipantsField.setText("1000");
        contestStatusLabel.setText("");
    }
    
    @FXML
    private void handleLoadParticipants() {
        String selected = participantContestSelector.getValue();
        if (selected == null || selected.isEmpty()) {
            showAlert("Selection Required", "Please select a contest first");
            return;
        }
        
        // Extract contest ID from the format "contestId - contestName"
        if (selected.contains(" - ")) {
            selectedContestId = selected.split(" - ")[0].trim();
        } else {
            selectedContestId = selected.trim();
        }
        
        try {
            DatabaseManager dbManager = DatabaseManager.getInstance();
            List<Participant> participants = dbManager.getParticipantsByContest(selectedContestId);
            
            ObservableList<Participant> participantList = FXCollections.observableArrayList(participants);
            participantsTable.setItems(participantList);
            participantCountLabel.setText("(" + participants.size() + " participants)");
            
        } catch (Exception e) {
            showAlert("Error", "Failed to load participants: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @FXML
    private void handleAddParticipant() {
        if (selectedContestId == null || selectedContestId.isEmpty()) {
            showInfo("Selection Required", "Please select a contest and click 'Load Participants' first");
            return;
        }
        
        String username = addParticipantUsernameField.getText().trim();
        if (username.isEmpty()) {
            showAlert("Input Required", "Please enter a username");
            return;
        }
        
        try {
            // Check if user exists
            UserDatabase userDB = UserDatabase.getInstance();
            User user = userDB.getUserByUsername(username);
            
            if (user == null) {
                showAlert("User Not Found", "User '" + username + "' does not exist in the system");
                return;
            }
            
            // Create participant
            Participant participant = new Participant(
                username,
                user.getCurrentRating(),
                0, // problems solved
                0, // penalty
                0  // rank
            );
            
            // Save to database
            DatabaseManager dbManager = DatabaseManager.getInstance();
            boolean success = dbManager.saveParticipant(selectedContestId, participant);
            
            if (success) {
                addParticipantStatusLabel.setText("✓ Participant added successfully!");
                addParticipantStatusLabel.setStyle("-fx-text-fill: #4CAF50;");
                addParticipantUsernameField.clear();
                handleLoadParticipants();
                updateStatistics();
            } else {
                showAlert("Error", "Failed to add participant. They may already be registered.");
            }
            
        } catch (Exception e) {
            showAlert("Error", "Failed to add participant: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void handleRemoveParticipant(Participant participant) {
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Confirm Removal");
        confirmDialog.setHeaderText("Remove Participant");
        confirmDialog.setContentText("Are you sure you want to remove " + participant.getUsername() + " from this contest?");
        
        Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                DatabaseManager dbManager = DatabaseManager.getInstance();
                boolean success = dbManager.removeParticipant(selectedContestId, participant.getUsername());
                
                if (success) {
                    showInfo("Success", "Participant removed successfully");
                    handleLoadParticipants();
                    updateStatistics();
                } else {
                    showAlert("Error", "Failed to remove participant");
                }
            } catch (Exception e) {
                showAlert("Error", "Failed to remove participant: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    private void handleDeleteContest(Contest contest) {
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Confirm Deletion");
        confirmDialog.setHeaderText("Delete Contest");
        confirmDialog.setContentText("Are you sure you want to delete contest: " + contest.getName() + "?");
        
        Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                ContestDatabase contestDB = ContestDatabase.getInstance();
                boolean success = contestDB.deleteContest(contest.getContestId());
                
                if (success) {
                    showInfo("Success", "Contest deleted successfully");
                    loadContests();
                    loadContestSelectors();
                    updateStatistics();
                } else {
                    showAlert("Error", "Failed to delete contest");
                }
            } catch (Exception e) {
                showAlert("Error", "Failed to delete contest: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    @FXML
    private void handleRefreshStats() {
        updateStatistics();
        showInfo("Refreshed", "Statistics updated successfully");
    }
    
    @FXML
    private void handleManageStandings() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ContestStandings.fxml"));
            Parent root = loader.load();
            
            ContestStandingsController controller = loader.getController();
            controller.setCurrentUser(currentAdmin.getUsername(), true);
            
            Stage stage = (Stage) logoutButton.getScene().getWindow();
            
            // Preserve window state
            boolean wasFullScreen = stage.isFullScreen();
            boolean wasMaximized = stage.isMaximized();
            double currentWidth = stage.getWidth();
            double currentHeight = stage.getHeight();
            
            Scene scene = new Scene(root, currentWidth, currentHeight);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle("Contest Standings - Admin");
            
            // Restore window state
            if (wasMaximized) {
                stage.setMaximized(true);
            }
            if (wasFullScreen) {
                stage.setFullScreen(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to open standings manager: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleLogout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) logoutButton.getScene().getWindow();
            
            // Preserve window state
            boolean wasFullScreen = stage.isFullScreen();
            boolean wasMaximized = stage.isMaximized();
            double currentWidth = stage.getWidth();
            double currentHeight = stage.getHeight();
            
            Scene scene = new Scene(root, currentWidth, currentHeight);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle("Login - Contest Rating Predictor");
            
            // Restore window state
            if (wasMaximized) {
                stage.setMaximized(true);
            }
            if (wasFullScreen) {
                stage.setFullScreen(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to logout: " + e.getMessage());
        }
    }
    
    private void loadContests() {
        try {
            ContestDatabase contestDB = ContestDatabase.getInstance();
            List<Contest> contests = contestDB.getAllContests();
            
            ObservableList<Contest> contestList = FXCollections.observableArrayList(contests);
            contestsTable.setItems(contestList);
            
        } catch (Exception e) {
            System.err.println("Error loading contests: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void loadContestSelectors() {
        try {
            ContestDatabase contestDB = ContestDatabase.getInstance();
            List<Contest> contests = contestDB.getAllContests();
            
            ObservableList<String> contestIds = FXCollections.observableArrayList();
            for (Contest contest : contests) {
                contestIds.add(contest.getContestId() + " - " + contest.getName());
            }
            
            if (participantContestSelector != null) {
                participantContestSelector.setItems(contestIds);
                participantContestSelector.setOnAction(event -> {
                    String selected = participantContestSelector.getValue();
                    if (selected != null) {
                        selectedContestId = selected.split(" - ")[0];
                    }
                });
            }
            
        } catch (Exception e) {
            System.err.println("Error loading contest selectors: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void updateStatistics() {
        try {
            ContestDatabase contestDB = ContestDatabase.getInstance();
            UserDatabase userDB = UserDatabase.getInstance();
            DatabaseManager dbManager = DatabaseManager.getInstance();
            
            List<Contest> allContests = contestDB.getAllContests();
            
            if (totalContestsLabel != null) {
                totalContestsLabel.setText(String.valueOf(allContests.size()));
            }
            
            long futureCount = allContests.stream().filter(c -> !c.isPast()).count();
            if (futureContestsLabel != null) {
                futureContestsLabel.setText(String.valueOf(futureCount));
            }
            
            // Count total users
            if (totalUsersLabel != null) {
                int userCount = userDB.getAllUsers().size();
                totalUsersLabel.setText(String.valueOf(userCount));
            }
            
            // Count total participants across all contests
            if (totalParticipantsLabel != null) {
                int totalParticipants = 0;
                for (Contest contest : allContests) {
                    totalParticipants += dbManager.getParticipantsByContest(contest.getContestId()).size();
                }
                totalParticipantsLabel.setText(String.valueOf(totalParticipants));
            }
            
        } catch (Exception e) {
            System.err.println("Error updating statistics: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
    
    private void showInfo(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
