package com.hrs.ui; // Package for UI components

import com.hrs.RepeaterController; // Importing RepeaterController for handling HTTP requests
import com.hrs.model.ScanResult; // Importing ScanResult model for table data
import com.hrs.utils.Logger; // Importing Logger for logging messages
import javafx.application.Platform; // Importing Platform for JavaFX threading
import javafx.collections.FXCollections; // Importing FXCollections for observable collections
import javafx.collections.ObservableList; // Importing ObservableList for data binding
import javafx.fxml.FXMLLoader; // Importing FXMLLoader for loading FXML files
import javafx.scene.Scene; // Importing Scene for creating scenes
import javafx.scene.control.TableCell; // Importing TableCell for table cell customization
import javafx.scene.control.TableColumn; // Importing TableColumn for table columns
import javafx.scene.control.TableRow; // Importing TableRow for table rows
import javafx.scene.control.TableView; // Importing TableView for displaying data
import javafx.scene.control.cell.PropertyValueFactory; // Importing PropertyValueFactory for table column bindings
import javafx.scene.control.Tooltip; // Importing Tooltip for displaying tooltips
import javafx.stage.Stage; // Importing Stage for window representation

/**
 * Manages the table view for displaying scan results.
 */
public class TableManager {
    // TableView for displaying scan results
    private final TableView<ScanResult> dataTable;
    // TableColumn for displaying URLs
    private final TableColumn<ScanResult, String> urlColumn;
    // TableColumn for displaying HTTP methods
    private final TableColumn<ScanResult, String> methodColumn;
    // TableColumn for displaying Transfer-Encoding headers
    private final TableColumn<ScanResult, String> teHeaderColumn;
    // TableColumn for displaying payloads
    private final TableColumn<ScanResult, String> payloadColumn;
    // ObservableList for holding table data
    private final ObservableList<ScanResult> tableData;

    /**
     * Constructor to initialize the TableManager.
     * @param dataTable The TableView to manage
     * @param urlColumn The URL column
     * @param methodColumn The method column
     * @param teHeaderColumn The Transfer-Encoding header column
     * @param payloadColumn The payload column
     */
    public TableManager(TableView<ScanResult> dataTable,
                       TableColumn<ScanResult, String> urlColumn,
                       TableColumn<ScanResult, String> methodColumn,
                       TableColumn<ScanResult, String> teHeaderColumn,
                       TableColumn<ScanResult, String> payloadColumn) {
        Logger.info("Initializing TableManager"); // Logs the initialization
        this.dataTable = dataTable; // Assigns the TableView
        this.urlColumn = urlColumn; // Assigns the URL column
        this.methodColumn = methodColumn; // Assigns the method column
        this.teHeaderColumn = teHeaderColumn; // Assigns the Transfer-Encoding header column
        this.payloadColumn = payloadColumn; // Assigns the payload column
        this.tableData = FXCollections.observableArrayList(); // Initializes the observable list for table data
        initializeTable(); // Calls method to initialize the table
    }

