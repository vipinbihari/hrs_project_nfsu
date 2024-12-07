package com.hrs.service; // Package for service classes

import javafx.application.Platform; // Importing Platform for JavaFX threading
import javafx.collections.ObservableList; // Importing ObservableList for observable collections
import javafx.scene.control.Button; // Importing Button for UI button control
import javafx.scene.control.TextArea; // Importing TextArea for UI text area control
import com.hrs.model.ScanResult; // Importing ScanResult model for storing scan results
import com.hrs.ui.UIHelper; // Importing UIHelper for UI utility methods
import com.hrs.utils.Logger; // Importing Logger for logging messages
import java.io.IOException; // Importing IOException for handling I/O exceptions
import java.util.List; // Importing List interface for list operations
import java.util.Queue; // Importing Queue interface for queue operations
import java.util.concurrent.*; // Importing concurrent utilities for multithreading
import java.util.concurrent.atomic.AtomicInteger; // Importing AtomicInteger for atomic operations

/**
 * ScanService manages the scanning operations using the smuggler.py script.
 */
public class ScanService {
    // Maximum number of concurrent scans allowed
    private static final int MAX_CONCURRENT_SCANS = 5; // Defines the maximum number of concurrent scans
    // TextArea for displaying output logs
    private final TextArea outputArea; // Holds the TextArea for output logs
    // ObservableList for holding scan results
    private final ObservableList<ScanResult> tableData; // Holds the ObservableList for scan results
    // Button to stop the scanning process
    private final Button stopButton; // Holds the Button to stop the scan
    // Current ProcessManager handling the scan
    private ProcessManager currentProcessManager; // Holds the current ProcessManager
    // Flag to indicate if a batch scan is running
    private volatile boolean isBatchRunning; // Flag to track batch scan status
    // ExecutorService for managing concurrent scan tasks
    private ExecutorService executorService; // Manages concurrent scan tasks
    // Queue for holding pending URLs to be scanned
    private Queue<String> pendingUrls; // Holds pending URLs for scanning
    // AtomicInteger to track the number of active scans
    private AtomicInteger activeScans; // Tracks the number of active scans
    // Lock object for synchronizing access
    private Object lock = new Object(); // Lock object for synchronization

    /**
     * Constructor to initialize the ScanService.
     * @param outputArea The TextArea for displaying output logs
     * @param tableData The ObservableList for holding scan results
     * @param stopButton The Button to stop the scanning process
     */
    public ScanService(TextArea outputArea, ObservableList<ScanResult> tableData, Button stopButton) {
        this.outputArea = outputArea; // Assigns the TextArea for output logs
        this.tableData = tableData; // Assigns the ObservableList for scan results
        this.stopButton = stopButton; // Assigns the Button to stop the scan
        this.executorService = Executors.newFixedThreadPool(MAX_CONCURRENT_SCANS); // Initializes the ExecutorService
        this.activeScans = new AtomicInteger(0); // Initializes the AtomicInteger for active scans
    }

    /**
     * Starts a single scan with the provided parameters.
     * @param url The target URL
     * @param vhost The virtual host
     * @param method The HTTP method
     * @param logFile The log file path
     * @param configFile The config file path
     * @param timeout The timeout duration
     * @param exitEarly Flag to exit early
     * @param quietMode Flag for quiet mode
     * @param noColor Flag to disable color output
     */
    public void startSingleScan(String url, String vhost, String method, String logFile,
                              String configFile, int timeout, boolean exitEarly,
                              boolean quietMode, boolean noColor) {
        Logger.info("Starting single scan for URL: " + url); // Logs the start of a single scan
        if (!validateUrl(url)) { // Validates the URL
            return; // Returns if the URL is invalid
        }

        try {
            // Builds the command for the scan
            CommandBuilder commandBuilder = buildCommand(url, vhost, method, logFile,
                                                       configFile, timeout, exitEarly,
                                                       quietMode, noColor);

            List<String> command = commandBuilder.build(); // Builds the command
            Logger.debug("Executing command: " + String.join(" ", command)); // Logs the command
            outputArea.appendText("Executing command: " + String.join(" ", command) + "\n"); // Appends the command to the output area

            startProcess(command); // Starts the process
            Platform.runLater(() -> stopButton.setDisable(false)); // Enables the stop button
        } catch (Exception e) {
            Logger.error("Error executing single scan", e); // Logs the error
            UIHelper.showError("Error executing command: " + e.getMessage()); // Shows the error message
            Platform.runLater(() -> stopButton.setDisable(true)); // Disables the stop button
        }
    }

