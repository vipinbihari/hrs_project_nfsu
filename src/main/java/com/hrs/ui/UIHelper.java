package com.hrs.ui; // Package for UI components

import com.hrs.utils.Logger; // Importing Logger for logging messages
import javafx.application.Platform; // Importing Platform for JavaFX threading
import javafx.scene.control.Alert; // Importing Alert for displaying alerts
import javafx.scene.control.Alert.AlertType; // Importing AlertType for specifying alert types
import javafx.scene.control.TextArea; // Importing TextArea for displaying text areas
import javafx.stage.FileChooser; // Importing FileChooser for file selection dialogs

import java.io.File; // Importing File for file handling

/**
 * UIHelper provides utility methods for common UI operations in the application.
 */
public class UIHelper {

    /**
     * Shows an error dialog with the given message.
     * @param message The error message to display.
     */
    public static void showError(String message) {
        // Logs the error message with a null exception
        Logger.error("UI Error: " + message, null);
        // Runs the following code on the JavaFX Application Thread
        Platform.runLater(() -> {
            // Creates a new error alert
            Alert alert = new Alert(AlertType.ERROR);
            // Sets the title of the alert
            alert.setTitle("Error");
            // Sets the header text to null, which means no header will be displayed
            alert.setHeaderText(null);
            // Sets the content text of the alert to the given message
            alert.setContentText(message);
            // Shows the alert and waits for user response
            alert.showAndWait();
        });
    }

    /**
     * Shows an information dialog with the given message.
     * @param message The information message to display.
     */
    public static void showInfo(String message) {
        // Logs the information message
        Logger.info("UI Info: " + message);
        // Runs the following code on the JavaFX Application Thread
        Platform.runLater(() -> {
            // Creates a new information alert
            Alert alert = new Alert(AlertType.INFORMATION);
            // Sets the title of the alert
            alert.setTitle("Information");
            // Sets the header text to null, which means no header will be displayed
            alert.setHeaderText(null);
            // Sets the content text of the alert to the given message
            alert.setContentText(message);
            // Shows the alert and waits for user response
            alert.showAndWait();
        });
    }

    /**
     * Shows a warning dialog with the given message.
     * @param message The warning message to display.
     */
    public static void showWarning(String message) {
        // Logs the warning message
        Logger.warn("UI Warning: " + message);
        // Runs the following code on the JavaFX Application Thread
        Platform.runLater(() -> {
            // Creates a new warning alert
            Alert alert = new Alert(AlertType.WARNING);
            // Sets the title of the alert
            alert.setTitle("Warning");
            // Sets the header text to null, which means no header will be displayed
            alert.setHeaderText(null);
            // Sets the content text of the alert to the given message
            alert.setContentText(message);
            // Shows the alert and waits for user response
            alert.showAndWait();
        });
    }

    /**
     * Appends text to the output area and scrolls to the bottom.
     * @param outputArea The TextArea to append to.
     * @param text The text to append.
     */
    public static void appendOutput(TextArea outputArea, String text) {
        // Checks if the text is null or empty
        if (text == null || text.isEmpty()) {
            // If the text is null or empty, returns immediately
            return;
        }
        
        // Runs the following code on the JavaFX Application Thread
        Platform.runLater(() -> {
            // Appends the text to the output area
            outputArea.appendText(text);
            // Scrolls the output area to the bottom
            outputArea.setScrollTop(Double.MAX_VALUE);
        });
    }

    /**
     * Opens a file chooser dialog to select a log file location.
     * @return The selected log file path, or an empty string if no file was selected.
     */
    public static String chooseLogFile() {
        // Creates a new file chooser
        FileChooser fileChooser = new FileChooser();
        // Sets the title of the file chooser
        fileChooser.setTitle("Select Log File Location");
        // Shows the file chooser dialog
        File file = fileChooser.showSaveDialog(null);
        // Returns the selected file path, or an empty string if no file was selected
        return file != null ? file.getAbsolutePath() : "";
    }

    /**
     * Opens a file chooser dialog to select a configuration file.
     * @return The selected configuration file path, or an empty string if no file was selected.
     */
    public static String chooseConfigFile() {
        // Creates a new file chooser
        FileChooser fileChooser = new FileChooser();
        // Sets the title of the file chooser
        fileChooser.setTitle("Select Config File");
        // Adds a file extension filter to the file chooser
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Config Files", "*.txt", "*.json", "*.conf", "*.py")
        );
        // Shows the file chooser dialog
        File file = fileChooser.showOpenDialog(null);
        // Returns the selected file path, or an empty string if no file was selected
        return file != null ? file.getAbsolutePath() : "";
    }
}
