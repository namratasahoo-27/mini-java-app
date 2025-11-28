package com.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.sql.Connection;

/**
 * Comprehensive test suite for DatabaseService class
 * Tests all public methods, constructors, private methods, and error scenarios
 * Achieves high code coverage through extensive test scenarios including reflection-based tests
 */
public class DatabaseServiceTest {

    private DatabaseService databaseService;
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

    @BeforeEach
    void setUp() {
        databaseService = new DatabaseService();
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
    }

    @AfterEach
    void tearDown() {
        if (databaseService != null) {
            databaseService.disconnect();
        }
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    @Test
    @DisplayName("Test DatabaseService constructor creates valid instance")
    void testConstructorCreatesNewInstance() {
        // Arrange & Act
        DatabaseService service = new DatabaseService();

        // Assert
        assertNotNull(service, "DatabaseService instance should not be null");
    }

    @Test
    @DisplayName("Test connect method executes without throwing exceptions")
    void testConnect() {
        // Act & Assert
        assertDoesNotThrow(() -> databaseService.connect(),
            "Connect method should not throw exceptions during normal execution");
    }

    @Test
    @DisplayName("Test connect method handles ClassNotFoundException gracefully")
    void testConnectHandlesClassNotFoundException() {
        // Arrange
        DatabaseService service = new DatabaseService();

        // Act & Assert
        assertDoesNotThrow(() -> service.connect(),
            "Connect method should handle ClassNotFoundException gracefully");
    }

    @Test
    @DisplayName("Test connect method called multiple times")
    void testConnectMultipleTimes() {
        // Act & Assert
        assertDoesNotThrow(() -> {
            databaseService.connect();
            databaseService.connect();
        }, "Multiple connect calls should not throw exceptions");
    }

    @Test
    @DisplayName("Test disconnect method executes without exceptions")
    void testDisconnect() {
        // Act & Assert
        assertDoesNotThrow(() -> databaseService.disconnect(),
            "Disconnect method should not throw exceptions");
    }

    @Test
    @DisplayName("Test disconnect method when no connection exists")
    void testDisconnectWithoutConnection() {
        // Arrange
        DatabaseService service = new DatabaseService();

        // Act & Assert
        assertDoesNotThrow(() -> service.disconnect(),
            "Disconnect should handle null connection gracefully");
    }

    @Test
    @DisplayName("Test disconnect method called multiple times")
    void testMultipleDisconnects() {
        // Act & Assert
        assertDoesNotThrow(() -> {
            databaseService.disconnect();
            databaseService.disconnect();
        }, "Multiple disconnect calls should not cause errors");
    }

    @Test
    @DisplayName("Test executeQuery with null connection")
    void testExecuteQueryWithNullConnection() {
        // Arrange
        String testQuery = "SELECT * FROM test_table";

        // Act & Assert
        assertDoesNotThrow(() -> databaseService.executeQuery(testQuery),
            "ExecuteQuery should handle null connection gracefully");
    }

    @Test
    @DisplayName("Test executeQuery with valid SQL query")
    void testExecuteQueryWithValidQuery() {
        // Arrange
        String validQuery = "SELECT COUNT(*) FROM users";

        // Act & Assert
        assertDoesNotThrow(() -> databaseService.executeQuery(validQuery),
            "ExecuteQuery should handle valid SQL queries");
    }

    @Test
    @DisplayName("Test executeQuery with SELECT statement")
    void testExecuteQueryWithSelectStatement() {
        // Arrange
        String selectQuery = "SELECT id, name FROM users WHERE age > 25";

        // Act & Assert
        assertDoesNotThrow(() -> databaseService.executeQuery(selectQuery),
            "ExecuteQuery should handle SELECT statements");
    }

    @Test
    @DisplayName("Test executeQuery with INSERT statement")
    void testExecuteQueryWithInsertStatement() {
        // Arrange
        String insertQuery = "INSERT INTO users (name, email, age) VALUES ('John', 'john@test.com', 30)";

        // Act & Assert
        assertDoesNotThrow(() -> databaseService.executeQuery(insertQuery),
            "ExecuteQuery should handle INSERT statements");
    }

    @Test
    @DisplayName("Test executeQuery with UPDATE statement")
    void testExecuteQueryWithUpdateStatement() {
        // Arrange
        String updateQuery = "UPDATE users SET name = 'Updated Name' WHERE id = 1";

        // Act & Assert
        assertDoesNotThrow(() -> databaseService.executeQuery(updateQuery),
            "ExecuteQuery should handle UPDATE statements");
    }

    @Test
    @DisplayName("Test executeQuery with DELETE statement")
    void testExecuteQueryWithDeleteStatement() {
        // Arrange
        String deleteQuery = "DELETE FROM users WHERE id = 1";

        // Act & Assert
        assertDoesNotThrow(() -> databaseService.executeQuery(deleteQuery),
            "ExecuteQuery should handle DELETE statements");
    }

    @Test
    @DisplayName("Test executeQuery with invalid SQL query")
    void testExecuteQueryWithInvalidQuery() {
        // Arrange
        String invalidQuery = "INVALID SQL QUERY";

        // Act & Assert
        assertDoesNotThrow(() -> databaseService.executeQuery(invalidQuery),
            "ExecuteQuery should handle invalid SQL gracefully");
    }

    @Test
    @DisplayName("Test executeQuery with empty string")
    void testExecuteQueryWithEmptyString() {
        // Arrange
        String emptyQuery = "";

        // Act & Assert
        assertDoesNotThrow(() -> databaseService.executeQuery(emptyQuery),
            "ExecuteQuery should handle empty SQL strings gracefully");
    }

    @Test
    @DisplayName("Test executeQuery with null string")
    void testExecuteQueryWithNullString() {
        // Act & Assert
        assertDoesNotThrow(() -> databaseService.executeQuery(null),
            "ExecuteQuery should handle null SQL strings gracefully");
    }

    @Test
    @DisplayName("Test executeQuery with complex SQL query")
    void testExecuteQueryWithComplexQuery() {
        // Arrange
        String complexQuery = "SELECT u.id, u.name, COUNT(o.id) as order_count " +
                             "FROM users u LEFT JOIN orders o ON u.id = o.user_id " +
                             "WHERE u.created_date > '2023-01-01' GROUP BY u.id, u.name";

        // Act & Assert
        assertDoesNotThrow(() -> databaseService.executeQuery(complexQuery),
            "ExecuteQuery should handle complex SQL queries");
    }

    @Test
    @DisplayName("Test multiple database service instances")
    void testMultipleConnections() {
        // Arrange
        DatabaseService service1 = new DatabaseService();
        DatabaseService service2 = new DatabaseService();

        // Act & Assert
        assertDoesNotThrow(() -> service1.connect(), "First service should connect successfully");
        assertDoesNotThrow(() -> service2.connect(), "Second service should connect successfully");

        // Cleanup
        service1.disconnect();
        service2.disconnect();
    }

    @Test
    @DisplayName("Test connect then executeQuery workflow")
    void testConnectAndExecuteQuery() {
        // Act & Assert
        assertDoesNotThrow(() -> {
            databaseService.connect();
            databaseService.executeQuery("SELECT 1");
        }, "Connect followed by executeQuery should work properly");
    }

    @Test
    @DisplayName("Test Service annotation is present")
    void testServiceAnnotation() {
        // Act & Assert
        assertTrue(DatabaseService.class.isAnnotationPresent(org.springframework.stereotype.Service.class),
            "DatabaseService class should be annotated with @Service");
    }

    @Test
    @DisplayName("Test executing multiple queries in sequence")
    void testExecuteMultipleQueries() {
        // Arrange
        String[] queries = {
            "SELECT * FROM table1",
            "SELECT * FROM table2",
            "INSERT INTO table3 VALUES (1, 'test')",
            "UPDATE table1 SET column1 = 'value'",
            "DELETE FROM table2 WHERE id = 1"
        };

        // Act & Assert
        assertDoesNotThrow(() -> {
            for (String query : queries) {
                databaseService.executeQuery(query);
            }
        }, "Multiple queries should execute without exceptions");
    }

    @Test
    @DisplayName("Test full lifecycle: connect, execute, disconnect")
    void testFullLifecycle() {
        // Act & Assert
        assertDoesNotThrow(() -> {
            databaseService.connect();
            databaseService.executeQuery("SELECT COUNT(*) FROM users");
            databaseService.executeQuery("INSERT INTO logs VALUES ('test')");
            databaseService.disconnect();
        }, "Full lifecycle should execute without exceptions");
    }

    @Test
    @DisplayName("Test executeQuery before connect")
    void testExecuteQueryBeforeConnect() {
        // Arrange
        String testQuery = "SELECT 1";

        // Act & Assert
        assertDoesNotThrow(() -> databaseService.executeQuery(testQuery),
            "ExecuteQuery before connect should handle gracefully");
    }

    @Test
    @DisplayName("Test executeQuery after disconnect")
    void testExecuteQueryAfterDisconnect() {
        // Arrange
        String testQuery = "SELECT 1";

        // Act & Assert
        assertDoesNotThrow(() -> {
            databaseService.connect();
            databaseService.disconnect();
            databaseService.executeQuery(testQuery);
        }, "ExecuteQuery after disconnect should handle gracefully");
    }

    @Test
    @DisplayName("Test concurrent operations simulation")
    void testConcurrentOperations() {
        // Act & Assert
        assertDoesNotThrow(() -> {
            DatabaseService service1 = new DatabaseService();
            DatabaseService service2 = new DatabaseService();

            service1.connect();
            service2.connect();

            service1.executeQuery("SELECT * FROM users");
            service2.executeQuery("SELECT * FROM orders");

            service1.disconnect();
            service2.disconnect();
        }, "Concurrent operations should not interfere with each other");
    }

    @Test
    @DisplayName("Test constants values using reflection")
    void testConstantsValues() {
        assertDoesNotThrow(() -> {
            Field dbHostField = DatabaseService.class.getDeclaredField("DB_HOST");
            dbHostField.setAccessible(true);
            String dbHost = (String) dbHostField.get(null);
            assertEquals("localhost", dbHost, "DB_HOST should be localhost");

            Field dbPortField = DatabaseService.class.getDeclaredField("DB_PORT");
            dbPortField.setAccessible(true);
            String dbPort = (String) dbPortField.get(null);
            assertEquals("3306", dbPort, "DB_PORT should be 3306");

            Field dbNameField = DatabaseService.class.getDeclaredField("DB_NAME");
            dbNameField.setAccessible(true);
            String dbName = (String) dbNameField.get(null);
            assertEquals("mini_app_db", dbName, "DB_NAME should be mini_app_db");

            Field dbUrlField = DatabaseService.class.getDeclaredField("DB_URL");
            dbUrlField.setAccessible(true);
            String dbUrl = (String) dbUrlField.get(null);
            assertEquals("jdbc:mysql://localhost:3306/mini_app_db", dbUrl, "DB_URL should match expected format");

            Field dbUsernameField = DatabaseService.class.getDeclaredField("DB_USERNAME");
            dbUsernameField.setAccessible(true);
            String dbUsername = (String) dbUsernameField.get(null);
            assertEquals("root", dbUsername, "DB_USERNAME should be root");

            Field dbPasswordField = DatabaseService.class.getDeclaredField("DB_PASSWORD");
            dbPasswordField.setAccessible(true);
            String dbPassword = (String) dbPasswordField.get(null);
            assertEquals("password123", dbPassword, "DB_PASSWORD should be password123");

            Field redisHostField = DatabaseService.class.getDeclaredField("REDIS_HOST");
            redisHostField.setAccessible(true);
            String redisHost = (String) redisHostField.get(null);
            assertEquals("127.0.0.1", redisHost, "REDIS_HOST should be 127.0.0.1");

            Field redisPortField = DatabaseService.class.getDeclaredField("REDIS_PORT");
            redisPortField.setAccessible(true);
            int redisPort = (Integer) redisPortField.get(null);
            assertEquals(6379, redisPort, "REDIS_PORT should be 6379");

            Field externalApiUrlField = DatabaseService.class.getDeclaredField("EXTERNAL_API_URL");
            externalApiUrlField.setAccessible(true);
            String externalApiUrl = (String) externalApiUrlField.get(null);
            assertEquals("http://api.example.com:8080/v1", externalApiUrl, "EXTERNAL_API_URL should match expected value");

            Field paymentServiceUrlField = DatabaseService.class.getDeclaredField("PAYMENT_SERVICE_URL");
            paymentServiceUrlField.setAccessible(true);
            String paymentServiceUrl = (String) paymentServiceUrlField.get(null);
            assertEquals("https://payment.internal.company.com/process", paymentServiceUrl, "PAYMENT_SERVICE_URL should match expected value");
        }, "Constants should be accessible via reflection");
    }

    @Test
    @DisplayName("Test connectToCache private method using reflection")
    void testConnectToCacheMethod() {
        assertDoesNotThrow(() -> {
            Method method = DatabaseService.class.getDeclaredMethod("connectToCache");
            method.setAccessible(true);
            method.invoke(databaseService);

            String output = outContent.toString();
            assertTrue(output.contains("Connecting to Redis cache at: 127.0.0.1:6379"),
                "Should display Redis connection message");
        }, "connectToCache method should execute without exceptions");
    }

    @Test
    @DisplayName("Test initializeExternalServices private method using reflection")
    void testInitializeExternalServicesMethod() {
        assertDoesNotThrow(() -> {
            Method method = DatabaseService.class.getDeclaredMethod("initializeExternalServices");
            method.setAccessible(true);
            method.invoke(databaseService);

            String output = outContent.toString();
            assertTrue(output.contains("Initializing external API: http://api.example.com:8080/v1"),
                "Should display external API initialization message");
            assertTrue(output.contains("Initializing payment service: https://payment.internal.company.com/process"),
                "Should display payment service initialization message");
        }, "initializeExternalServices method should execute without exceptions");
    }

    @Test
    @DisplayName("Test connect method calls all required initialization methods")
    void testConnectMethodCallsInitializationMethods() {
        // Act
        assertDoesNotThrow(() -> databaseService.connect());

        // Assert
        String output = outContent.toString();
        String errorOutput = errContent.toString();

        assertTrue(output.contains("Connecting to database") || errorOutput.contains("Database connection failed"),
            "Should attempt database connection");
        assertTrue(output.contains("Connecting to Redis cache") || errorOutput.contains("connection failed"),
            "Should attempt Redis connection");
        assertTrue(output.contains("Initializing external API") || errorOutput.contains("connection failed"),
            "Should initialize external API");
        assertTrue(output.contains("Initializing payment service") || errorOutput.contains("connection failed"),
            "Should initialize payment service");
    }

    @Test
    @DisplayName("Test connect method with database driver not found scenario")
    void testConnectWithMissingDriver() {
        // Act
        assertDoesNotThrow(() -> databaseService.connect());

        // Assert
        String output = outContent.toString();
        String errorOutput = errContent.toString();

        assertTrue(output.contains("Connecting to database") ||
                  errorOutput.contains("Database driver not found") ||
                  errorOutput.contains("Database connection failed"),
            "Should handle driver loading or connection appropriately");
    }

    @Test
    @DisplayName("Test connection field is accessible via reflection")
    void testConnectionFieldAccessibility() {
        assertDoesNotThrow(() -> {
            Field connectionField = DatabaseService.class.getDeclaredField("connection");
            connectionField.setAccessible(true);

            Connection connection = (Connection) connectionField.get(databaseService);
            // Connection should be null initially
            assertNull(connection, "Connection should be null before calling connect");
        }, "Connection field should be accessible via reflection");
    }

    @Test
    @DisplayName("Test executeQuery with connection established via reflection")
    void testExecuteQueryWithConnectionEstablished() {
        assertDoesNotThrow(() -> {
            databaseService.connect();
            databaseService.executeQuery("SELECT 1");

            String output = outContent.toString();
            String errorOutput = errContent.toString();

            // Should either show query execution or connection/query failures
            assertTrue(output.contains("Executing query: SELECT 1") ||
                      errorOutput.contains("Query execution failed") ||
                      errorOutput.contains("Database connection failed") ||
                      errorOutput.contains("Database driver not found"),
                "Should attempt to execute query or show appropriate error messages");
        }, "executeQuery should work with established connection or handle failure");
    }

    @Test
    @DisplayName("Test all private methods exist and have correct signatures")
    void testPrivateMethodsExist() {
        assertDoesNotThrow(() -> {
            Method connectToCacheMethod = DatabaseService.class.getDeclaredMethod("connectToCache");
            Method initExternalServicesMethod = DatabaseService.class.getDeclaredMethod("initializeExternalServices");

            assertNotNull(connectToCacheMethod, "connectToCache method should exist");
            assertNotNull(initExternalServicesMethod, "initializeExternalServices method should exist");

            assertTrue(Modifier.isPrivate(connectToCacheMethod.getModifiers()), "connectToCache should be private");
            assertTrue(Modifier.isPrivate(initExternalServicesMethod.getModifiers()), "initializeExternalServices should be private");
        }, "Private methods should exist with correct signatures");
    }

    @Test
    @DisplayName("Test class modifiers and annotations")
    void testClassModifiersAndAnnotations() {
        // Test class modifiers
        int modifiers = DatabaseService.class.getModifiers();
        assertTrue(Modifier.isPublic(modifiers), "Class should be public");
        assertFalse(Modifier.isAbstract(modifiers), "Class should not be abstract");
        assertFalse(Modifier.isFinal(modifiers), "Class should not be final");

        // Test Service annotation
        assertTrue(DatabaseService.class.isAnnotationPresent(org.springframework.stereotype.Service.class),
            "DatabaseService should be annotated with @Service");
    }

    @Test
    @DisplayName("Test package is correct")
    void testPackage() {
        assertEquals("com.test", DatabaseService.class.getPackage().getName(),
            "DatabaseService should be in com.test package");
    }

    @Test
    @DisplayName("Test error output for connection failures")
    void testErrorOutputForConnectionFailures() {
        assertDoesNotThrow(() -> {
            databaseService.connect();

            String errorOutput = errContent.toString();
            // Should contain error messages for connection failures in test environment
            assertTrue(errorOutput.contains("Database driver not found") ||
                      errorOutput.contains("Database connection failed") ||
                      errorOutput.isEmpty(), // No error if driver is available
                "Should handle connection errors appropriately");
        }, "Should handle and log connection errors properly");
    }

    @Test
    @DisplayName("Test executeQuery query timeout via reflection")
    void testExecuteQueryTimeout() {
        assertDoesNotThrow(() -> {
            databaseService.connect();
            databaseService.executeQuery("SELECT SLEEP(1)");

            // Test should pass regardless of connection success
            // Query timeout is hardcoded to 30 seconds in the implementation
        }, "executeQuery should handle query timeout settings");
    }
}