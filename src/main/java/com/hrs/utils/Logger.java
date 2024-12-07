package com.hrs.utils; // Package for utility classes

import java.io.File; // Importing File class for file operations
import java.io.FileWriter; // Importing FileWriter for writing to files
import java.io.PrintWriter; // Importing PrintWriter for formatted output
import java.text.SimpleDateFormat; // Importing SimpleDateFormat for date formatting
import java.util.Date; // Importing Date class for handling dates

/**
 * Logger class for logging messages to console and file.
 */
public class Logger { 
    // Date format for log timestamps
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS"); 
    // Log file name
    private static final String LOG_FILE = "hrs_tool.log"; 
    // PrintWriter for writing log messages
    private static PrintWriter logWriter; 

    // Static block to initialize the log writer
    static { 
        try {
            // Initialize PrintWriter with log file
            logWriter = new PrintWriter(new FileWriter(LOG_FILE, true), true); 
        } catch (Exception e) {
            // Error message if initialization fails
            System.err.println("Failed to initialize logger: " + e.getMessage()); 
            // Print stack trace for debugging
            e.printStackTrace(); 
        }
    }

    /**
     * Method to log info messages.
     * @param message the message to be logged
     */
    public static void info(String message) { 
        // Calls log method with INFO level
        log("INFO", message, null); 
    }

    /**
     * Method to log error messages.
     * @param message the message to be logged
     * @param error the error to be logged
     */
    public static void error(String message, Throwable error) { 
        // Calls log method with ERROR level
        log("ERROR", message, error); 
    }

    /**
     * Method to log debug messages.
     * @param message the message to be logged
     */
    public static void debug(String message) { 
        // Calls log method with DEBUG level
        log("DEBUG", message, null); 
    }

    /**
     * Method to log warning messages.
     * @param message the message to be logged
     */
    public static void warn(String message) { 
        // Calls log method with WARN level
        log("WARN", message, null); 
    }

    /**
     * Private method to handle logging.
     * @param level the log level
     * @param message the message to be logged
     * @param error the error to be logged
     */
    private static void log(String level, String message, Throwable error) { 
        // Formats current date for timestamp
        String timestamp = dateFormat.format(new Date()); 
        // Gets the current thread name
        String threadName = Thread.currentThread().getName(); 
        // Formats the log message
        String logMessage = String.format("[%s] [%s] [%s] %s", timestamp, threadName, level, message); 
        
        // Print to console
        if (level.equals("ERROR")) { // Checks if the log level is ERROR
            // Prints error message to standard error
            System.err.println(logMessage); 
            if (error != null) {
                // Prints stack trace if error is not null
                error.printStackTrace(System.err); 
            }
        } else {
            // Prints log message to standard output
            System.out.println(logMessage); 
            if (error != null) {
                // Prints stack trace to standard output
                error.printStackTrace(System.out); 
            }
        }

        // Write to file
        try {
            if (logWriter != null) { // Checks if logWriter is initialized
                // Writes log message to file
                logWriter.println(logMessage); 
                if (error != null) {
                    // Writes stack trace to log file
                    error.printStackTrace(logWriter); 
                }
                // Flushes the writer to ensure data is written
                logWriter.flush(); 
            }
        } catch (Exception e) {
            // Error message if writing fails
            System.err.println("Failed to write to log file: " + e.getMessage()); 
            // Prints stack trace for debugging
            e.printStackTrace(); 
        }
    }

    /**
     * Method to close the log writer.
     */
    public static void close() { 
        if (logWriter != null) { // Checks if logWriter is initialized
            // Closes the log writer
            logWriter.close(); 
        }
    }
}
