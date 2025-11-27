package com.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;

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
    void testDatabaseServiceCreation() {
        assertNotNull(databaseService);
    }

    @Test
    void testConnectMethod() {
        assertDoesNotThrow(() -> databaseService.connect());
    }

    @Test
    void testExecuteQuery() {
        databaseService.connect();
        assertDoesNotThrow(() -> databaseService.executeQuery("SELECT 1"));
    }

    @Test
    void testDisconnect() {
        databaseService.connect();
        assertDoesNotThrow(() -> databaseService.disconnect());
    }

    @Test
    void testMultipleConnections() {
        assertDoesNotThrow(() -> {
            databaseService.connect();
            databaseService.disconnect();
            databaseService.connect();
            databaseService.disconnect();
        });
    }
}
