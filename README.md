# HRS Tool

## Overview
The HRS Tool is a comprehensive application designed to facilitate network operations, manage UI components, and execute various services efficiently. This tool is structured to provide a seamless experience for users needing robust network and UI management capabilities.

## Features
- **Network Logging**: Log network activities with timestamps.
- **HTTP Client**: Send HTTP requests and handle responses.
- **UI Management**: Manage UI components like tables and text areas with line numbers.
- **Command Execution**: Build and execute command-line instructions.
- **File Operations**: Handle file selection and URL loading.
- **Scan Management**: Manage and execute scan operations.

## Project Structure
```
new-hrs-tool/
│
├── pom.xml
├── UPDATES.md
├── hrs_tool.log
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   ├── com/
│   │   │   │   ├── hrs/
│   │   │   │   │   ├── Launcher.java
│   │   │   │   │   ├── Main.java
│   │   │   │   │   ├── MainController.java
│   │   │   │   │   ├── RepeaterController.java
│   │   │   │   │   ├── model/
│   │   │   │   │   │   └── ScanResult.java
│   │   │   │   │   ├── network/
│   │   │   │   │   │   ├── NetworkLogger.java
│   │   │   │   │   │   └── RawHttpClient.java
│   │   │   │   │   ├── service/
│   │   │   │   │   │   ├── CommandBuilder.java
│   │   │   │   │   │   ├── FileOperations.java
│   │   │   │   │   │   ├── ProcessManager.java
│   │   │   │   │   │   └── ScanService.java
│   │   │   │   │   ├── ui/
│   │   │   │   │   │   ├── LineNumberedTextArea.java
│   │   │   │   │   │   ├── TableManager.java
│   │   │   │   │   │   └── UIHelper.java
│   │   │   │   │   └── utils/
│   │   │   │   │       └── Logger.java
│   │   └── resources/
│   │       ├── com/
│   │       │   └── hrs/
│   │       │       ├── main.fxml
│   │       │       ├── repeater.fxml
│   │       │       └── styles.css
└── target/
```

## File Descriptions
- **Launcher.java**: Initializes and launches the application.
- **Main.java**: Sets up the JavaFX application stage.
- **MainController.java**: Controls the main UI components and interactions.
- **RepeaterController.java**: Manages the repeater functionality within the UI.
- **ScanResult.java**: Represents the data model for scan results.
- **NetworkLogger.java**: Provides logging capabilities for network operations.
- **RawHttpClient.java**: Handles HTTP requests and responses.
- **CommandBuilder.java**: Constructs command-line arguments for execution.
- **FileOperations.java**: Manages file selection and URL loading.
- **ProcessManager.java**: Executes and manages external processes.
- **ScanService.java**: Controls scan operations and processes.
- **LineNumberedTextArea.java**: UI component for text areas with line numbers.
- **TableManager.java**: Manages table UI components and data.
- **UIHelper.java**: Provides utility methods for UI operations.
- **Logger.java**: Utility for logging messages and errors.
- **main.fxml**: Defines the main UI layout.
- **repeater.fxml**: Defines the layout for the repeater functionality.
- **styles.css**: Contains styling for the UI components.

## Usage
To use this tool, ensure you have Java installed and properly configured. You can build and run the application using Maven with the following command:
```
mvn clean install
```
This will compile the project and package it into a runnable JAR file located in the `target` directory.

## Current Time
The current local time is: 2024-12-07T13:48:39+05:30.

For further assistance, please refer to the inline comments within the codebase or contact the development team.