    /**
     * Initializes the table by setting up columns, data, and event handlers.
     */
    private void initializeTable() {
        Logger.debug("Setting up table columns and data"); // Logs the initialization process
        // Sets the cell value factory for the URL column to display the URL property of ScanResult
        urlColumn.setCellValueFactory(new PropertyValueFactory<>("url"));
        // Sets the cell value factory for the method column to display the method property of ScanResult
        methodColumn.setCellValueFactory(new PropertyValueFactory<>("method"));
        // Sets the cell value factory for the Transfer-Encoding header column to display the teHeader property of ScanResult
        teHeaderColumn.setCellValueFactory(new PropertyValueFactory<>("teHeader"));
        // Sets the cell value factory for the payload column to display the payload property of ScanResult
        payloadColumn.setCellValueFactory(new PropertyValueFactory<>("payload"));

        // Configures the payload column to show truncated text
        payloadColumn.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null); // Clears the text if the item is empty or null
                } else {
                    // Gets the first line of the payload and truncates it if it's too long
                    String firstLine = item.split("\n")[0];
                    setText(firstLine.length() > 50 ? firstLine.substring(0, 47) + "..." : firstLine);
                    // Sets a tooltip to display the full payload on hover
                    setTooltip(new Tooltip(item));
                }
            }
        });

        // Sets the preferred widths for each column
        urlColumn.setPrefWidth(200); // Sets the preferred width for the URL column
        methodColumn.setPrefWidth(80); // Sets the preferred width for the method column
        teHeaderColumn.setPrefWidth(150); // Sets the preferred width for the Transfer-Encoding header column
        payloadColumn.setPrefWidth(300); // Sets the preferred width for the payload column

        // Sets the table items to the observable list of ScanResult objects
        dataTable.setItems(tableData);

        // Adds a dummy row to the table for demonstration purposes
        tableData.add(new ScanResult(
            "https://example.com", // URL
            "POST", // HTTP method
            "Transfer-Encoding: chunked", // Transfer-Encoding header
            "POST / HTTP/1.1\nHost: example.com\nContent-Length: 4\nTransfer-Encoding: chunked\n\n1\nZ\n0\n\n" // Payload
        ));

        // Sets up a row factory to handle double-click events on table rows
        dataTable.setRowFactory(tv -> {
            TableRow<ScanResult> row = new TableRow<>();
            // Sets an event handler to open the repeater when a row is double-clicked
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    openRepeater(row.getItem()); // Opens the repeater for the selected ScanResult
                }
            });
            return row;
        });
        
        Logger.debug("Table initialization complete"); // Logs the completion of table initialization
    }
    
    /**
     * Returns the observable list of ScanResult objects.
     * @return The observable list of ScanResult objects
     */
    public ObservableList<ScanResult> getTableData() {
        return tableData; // Returns the observable list of ScanResult objects
    }

    /**
     * Clears the table data.
     */
    public void clearTable() {
        // Clears the table data on the JavaFX application thread
        Platform.runLater(() -> {
            tableData.clear(); // Clears the observable list of ScanResult objects
            Logger.debug("Table data cleared"); // Logs the clearing of table data
        });
    }

    /**
     * Adds a ScanResult object to the table data.
     * @param result The ScanResult object to add
     */
    public void addResult(ScanResult result) {
        Logger.debug("Adding result to table: " + result.getUrl()); // Logs the addition of a ScanResult object
        tableData.add(result); // Adds the ScanResult object to the observable list
    }

    /**
     * Opens the repeater for a given ScanResult object.
     * @param scanResult The ScanResult object to open the repeater for
     */
    private void openRepeater(ScanResult scanResult) {
        Logger.info("Opening repeater for URL: " + scanResult.getUrl()); // Logs the opening of the repeater
        try {
            // Loads the repeater FXML file
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/hrs/repeater.fxml"));
            // Creates a new scene for the repeater
            Scene scene = new Scene(loader.load());

            // Gets the RepeaterController instance from the loader
            RepeaterController repeaterController = loader.getController();
            // Sets the URL for the repeater
            repeaterController.setUrl(scanResult.getUrl());
            
            // Waits for the next frame to ensure the request is initialized
            Platform.runLater(() -> {
                // Sets the raw request directly without default headers since we have a complete request
                repeaterController.getRawRequestArea().setText(scanResult.getPayload());
            });

            // Creates a new stage for the repeater
            Stage repeaterStage = new Stage();
            // Sets the title for the repeater stage
            repeaterStage.setTitle("HRS Repeater - " + scanResult.getUrl());
            // Sets the scene for the repeater stage
            repeaterStage.setScene(scene);
            // Shows the repeater stage
            repeaterStage.show();

            Logger.info("Repeater window opened successfully"); // Logs the successful opening of the repeater
        } catch (Exception e) {
            Logger.error("Error opening repeater", e); // Logs the error opening the repeater
            UIHelper.showError("Error opening repeater: " + e.getMessage()); // Shows an error message
        }
    }
}
