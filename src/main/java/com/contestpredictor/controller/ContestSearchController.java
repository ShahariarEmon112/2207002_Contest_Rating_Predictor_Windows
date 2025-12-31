package com.contestpredictor.controller;

import com.contestpredictor.data.ContestDatabase;
import com.contestpredictor.data.UserDatabase;
import com.contestpredictor.model.Contest;
import com.contestpredictor.model.Participant;
import com.contestpredictor.model.User;

import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class ContestSearchController {

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<String> filterComboBox;

    @FXML
    private VBox contestsContainer;

    @FXML
    private Label resultsCountLabel;

    @FXML
    private Label usernameLabel;

    private ContestDatabase contestDB;
    private User currentUser;

    @FXML
    private void initialize() {
        System.out.println("ContestSearchController initialized");
        contestDB = ContestDatabase.getInstance();
        currentUser = UserDatabase.getInstance().getCurrentUser();

        if (currentUser != null) {
            usernameLabel.setText(currentUser.getUsername());
            System.out.println("Current user: " + currentUser.getUsername());
        }

        // Setup filter combo box
        filterComboBox.getItems().addAll("All Contests", "Past Contests", "Upcoming Contests");
        filterComboBox.setValue("All Contests");

        // Load all contests initially
        List<Contest> allContests = contestDB.getAllContests();
        System.out.println("Loading " + allContests.size() + " contests initially");
        loadContests(allContests);
    }

    @FXML
    private void handleSearch() {
        System.out.println("Search button clicked!");
        String query = searchField.getText().trim();
        String filter = filterComboBox.getValue();
        
        System.out.println("Search query: '" + query + "'");
        System.out.println("Filter: " + filter);

        List<Contest> results;

        if (query.isEmpty()) {
            // No search query, filter by type
            results = getFilteredContests(filter);
            System.out.println("Filtered results: " + results.size());
        } else {
            // Search with query
            results = contestDB.searchContests(query);
            System.out.println("Search results before filter: " + results.size());
            // Apply filter on search results
            results = applyFilter(results, filter);
            System.out.println("Search results after filter: " + results.size());
        }

        loadContests(results);
    }

    @FXML
    private void handleClear() {
        System.out.println("Clear button clicked!");
        searchField.clear();
        filterComboBox.setValue("All Contests");
        loadContests(contestDB.getAllContests());
    }

    private List<Contest> getFilteredContests(String filter) {
        switch (filter) {
            case "Past Contests":
                return contestDB.getPastContests();
            case "Upcoming Contests":
                return contestDB.getFutureContests();
            default:
                return contestDB.getAllContests();
        }
    }

    private List<Contest> applyFilter(List<Contest> contests, String filter) {
        if (filter.equals("Past Contests")) {
            contests.removeIf(c -> !c.isPast());
        } else if (filter.equals("Upcoming Contests")) {
            contests.removeIf(Contest::isPast);
        }
        return contests;
    }

    private void loadContests(List<Contest> contests) {
        System.out.println("loadContests called with " + contests.size() + " contests");
        
        if (contestsContainer == null) {
            System.err.println("ERROR: contestsContainer is null!");
            return;
        }
        
        contestsContainer.getChildren().clear();
        resultsCountLabel.setText(contests.size() + " contest(s) found");

        if (contests.isEmpty()) {
            Label noResults = new Label("No contests found");
            noResults.setStyle("-fx-text-fill: #999; -fx-font-size: 16px;");
            VBox.setMargin(noResults, new Insets(50, 0, 0, 0));
            contestsContainer.getChildren().add(noResults);
            System.out.println("No contests to display");
            return;
        }

        System.out.println("Creating " + contests.size() + " contest cards");
        for (Contest contest : contests) {
            contestsContainer.getChildren().add(createContestCard(contest));
        }
        System.out.println("Contest cards created successfully");
    }

    private VBox createContestCard(Contest contest) {
        VBox card = new VBox(15);
        card.getStyleClass().add("contest-card");
        card.setPadding(new Insets(20));
        card.setMaxWidth(Double.MAX_VALUE);

        // Contest header
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);

        // Contest type badge
        Label badge = new Label(contest.isPast() ? "PAST" : "UPCOMING");
        badge.setStyle(contest.isPast() 
            ? "-fx-background-color: #e0e0e0; -fx-text-fill: #666; -fx-padding: 5 12; -fx-background-radius: 12; -fx-font-weight: bold; -fx-font-size: 11px;"
            : "-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-padding: 5 12; -fx-background-radius: 12; -fx-font-weight: bold; -fx-font-size: 11px;");

        // Contest ID
        Label idLabel = new Label(contest.getContestId());
        idLabel.setStyle("-fx-text-fill: #667eea; -fx-font-weight: bold; -fx-font-size: 14px;");

        header.getChildren().addAll(badge, idLabel);

        // Contest name
        Label nameLabel = new Label(contest.getContestName());
        nameLabel.setStyle("-fx-text-fill: #1a237e; -fx-font-size: 18px; -fx-font-weight: bold;");
        nameLabel.setWrapText(true);

        // Contest details
        HBox details = new HBox(30);
        details.setAlignment(Pos.CENTER_LEFT);

        // Date/Time
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
        Label dateLabel = new Label("📅 " + contest.getDateTime().format(formatter));
        dateLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 13px;");

        // Duration
        Label durationLabel = new Label("⏱️ " + contest.getDuration() + " mins");
        durationLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 13px;");

        // Participants
        Label participantsLabel = new Label("👥 " + contest.getParticipants().size() + " participants");
        participantsLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 13px;");

        details.getChildren().addAll(dateLabel, durationLabel, participantsLabel);

        // Action button
        Button actionButton = new Button(contest.isPast() ? "View Results" : "View Details");
        actionButton.setStyle("-fx-background-color: #8B5CF6; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 10 20; -fx-cursor: hand; -fx-font-weight: bold;");
        actionButton.setOnMouseEntered(e -> actionButton.setStyle("-fx-background-color: #7C3AED; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 10 20; -fx-cursor: hand; -fx-font-weight: bold;"));
        actionButton.setOnMouseExited(e -> actionButton.setStyle("-fx-background-color: #8B5CF6; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 10 20; -fx-cursor: hand; -fx-font-weight: bold;"));
        actionButton.setOnAction(e -> showContestDetails(contest));

        card.getChildren().addAll(header, nameLabel, details, actionButton);

        return card;
    }

    private void showContestDetails(Contest contest) {
        // Create a new stage for contest details
        Stage detailsStage = new Stage();
        detailsStage.setTitle(contest.getContestName() + " - Details & Rankings");
        
        // Main container
        VBox mainContainer = new VBox(20);
        mainContainer.setPadding(new Insets(20));
        mainContainer.setStyle("-fx-background-color: #f5f7fa;");
        
        // Contest header
        Label titleLabel = new Label(contest.getContestName());
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #1a237e;");
        
        // Contest info
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy 'at' HH:mm");
        String contestInfo = String.format(
            "Contest ID: %s  |  Date: %s  |  Duration: %d mins  |  Status: %s",
            contest.getContestId(),
            contest.getDateTime().format(formatter),
            contest.getDuration(),
            contest.isPast() ? "Completed" : "Upcoming"
        );
        Label infoLabel = new Label(contestInfo);
        infoLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #666;");
        infoLabel.setWrapText(true);
        
        // Separator
        javafx.scene.control.Separator separator = new javafx.scene.control.Separator();
        
        if (contest.isPast()) {
            // Show standings for past contests
            showPastContestStandings(mainContainer, contest, titleLabel, infoLabel, separator, detailsStage);
        } else {
            // Show registration for upcoming contests
            showUpcomingContestRegistration(mainContainer, contest, titleLabel, infoLabel, separator, detailsStage);
        }
    }
    
    /**
     * Display standings table for past contests
     */
    private void showPastContestStandings(VBox mainContainer, Contest contest, Label titleLabel, 
                                          Label infoLabel, javafx.scene.control.Separator separator, Stage detailsStage) {
        // Rankings section
        Label rankingsLabel = new Label("Contest Standings (" + contest.getParticipants().size() + " participants)");
        rankingsLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #667eea;");
        
        // Create TableView for standings
        javafx.scene.control.TableView<com.contestpredictor.model.Participant> table = new javafx.scene.control.TableView<>();
        table.setPrefHeight(450);
        table.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-radius: 8;");
        table.setEditable(true); // Make table editable
        
        // Rank column
        javafx.scene.control.TableColumn<com.contestpredictor.model.Participant, Integer> rankCol = 
            new javafx.scene.control.TableColumn<>("Rank");
        rankCol.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleIntegerProperty(cellData.getValue().getRank()).asObject());
        rankCol.setPrefWidth(60);
        rankCol.setStyle("-fx-alignment: CENTER;");
        
        // Username column
        javafx.scene.control.TableColumn<com.contestpredictor.model.Participant, String> usernameCol = 
            new javafx.scene.control.TableColumn<>("Username");
        usernameCol.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getUsername()));
        usernameCol.setPrefWidth(150);
        
        // Rating column
        javafx.scene.control.TableColumn<com.contestpredictor.model.Participant, Integer> ratingCol = 
            new javafx.scene.control.TableColumn<>("Rating");
        ratingCol.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleIntegerProperty(cellData.getValue().getCurrentRating()).asObject());
        ratingCol.setPrefWidth(80);
        ratingCol.setStyle("-fx-alignment: CENTER;");
        
        // Problems solved column (EDITABLE)
        javafx.scene.control.TableColumn<com.contestpredictor.model.Participant, Integer> solvedCol = 
            new javafx.scene.control.TableColumn<>("Solved");
        solvedCol.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleIntegerProperty(cellData.getValue().getProblemsSolved()).asObject());
        solvedCol.setPrefWidth(70);
        solvedCol.setStyle("-fx-alignment: CENTER;");
        solvedCol.setCellFactory(javafx.scene.control.cell.TextFieldTableCell.forTableColumn(new javafx.util.converter.IntegerStringConverter()));
        solvedCol.setEditable(true);
        solvedCol.setOnEditCommit(event -> {
            Participant p = event.getRowValue();
            p.setProblemsSolved(event.getNewValue());
            recalculateContestStandings(contest);
            table.refresh();
        });
        
        // Penalty column (EDITABLE)
        javafx.scene.control.TableColumn<com.contestpredictor.model.Participant, Integer> penaltyCol = 
            new javafx.scene.control.TableColumn<>("Penalty");
        penaltyCol.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleIntegerProperty(cellData.getValue().getTotalPenalty()).asObject());
        penaltyCol.setPrefWidth(80);
        penaltyCol.setStyle("-fx-alignment: CENTER;");
        penaltyCol.setCellFactory(javafx.scene.control.cell.TextFieldTableCell.forTableColumn(new javafx.util.converter.IntegerStringConverter()));
        penaltyCol.setEditable(true);
        penaltyCol.setOnEditCommit(event -> {
            Participant p = event.getRowValue();
            p.setTotalPenalty(event.getNewValue());
            recalculateContestStandings(contest);
            table.refresh();
        });
        
        // Performance (New Rating) column
        javafx.scene.control.TableColumn<com.contestpredictor.model.Participant, Integer> performanceCol = 
            new javafx.scene.control.TableColumn<>("Performance");
        performanceCol.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleIntegerProperty(cellData.getValue().getPredictedRating()).asObject());
        performanceCol.setPrefWidth(100);
        performanceCol.setStyle("-fx-alignment: CENTER;");
        
        // Rating change column with color coding
        javafx.scene.control.TableColumn<com.contestpredictor.model.Participant, String> changeCol = 
            new javafx.scene.control.TableColumn<>("Δ Rating");
        changeCol.setCellValueFactory(cellData -> {
            int change = cellData.getValue().getRatingChange();
            String text = (change >= 0 ? "+" : "") + change;
            return new javafx.beans.property.SimpleStringProperty(text);
        });
        changeCol.setPrefWidth(90);
        changeCol.setStyle("-fx-alignment: CENTER;");
        
        // Style rating change with colors
        changeCol.setCellFactory(column -> {
            return new javafx.scene.control.TableCell<com.contestpredictor.model.Participant, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setStyle("");
                    } else {
                        setText(item);
                        if (item.startsWith("+")) {
                            setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;");
                        } else if (item.startsWith("-")) {
                            setStyle("-fx-text-fill: #f44336; -fx-font-weight: bold;");
                        } else {
                            setStyle("-fx-text-fill: #999;");
                        }
                    }
                }
            };
        });
        
        // Add columns to table
        table.getColumns().addAll(rankCol, usernameCol, ratingCol, solvedCol, penaltyCol, performanceCol, changeCol);
        
        // Add participants to table
        if (!contest.getParticipants().isEmpty()) {
            table.getItems().addAll(contest.getParticipants());
        }
        
        // Info label for editing
        Label editInfo = new Label("💡 Tip: Double-click 'Solved' or 'Penalty' cells to edit. Rankings update automatically!");
        editInfo.setStyle("-fx-text-fill: #2196F3; -fx-font-size: 12px; -fx-font-style: italic;");
        
        // Close button
        Button closeButton = new Button("Close");
        closeButton.setStyle("-fx-background-color: #3B82F6; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 30; -fx-background-radius: 8; -fx-cursor: hand;");
        closeButton.setOnMouseEntered(e -> closeButton.setStyle("-fx-background-color: #2563EB; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 30; -fx-background-radius: 8; -fx-cursor: hand;"));
        closeButton.setOnMouseExited(e -> closeButton.setStyle("-fx-background-color: #3B82F6; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 30; -fx-background-radius: 8; -fx-cursor: hand;"));
        closeButton.setOnAction(e -> detailsStage.close());
        
        HBox buttonBox = new HBox(closeButton);
        buttonBox.setAlignment(Pos.CENTER);
        
        // Add all to main container
        mainContainer.getChildren().addAll(titleLabel, infoLabel, separator, rankingsLabel, table, editInfo, buttonBox);
        
        // Create scene and show
        Scene scene = new Scene(mainContainer, 900, 700);
        detailsStage.setScene(scene);
        detailsStage.show();
    }
    
    /**
     * Display registration form for upcoming contests
     */
    private void showUpcomingContestRegistration(VBox mainContainer, Contest contest, Label titleLabel,
                                                  Label infoLabel, javafx.scene.control.Separator separator, Stage detailsStage) {
        Label upcomingLabel = new Label("📅 This contest is upcoming. Register now!");
        upcomingLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #667eea;");
        
        // Registration info
        VBox regBox = new VBox(15);
        regBox.setStyle("-fx-background-color: white; -fx-padding: 20; -fx-background-radius: 10; -fx-border-color: #e0e0e0; -fx-border-radius: 10;");
        regBox.setMaxWidth(500);
        
        Label regTitle = new Label("Contest Registration");
        regTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        
        Label regInfo = new Label("You are registering as: " + (currentUser != null ? currentUser.getUsername() : "Guest"));
        regInfo.setStyle("-fx-font-size: 14px; -fx-text-fill: #666;");
        
        Label statusLabel = new Label("");
        statusLabel.setStyle("-fx-font-size: 14px;");
        
        // Registration button
        Button registerButton = new Button("Register for Contest");
        registerButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 16px; -fx-padding: 12 40; -fx-background-radius: 8; -fx-cursor: hand;");
        registerButton.setOnMouseEntered(e -> registerButton.setStyle("-fx-background-color: #45a049; -fx-text-fill: white; -fx-font-size: 16px; -fx-padding: 12 40; -fx-background-radius: 8; -fx-cursor: hand;"));
        registerButton.setOnMouseExited(e -> registerButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 16px; -fx-padding: 12 40; -fx-background-radius: 8; -fx-cursor: hand;"));
        registerButton.setOnAction(e -> {
            if (currentUser != null) {
                // Add user to contest participants
                Participant newParticipant = new Participant(
                    currentUser.getUsername(),
                    currentUser.getCurrentRating(),
                    0, 0
                );
                contest.addParticipant(newParticipant);
                statusLabel.setText("✅ Successfully registered for " + contest.getContestName());
                statusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #4CAF50; -fx-font-weight: bold;");
                registerButton.setDisable(true);
            } else {
                statusLabel.setText("❌ Please login to register");
                statusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #f44336;");
            }
        });
        
        regBox.getChildren().addAll(regTitle, regInfo, registerButton, statusLabel);
        regBox.setAlignment(Pos.CENTER);
        
        // Contest details
        Label detailsLabel = new Label("Contest Details");
        detailsLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1a237e;");
        
        VBox detailsBox = new VBox(8);
        detailsBox.setStyle("-fx-background-color: white; -fx-padding: 15; -fx-background-radius: 10;");
        detailsBox.getChildren().addAll(
            createDetailRow("Max Participants:", String.valueOf(contest.getMaxParticipants())),
            createDetailRow("Current Registrations:", String.valueOf(contest.getParticipants().size())),
            createDetailRow("Spots Available:", String.valueOf(contest.getMaxParticipants() - contest.getParticipants().size()))
        );
        
        // Close button
        Button closeButton = new Button("Close");
        closeButton.setStyle("-fx-background-color: #3B82F6; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 30; -fx-background-radius: 8; -fx-cursor: hand;");
        closeButton.setOnMouseEntered(e -> closeButton.setStyle("-fx-background-color: #2563EB; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 30; -fx-background-radius: 8; -fx-cursor: hand;"));
        closeButton.setOnMouseExited(e -> closeButton.setStyle("-fx-background-color: #3B82F6; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 30; -fx-background-radius: 8; -fx-cursor: hand;"));
        closeButton.setOnAction(e -> detailsStage.close());
        
        HBox buttonBox = new HBox(closeButton);
        buttonBox.setAlignment(Pos.CENTER);
        
        // Add all to main container
        mainContainer.getChildren().addAll(titleLabel, infoLabel, separator, upcomingLabel, regBox, detailsLabel, detailsBox, buttonBox);
        mainContainer.setAlignment(Pos.TOP_CENTER);
        
        // Create scene and show
        Scene scene = new Scene(mainContainer, 800, 650);
        detailsStage.setScene(scene);
        detailsStage.show();
    }
    
    /**
     * Creates a detail row for contest information
     */
    private HBox createDetailRow(String label, String value) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        Label lblLabel = new Label(label);
        lblLabel.setStyle("-fx-font-weight: bold; -fx-min-width: 180px;");
        Label lblValue = new Label(value);
        lblValue.setStyle("-fx-text-fill: #666;");
        row.getChildren().addAll(lblLabel, lblValue);
        return row;
    }
    
    /**
     * Recalculates contest standings when participant data changes
     * Uses AtCoder-style formula
     */
    private void recalculateContestStandings(Contest contest) {
        List<Participant> participants = contest.getParticipants();
        if (participants.isEmpty()) return;
        
        // Sort by problems solved (desc), then by penalty (asc)
        participants.sort((a, b) -> {
            if (a.getProblemsSolved() != b.getProblemsSolved()) {
                return b.getProblemsSolved() - a.getProblemsSolved();
            }
            return a.getTotalPenalty() - b.getTotalPenalty();
        });
        
        // Assign ranks
        for (int i = 0; i < participants.size(); i++) {
            participants.get(i).setRank(i + 1);
        }
        
        // Calculate average rating
        double avgRating = participants.stream()
            .mapToInt(Participant::getCurrentRating)
            .average()
            .orElse(1500.0);
        
        int totalParticipants = participants.size();
        
        // Calculate rating changes using AtCoder formula
        for (Participant p : participants) {
            // Performance = avgRating + 400 * log2(N / rank)
            int performance = (int) Math.round(avgRating + 400.0 * (Math.log(totalParticipants / (double) p.getRank()) / Math.log(2)));
            p.setPredictedRating(performance);
            
            // Rating change with adjustment factor
            int contestCount = 10; // Assume average
            double adjustmentFactor = contestCount <= 10 ? 0.9 - (contestCount * 0.05) : 0.2;
            int ratingChange = (int) Math.round((performance - p.getCurrentRating()) * adjustmentFactor);
            p.setRatingChange(ratingChange);
        }
    }

    @FXML
    private void handlePredictor() {
        System.out.println("Navigating to Predictor");
        navigateTo("/fxml/Predictor.fxml", "Rating Predictor");
    }

    @FXML
    private void handleProfile() {
        System.out.println("Navigating to Profile");
        navigateTo("/fxml/Profile.fxml", "Profile");
    }

    @FXML
    private void handleLogout() {
        System.out.println("Logging out");
        UserDatabase.getInstance().logout();
        navigateTo("/fxml/Login.fxml", "Login");
    }

    private void navigateTo(String fxmlPath, String title) {
        try {
            System.out.println("Loading FXML: " + fxmlPath);
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            
            Stage stage = (Stage) searchField.getScene().getWindow();
            
            // Preserve window state
            boolean wasFullScreen = stage.isFullScreen();
            boolean wasMaximized = stage.isMaximized();
            double currentWidth = stage.getWidth();
            double currentHeight = stage.getHeight();
            
            Scene scene = new Scene(root, currentWidth, currentHeight);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            
            stage.setScene(scene);
            stage.setTitle(title + " - Contest Rating Predictor");
            
            // Restore window state
            if (wasMaximized) {
                stage.setMaximized(true);
            }
            if (wasFullScreen) {
                stage.setFullScreen(true);
            }
            
            System.out.println("Navigation successful");
        } catch (Exception e) {
            System.err.println("Navigation error:");
            e.printStackTrace();
        }
    }
}
