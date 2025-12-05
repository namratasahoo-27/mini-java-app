package com.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.retry.annotation.EnableRetry;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Cloud-native Spring Boot Application
 * - Externalized configuration via environment variables
 * - Structured logging with SLF4J
 * - Spring Boot managed lifecycle
 * - Health checks and metrics enabled
 * - Retry logic for resilience
 */
@SpringBootApplication
@EnableRetry
public class MiniApp {

    private static final Logger logger = LoggerFactory.getLogger(MiniApp.class);

    @Value("${app.config.directory}")
    private String configDirectory;

    @Value("${app.log.directory}")
    private String logDirectory;

    @Value("${app.temp.directory}")
    private String tempDirectory;

    @Value("${app.upload.directory}")
    private String uploadDirectory;

    public static void main(String[] args) {
        if (args == null) {
            throw new IllegalArgumentException("Arguments cannot be null");
        }
        logger.info("Starting Cloud-Native Mini Java Application...");
        SpringApplication.run(MiniApp.class, args);
    }

    /**
     * Application initialization logic - runs after Spring context is loaded
     */
    @Bean
    public CommandLineRunner init(DatabaseService databaseService) {
        return args -> {
            logger.info("Initializing application...");

            // Create required directories with externalized paths
            initializeDirectories();

            // Validate database connection
            if (databaseService.isConnectionValid()) {
                logger.info("Database connection validated successfully");
            } else {
                logger.error("Database connection validation failed");
            }

            // Initialize external services
            databaseService.initializeExternalServices();

            logger.info("Application initialization completed successfully");
        };
    }

    /**
     * Initialize application directories using externalized configuration
     */
    private void initializeDirectories() {
        try {
            createDirectoryIfNotExists(configDirectory, "Configuration");
            createDirectoryIfNotExists(logDirectory, "Log");
            createDirectoryIfNotExists(tempDirectory, "Temporary");
            createDirectoryIfNotExists(uploadDirectory, "Upload");

            logger.info("All application directories initialized successfully");
        } catch (IOException e) {
            logger.error("Failed to initialize application directories: {}", e.getMessage(), e);
            throw new RuntimeException("Directory initialization failed", e);
        }
    }

    /**
     * Create directory if it doesn't exist
     */
    private void createDirectoryIfNotExists(String directoryPath, String directoryType) throws IOException {
        Path path = Paths.get(directoryPath);

        if (!Files.exists(path)) {
            Files.createDirectories(path);
            logger.info("{} directory created at: {}", directoryType, directoryPath);
        } else {
            logger.debug("{} directory already exists at: {}", directoryType, directoryPath);
        }
    }
}
