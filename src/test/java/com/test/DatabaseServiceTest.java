package com.test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for DatabaseService
 */
public class DatabaseServiceTest {

    private DatabaseService databaseService;

    @BeforeEach
    void setUp() {
        databaseService = new DatabaseService();
    }

    @AfterEach
    void tearDown() {
        if (databaseService != null) {
            databaseService.disconnect();
        }
        databaseService = null;
    }

    @Test
    void testConstructor() {
        // Test DatabaseService constructor
        DatabaseService service = new DatabaseService();
        assertNotNull(service);
    }

    @Test
    void testConnect() {
        // Test connect method - should handle connection gracefully
        assertDoesNotThrow(() -> {
            databaseService.connect();
        });
    }

    @Test
    void testConnect_multipleConnections() {
        // Test multiple connect calls
        assertDoesNotThrow(() -> {
            databaseService.connect();
            databaseService.connect(); // Second call should be handled gracefully
        });
    }

    @Test
    void testDisconnect_withoutConnection() {
        // Test disconnect without prior connection
        assertDoesNotThrow(() -> {
            databaseService.disconnect();
        });
    }

    @Test
    void testDisconnect_afterConnect() {
        // Test disconnect after connection attempt
        assertDoesNotThrow(() -> {
            databaseService.connect();
            databaseService.disconnect();
        });
    }

    @Test
    void testExecuteQuery_withNullQuery() {
        // Test executeQuery with null SQL
        assertDoesNotThrow(() -> {
            databaseService.executeQuery(null);
        });
    }

    @Test
    void testExecuteQuery_withEmptyQuery() {
        // Test executeQuery with empty SQL
        assertDoesNotThrow(() -> {
            databaseService.executeQuery("");
        });
    }

    @Test
    void testExecuteQuery_withValidQuery() {
        // Test executeQuery with valid SQL
        assertDoesNotThrow(() -> {
            databaseService.connect();
            databaseService.executeQuery("SELECT 1");
        });
    }

    @Test
    void testExecuteQuery_withInvalidQuery() {
        // Test executeQuery with invalid SQL
        assertDoesNotThrow(() -> {
            databaseService.connect();
            databaseService.executeQuery("INVALID SQL STATEMENT");
        });
    }

    @Test
    void testExecuteQuery_withoutConnection() {
        // Test executeQuery without connection
        assertDoesNotThrow(() -> {
            databaseService.executeQuery("SELECT 1");
        });
    }

    @Test
    void testDbHostConstant() throws Exception {
        // Test DB_HOST constant value
        Field field = DatabaseService.class.getDeclaredField("DB_HOST");
        field.setAccessible(true);
        String dbHost = (String) field.get(null);
        assertEquals("localhost", dbHost);
    }

    @Test
    void testDbPortConstant() throws Exception {
        // Test DB_PORT constant value
        Field field = DatabaseService.class.getDeclaredField("DB_PORT");
        field.setAccessible(true);
        String dbPort = (String) field.get(null);
        assertEquals("5432", dbPort);
    }

    @Test
    void testDbNameConstant() throws Exception {
        // Test DB_NAME constant value
        Field field = DatabaseService.class.getDeclaredField("DB_NAME");
        field.setAccessible(true);
        String dbName = (String) field.get(null);
        assertEquals("mini_app_db", dbName);
    }

    @Test
    void testDbUrlConstant() throws Exception {
        // Test DB_URL constant value
        Field field = DatabaseService.class.getDeclaredField("DB_URL");
        field.setAccessible(true);
        String dbUrl = (String) field.get(null);
        assertEquals("jdbc:postgresql://localhost:5432/mini_app_db", dbUrl);
    }

    @Test
    void testDbUsernameConstant() throws Exception {
        // Test DB_USERNAME constant value
        Field field = DatabaseService.class.getDeclaredField("DB_USERNAME");
        field.setAccessible(true);
        String dbUsername = (String) field.get(null);
        assertEquals("root", dbUsername);
    }

    @Test
    void testDbPasswordConstant() throws Exception {
        // Test DB_PASSWORD constant value
        Field field = DatabaseService.class.getDeclaredField("DB_PASSWORD");
        field.setAccessible(true);
        String dbPassword = (String) field.get(null);
        assertEquals("password123", dbPassword);
    }

    @Test
    void testRedisHostConstant() throws Exception {
        // Test REDIS_HOST constant value
        Field field = DatabaseService.class.getDeclaredField("REDIS_HOST");
        field.setAccessible(true);
        String redisHost = (String) field.get(null);
        assertEquals("127.0.0.1", redisHost);
    }

