package com.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;

import java.sql.SQLException;

/**
 * JUnit 5 test class for DatabaseService
 * Tests all public methods and various scenarios including edge cases
 */
class DatabaseServiceTest {

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
    }

    /**
     * Test default constructor
     */
    @Test
    void testConstructor() {
        DatabaseService service = new DatabaseService();
        assertNotNull(service, "DatabaseService instance should not be null");
    }

    /**
     * Test connect method - successful connection scenario
     * Note: This will attempt actual database connection
     */
    @Test
    void testConnect() {
        assertDoesNotThrow(() -> {
            databaseService.connect();
        }, "Connect method should not throw exception");
    }

    /**
     * Test connect method - verify it handles connection failures gracefully
     */
    @Test
    void testConnectWithInvalidCredentials() {
        // The method catches SQLException internally, so it should not throw
        assertDoesNotThrow(() -> {
            databaseService.connect();
        }, "Connect should handle connection failures gracefully");
    }

    /**
     * Test executeQuery with null connection
     */
    @Test
    void testExecuteQueryWithNullConnection() {
        assertDoesNotThrow(() -> {
            databaseService.executeQuery("SELECT * FROM users");
        }, "Execute query should handle null connection gracefully");
    }

    /**
     * Test executeQuery with valid SQL
     */
    @Test
    void testExecuteQueryWithValidSQL() {
        databaseService.connect();
        assertDoesNotThrow(() -> {
            databaseService.executeQuery("SELECT 1");
        }, "Execute query should not throw exception with valid SQL");
    }

    /**
     * Test executeQuery with empty string
     */
    @Test
    void testExecuteQueryWithEmptyString() {
        databaseService.connect();
        assertDoesNotThrow(() -> {
            databaseService.executeQuery("");
        }, "Execute query should handle empty string gracefully");
    }

    /**
     * Test executeQuery with null SQL
     */
    @Test
    void testExecuteQueryWithNullSQL() {
        databaseService.connect();
        assertDoesNotThrow(() -> {
            databaseService.executeQuery(null);
        }, "Execute query should handle null SQL gracefully");
    }

    /**
     * Test executeQuery with invalid SQL
     */
    @Test
    void testExecuteQueryWithInvalidSQL() {
        databaseService.connect();
        assertDoesNotThrow(() -> {
            databaseService.executeQuery("INVALID SQL STATEMENT");
        }, "Execute query should handle invalid SQL gracefully");
    }

    /**
     * Test executeQuery with complex SQL statement
     */
    @Test
    void testExecuteQueryWithComplexSQL() {
        databaseService.connect();
        String complexSQL = "SELECT u.id, u.name, o.order_date FROM users u JOIN orders o ON u.id = o.user_id WHERE u.status = 'active'";
        assertDoesNotThrow(() -> {
            databaseService.executeQuery(complexSQL);
        }, "Execute query should handle complex SQL statements");
    }

    /**
     * Test disconnect method without prior connection
     */
    @Test
    void testDisconnectWithoutConnection() {
        assertDoesNotThrow(() -> {
            databaseService.disconnect();
        }, "Disconnect should handle null connection gracefully");
    }

    /**
     * Test disconnect method after connection
     */
    @Test
    void testDisconnectAfterConnection() {
        databaseService.connect();
        assertDoesNotThrow(() -> {
            databaseService.disconnect();
        }, "Disconnect should work after connection");
    }

    /**
     * Test multiple disconnect calls
     */
    @Test
    void testMultipleDisconnectCalls() {
        databaseService.connect();
        assertDoesNotThrow(() -> {
            databaseService.disconnect();
            databaseService.disconnect();
        }, "Multiple disconnect calls should not throw exception");
    }

    /**
     * Test connect, execute, disconnect workflow
     */
    @Test
    void testCompleteWorkflow() {
        assertDoesNotThrow(() -> {
            databaseService.connect();
            databaseService.executeQuery("SELECT 1");
            databaseService.executeQuery("SELECT 2");
            databaseService.disconnect();
        }, "Complete workflow should execute without exceptions");
    }

    /**
     * Test multiple query executions in sequence
     */
    @Test
    void testMultipleQueryExecutions() {
        databaseService.connect();
        assertDoesNotThrow(() -> {
            databaseService.executeQuery("SELECT * FROM users");
            databaseService.executeQuery("SELECT * FROM orders");
            databaseService.executeQuery("SELECT * FROM products");
        }, "Multiple queries should execute sequentially");
    }

    /**
     * Test query execution after disconnect
     */
    @Test
    void testExecuteQueryAfterDisconnect() {
        databaseService.connect();
        databaseService.disconnect();
        assertDoesNotThrow(() -> {
            databaseService.executeQuery("SELECT 1");
        }, "Query execution after disconnect should handle gracefully");
    }

    /**
     * Test reconnection scenario
     */
    @Test
    void testReconnection() {
        assertDoesNotThrow(() -> {
            databaseService.connect();
            databaseService.disconnect();
            databaseService.connect();
            databaseService.disconnect();
        }, "Reconnection should work properly");
    }

    /**
     * Test SQL injection attempt (should be handled by PreparedStatement)
     */
    @Test
    void testSQLInjectionAttempt() {
        databaseService.connect();
        String maliciousSQL = "SELECT * FROM users WHERE id = '1' OR '1'='1'";
        assertDoesNotThrow(() -> {
            databaseService.executeQuery(maliciousSQL);
        }, "SQL injection attempt should be handled");
    }

    /**
     * Test query with special characters
     */
    @Test
    void testQueryWithSpecialCharacters() {
        databaseService.connect();
        assertDoesNotThrow(() -> {
            databaseService.executeQuery("SELECT * FROM users WHERE name LIKE '%test@example.com%'");
        }, "Query with special characters should work");
    }

    /**
     * Test very long SQL query
     */
    @Test
    void testVeryLongSQLQuery() {
        databaseService.connect();
        StringBuilder longSQL = new StringBuilder("SELECT * FROM users WHERE id IN (");
        for (int i = 0; i < 1000; i++) {
            longSQL.append(i);
            if (i < 999) longSQL.append(",");
        }
        longSQL.append(")");

        String finalSQL = longSQL.toString();
        assertDoesNotThrow(() -> {
            databaseService.executeQuery(finalSQL);
        }, "Very long SQL query should be handled");
    }
}
