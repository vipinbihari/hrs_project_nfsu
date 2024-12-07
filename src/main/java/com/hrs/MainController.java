package com.hrs; // Defines the package for the class, used for organizing classes

import com.hrs.model.ScanResult; // Importing ScanResult model for table data
import com.hrs.service.FileOperations; // Importing FileOperations service for file handling
import com.hrs.service.ScanService; // Importing ScanService for managing scan operations
import com.hrs.ui.TableManager; // Importing TableManager for managing table UI
import com.hrs.ui.UIHelper; // Importing UIHelper for UI utilities
import com.hrs.utils.Logger; // Importing Logger for logging messages
import javafx.application.Platform; // Importing Platform for JavaFX threading
import javafx.collections.FXCollections; // Importing FXCollections for observable collections
import javafx.collections.ObservableList; // Importing ObservableList for data binding
import javafx.fxml.FXML; // Importing FXML for UI annotations
import javafx.fxml.FXMLLoader; // Importing FXMLLoader for loading FXML files
import javafx.scene.Parent; // Importing Parent as a base class for all nodes
import javafx.scene.Scene; // Importing Scene to represent the contents of a stage
import javafx.scene.control.*; // Importing JavaFX controls for UI components
import javafx.scene.control.cell.PropertyValueFactory; // Importing PropertyValueFactory for table column bindings
import javafx.scene.layout.VBox; // Importing VBox layout for arranging UI components vertically
import javafx.stage.FileChooser; // Importing FileChooser for file selection dialogs
import javafx.stage.Stage; // Importing Stage for window representation
import java.io.File; // Importing File for file handling
import java.io.IOException; // Importing IOException for handling I/O exceptions
import java.util.List; // Importing List for handling collections

/**
 * MainController class to manage the main application logic.
 */
public class MainController {
    // TextField for entering a single URL
    @FXML private TextField urlField;
    
    // TextField for entering a file path containing URLs
    @FXML private TextField urlFileField;
    
    // TextField for entering a virtual host
    @FXML private TextField vhostField;
    
    // ComboBox for selecting HTTP methods
    @FXML private ComboBox<String> methodComboBox;
    
    // TextField for specifying the log file path
    @FXML private TextField logFileField;
    
    // TextField for specifying the config file path
    @FXML private TextField configFileField;
    
    // Spinner for setting the timeout duration
    @FXML private Spinner<Integer> timeoutSpinner;
    
    // CheckBox for enabling early exit
    @FXML private CheckBox exitEarlyCheckBox;
    
    // CheckBox for enabling quiet mode
    @FXML private CheckBox quietModeCheckBox;
    
    // CheckBox for disabling color output
    @FXML private CheckBox noColorCheckBox;
    
    // TextArea for displaying output logs
    @FXML private TextArea outputArea;
    
    // Button to start the scanning process
    @FXML private Button startButton;
    
    // Button to stop the scanning process
    @FXML private Button stopButton;
    
    // TableView for displaying scan results
    @FXML private TableView<ScanResult> dataTable;
    
    // TableColumn for displaying URLs
    @FXML private TableColumn<ScanResult, String> urlColumn;
    
    // TableColumn for displaying HTTP methods
    @FXML private TableColumn<ScanResult, String> methodColumn;
    
    // TableColumn for displaying Transfer-Encoding headers
    @FXML private TableColumn<ScanResult, String> teHeaderColumn;
    
    // TableColumn for displaying payloads
    @FXML private TableColumn<ScanResult, String> payloadColumn;
    
    // RadioButton for selecting URL mode
    @FXML private RadioButton urlModeRadio;
    
    // RadioButton for selecting file mode
    @FXML private RadioButton fileModeRadio;
    
    // VBox for arranging URL input components
    @FXML private VBox urlInputBox;
    
    // VBox for arranging file input components
    @FXML private VBox fileInputBox;

    // Service for managing scan operations
    private ScanService scanService;
    
    // Service for handling file operations
    private FileOperations fileOperations;
    
    // Manager for handling table operations
    private TableManager tableManager;

    /**
     * Method to clear all rows in the data table.
     */
    @FXML
    private void clearTableRows() {
        // Clears the items in the data table
        dataTable.getItems().clear();
    }

    /**
     * Method called to initialize the controller.
     */
    @FXML
    private void initialize() {
        // Logs the initialization
        Logger.info("Initializing MainController");
        
        // Calls method to initialize UI components
        initializeComponents();
        
        // Calls method to setup event handlers
        setupEventHandlers();
    }

