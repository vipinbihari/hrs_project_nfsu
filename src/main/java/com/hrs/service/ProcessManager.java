package com.hrs.service; // Package for service classes

import com.hrs.model.ScanResult; // Importing ScanResult model for storing scan results
import com.hrs.utils.Logger; // Importing Logger for logging messages
import javafx.application.Platform; // Importing Platform for JavaFX threading
import javafx.collections.ObservableList; // Importing ObservableList for observable collections
import java.io.BufferedReader; // Importing BufferedReader for reading input streams
import java.io.IOException; // Importing IOException for handling I/O exceptions
import java.io.InputStreamReader; // Importing InputStreamReader for converting byte streams to character streams
import java.util.List; // Importing List interface for list operations
import java.util.concurrent.atomic.AtomicBoolean; // Importing AtomicBoolean for atomic operations
import java.util.function.Consumer; // Importing Consumer for handling input
import java.util.regex.Matcher; // Importing Matcher for regex operations
import java.util.regex.Pattern; // Importing Pattern for regex operations

/**
 * ProcessManager is responsible for managing the execution of the Python script.
 * It handles process output, captures content, and updates the scan results table.
 */
public class ProcessManager {
    // Current process being managed
    private Process currentProcess;
    // AtomicBoolean to track if the process is running
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    // Consumer to handle output from the process
    private final Consumer<String> outputHandler;
    // ObservableList to update with scan results
    private final ObservableList<ScanResult> tableData;
    // StringBuilder to capture output content
    private StringBuilder capturedContent;
    // Boolean to track if content is being captured
    private boolean isCapturing;
    
    // Pattern to match the Host header in HTTP requests
    private static final Pattern HOST_PATTERN = Pattern.compile("Host:\\s*([^\\r\\n]+)", Pattern.CASE_INSENSITIVE);
    // Pattern to match HTTP methods
    private static final Pattern METHOD_PATTERN = Pattern.compile("^(GET|POST|PUT|DELETE|PATCH|HEAD|OPTIONS)\\s+", Pattern.CASE_INSENSITIVE);
    // Pattern to match the Transfer-Encoding header
    private static final Pattern TE_PATTERN = Pattern.compile("Transfer-Encoding:\\s*([^\\r\\n]+)", Pattern.CASE_INSENSITIVE);

    /**
     * Constructor that sets the output handler for the process.
     * @param outputHandler A Consumer to handle process output.
     * @param tableData ObservableList to update with scan results
     */
    public ProcessManager(Consumer<String> outputHandler, ObservableList<ScanResult> tableData) {
        // Assigns the output handler to handle process output
        this.outputHandler = outputHandler; 
        // Assigns the observable list for scan results
        this.tableData = tableData; 
        // Initializes the StringBuilder for capturing content
        this.capturedContent = new StringBuilder(); 
        // Sets the capturing flag to false
        this.isCapturing = false; 
    }

    /**
     * Starts the process with the given command.
     * @param command The command to execute as a list of strings.
     * @throws IOException If an I/O error occurs.
     */
    public void startProcess(List<String> command) throws IOException {
        // Creates a new ProcessBuilder with the given command
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        // Redirects error stream to output stream
        processBuilder.redirectErrorStream(true);
        // Sets the PYTHONUNBUFFERED environment variable to 1
        processBuilder.environment().put("PYTHONUNBUFFERED", "1");
        
        // Logs the command being executed
        Logger.debug("Starting process with command: " + String.join(" ", command));
        // Starts the process
        currentProcess = processBuilder.start();
        // Sets the running flag to true
        isRunning.set(true);

        // Reads output in a separate thread
        new Thread(() -> {
            try {
                // Creates a BufferedReader to read the process output
                BufferedReader reader = new BufferedReader(new InputStreamReader(currentProcess.getInputStream()));
                String line;
                // Continuously reads lines from the process output
                while (isRunning.get() && (line = reader.readLine()) != null) {
                    // Stores the current line in a final variable
                    final String currentLine = line;
                    // Logs the line read from the process
                    Logger.debug("Read line from process: " + currentLine);
                    // Processes the output line
                    processOutput(currentLine);
                }
            } catch (IOException e) {
                // Logs any I/O errors that occur
                Logger.error("Error reading process output", e);
            }
        }).start();
    }

