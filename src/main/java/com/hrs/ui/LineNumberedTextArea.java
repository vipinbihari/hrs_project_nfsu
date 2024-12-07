package com.hrs.ui; // Package for UI components

import javafx.geometry.Pos; // Import for positioning elements
import javafx.scene.Node; // Import for JavaFX Node class
import javafx.scene.control.ScrollPane; // Import for scroll pane functionality
import javafx.scene.control.TextArea; // Import for text area functionality
import javafx.scene.layout.HBox; // Import for horizontal box layout
import javafx.scene.layout.Priority; // Import for setting layout priority
import javafx.scene.layout.VBox; // Import for vertical box layout
import javafx.scene.text.Text; // Import for text nodes
import javafx.scene.text.TextFlow; // Import for flowing text layout

/**
 * Custom TextArea with line numbers.
 */
public class LineNumberedTextArea extends HBox {
    // Main text area for user input
    private final TextArea textArea;
    // VBox to hold line numbers
    private final VBox lineNumbersBox;
    // ScrollPane for line numbers
    private final ScrollPane lineNumbersScroll;

    /**
     * Constructor to initialize the LineNumberedTextArea.
     */
    public LineNumberedTextArea() {
        // Create the main components
        textArea = new TextArea(); // Initializes the text area
        lineNumbersBox = new VBox(); // Initializes the VBox for line numbers
        lineNumbersScroll = new ScrollPane(lineNumbersBox); // Initializes the scroll pane for line numbers

        // Style the line numbers
        lineNumbersBox.setStyle("-fx-background-color: #f0f0f0;"); // Sets background color for line numbers
        lineNumbersBox.setMinWidth(40); // Sets minimum width for line numbers
        lineNumbersBox.setMaxWidth(40); // Sets maximum width for line numbers
        lineNumbersBox.setAlignment(Pos.TOP_RIGHT); // Aligns line numbers to the top right
        lineNumbersBox.setSpacing(0); // Ensure no gaps between line numbers

        // Style the text area with exact line height
        textArea.setStyle("-fx-control-inner-background: #f0f0f0; -fx-text-fill: #000000; " +
                         "-fx-font-family: 'Consolas'; -fx-font-size: 12px; " +
                         "-fx-line-spacing: 0; -fx-padding: 3;"); // Sets style for text area
        
        // Style the line numbers scroll pane
        lineNumbersScroll.setStyle("-fx-background: #f0f0f0; -fx-background-color: #f0f0f0;"); // Sets background style for scroll pane
        lineNumbersScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER); // Disables horizontal scroll bar
        lineNumbersScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER); // Disables vertical scroll bar
        lineNumbersScroll.setFitToWidth(true); // Ensures the scroll pane fits to width

        // Add components to the HBox
        this.getChildren().addAll(lineNumbersScroll, textArea); // Adds line numbers scroll pane and text area to the HBox
        HBox.setHgrow(textArea, Priority.ALWAYS); // Sets the text area to grow horizontally

        // Update line numbers when text changes
        textArea.textProperty().addListener((obs, oldText, newText) -> updateLineNumbers()); // Listens for text changes and updates line numbers
        textArea.scrollTopProperty().addListener((obs, oldVal, newVal) -> 
            lineNumbersScroll.setVvalue(newVal.doubleValue() / textArea.getScrollTop())); // Listens for scroll top changes and updates line numbers scroll

        // Initial line numbers
        updateLineNumbers(); // Initializes line numbers
    }

    /**
     * Updates the line numbers based on the current text.
     */
    private void updateLineNumbers() {
        lineNumbersBox.getChildren().clear(); // Clears the line numbers box
        
        String text = textArea.getText(); // Gets the current text
        
        // Handle empty text case
        if (text.isEmpty()) {
            addLineNumber(1); // Adds a single line number for empty text
            return;
        }
        
        // Count actual lines including trailing newlines
        int lineCount = 1; // Initializes line count
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                lineCount++; // Increments line count for each newline
            }
        }
        
        // Calculate padding based on total number of lines
        int maxDigits = String.valueOf(lineCount).length(); // Calculates maximum digits for line numbers
        String format = "%" + maxDigits + "d "; // Creates format string for line numbers
        
        // Add line numbers
        for (int i = 0; i < lineCount; i++) {
            addLineNumber(i + 1, format); // Adds line numbers
        }
    }

    /**
     * Adds a line number to the line numbers box.
     * 
     * @param number The line number to add.
     */
    private void addLineNumber(int number) {
        addLineNumber(number, "%d "); // Calls the overloaded method with default format
    }

    /**
     * Adds a line number to the line numbers box with a custom format.
     * 
     * @param number The line number to add.
     * @param format The format string for the line number.
     */
    private void addLineNumber(int number, String format) {
        Text lineNumber = new Text(String.format(format, number)); // Creates a text node for the line number
        lineNumber.setStyle("-fx-fill: #808080; -fx-font-family: 'Consolas'; -fx-font-size: 12px;"); // Sets style for the line number
        
        TextFlow lineFlow = new TextFlow(lineNumber); // Creates a text flow for the line number
        lineFlow.setStyle("-fx-padding: 4 5 0 0;"); // Sets style for the text flow
        lineFlow.setPrefHeight(16.0); // Sets preferred height for the text flow
        
        lineNumbersBox.getChildren().add(lineFlow); // Adds the text flow to the line numbers box
    }

    /**
     * Sets the text of the text area.
     * 
     * @param text The text to set.
     */
    public void setText(String text) {
        // Preserve CRLF line endings by replacing them temporarily
        String preservedText = text.replace("\r\n", "\n"); // Replaces CRLF with LF
        textArea.setText(preservedText); // Sets the text of the text area
    }

    /**
     * Gets the text of the text area.
     * 
     * @return The text of the text area.
     */
    public String getText() {
        // Convert back to CRLF when getting text
        return textArea.getText().replace("\n", "\r\n"); // Replaces LF with CRLF
    }

    /**
     * Gets the text area.
     * 
     * @return The text area.
     */
    public TextArea getTextArea() {
        return textArea; // Returns the text area
    }

    /**
     * Sets whether the text area is editable.
     * 
     * @param editable Whether the text area is editable.
     */
    public void setEditable(boolean editable) {
        textArea.setEditable(editable); // Sets whether the text area is editable
    }
}
