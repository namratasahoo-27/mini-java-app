package com.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Modernized Database Service with cloud-native patterns
 * - Uses Spring Boot DataSource with HikariCP connection pooling
 * - Externalized configuration via environment variables
 * - SLF4J logging for structured logging
 * - Connection validation and health checks
 * - Retry logic for transient failures
 * - Proper resource management with try-with-resources
 */
@Service
public class DatabaseService {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseService.class);

    private final DataSource dataSource;

    @Value("${spring.datasource.url}")
    private String databaseUrl;

    @Value("${spring.datasource.hikari.connection-timeout:30000}")
    private int connectionTimeout;

    @Value("${external.api.base-url}")
    private String externalApiUrl;

    @Value("${payment.service.url}")
    private String paymentServiceUrl;

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    /**
     * Constructor injection for DataSource (managed by Spring Boot)
     */
    public DatabaseService(DataSource dataSource) {
        this.dataSource = dataSource;
        logger.info("DatabaseService initialized with HikariCP connection pool");
    }

    /**
     * Validate database connection for health checks
     * Used by Spring Boot Actuator health endpoint
     */
    public boolean isConnectionValid() {
        try (Connection connection = dataSource.getConnection()) {
            boolean isValid = connection.isValid(5);
            if (isValid) {
                logger.debug("Database connection validation successful");
            } else {
                logger.warn("Database connection validation failed");
            }
            return isValid;
        } catch (SQLException e) {
            logger.error("Database connection validation error: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Execute parameterized query with retry logic for transient failures
     * Prevents SQL injection by using PreparedStatement parameters
     */
    @Retryable(
        retryFor = {SQLException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void executeQuery(String sql, Object... params) throws SQLException {
        // Obtain connection from pool - automatically managed by HikariCP
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            // Set query timeout (externalized configuration)
            stmt.setQueryTimeout(connectionTimeout / 1000);

            // Bind parameters to prevent SQL injection
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }

            logger.debug("Executing query: {} with {} parameters", sql, params.length);
            stmt.execute();
            logger.info("Query executed successfully");

        } catch (SQLException e) {
            logger.error("Query execution failed: {} - SQL State: {}",
                e.getMessage(), e.getSQLState(), e);

            // Check for transient errors that should trigger retry
            if (isTransientError(e)) {
                logger.warn("Transient database error detected, retry will be attempted");
                throw e; // Trigger retry
            }

            throw new RuntimeException("Database query execution failed", e);
        }
    }

    /**
     * Execute query with single parameter (convenience method)
     */
    public void executeQuery(String sql, Object param) throws SQLException {
        executeQuery(sql, new Object[]{param});
    }

    /**
     * Execute query without parameters
     */
    public void executeQuery(String sql) throws SQLException {
        executeQuery(sql, new Object[]{});
    }

    /**
     * Determine if SQLException is transient and should trigger retry
     */
    private boolean isTransientError(SQLException e) {
        String sqlState = e.getSQLState();
        // PostgreSQL transient error codes
        return sqlState != null && (
            sqlState.startsWith("08") ||  // Connection exception
            sqlState.equals("40001") ||   // Serialization failure
            sqlState.equals("40P01")      // Deadlock detected
        );
    }

    /**
     * Initialize external services with externalized configuration
     */
    public void initializeExternalServices() {
        logger.info("Initializing external services...");
        logger.info("External API: {}", externalApiUrl);
        logger.info("Payment Service: {}", paymentServiceUrl);
        logger.info("Redis Cache: {}:{}", redisHost, redisPort);
        logger.info("External services initialized successfully");
    }

    /**
     * Get database connection for advanced operations
     * Caller is responsible for closing the connection
     */
    public Connection getConnection() throws SQLException {
        logger.debug("Obtaining database connection from pool");
        Connection connection = dataSource.getConnection();

        if (!connection.isValid(5)) {
            logger.error("Obtained invalid connection from pool");
            throw new SQLException("Invalid database connection");
        }

        return connection;
    }
}