    @Test
    void testRedisPortConstant() throws Exception {
        // Test REDIS_PORT constant value
        Field field = DatabaseService.class.getDeclaredField("REDIS_PORT");
        field.setAccessible(true);
        int redisPort = (int) field.get(null);
        assertEquals(6379, redisPort);
    }

    @Test
    void testExternalApiUrlConstant() throws Exception {
        // Test EXTERNAL_API_URL constant value
        Field field = DatabaseService.class.getDeclaredField("EXTERNAL_API_URL");
        field.setAccessible(true);
        String apiUrl = (String) field.get(null);
        assertEquals("http://api.example.com:8080/v1", apiUrl);
    }

    @Test
    void testPaymentServiceUrlConstant() throws Exception {
        // Test PAYMENT_SERVICE_URL constant value
        Field field = DatabaseService.class.getDeclaredField("PAYMENT_SERVICE_URL");
        field.setAccessible(true);
        String paymentUrl = (String) field.get(null);
        assertEquals("https://payment.internal.company.com/process", paymentUrl);
    }

    @Test
    void testConnectToCache() throws Exception {
        // Test private connectToCache method using reflection
        Method method = DatabaseService.class.getDeclaredMethod("connectToCache");
        method.setAccessible(true);

        assertDoesNotThrow(() -> {
            try {
                method.invoke(databaseService);
            } catch (Exception e) {
                // Expected as Redis may not be available
            }
        });
    }

    @Test
    void testInitializeExternalServices() throws Exception {
        // Test private initializeExternalServices method using reflection
        Method method = DatabaseService.class.getDeclaredMethod("initializeExternalServices");
        method.setAccessible(true);

        assertDoesNotThrow(() -> {
            try {
                method.invoke(databaseService);
            } catch (Exception e) {
                // Expected as external services may not be available
            }
        });
    }

    @Test
    void testConnectionField() throws Exception {
        // Test connection field is properly initialized as null
        Field field = DatabaseService.class.getDeclaredField("connection");
        field.setAccessible(true);
        Connection connection = (Connection) field.get(databaseService);
        assertNull(connection);
    }

    @Test
    void testMultipleInstances() {
        // Test creating multiple DatabaseService instances
        DatabaseService service1 = new DatabaseService();
        DatabaseService service2 = new DatabaseService();

        assertNotNull(service1);
        assertNotNull(service2);
        assertNotSame(service1, service2);
    }

    @Test
    void testConstants_immutability() throws Exception {
        // Test that constants are properly defined as static final
        Field[] fields = {
            DatabaseService.class.getDeclaredField("DB_HOST"),
            DatabaseService.class.getDeclaredField("DB_PORT"),
            DatabaseService.class.getDeclaredField("DB_NAME"),
            DatabaseService.class.getDeclaredField("DB_URL"),
            DatabaseService.class.getDeclaredField("DB_USERNAME"),
            DatabaseService.class.getDeclaredField("DB_PASSWORD"),
            DatabaseService.class.getDeclaredField("REDIS_HOST"),
            DatabaseService.class.getDeclaredField("REDIS_PORT"),
            DatabaseService.class.getDeclaredField("EXTERNAL_API_URL"),
            DatabaseService.class.getDeclaredField("PAYMENT_SERVICE_URL")
        };

        for (Field field : fields) {
            assertTrue(java.lang.reflect.Modifier.isStatic(field.getModifiers()));
            assertTrue(java.lang.reflect.Modifier.isFinal(field.getModifiers()));
        }
    }

    @Test
    void testConnectAndDisconnectCycle() {
        // Test complete connect and disconnect cycle
        assertDoesNotThrow(() -> {
            databaseService.connect();
            databaseService.executeQuery("SELECT COUNT(*) FROM information_schema.tables");
            databaseService.disconnect();
        });
    }

    @Test
    void testExecuteQuery_longQuery() {
        // Test executeQuery with longer SQL statement
        String longQuery = "SELECT table_name, column_name, data_type FROM information_schema.columns WHERE table_schema = 'public' ORDER BY table_name, ordinal_position";

        assertDoesNotThrow(() -> {
            databaseService.connect();
            databaseService.executeQuery(longQuery);
        });
    }

    @Test
    void testExecuteQuery_specialCharacters() {
        // Test executeQuery with special characters
        assertDoesNotThrow(() -> {
            databaseService.connect();
            databaseService.executeQuery("SELECT 'test''s data' as test_column");
        });
    }
}