    /**
     * Processes the output from the process.
     * @param output The output line from the process.
     */
    private void processOutput(String output) {
        // Always sends output to log area immediately with a newline
        Platform.runLater(() -> outputHandler.accept(output + "\n"));
        
        // Checks for markers in the output
        String trimmedOutput = output.trim();
        // Checks if the output is the START marker
        if (trimmedOutput.equals("START")) {
            // Logs the START marker
            Logger.debug("Found START marker, beginning capture");
            // Sets the capturing flag to true
            isCapturing = true;
            // Initializes the captured content
            capturedContent = new StringBuilder();
        } 
        // Checks if the output is the END marker
        else if (trimmedOutput.equals("END")) {
            // Logs the END marker
            Logger.debug("Found END marker, processing captured content");
            // Sets the capturing flag to false
            isCapturing = false;
            // Gets the captured content as a string
            String captured = capturedContent.toString();
            // Logs the captured content
            Logger.debug("Captured content:\n" + captured);
            // Processes the captured content
            processRequest(captured);
        } 
        // Checks if content is being captured
        else if (isCapturing) {
            // Logs the line being captured
            Logger.debug("Capturing line: " + output);
            // Adds the line to the captured content with a newline
            if (capturedContent.length() > 0) {
                capturedContent.append("\n");
            }
            capturedContent.append(output);
        }
    }

    /**
     * Processes the captured request content.
     * @param content The captured request content.
     */
    private void processRequest(String content) {
        // Logs the processing of the request content
        Logger.debug("Processing request content");
        // No need to normalize line endings here - RawHttpClient will handle it
        
        // Extracts the host from the content using the HOST_PATTERN
        String host = extractPattern(HOST_PATTERN, content);
        // Logs the extracted host
        Logger.debug("Extracted host: " + host);
        
        // Extracts the method from the content using the METHOD_PATTERN
        String method = extractPattern(METHOD_PATTERN, content);
        // Logs the extracted method
        Logger.debug("Extracted method: " + method);
        
        // Extracts the Transfer-Encoding header from the content using the TE_PATTERN
        String transferEncoding = extractPattern(TE_PATTERN, content);
        // Logs the extracted Transfer-Encoding header
        Logger.debug("Extracted TE header: " + transferEncoding);
        
        // Constructs the URL from the host
        String url = host != null ? (host.startsWith("http") ? host : "https://" + host) : "";
        // Logs the constructed URL
        Logger.debug("Final URL: " + url);
        
        // Updates the table on the JavaFX thread
        final String finalContent = content;
        Platform.runLater(() -> {
            try {
                // Logs the creation of a new ScanResult object
                Logger.debug("Creating new ScanResult object");
                // Creates a new ScanResult object
                ScanResult result = new ScanResult(
                    url, 
                    method != null ? method.trim() : "", 
                    transferEncoding != null ? transferEncoding : "",
                    finalContent
                );
                // Logs the addition of the result to the table data
                Logger.debug("Adding result to table data. Current table size: " + tableData.size());
                // Adds the result to the table data
                tableData.add(result);
                // Logs the successful addition of the result to the table
                Logger.info("Successfully added new row to table. New size: " + tableData.size());
            } catch (Exception e) {
                // Logs any errors that occur while adding the result to the table
                Logger.error("Error adding result to table", e);
            }
        });
    }

    /**
     * Extracts a pattern from the given content.
     * @param pattern The pattern to extract.
     * @param content The content to extract from.
     * @return The extracted pattern or null if not found.
     */
    private String extractPattern(Pattern pattern, String content) {
        try {
            // Creates a Matcher for the pattern
            Matcher matcher = pattern.matcher(content);
            // Finds the pattern in the content
            String result = matcher.find() ? matcher.group(1) : null;
            // Logs the extracted pattern
            Logger.debug("Pattern " + pattern + " extracted: " + result);
            // Returns the extracted pattern
            return result;
        } catch (Exception e) {
            // Logs any errors that occur while extracting the pattern
            Logger.error("Error extracting pattern: " + pattern, e);
            // Returns null if an error occurs
            return null;
        }
    }

    /**
     * Stops the current process if it is running.
     */
    public void stopProcess() {
        // Checks if the process is running
        if (currentProcess != null) {
            // Sets the running flag to false
            isRunning.set(false);
            // Destroys the process
            currentProcess.destroy();
        }
    }

    /**
     * Checks if the process is currently running.
     * @return true if the process is running, false otherwise.
     */
    public boolean isProcessRunning() {
        // Returns true if the process is not null and is alive
        return currentProcess != null && currentProcess.isAlive();
    }
}
