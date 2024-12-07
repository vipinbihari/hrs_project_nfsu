package com.hrs.service; // Package for service classes

import java.util.ArrayList; // Importing ArrayList for dynamic array implementation
import java.util.List; // Importing List interface for list operations

/**
 * CommandBuilder is responsible for constructing the command-line arguments
 * for executing the smuggler.py script.
 */
public class CommandBuilder {
    // List to hold the command and its arguments
    private final List<String> command = new ArrayList<>(); // Initializes an empty list to store the command and its arguments

    /**
     * Initializes the command with the base Python command and script path.
     */
    public CommandBuilder() {
        command.add("python"); // Adds the Python executable to the command
        //let it be test.py for now
        command.add("./smuggler/smuggler.py"); // Adds the script path to the command
    }

    /**
     * Adds the URL argument to the command.
     * @param url The target URL.
     * @return The current CommandBuilder instance.
     */
    public CommandBuilder withUrl(String url) {
        if (!url.isEmpty()) { // Checks if the URL is not empty
            command.add("-u"); // Adds the URL flag to the command
            command.add(url); // Adds the URL to the command
        }
        return this; // Returns the current instance for method chaining
    }

    /**
     * Adds the virtual host argument to the command.
     * @param vhost The virtual host.
     * @return The current CommandBuilder instance.
     */
    public CommandBuilder withVhost(String vhost) {
        if (!vhost.isEmpty()) { // Checks if the virtual host is not empty
            command.add("-v"); // Adds the virtual host flag to the command
            command.add(vhost); // Adds the virtual host to the command
        }
        return this; // Returns the current instance for method chaining
    }

    /**
     * Adds the HTTP method argument to the command.
     * @param method The HTTP method to use.
     * @return The current CommandBuilder instance.
     */
    public CommandBuilder withMethod(String method) {
        command.add("-m"); // Adds the HTTP method flag to the command
        command.add(method); // Adds the HTTP method to the command
        return this; // Returns the current instance for method chaining
    }

    /**
     * Adds the log file argument to the command.
     * @param logFile The log file path.
     * @return The current CommandBuilder instance.
     */
    public CommandBuilder withLogFile(String logFile) {
        if (!logFile.isEmpty()) { // Checks if the log file path is not empty
            command.add("-l"); // Adds the log file flag to the command
            command.add(logFile); // Adds the log file path to the command
        }
        return this; // Returns the current instance for method chaining
    }

    /**
     * Adds the timeout argument to the command.
     * @param timeout The socket timeout value.
     * @return The current CommandBuilder instance.
     */
    public CommandBuilder withTimeout(int timeout) {
        command.add("-t"); // Adds the timeout flag to the command
        command.add(String.valueOf(timeout)); // Adds the timeout value to the command
        return this; // Returns the current instance for method chaining
    }

    /**
     * Adds the configuration file argument to the command.
     * @param configFile The configuration file path.
     * @return The current CommandBuilder instance.
     */
    public CommandBuilder withConfigFile(String configFile) {
        if (!configFile.isEmpty()) { // Checks if the configuration file path is not empty
            command.add("-c"); // Adds the configuration file flag to the command
            command.add(configFile); // Adds the configuration file path to the command
        }
        return this; // Returns the current instance for method chaining
    }

    /**
     * Adds the exit early flag to the command.
     * @param exitEarly Whether to exit early on the first finding.
     * @return The current CommandBuilder instance.
     */
    public CommandBuilder withExitEarly(boolean exitEarly) {
        if (exitEarly) { // Checks if exit early is enabled
            command.add("-x"); // Adds the exit early flag to the command
        }
        return this; // Returns the current instance for method chaining
    }

    /**
     * Adds the quiet mode flag to the command.
     * @param quietMode Whether to enable quiet mode.
     * @return The current CommandBuilder instance.
     */
    public CommandBuilder withQuietMode(boolean quietMode) {
        if (quietMode) { // Checks if quiet mode is enabled
            command.add("-q"); // Adds the quiet mode flag to the command
        }
        return this; // Returns the current instance for method chaining
    }

    /**
     * Adds the no color flag to the command.
     * @param noColor Whether to suppress color codes.
     * @return The current CommandBuilder instance.
     */
    public CommandBuilder withNoColor(boolean noColor) {
        if (noColor) { // Checks if no color is enabled
            command.add("--no-color"); // Adds the no color flag to the command
        }
        return this; // Returns the current instance for method chaining
    }

    /**
     * Builds and returns the complete command as a list of strings.
     * @return The command as a list of strings.
     */
    public List<String> build() {
        return new ArrayList<>(command); // Returns a copy of the command list
    }
}