    /**
     * Starts a batch scan with the provided parameters.
     * @param urls The list of URLs to scan
     * @param vhost The virtual host
     * @param method The HTTP method
     * @param logFile The log file path
     * @param configFile The config file path
     * @param timeout The timeout duration
     * @param exitEarly Flag to exit early
     * @param quietMode Flag for quiet mode
     * @param noColor Flag to disable color output
     */
    public void startBatchScan(List<String> urls, String vhost, String method, String logFile,
                             String configFile, int timeout, boolean exitEarly,
                             boolean quietMode, boolean noColor) {
        Logger.info("Starting batch scan with " + urls.size() + " URLs"); // Logs the start of a batch scan
        if (urls.isEmpty()) { // Checks if the URL list is empty
            Logger.error("No URLs found in file", null); // Logs the error
            UIHelper.showError("No URLs found in the file or file is empty"); // Shows the error message
            return; // Returns if the URL list is empty
        }

        isBatchRunning = true; // Sets the batch scan flag to true
        Platform.runLater(() -> stopButton.setDisable(false)); // Enables the stop button
        outputArea.appendText("Starting batch scan for " + urls.size() + " URLs\n"); // Appends the batch scan message to the output area

        // Initializes the pending URLs queue
        pendingUrls = new ConcurrentLinkedQueue<>(urls); // Initializes the pending URLs queue
        activeScans.set(0); // Resets the active scans counter

        // Starts the initial batch of scans
        for (int i = 0; i < Math.min(MAX_CONCURRENT_SCANS, urls.size()); i++) {
            startNextScan(vhost, method, logFile, configFile, timeout, exitEarly, quietMode, noColor); // Starts the next scan
        }
    }

    /**
     * Starts the next scan in the batch.
     * @param vhost The virtual host
     * @param method The HTTP method
     * @param logFile The log file path
     * @param configFile The config file path
     * @param timeout The timeout duration
     * @param exitEarly Flag to exit early
     * @param quietMode Flag for quiet mode
     * @param noColor Flag to disable color output
     */
    private void startNextScan(String vhost, String method, String logFile,
                             String configFile, int timeout, boolean exitEarly,
                             boolean quietMode, boolean noColor) {
        String url = pendingUrls.poll(); // Retrieves the next URL from the queue
        if (url != null && isBatchRunning) { // Checks if the URL is valid and the batch scan is running
            activeScans.incrementAndGet(); // Increments the active scans counter
            executorService.submit(() -> {
                try {
                    processUrl(url, vhost, method, logFile, configFile, timeout, exitEarly, quietMode, noColor); // Processes the URL
                } finally {
                    activeScans.decrementAndGet(); // Decrements the active scans counter
                    synchronized (lock) {
                        // Starts the next URL if available
                        if (isBatchRunning && !pendingUrls.isEmpty()) {
                            startNextScan(vhost, method, logFile, configFile, timeout, exitEarly, quietMode, noColor); // Starts the next scan
                        } else if (activeScans.get() == 0) {
                            // All scans completed
                            finishBatchScan(); // Finishes the batch scan
                        }
                    }
                }
            });
        }
    }

    /**
     * Finishes the batch scan.
     */
    private void finishBatchScan() {
        boolean wasRunning = isBatchRunning; // Retrieves the batch scan flag
        isBatchRunning = false; // Resets the batch scan flag
        Platform.runLater(() -> {
            if (wasRunning) {
                Logger.info("Batch scan completed"); // Logs the batch scan completion
                outputArea.appendText("\nBatch scan completed.\n"); // Appends the batch scan completion message to the output area
            }
            stopButton.setDisable(true); // Disables the stop button
        });
    }

    /**
     * Stops the current process.
     */
    public void stopCurrentProcess() {
        Logger.info("Stopping current process"); // Logs the stop process message
        isBatchRunning = false; // Resets the batch scan flag
        if (currentProcessManager != null) {
            currentProcessManager.stopProcess(); // Stops the current process
        }
        pendingUrls.clear(); // Clears the pending URLs queue
        executorService.shutdownNow(); // Shuts down the executor service
        executorService = Executors.newFixedThreadPool(MAX_CONCURRENT_SCANS); // Reinitializes the executor service
        Platform.runLater(() -> stopButton.setDisable(true)); // Disables the stop button
    }

