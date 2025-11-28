package com.test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Database service - Cloud-native compatible with connection pooling
 */
public class DatabaseService {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    // Use environment variables for database configuration
    private static final String DB_HOST = System.getenv().getOrDefault("DB_HOST", "localhost");
    private static final String DB_PORT = System.getenv().getOrDefault("DB_PORT", "3306");
    private static final String DB_NAME = System.getenv().getOrDefault("DB_NAME", "mini_app_db");
    private static final String DB_URL = "jdbc:mysql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME;
    private static final String DB_USERNAME = System.getenv().getOrDefault("DB_USERNAME", "app_user");
    private static final String DB_PASSWORD = System.getenv().getOrDefault("DB_PASSWORD", "defaultpassword");

    // Use environment variables for cache configuration
    private static final String REDIS_HOST = System.getenv().getOrDefault("REDIS_HOST", "redis");
    private static final int REDIS_PORT = Integer.parseInt(System.getenv().getOrDefault("REDIS_PORT", "6379"));

    // Use environment variables for external service URLs
    private static final String EXTERNAL_API_URL = System.getenv().getOrDefault("EXTERNAL_API_URL", "https://api.example.com/v1");
    private static final String PAYMENT_SERVICE_URL = System.getenv().getOrDefault("PAYMENT_SERVICE_URL", "https://payment.example.com/process");

    private HikariDataSource dataSource;
    
    public void connect() {
        try {
            logStructuredMessage("info", "Initializing database connection pool...");

            // Initialize HikariCP connection pool
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(DB_URL);
            config.setUsername(DB_USERNAME);
            config.setPassword(DB_PASSWORD);
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");

            // Cloud-native connection pool settings
            config.setMaximumPoolSize(Integer.parseInt(System.getenv().getOrDefault("DB_POOL_MAX_SIZE", "10")));
            config.setMinimumIdle(Integer.parseInt(System.getenv().getOrDefault("DB_POOL_MIN_IDLE", "5")));
            config.setConnectionTimeout(Long.parseLong(System.getenv().getOrDefault("DB_CONNECTION_TIMEOUT", "30000")));
            config.setIdleTimeout(Long.parseLong(System.getenv().getOrDefault("DB_IDLE_TIMEOUT", "600000")));
            config.setMaxLifetime(Long.parseLong(System.getenv().getOrDefault("DB_MAX_LIFETIME", "1800000")));

            dataSource = new HikariDataSource(config);

            logStructuredMessage("info", "Database connection pool initialized successfully");
            logStructuredMessage("info", "Database URL: " + DB_URL);

            // Initialize cache connection with environment variables
            connectToCache();

            // Initialize external services with environment variables
            initializeExternalServices();

        } catch (Exception e) {
            logStructuredMessage("error", "Database connection failed: " + e.getMessage());
        }
    }
    
    private void connectToCache() {
        // Use environment variables for cache connection
        logStructuredMessage("info", "Connecting to Redis cache at: " + REDIS_HOST + ":" + REDIS_PORT);
        // Simulate cache connection with environment variables
    }

    private void initializeExternalServices() {
        // Use environment variables for external service URLs
        logStructuredMessage("info", "Initializing external API: " + EXTERNAL_API_URL);
        logStructuredMessage("info", "Initializing payment service: " + PAYMENT_SERVICE_URL);
    }
    
    public void executeQuery(String sql) {
        try (Connection connection = dataSource.getConnection()) {
            if (connection != null) {
                PreparedStatement stmt = connection.prepareStatement(sql);
                // Use environment variable for query timeout
                int queryTimeout = Integer.parseInt(System.getenv().getOrDefault("DB_QUERY_TIMEOUT", "30"));
                stmt.setQueryTimeout(queryTimeout);

                logStructuredMessage("info", "Executing query: " + sql);
                stmt.execute();
                stmt.close();
            }
        } catch (SQLException e) {
            logStructuredMessage("error", "Query execution failed: " + e.getMessage());
        }
    }

    public void disconnect() {
        try {
            if (dataSource != null && !dataSource.isClosed()) {
                dataSource.close();
                logStructuredMessage("info", "Database connection pool closed");
            }
        } catch (SQLException e) {
            logStructuredMessage("error", "Failed to close database connection pool: " + e.getMessage());
        }
    }

    // Structured logging method for cloud-native JSON output
    private static void logStructuredMessage(String level, String message) {
        try {
            ObjectNode logEntry = objectMapper.createObjectNode();
            logEntry.put("timestamp", java.time.Instant.now().toString());
            logEntry.put("level", level.toUpperCase());
            logEntry.put("message", message);
            logEntry.put("service", "database-service");
            logEntry.put("version", "1.0.0");

            System.out.println(objectMapper.writeValueAsString(logEntry));
        } catch (Exception e) {
            // Fallback to simple logging if JSON formatting fails
            System.out.println(String.format("[%s] %s: %s", java.time.Instant.now(), level.toUpperCase(), message));
        }
    }
}