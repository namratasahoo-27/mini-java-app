package com.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for DatabaseService
 * Tests all public methods, constructors, and edge cases
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

    @Test
    @DisplayName("Test DatabaseService default constructor")
    void testDatabaseServiceConstructor() {
        // Arrange & Act
        DatabaseService service = new DatabaseService();

        // Assert
        assertNotNull(service, "DatabaseService instance should not be null");
    }

    @Test
    @DisplayName("Test connect method - successful connection simulation")
    void testConnect() {
        // Arrange
        DatabaseService service = new DatabaseService();

        // Act & Assert - method should not throw exception
        assertDoesNotThrow(() -> service.connect(),
                "connect() should not throw exception");
    }

    @Test
    @DisplayName("Test connect method - multiple calls")
    void testConnectMultipleCalls() {
        // Arrange
        DatabaseService service = new DatabaseService();

        // Act & Assert - multiple connect calls should not throw exception
        assertDoesNotThrow(() -> {
            service.connect();
            service.connect();
        }, "Multiple connect() calls should not throw exception");
    }

    @Test
    @DisplayName("Test executeQuery with null SQL")
    void testExecuteQueryWithNullSql() {
        // Arrange
        DatabaseService service = new DatabaseService();

        // Act & Assert - should handle null gracefully
        assertDoesNotThrow(() -> service.executeQuery(null),
                "executeQuery() should handle null SQL gracefully");
    }

    @Test
    @DisplayName("Test executeQuery with empty SQL")
    void testExecuteQueryWithEmptyString() {
        // Arrange
        DatabaseService service = new DatabaseService();

        // Act & Assert - should handle empty string gracefully
        assertDoesNotThrow(() -> service.executeQuery(""),
                "executeQuery() should handle empty SQL gracefully");
    }

    @Test
    @DisplayName("Test executeQuery with valid SQL before connection")
    void testExecuteQueryBeforeConnection() {
        // Arrange
        DatabaseService service = new DatabaseService();
        String sql = "SELECT * FROM users";

        // Act & Assert - should handle query without connection
        assertDoesNotThrow(() -> service.executeQuery(sql),
                "executeQuery() should handle execution before connection");
    }

    @Test
    @DisplayName("Test executeQuery with valid SQL after connection")
    void testExecuteQueryAfterConnection() {
        // Arrange
        DatabaseService service = new DatabaseService();
        service.connect();
        String sql = "SELECT * FROM users";

        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(sql),
                "executeQuery() should execute after connection");
    }

    @Test
    @DisplayName("Test executeQuery with SELECT statement")
    void testExecuteQueryWithSelect() {
        // Arrange
        DatabaseService service = new DatabaseService();
        String sql = "SELECT id, name, email FROM users WHERE id = 1";

        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(sql),
                "executeQuery() should handle SELECT statement");
    }

    @Test
    @DisplayName("Test executeQuery with INSERT statement")
    void testExecuteQueryWithInsert() {
        // Arrange
        DatabaseService service = new DatabaseService();
        String sql = "INSERT INTO users (name, email) VALUES ('Test', 'test@example.com')";

        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(sql),
                "executeQuery() should handle INSERT statement");
    }

    @Test
    @DisplayName("Test executeQuery with UPDATE statement")
    void testExecuteQueryWithUpdate() {
        // Arrange
        DatabaseService service = new DatabaseService();
        String sql = "UPDATE users SET name = 'Updated' WHERE id = 1";

        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(sql),
                "executeQuery() should handle UPDATE statement");
    }

    @Test
    @DisplayName("Test executeQuery with DELETE statement")
    void testExecuteQueryWithDelete() {
        // Arrange
        DatabaseService service = new DatabaseService();
        String sql = "DELETE FROM users WHERE id = 1";

        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(sql),
                "executeQuery() should handle DELETE statement");
    }

    @Test
    @DisplayName("Test executeQuery with complex SQL")
    void testExecuteQueryWithComplexSql() {
        // Arrange
        DatabaseService service = new DatabaseService();
        String sql = "SELECT u.id, u.name, COUNT(o.id) as order_count " +
                    "FROM users u LEFT JOIN orders o ON u.id = o.user_id " +
                    "GROUP BY u.id, u.name HAVING COUNT(o.id) > 5";

        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(sql),
                "executeQuery() should handle complex SQL statement");
    }

    @Test
    @DisplayName("Test executeQuery with SQL injection attempt")
    void testExecuteQueryWithSqlInjection() {
        // Arrange
        DatabaseService service = new DatabaseService();
        String sql = "SELECT * FROM users WHERE id = 1; DROP TABLE users;--";

        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(sql),
                "executeQuery() should handle SQL injection attempt");
    }

    @Test
    @DisplayName("Test disconnect method without connection")
    void testDisconnectWithoutConnection() {
        // Arrange
        DatabaseService service = new DatabaseService();

        // Act & Assert - should not throw exception
        assertDoesNotThrow(() -> service.disconnect(),
                "disconnect() should not throw exception without connection");
    }

    @Test
    @DisplayName("Test disconnect method after connection")
    void testDisconnectAfterConnection() {
        // Arrange
        DatabaseService service = new DatabaseService();
        service.connect();

        // Act & Assert
        assertDoesNotThrow(() -> service.disconnect(),
                "disconnect() should successfully close connection");
    }

    @Test
    @DisplayName("Test disconnect method - multiple calls")
    void testDisconnectMultipleCalls() {
        // Arrange
        DatabaseService service = new DatabaseService();
        service.connect();

        // Act & Assert - multiple disconnect calls should not throw exception
        assertDoesNotThrow(() -> {
            service.disconnect();
            service.disconnect();
            service.disconnect();
        }, "Multiple disconnect() calls should not throw exception");
    }

    @Test
    @DisplayName("Test complete workflow: connect, execute, disconnect")
    void testCompleteWorkflow() {
        // Arrange
        DatabaseService service = new DatabaseService();
        String sql = "SELECT * FROM users";

        // Act & Assert - full workflow should work
        assertDoesNotThrow(() -> {
            service.connect();
            service.executeQuery(sql);
            service.disconnect();
        }, "Complete workflow should execute without exception");
    }

    @Test
    @DisplayName("Test executeQuery multiple times after connection")
    void testMultipleQueriesAfterConnection() {
        // Arrange
        DatabaseService service = new DatabaseService();
        service.connect();

        // Act & Assert - multiple queries should work
        assertDoesNotThrow(() -> {
            service.executeQuery("SELECT * FROM users");
            service.executeQuery("SELECT * FROM orders");
            service.executeQuery("SELECT * FROM products");
        }, "Multiple queries should execute successfully");
    }

    @Test
    @DisplayName("Test executeQuery after disconnect")
    void testExecuteQueryAfterDisconnect() {
        // Arrange
        DatabaseService service = new DatabaseService();
        service.connect();
        service.disconnect();
        String sql = "SELECT * FROM users";

        // Act & Assert - should handle query after disconnect
        assertDoesNotThrow(() -> service.executeQuery(sql),
                "executeQuery() should handle execution after disconnect");
    }

    @Test
    @DisplayName("Test reconnect after disconnect")
    void testReconnectAfterDisconnect() {
        // Arrange
        DatabaseService service = new DatabaseService();

        // Act & Assert - reconnect workflow should work
        assertDoesNotThrow(() -> {
            service.connect();
            service.disconnect();
            service.connect();
            service.disconnect();
        }, "Reconnect after disconnect should work");
    }

    @Test
    @DisplayName("Test executeQuery with very long SQL statement")
    void testExecuteQueryWithLongSql() {
        // Arrange
        DatabaseService service = new DatabaseService();
        StringBuilder longSql = new StringBuilder("SELECT * FROM users WHERE id IN (");
        for (int i = 0; i < 1000; i++) {
            longSql.append(i);
            if (i < 999) longSql.append(",");
        }
        longSql.append(")");

        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(longSql.toString()),
                "executeQuery() should handle very long SQL statement");
    }

    @Test
    @DisplayName("Test executeQuery with special characters")
    void testExecuteQueryWithSpecialCharacters() {
        // Arrange
        DatabaseService service = new DatabaseService();
        String sql = "SELECT * FROM users WHERE name = 'O''Brien' AND email LIKE '%@%'";

        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(sql),
                "executeQuery() should handle SQL with special characters");
    }

    @Test
    @DisplayName("Test null safety - new instance")
    void testNullSafety() {
        // Arrange & Act
        DatabaseService service = new DatabaseService();

        // Assert - verify instance is not null
        assertNotNull(service, "New DatabaseService instance should not be null");
    }
}
