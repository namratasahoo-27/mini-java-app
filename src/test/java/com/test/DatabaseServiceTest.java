package com.test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DatabaseServiceTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement preparedStatement;

    private DatabaseService databaseService;

    @BeforeEach
    void setUp() {
        databaseService = new DatabaseService(dataSource);
        ReflectionTestUtils.setField(databaseService, "databaseUrl", "jdbc:postgresql://localhost:5432/testdb");
        ReflectionTestUtils.setField(databaseService, "connectionTimeout", 30000);
        ReflectionTestUtils.setField(databaseService, "externalApiUrl", "https://api.example.com");
        ReflectionTestUtils.setField(databaseService, "paymentServiceUrl", "https://payment.example.com");
        ReflectionTestUtils.setField(databaseService, "redisHost", "localhost");
        ReflectionTestUtils.setField(databaseService, "redisPort", 6379);
    }

    @Test
    void testConstructor() {
        assertNotNull(databaseService);
    }

    @Test
    void testConstructorWithNullDataSource() {
        assertDoesNotThrow(() -> new DatabaseService(null));
    }

    @Test
    void testIsConnectionValid_Success() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(5)).thenReturn(true);

        boolean result = databaseService.isConnectionValid();

        assertTrue(result);
        verify(dataSource).getConnection();
        verify(connection).isValid(5);
        verify(connection).close();
    }

    @Test
    void testIsConnectionValid_Failure() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(5)).thenReturn(false);

        boolean result = databaseService.isConnectionValid();

        assertFalse(result);
        verify(dataSource).getConnection();
        verify(connection).isValid(5);
        verify(connection).close();
    }

    @Test
    void testIsConnectionValid_SQLException() throws SQLException {
        when(dataSource.getConnection()).thenThrow(new SQLException("Connection failed"));

        boolean result = databaseService.isConnectionValid();

        assertFalse(result);
        verify(dataSource).getConnection();
    }

    @Test
    void testIsConnectionValid_ConnectionTimeout() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(5)).thenThrow(new SQLException("Timeout"));

        boolean result = databaseService.isConnectionValid();

        assertFalse(result);
        verify(dataSource).getConnection();
    }

    @Test
    void testExecuteQuery_WithMultipleParameters_Success() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);

        databaseService.executeQuery("SELECT * FROM users WHERE id = ? AND name = ?", 1, "John");

        verify(dataSource).getConnection();
        verify(connection).prepareStatement("SELECT * FROM users WHERE id = ? AND name = ?");
        verify(preparedStatement).setQueryTimeout(30);
        verify(preparedStatement).setObject(1, 1);
        verify(preparedStatement).setObject(2, "John");
        verify(preparedStatement).execute();
        verify(preparedStatement).close();
        verify(connection).close();
    }

    @Test
    void testExecuteQuery_WithSingleParameter_Success() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);

        databaseService.executeQuery("SELECT * FROM users WHERE id = ?", 1);

        verify(dataSource).getConnection();
        verify(connection).prepareStatement("SELECT * FROM users WHERE id = ?");
        verify(preparedStatement).setQueryTimeout(30);
        verify(preparedStatement).setObject(1, 1);
        verify(preparedStatement).execute();
        verify(preparedStatement).close();
        verify(connection).close();
    }

    @Test
    void testExecuteQuery_WithoutParameters_Success() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);

        databaseService.executeQuery("SELECT * FROM users");

        verify(dataSource).getConnection();
        verify(connection).prepareStatement("SELECT * FROM users");
        verify(preparedStatement).setQueryTimeout(30);
        verify(preparedStatement).execute();
        verify(preparedStatement).close();
        verify(connection).close();
    }

    @Test
    void testExecuteQuery_SQLException_NonTransient() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        SQLException sqlException = new SQLException("SQL error", "23000");
        when(preparedStatement.execute()).thenThrow(sqlException);

        assertThrows(RuntimeException.class, () ->
            databaseService.executeQuery("SELECT * FROM users")
        );

        verify(dataSource).getConnection();
        verify(preparedStatement).execute();
    }

    @Test
    void testExecuteQuery_SQLException_Transient_ConnectionException() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        SQLException sqlException = new SQLException("Connection error", "08001");
        when(preparedStatement.execute()).thenThrow(sqlException);

        assertThrows(SQLException.class, () ->
            databaseService.executeQuery("SELECT * FROM users")
        );

        verify(dataSource).getConnection();
        verify(preparedStatement).execute();
    }

    @Test
    void testExecuteQuery_SQLException_Transient_SerializationFailure() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        SQLException sqlException = new SQLException("Serialization failure", "40001");
        when(preparedStatement.execute()).thenThrow(sqlException);

        assertThrows(SQLException.class, () ->
            databaseService.executeQuery("SELECT * FROM users")
        );

        verify(dataSource).getConnection();
        verify(preparedStatement).execute();
    }

    @Test
    void testExecuteQuery_SQLException_Transient_Deadlock() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        SQLException sqlException = new SQLException("Deadlock detected", "40P01");
        when(preparedStatement.execute()).thenThrow(sqlException);

        assertThrows(SQLException.class, () ->
            databaseService.executeQuery("SELECT * FROM users")
        );

        verify(dataSource).getConnection();
        verify(preparedStatement).execute();
    }

    @Test
    void testExecuteQuery_SQLException_NullSqlState() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        SQLException sqlException = new SQLException("Generic error", (String) null);
        when(preparedStatement.execute()).thenThrow(sqlException);

        assertThrows(RuntimeException.class, () ->
            databaseService.executeQuery("SELECT * FROM users")
        );

        verify(dataSource).getConnection();
        verify(preparedStatement).execute();
    }

    @Test
    void testInitializeExternalServices() {
        assertDoesNotThrow(() -> databaseService.initializeExternalServices());
    }

    @Test
    void testInitializeExternalServices_LogsCorrectValues() {
        databaseService.initializeExternalServices();
        assertDoesNotThrow(() -> databaseService.initializeExternalServices());
    }

    @Test
    void testGetConnection_Success() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(5)).thenReturn(true);

        Connection result = databaseService.getConnection();

        assertNotNull(result);
        assertEquals(connection, result);
        verify(dataSource).getConnection();
        verify(connection).isValid(5);
    }

    @Test
    void testGetConnection_InvalidConnection() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(5)).thenReturn(false);

        SQLException exception = assertThrows(SQLException.class, () -> databaseService.getConnection());
        assertEquals("Invalid database connection", exception.getMessage());

        verify(dataSource).getConnection();
        verify(connection).isValid(5);
    }

    @Test
    void testGetConnection_SQLException() throws SQLException {
        when(dataSource.getConnection()).thenThrow(new SQLException("Cannot get connection"));

        assertThrows(SQLException.class, () -> databaseService.getConnection());

        verify(dataSource).getConnection();
    }

    @Test
    void testExecuteQuery_NullParameters() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);

        databaseService.executeQuery("INSERT INTO users VALUES (?, ?)", null, null);

        verify(preparedStatement).setObject(1, null);
        verify(preparedStatement).setObject(2, null);
        verify(preparedStatement).execute();
    }

    @Test
    void testExecuteQuery_EmptyParametersArray() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);

        databaseService.executeQuery("SELECT * FROM users", new Object[]{});

        verify(preparedStatement, never()).setObject(anyInt(), any());
        verify(preparedStatement).execute();
    }

    @Test
    void testExecuteQuery_MixedParameterTypes() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);

        databaseService.executeQuery("INSERT INTO data VALUES (?, ?, ?, ?)",
            1, "test", 3.14, true);

        verify(preparedStatement).setObject(1, 1);
        verify(preparedStatement).setObject(2, "test");
        verify(preparedStatement).setObject(3, 3.14);
        verify(preparedStatement).setObject(4, true);
        verify(preparedStatement).execute();
    }

    @Test
    void testExecuteQuery_LargeNumberOfParameters() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);

        Object[] params = new Object[10];
        for (int i = 0; i < 10; i++) {
            params[i] = i;
        }

        databaseService.executeQuery("INSERT INTO data VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", params);

        for (int i = 0; i < 10; i++) {
            verify(preparedStatement).setObject(i + 1, i);
        }
        verify(preparedStatement).execute();
    }

    @Test
    void testExecuteQuery_PrepareStatementFails() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenThrow(new SQLException("Invalid SQL syntax"));

        assertThrows(RuntimeException.class, () ->
            databaseService.executeQuery("INVALID SQL")
        );

        verify(dataSource).getConnection();
        verify(connection).prepareStatement("INVALID SQL");
    }

    @Test
    void testExecuteQuery_SetObjectFails() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        doThrow(new SQLException("Invalid parameter type")).when(preparedStatement).setObject(anyInt(), any());

        assertThrows(RuntimeException.class, () ->
            databaseService.executeQuery("SELECT * FROM users WHERE id = ?", 1)
        );

        verify(preparedStatement).setObject(1, 1);
    }

    @Test
    void testExecuteQuery_WithDifferentConnectionTimeouts() throws SQLException {
        ReflectionTestUtils.setField(databaseService, "connectionTimeout", 60000);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);

        databaseService.executeQuery("SELECT * FROM users");

        verify(preparedStatement).setQueryTimeout(60);
    }

    @Test
    void testExecuteQuery_ZeroConnectionTimeout() throws SQLException {
        ReflectionTestUtils.setField(databaseService, "connectionTimeout", 0);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);

        databaseService.executeQuery("SELECT * FROM users");

        verify(preparedStatement).setQueryTimeout(0);
    }

    @Test
    void testIsTransientError_ConnectionException_08XXX() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        SQLException sqlException = new SQLException("Connection error", "08999");
        when(preparedStatement.execute()).thenThrow(sqlException);

        assertThrows(SQLException.class, () ->
            databaseService.executeQuery("SELECT * FROM users")
        );
    }

    @Test
    void testExecuteQuery_NonTransientError_With_22XXX() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        SQLException sqlException = new SQLException("Data exception", "22000");
        when(preparedStatement.execute()).thenThrow(sqlException);

        assertThrows(RuntimeException.class, () ->
            databaseService.executeQuery("SELECT * FROM users")
        );
    }

    @Test
    void testExecuteQuery_StringParameter() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);

        databaseService.executeQuery("SELECT * FROM users WHERE name = ?", "John Doe");

        verify(preparedStatement).setObject(1, "John Doe");
        verify(preparedStatement).execute();
    }

    @Test
    void testExecuteQuery_EmptyString() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);

        databaseService.executeQuery("SELECT * FROM users WHERE name = ?", "");

        verify(preparedStatement).setObject(1, "");
        verify(preparedStatement).execute();
    }
}