    /**
     * Processes the URL.
     * @param url The target URL
     * @param vhost The virtual host
     * @param method The HTTP method
     * @param logFile The log file path
     * @param configFile The config file path
     * @param timeout The timeout duration
     * @param exitEarly Flag to exit early
     * @param quietMode Flag for quiet mode
     * @param noColor Flag to disable color output
     */
    private void processUrl(String url, String vhost, String method, String logFile,
                          String configFile, int timeout, boolean exitEarly,
                          boolean quietMode, boolean noColor) {
        try {
            // Builds the command for the scan
            CommandBuilder commandBuilder = buildCommand(url, vhost, method, logFile,
                                                       configFile, timeout, exitEarly,
                                                       quietMode, noColor);

            List<String> command = commandBuilder.build(); // Builds the command
            Logger.debug("Executing command: " + String.join(" ", command)); // Logs the command
            Platform.runLater(() -> outputArea.appendText("\n=== Processing URL: " + url + " ===\n" +
                                                        "Executing command: " + String.join(" ", command) + "\n")); // Appends the command to the output area

            synchronized (lock) {
                if (currentProcessManager != null && currentProcessManager.isProcessRunning()) {
                    currentProcessManager.stopProcess(); // Stops the current process
                }
                currentProcessManager = new ProcessManager(s -> Platform.runLater(() -> outputArea.appendText(s)), tableData); // Initializes the ProcessManager
                currentProcessManager.startProcess(command); // Starts the process
            }

            while (currentProcessManager.isProcessRunning() && isBatchRunning) {
                Thread.sleep(100); // Waits for the process to complete
            }
        } catch (Exception e) {
            Logger.error("Error processing URL: " + url, e); // Logs the error
            Platform.runLater(() -> outputArea.appendText("Error executing command: " + e.getMessage() + "\n")); // Appends the error message to the output area
        }
    }

    /**
     * Starts the process.
     * @param command The command to execute
     */
    private void startProcess(List<String> command) {
        if (currentProcessManager != null && currentProcessManager.isProcessRunning()) {
            currentProcessManager.stopProcess(); // Stops the current process
        }
        currentProcessManager = new ProcessManager(s -> Platform.runLater(() -> outputArea.appendText(s)), tableData); // Initializes the ProcessManager
        try {
            currentProcessManager.startProcess(command); // Starts the process
        } catch (IOException e) {
            Logger.error("Error starting process", e); // Logs the error
            Platform.runLater(() -> {
                outputArea.appendText("Error starting process: " + e.getMessage() + "\n"); // Appends the error message to the output area
                stopButton.setDisable(true); // Disables the stop button
            });
        }
    }

    /**
     * Validates the URL.
     * @param url The target URL
     * @return True if the URL is valid, false otherwise
     */
    private boolean validateUrl(String url) {
        if (url == null || url.trim().isEmpty()) { // Checks if the URL is empty
            UIHelper.showError("URL cannot be empty"); // Shows the error message
            return false; // Returns false if the URL is empty
        }
        return true; // Returns true if the URL is valid
    }

    /**
     * Checks if a scan is running.
     * @return True if a scan is running, false otherwise
     */
    public boolean isScanning() {
        return (currentProcessManager != null && currentProcessManager.isProcessRunning()) || isBatchRunning; // Checks if a scan is running
    }

    /**
     * Builds the command for the scan.
     * @param url The target URL
     * @param vhost The virtual host
     * @param method The HTTP method
     * @param logFile The log file path
     * @param configFile The config file path
     * @param timeout The timeout duration
     * @param exitEarly Flag to exit early
     * @param quietMode Flag for quiet mode
     * @param noColor Flag to disable color output
     * @return The CommandBuilder instance
     */
    private CommandBuilder buildCommand(String url, String vhost, String method, String logFile,
                                     String configFile, int timeout, boolean exitEarly,
                                     boolean quietMode, boolean noColor) {
        return new CommandBuilder()
                .withUrl(url) // Sets the URL
                .withVhost(vhost) // Sets the virtual host
                .withMethod(method) // Sets the HTTP method
                .withLogFile(logFile) // Sets the log file path
                .withConfigFile(configFile) // Sets the config file path
                .withTimeout(timeout) // Sets the timeout duration
                .withExitEarly(exitEarly) // Sets the exit early flag
                .withQuietMode(quietMode) // Sets the quiet mode flag
                .withNoColor(noColor); // Sets the no color flag
    }
}
