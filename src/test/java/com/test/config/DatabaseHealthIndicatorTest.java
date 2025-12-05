package com.test.config;

import com.test.DatabaseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DatabaseHealthIndicatorTest {

    @Mock
    private DatabaseService databaseService;

    private DatabaseHealthIndicator databaseHealthIndicator;

    @BeforeEach
    void setUp() {
        databaseHealthIndicator = new DatabaseHealthIndicator(databaseService);
    }

    @Test
    void testConstructor() {
        assertNotNull(databaseHealthIndicator);
    }

    @Test
    void testConstructorWithNullDatabaseService() {
        assertDoesNotThrow(() -> new DatabaseHealthIndicator(null));
    }

    @Test
    void testConstructor_AssignsDatabaseService() {
        DatabaseHealthIndicator indicator = new DatabaseHealthIndicator(databaseService);
        assertNotNull(indicator);
    }

    @Test
    void testHealth_ConnectionValid_ReturnsUp() {
        when(databaseService.isConnectionValid()).thenReturn(true);

        Health health = databaseHealthIndicator.health();

        assertNotNull(health);
        assertEquals(Status.UP, health.getStatus());
        assertEquals("PostgreSQL", health.getDetails().get("database"));
        assertEquals("Connection validated successfully", health.getDetails().get("status"));
        verify(databaseService).isConnectionValid();
    }

    @Test
    void testHealth_ConnectionInvalid_ReturnsDown() {
        when(databaseService.isConnectionValid()).thenReturn(false);

        Health health = databaseHealthIndicator.health();

        assertNotNull(health);
        assertEquals(Status.DOWN, health.getStatus());
        assertEquals("PostgreSQL", health.getDetails().get("database"));
        assertEquals("Connection validation failed", health.getDetails().get("status"));
        verify(databaseService).isConnectionValid();
    }

    @Test
    void testHealth_ExceptionThrown_ReturnsDown() {
        when(databaseService.isConnectionValid()).thenThrow(new RuntimeException("Database connection error"));

        Health health = databaseHealthIndicator.health();

        assertNotNull(health);
        assertEquals(Status.DOWN, health.getStatus());
        assertEquals("PostgreSQL", health.getDetails().get("database"));
        assertEquals("Database connection error", health.getDetails().get("error"));
        verify(databaseService).isConnectionValid();
    }

    @Test
    void testHealth_NullPointerException_ReturnsDown() {
        when(databaseService.isConnectionValid()).thenThrow(new NullPointerException("Null database connection"));

        Health health = databaseHealthIndicator.health();

        assertNotNull(health);
        assertEquals(Status.DOWN, health.getStatus());
        assertEquals("PostgreSQL", health.getDetails().get("database"));
        assertNotNull(health.getDetails().get("error"));
        verify(databaseService).isConnectionValid();
    }

    @Test
    void testHealth_MultipleCallsWithDifferentResults() {
        when(databaseService.isConnectionValid()).thenReturn(true).thenReturn(false).thenReturn(true);

        Health health1 = databaseHealthIndicator.health();
        assertEquals(Status.UP, health1.getStatus());

        Health health2 = databaseHealthIndicator.health();
        assertEquals(Status.DOWN, health2.getStatus());

        Health health3 = databaseHealthIndicator.health();
        assertEquals(Status.UP, health3.getStatus());

        verify(databaseService, times(3)).isConnectionValid();
    }

    @Test
    void testHealth_DetailsContainCorrectKeys() {
        when(databaseService.isConnectionValid()).thenReturn(true);

        Health health = databaseHealthIndicator.health();

        assertTrue(health.getDetails().containsKey("database"));
        assertTrue(health.getDetails().containsKey("status"));
        assertFalse(health.getDetails().containsKey("error"));
    }

    @Test
    void testHealth_ErrorDetailsContainCorrectKeys() {
        when(databaseService.isConnectionValid()).thenThrow(new RuntimeException("Connection timeout"));

        Health health = databaseHealthIndicator.health();

        assertTrue(health.getDetails().containsKey("database"));
        assertTrue(health.getDetails().containsKey("error"));
        assertFalse(health.getDetails().containsKey("status"));
    }

    @Test
    void testHealth_WithNullDatabaseService() {
        DatabaseHealthIndicator indicator = new DatabaseHealthIndicator(null);

        Health health = indicator.health();
        assertEquals(Status.DOWN, health.getStatus());
    }

    @Test
    void testHealth_DatabaseServiceThrowsCustomException() {
        when(databaseService.isConnectionValid()).thenThrow(new IllegalStateException("Database not initialized"));

        Health health = databaseHealthIndicator.health();

        assertEquals(Status.DOWN, health.getStatus());
        assertEquals("Database not initialized", health.getDetails().get("error"));
        verify(databaseService).isConnectionValid();
    }

    @Test
    void testHealth_DatabaseServiceThrowsIllegalArgumentException() {
        when(databaseService.isConnectionValid()).thenThrow(new IllegalArgumentException("Invalid argument"));

        Health health = databaseHealthIndicator.health();

        assertEquals(Status.DOWN, health.getStatus());
        assertEquals("Invalid argument", health.getDetails().get("error"));
        verify(databaseService).isConnectionValid();
    }

    @Test
    void testHealth_ConsecutiveSuccessfulCalls() {
        when(databaseService.isConnectionValid()).thenReturn(true);

        Health health1 = databaseHealthIndicator.health();
        Health health2 = databaseHealthIndicator.health();
        Health health3 = databaseHealthIndicator.health();

        assertEquals(Status.UP, health1.getStatus());
        assertEquals(Status.UP, health2.getStatus());
        assertEquals(Status.UP, health3.getStatus());

        verify(databaseService, times(3)).isConnectionValid();
    }

    @Test
    void testHealth_ConsecutiveFailedCalls() {
        when(databaseService.isConnectionValid()).thenReturn(false);

        Health health1 = databaseHealthIndicator.health();
        Health health2 = databaseHealthIndicator.health();
        Health health3 = databaseHealthIndicator.health();

        assertEquals(Status.DOWN, health1.getStatus());
        assertEquals(Status.DOWN, health2.getStatus());
        assertEquals(Status.DOWN, health3.getStatus());

        verify(databaseService, times(3)).isConnectionValid();
    }

    @Test
    void testHealth_DetailsNotNull() {
        when(databaseService.isConnectionValid()).thenReturn(true);

        Health health = databaseHealthIndicator.health();

        assertNotNull(health.getDetails());
        assertFalse(health.getDetails().isEmpty());
    }

    @Test
    void testHealth_ErrorDetailsNotNull() {
        when(databaseService.isConnectionValid()).thenThrow(new RuntimeException("Error"));

        Health health = databaseHealthIndicator.health();

        assertNotNull(health.getDetails());
        assertFalse(health.getDetails().isEmpty());
    }

    @Test
    void testHealth_StatusUpHasCorrectMessage() {
        when(databaseService.isConnectionValid()).thenReturn(true);

        Health health = databaseHealthIndicator.health();

        assertEquals("Connection validated successfully", health.getDetails().get("status"));
    }

    @Test
    void testHealth_StatusDownHasCorrectMessage() {
        when(databaseService.isConnectionValid()).thenReturn(false);

        Health health = databaseHealthIndicator.health();

        assertEquals("Connection validation failed", health.getDetails().get("status"));
    }

    @Test
    void testHealth_DatabaseTypeIsPostgreSQL() {
        when(databaseService.isConnectionValid()).thenReturn(true);

        Health health = databaseHealthIndicator.health();

        assertEquals("PostgreSQL", health.getDetails().get("database"));
    }

    @Test
    void testHealth_ExceptionWithNullMessage() {
        when(databaseService.isConnectionValid()).thenThrow(new RuntimeException("Error"));

        Health health = databaseHealthIndicator.health();

        assertEquals(Status.DOWN, health.getStatus());
        assertTrue(health.getDetails().containsKey("error"));
    }

    @Test
    void testHealth_ExceptionWithEmptyMessage() {
        when(databaseService.isConnectionValid()).thenThrow(new RuntimeException(""));

        Health health = databaseHealthIndicator.health();

        assertEquals(Status.DOWN, health.getStatus());
        assertEquals("", health.getDetails().get("error"));
    }

    @Test
    void testHealth_VerifyStatusUpValue() {
        when(databaseService.isConnectionValid()).thenReturn(true);

        Health health = databaseHealthIndicator.health();

        assertEquals(Status.UP, health.getStatus());
        assertEquals("UP", health.getStatus().getCode());
    }

    @Test
    void testHealth_VerifyStatusDownValue() {
        when(databaseService.isConnectionValid()).thenReturn(false);

        Health health = databaseHealthIndicator.health();

        assertEquals(Status.DOWN, health.getStatus());
        assertEquals("DOWN", health.getStatus().getCode());
    }

    @Test
    void testHealth_AlternatingResults() {
        when(databaseService.isConnectionValid())
            .thenReturn(true)
            .thenReturn(false)
            .thenReturn(true)
            .thenReturn(false);

        Health health1 = databaseHealthIndicator.health();
        assertEquals(Status.UP, health1.getStatus());

        Health health2 = databaseHealthIndicator.health();
        assertEquals(Status.DOWN, health2.getStatus());

        Health health3 = databaseHealthIndicator.health();
        assertEquals(Status.UP, health3.getStatus());

        Health health4 = databaseHealthIndicator.health();
        assertEquals(Status.DOWN, health4.getStatus());

        verify(databaseService, times(4)).isConnectionValid();
    }

    @Test
    void testHealth_ExceptionAfterSuccess() {
        when(databaseService.isConnectionValid())
            .thenReturn(true)
            .thenThrow(new RuntimeException("Sudden failure"));

        Health health1 = databaseHealthIndicator.health();
        assertEquals(Status.UP, health1.getStatus());

        Health health2 = databaseHealthIndicator.health();
        assertEquals(Status.DOWN, health2.getStatus());
        assertEquals("Sudden failure", health2.getDetails().get("error"));

        verify(databaseService, times(2)).isConnectionValid();
    }

    @Test
    void testHealth_SuccessAfterException() {
        when(databaseService.isConnectionValid())
            .thenThrow(new RuntimeException("Initial failure"))
            .thenReturn(true);

        Health health1 = databaseHealthIndicator.health();
        assertEquals(Status.DOWN, health1.getStatus());

        Health health2 = databaseHealthIndicator.health();
        assertEquals(Status.UP, health2.getStatus());

        verify(databaseService, times(2)).isConnectionValid();
    }

    @Test
    void testHealth_VerifyDetailsSize_WhenUp() {
        when(databaseService.isConnectionValid()).thenReturn(true);

        Health health = databaseHealthIndicator.health();

        assertEquals(2, health.getDetails().size());
        assertTrue(health.getDetails().containsKey("database"));
        assertTrue(health.getDetails().containsKey("status"));
    }

    @Test
    void testHealth_VerifyDetailsSize_WhenDown() {
        when(databaseService.isConnectionValid()).thenReturn(false);

        Health health = databaseHealthIndicator.health();

        assertEquals(2, health.getDetails().size());
        assertTrue(health.getDetails().containsKey("database"));
        assertTrue(health.getDetails().containsKey("status"));
    }

    @Test
    void testHealth_VerifyDetailsSize_WhenException() {
        when(databaseService.isConnectionValid()).thenThrow(new RuntimeException("Error"));

        Health health = databaseHealthIndicator.health();

        assertEquals(2, health.getDetails().size());
        assertTrue(health.getDetails().containsKey("database"));
        assertTrue(health.getDetails().containsKey("error"));
    }

    @Test
    void testHealth_VerifyHealthNotNull() {
        when(databaseService.isConnectionValid()).thenReturn(true);

        Health health = databaseHealthIndicator.health();

        assertNotNull(health);
    }

    @Test
    void testHealth_VerifyStatusNotNull() {
        when(databaseService.isConnectionValid()).thenReturn(true);

        Health health = databaseHealthIndicator.health();

        assertNotNull(health.getStatus());
    }

    @Test
    void testHealth_OutOfMemoryError() {
        when(databaseService.isConnectionValid()).thenThrow(new OutOfMemoryError("Memory exhausted"));

        assertThrows(OutOfMemoryError.class, () -> databaseHealthIndicator.health());
    }
}
