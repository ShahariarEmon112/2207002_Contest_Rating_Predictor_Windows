package com.contestpredictor;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            System.out.println("Starting Contest Rating Predictor...");
            System.out.println("Loading FXML file: /fxml/Login.fxml");
            
            // Check if FXML file exists
            java.net.URL fxmlUrl = getClass().getResource("/fxml/Login.fxml");
            if (fxmlUrl == null) {
                System.err.println("ERROR: Login.fxml not found in resources!");
                System.err.println("Make sure the file exists at: src/main/resources/fxml/Login.fxml");
                showErrorDialog("Login.fxml not found in resources!");
                return;
            }
            
            // Start with Login screen
            Parent root = FXMLLoader.load(fxmlUrl);
            System.out.println("FXML loaded successfully");
            
            // Check if CSS file exists
            java.net.URL cssUrl = getClass().getResource("/css/styles.css");
            if (cssUrl == null) {
                System.err.println("WARNING: styles.css not found!");
            }
            
            Scene scene = new Scene(root, 1000, 650);
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
                System.out.println("CSS loaded successfully");
            }
            
            primaryStage.setTitle("Login - Contest Rating Predictor");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(900);
            primaryStage.setMinHeight(600);
            primaryStage.show();
            System.out.println("Application window displayed successfully!");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error starting application: " + e.getMessage());
            System.err.println("Cause: " + e.getCause());
            showErrorDialog("Error: " + e.getMessage());
        }
    }
    
    private void showErrorDialog(String message) {
        try {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle("Application Error");
            alert.setHeaderText("Failed to start application");
            alert.setContentText(message);
            alert.showAndWait();
        } catch (Exception e) {
            System.err.println("Could not show error dialog: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