    /**
     * Method to initialize UI components.
     */
    private void initializeComponents() {
        // Logs the setup process
        Logger.debug("Setting up UI components");
        
        // Initialize TableManager first since we need its ObservableList
        tableManager = new TableManager(dataTable, urlColumn, methodColumn, teHeaderColumn, payloadColumn);
        
        // Initialize services with TableManager's ObservableList
        scanService = new ScanService(outputArea, tableManager.getTableData(), stopButton);
        fileOperations = new FileOperations();

        // Setup method combo box
        methodComboBox.getItems().addAll("GET", "POST", "PUT", "DELETE", "HEAD", "OPTIONS", "TRACE", "PATCH");
        methodComboBox.setValue("POST");

        // Set default values
        timeoutSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 3600, 5));
        stopButton.setDisable(true);
    }

    /**
     * Method to setup event handlers.
     */
    private void setupEventHandlers() {
        // Logs the setup process
        Logger.debug("Setting up event handlers");
        
        // Setup mode toggle handlers
        urlModeRadio.selectedProperty().addListener((obs, oldVal, newVal) -> {
            urlInputBox.setVisible(newVal);
            urlInputBox.setManaged(newVal);
            fileInputBox.setVisible(!newVal);
            fileInputBox.setManaged(!newVal);
        });
        
        fileModeRadio.selectedProperty().addListener((obs, oldVal, newVal) -> {
            fileInputBox.setVisible(newVal);
            fileInputBox.setManaged(newVal);
            urlInputBox.setVisible(!newVal);
            urlInputBox.setManaged(!newVal);
        });
    }

    /**
     * Method to handle start button click.
     */
    @FXML
    private void handleStartButtonClick() {
        // Logs the start button click
        Logger.info("Start button clicked");
        
        // Check if a scan is already in progress
        if (scanService.isScanning()) {
            // Show warning message if a scan is already in progress
            UIHelper.showWarning("A scan is already in progress");
            return;
        }

        // Check if URL mode is selected
        if (urlModeRadio.isSelected()) {
            // Get the URL from the text field
            String url = urlField.getText().trim();
            
            // Check if the URL is empty
            if (url.isEmpty()) {
                // Show error message if the URL is empty
                UIHelper.showError("Please enter a URL");
                return;
            }

            try {
                // Get the timeout value from the spinner
                int timeout = timeoutSpinner.getValue();
                
                // Start the scan
                startScan(url, timeout);
            } catch (Exception e) {
                // Show error message if the timeout value is invalid
                UIHelper.showError("Invalid timeout value. Please enter a valid number.");
            }
        } else {
            // Get the file path from the text field
            String filePath = urlFileField.getText().trim();
            
            // Check if the file path is empty
            if (filePath.isEmpty()) {
                // Show error message if the file path is empty
                UIHelper.showError("Please select a URL file");
                return;
            }

            // Create a new file object
            File file = new File(filePath);
            
            // Check if the file exists
            if (!file.exists()) {
                // Show error message if the file does not exist
                UIHelper.showError("Selected file does not exist");
                return;
            }

            // Load URLs from the file
            List<String> urls = fileOperations.loadUrlsFromFile(file);
            
            // Check if any URLs were loaded
            if (!urls.isEmpty()) {
                // Start the batch scan
                startBatchScan(urls);
            } else {
                // Show warning message if no URLs were loaded
                UIHelper.showWarning("No valid URLs found in the selected file");
            }
        }
    }

    /**
     * Method to handle stop button click.
     */
    @FXML
    private void handleStopButtonClick() {
        // Logs the stop button click
        Logger.info("Stop button clicked");
        
        // Stop the current scan process
        scanService.stopCurrentProcess();
        
        // Disable the stop button
        stopButton.setDisable(true);
    }

    /**
     * Method to start a scan for a single URL.
     * 
     * @param url The URL to scan
     * @param timeout The timeout duration
     */
    private void startScan(String url, int timeout) {
        // Logs the start of the scan
        Logger.info("Starting scan for URL: " + url);
        
        // Start the single scan
        scanService.startSingleScan(
            url,
            vhostField.getText(),
            methodComboBox.getValue(),
            logFileField.getText(),
            configFileField.getText(),
            timeout,
            exitEarlyCheckBox.isSelected(),
            quietModeCheckBox.isSelected(),
            noColorCheckBox.isSelected()
        );
    }

    /**
     * Method to choose a log file.
     */
    @FXML
    private void chooseLogFile() {
        // Logs the choose log file button click
        Logger.debug("Choose log file button clicked");
        
        // Choose a file using the file chooser
        File file = fileOperations.chooseFile("Select Log File", "Log Files", "*.log", "*.*");
        
        // Check if a file was chosen
        if (file != null) {
            // Set the log file path in the text field
            logFileField.setText(file.getAbsolutePath());
        }
    }

    /**
     * Method to choose a config file.
     */
    @FXML
    private void chooseConfigFile() {
        // Logs the choose config file button click
        Logger.debug("Choose config file button clicked");
        
        // Choose a file using the file chooser
        File file = fileOperations.chooseFile("Select Config File", "Config Files", "*.conf", "*.config", "*.json", "*.*");
        
        // Check if a file was chosen
        if (file != null) {
            // Set the config file path in the text field
            configFileField.setText(file.getAbsolutePath());
        }
    }

    /**
     * Method to choose a URL file.
     */
    @FXML
    private void chooseUrlFile() {
        // Logs the choose URL file button click
        Logger.debug("Choose URL file button clicked");
        
        // Choose a file using the file chooser
        File file = fileOperations.chooseFile("Select URL File", "Text Files", "*.txt", "*.*");
        
        // Check if a file was chosen
        if (file != null) {
            // Set the URL file path in the text field
            urlFileField.setText(file.getAbsolutePath());
        }
    }

    /**
     * Method to load URLs from a file.
     */
    @FXML
    private void handleLoadUrlsFromFile() {
        // Logs the load URLs from file button click
        Logger.debug("Load URLs from file button clicked");
        
        // Choose a file using the file chooser
        File file = fileOperations.chooseFile("Select URL File", "Text Files", "*.txt", "*.*");
        
        // Check if a file was chosen
        if (file != null) {
            // Load URLs from the file
            List<String> urls = fileOperations.loadUrlsFromFile(file);
            
            // Check if any URLs were loaded
            if (!urls.isEmpty()) {
                // Start the batch scan
                startBatchScan(urls);
            } else {
                // Show warning message if no URLs were loaded
                UIHelper.showWarning("No valid URLs found in the selected file");
            }
        }
    }

    /**
     * Method to start a batch scan for multiple URLs.
     * 
     * @param urls The list of URLs to scan
     */
    private void startBatchScan(List<String> urls) {
        // Logs the start of the batch scan
        Logger.info("Starting batch scan with " + urls.size() + " URLs");
        
        try {
            // Get the timeout value from the spinner
            int timeout = timeoutSpinner.getValue();
            
            // Start the batch scan
            scanService.startBatchScan(
                urls,
                vhostField.getText(),
                methodComboBox.getValue(),
                logFileField.getText(),
                configFileField.getText(),
                timeout,
                exitEarlyCheckBox.isSelected(),
                quietModeCheckBox.isSelected(),
                noColorCheckBox.isSelected()
            );
        } catch (Exception e) {
            // Show error message if the timeout value is invalid
            UIHelper.showError("Invalid timeout value. Please enter a valid number.");
        }
    }

    /**
     * Method to start the scan.
     */
    @FXML
    private void startScan() {
        // Logs the start scan button click
        Logger.info("Start scan button clicked");
        
        // Call the handle start button click method
        handleStartButtonClick();
    }

    /**
     * Method to stop the scan.
     */
    @FXML
    private void stopScan() {
        // Logs the stop scan button click
        Logger.info("Stop scan button clicked");
        
        // Call the handle stop button click method
        handleStopButtonClick();
    }

    /**
     * Method to clear the log.
     */
    @FXML
    private void clearLog() {
        // Logs the clear log button click
        Logger.info("Clear log button clicked");
        
        // Clear the output area
        outputArea.clear();
    }

    /**
     * Method to open the repeater window.
     */
    @FXML
    private void openRepeater() {
        // Logs the open repeater button click
        Logger.info("Opening repeater window");
        
        try {
            // Load the repeater FXML file
            FXMLLoader loader = new FXMLLoader(getClass().getResource("repeater.fxml"));
            Parent root = loader.load();
            
            // Get the repeater controller
            RepeaterController controller = loader.getController();
            
            // Set the default URL in the repeater controller
            controller.setUrl("https://example.com");
            
            // Create a new stage for the repeater window
            Stage stage = new Stage();
            stage.setTitle("HRS Repeater");
            
            // Create a new scene for the repeater window
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());
            
            // Set the scene in the stage
            stage.setScene(scene);
            
            // Show the stage
            stage.show();
        } catch (IOException e) {
            // Log the error
            Logger.error("Error opening repeater window", e);
            
            // Show an error message
            UIHelper.showError("Error opening repeater window: " + e.getMessage());
        }
    }
}
