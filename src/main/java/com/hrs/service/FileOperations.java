package com.hrs.service; // Package for service classes

import com.hrs.utils.Logger; // Importing Logger for logging messages
import javafx.stage.FileChooser; // Importing FileChooser for file selection dialogs
import javafx.stage.Window; // Importing Window for GUI window handling
import java.io.*; // Importing I/O classes for file operations
import java.util.ArrayList; // Importing ArrayList for dynamic array implementation
import java.util.List; // Importing List interface for list operations

/**
 * FileOperations provides utility methods for file handling operations.
 */
public class FileOperations {
    // FileChooser for selecting files
    private final FileChooser fileChooser;

    /**
     * Constructor to initialize the FileOperations.
     */
    public FileOperations() {
        this.fileChooser = new FileChooser(); // Initializes the file chooser
    }

    /**
     * Opens a file chooser dialog to select a file.
     * @param title The title of the dialog
     * @param description The description of the file type
     * @param extensions The file extensions to filter
     * @return The selected file
     */
    public File chooseFile(String title, String description, String... extensions) {
        Logger.debug("Opening file chooser: " + title); // Logs the opening of the file chooser
        fileChooser.setTitle(title); // Sets the title of the file chooser
        
        // Creates a new extension filter for the specified file type
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter(
            description, extensions
        );
        // Clears any existing file extension filters
        fileChooser.getExtensionFilters().clear();
        // Adds the new file extension filter
        fileChooser.getExtensionFilters().add(extFilter);
        
        // Shows the open dialog for file selection
        File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            // Logs the selected file
            Logger.info("Selected file: " + file.getAbsolutePath());
            // Remember the directory for next time
            fileChooser.setInitialDirectory(file.getParentFile()); // Sets the initial directory
        }
        // Returns the selected file
        return file;
    }

    /**
     * Loads URLs from a file into a list.
     * @param file The file to read URLs from
     * @return A list of URLs
     */
    public List<String> loadUrlsFromFile(File file) {
        // Initializes an empty list to store the URLs
        List<String> urls = new ArrayList<>();
        if (file == null) { // Checks if the file is null
            // Logs an error message if the file is null
            Logger.error("Cannot load URLs: file is null", null);
            // Returns the empty list
            return urls;
        }

        // Logs the start of loading URLs from the file
        Logger.info("Loading URLs from file: " + file.getAbsolutePath());
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            // Reads the file line by line
            String line;
            while ((line = reader.readLine()) != null) {
                // Trims the line to remove leading and trailing whitespace
                line = line.trim();
                // Checks if the line is not empty
                if (!line.isEmpty()) {
                    // Adds the line to the list of URLs
                    urls.add(line);
                }
            }
            // Logs the number of URLs loaded from the file
            Logger.info("Loaded " + urls.size() + " URLs from file");
        } catch (IOException e) {
            // Logs an error message if there is an issue reading the file
            Logger.error("Error reading URLs from file: " + file.getAbsolutePath(), e);
        }
        // Returns the list of URLs
        return urls;
    }

    /**
     * Reads the content of a file into a string.
     * @param file The file to read content from
     * @return The content of the file as a string
     */
    public String readFileContent(File file) {
        if (file == null) { // Checks if the file is null
            // Logs an error message if the file is null
            Logger.error("Cannot read file: file is null", null);
            // Returns an empty string
            return "";
        }

        // Logs the start of reading content from the file
        Logger.info("Reading content from file: " + file.getAbsolutePath());
        // Initializes a StringBuilder to store the file content
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            // Reads the file line by line
            String line;
            while ((line = reader.readLine()) != null) {
                // Appends the line to the StringBuilder
                content.append(line).append("\n");
            }
            // Logs the number of characters read from the file
            Logger.debug("Read " + content.length() + " characters from file");
        } catch (IOException e) {
            // Logs an error message if there is an issue reading the file
            Logger.error("Error reading file: " + file.getAbsolutePath(), e);
        }
        // Returns the content of the file as a string
        return content.toString();
    }

    /**
     * Saves content to a file.
     * @param content The content to save
     * @param file The file to save content to
     */
    public void saveToFile(String content, File file) {
        if (file == null) { // Checks if the file is null
            // Logs an error message if the file is null
            Logger.error("Cannot save file: file is null", null);
            // Returns without saving the file
            return;
        }

        // Logs the start of saving content to the file
        Logger.info("Saving content to file: " + file.getAbsolutePath());
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            // Writes the content to the file
            writer.write(content);
            // Logs a success message after saving the file
            Logger.info("Successfully saved content to file");
        } catch (IOException e) {
            // Logs an error message if there is an issue saving the file
            Logger.error("Error saving to file: " + file.getAbsolutePath(), e);
        }
    }
}
