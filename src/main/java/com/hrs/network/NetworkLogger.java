package com.hrs.network; // Package for network-related classes

import java.time.LocalDateTime; // Importing LocalDateTime for handling date and time
import java.time.format.DateTimeFormatter; // Importing DateTimeFormatter for formatting date and time

/**
 * NetworkLogger provides logging functionalities for network operations.
 */
public class NetworkLogger {
    // Formatter for timestamps in log messages
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    /**
     * Logs a message with a timestamp.
     * @param message The message to log
     */
    public static void log(String message) {
        String timestamp = LocalDateTime.now().format(formatter); // Formats the current time
        String logMessage = String.format("[%s] %s", timestamp, message); // Creates the log message
        System.out.println(logMessage); // Prints the log message to standard output
    }

    /**
     * Logs an error message with a timestamp and exception details.
     * @param message The error message to log
     * @param e The exception to log
     */
    public static void error(String message, Throwable e) {
        String timestamp = LocalDateTime.now().format(formatter); // Formats the current time
        String logMessage = String.format("[%s] ERROR: %s - %s", timestamp, message, e.getMessage()); // Creates the log message
        System.err.println(logMessage); // Prints the log message to standard error
        e.printStackTrace(); // Prints the stack trace of the exception
    }
}
