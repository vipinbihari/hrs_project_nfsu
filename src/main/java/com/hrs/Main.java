package com.hrs; // Defines the package for the class, used for organizing classes

import javafx.application.Application; // Importing JavaFX Application class for creating GUI applications
import javafx.fxml.FXMLLoader; // Importing FXMLLoader to load FXML files
import javafx.scene.Parent; // Importing Parent class as the base class for all nodes
import javafx.scene.Scene; // Importing Scene class to represent the visual contents of a stage
import javafx.stage.Stage; // Importing Stage class which represents the main window

public class Main extends Application { // Main class extending Application, the entry point for JavaFX applications

    @Override
    public void start(Stage primaryStage) throws Exception { // Overridden start method, sets up the primary stage
        Parent root = FXMLLoader.load(getClass().getResource("main.fxml")); // Loads the main FXML file
        Scene scene = new Scene(root); // Creates a scene with the loaded FXML root
        scene.getStylesheets().add(getClass().getResource("styles.css").toExternalForm()); // Adds external CSS to the scene
        primaryStage.setTitle("HTTP Request Smuggling Detector"); // Sets the title of the primary stage
        primaryStage.setScene(scene); // Sets the scene on the primary stage
        primaryStage.show(); // Displays the primary stage
    }

    public static void main(String[] args) { // Main method to launch the JavaFX application
        launch(args); // Calls the launch method to start the JavaFX application lifecycle
    }
}
