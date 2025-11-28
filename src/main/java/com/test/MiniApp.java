package com.test;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.util.Properties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mini Java Application - Cloud-native compatible
 */
public class MiniApp {

    private static final Logger logger = LoggerFactory.getLogger(MiniApp.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // Use environment variables with fallback defaults
    private static final int SERVER_PORT = Integer.parseInt(System.getenv().getOrDefault("SERVER_PORT", "8080"));

    // Use classpath resources instead of hardcoded file paths
    private static final String CONFIG_FILE_PATH = System.getenv().getOrDefault("CONFIG_FILE_PATH", "application.properties");
    
    public static void main(String[] args) {
        logStructuredMessage("info", "Starting Mini Java Application...");

        MiniApp app = new MiniApp();
        app.initializeApplication();
        app.startServer();
    }

    private void initializeApplication() {
        // Load configuration from classpath resources
        loadConfiguration();

        // Initialize structured logging
        initializeLogging();

        // Initialize database connection with environment variables
        DatabaseService dbService = new DatabaseService();
        dbService.connect();
    }
    
    private void loadConfiguration() {
        try {
            // Load configuration from classpath resources
            InputStream configStream = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE_PATH);
            if (configStream != null) {
                Properties props = new Properties();
                props.load(configStream);
                logStructuredMessage("info", "Configuration loaded from classpath: " + CONFIG_FILE_PATH);
                configStream.close();
            } else {
                logStructuredMessage("warn", "Configuration file not found in classpath: " + CONFIG_FILE_PATH);
            }
        } catch (IOException e) {
            logStructuredMessage("error", "Failed to load configuration: " + e.getMessage());
        }
    }
    
    private void initializeLogging() {
        // Cloud-native logging: use console output with structured format
        // Logs will be captured by container runtime and sent to cloud logging service
        logStructuredMessage("info", "Structured logging initialized for cloud environment");
    }
    
    private void startServer() {
        try {
            // Use environment variable for port configuration
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            logStructuredMessage("info", "Server started on port: " + SERVER_PORT);
            logStructuredMessage("info", "Server ready to accept connections...");

            // Simulate server running
            Thread.sleep(1000);
            serverSocket.close();

        } catch (Exception e) {
            logStructuredMessage("error", "Failed to start server: " + e.getMessage());
        }
    }

    // Structured logging method for cloud-native JSON output
    private static void logStructuredMessage(String level, String message) {
        try {
            ObjectNode logEntry = objectMapper.createObjectNode();
            logEntry.put("timestamp", java.time.Instant.now().toString());
            logEntry.put("level", level.toUpperCase());
            logEntry.put("message", message);
            logEntry.put("service", "mini-app");
            logEntry.put("version", "1.0.0");

            System.out.println(objectMapper.writeValueAsString(logEntry));
        } catch (Exception e) {
            // Fallback to simple logging if JSON formatting fails
            System.out.println(String.format("[%s] %s: %s", java.time.Instant.now(), level.toUpperCase(), message));
        }
    }
}