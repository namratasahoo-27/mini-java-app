package com.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class DatabaseServiceTest {

    private DatabaseService databaseService;

    @BeforeEach
    public void setUp() {
        databaseService = new DatabaseService();
    }

    @AfterEach
    public void tearDown() {
        databaseService = null;
    }

    @Test
    public void testConstructor() {
        DatabaseService service = new DatabaseService();
        assertNotNull(service, "DatabaseService instance should not be null");
    }

    @Test
    public void testConnectSuccessful() throws Exception {
        try (MockedStatic<Class> mockedClass = mockStatic(Class.class);
             MockedStatic<DriverManager> mockedDriverManager = mockStatic(DriverManager.class)) {

            Connection mockConnection = mock(Connection.class);

            // Mock Class.forName
            mockedClass.when(() -> Class.forName("com.mysql.cj.jdbc.Driver"))
                     .thenReturn(null);

            // Mock DriverManager.getConnection
            mockedDriverManager.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                              .thenReturn(mockConnection);

            assertDoesNotThrow(() -> databaseService.connect());

            // Verify Class.forName was called
            mockedClass.verify(() -> Class.forName("com.mysql.cj.jdbc.Driver"));

            // Verify DriverManager.getConnection was called
            mockedDriverManager.verify(() -> DriverManager.getConnection(
                eq("jdbc:mysql://localhost:3306/mini_app_db"),
                eq("root"),
                eq("password123")));
        }
    }

    @Test
    public void testConnectClassNotFoundException() throws Exception {
        try (MockedStatic<Class> mockedClass = mockStatic(Class.class)) {
            // Mock Class.forName to throw ClassNotFoundException
            mockedClass.when(() -> Class.forName("com.mysql.cj.jdbc.Driver"))
                     .thenThrow(new ClassNotFoundException("Driver not found"));

            assertDoesNotThrow(() -> databaseService.connect());

            // Verify Class.forName was called
            mockedClass.verify(() -> Class.forName("com.mysql.cj.jdbc.Driver"));
        }
    }

    @Test
    public void testConnectSQLException() throws Exception {
        try (MockedStatic<Class> mockedClass = mockStatic(Class.class);
             MockedStatic<DriverManager> mockedDriverManager = mockStatic(DriverManager.class)) {

            // Mock Class.forName
            mockedClass.when(() -> Class.forName("com.mysql.cj.jdbc.Driver"))
                     .thenReturn(null);

            // Mock DriverManager.getConnection to throw SQLException
            mockedDriverManager.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                              .thenThrow(new SQLException("Connection failed"));

            assertDoesNotThrow(() -> databaseService.connect());

            // Verify Class.forName was called
            mockedClass.verify(() -> Class.forName("com.mysql.cj.jdbc.Driver"));

            // Verify DriverManager.getConnection was called
            mockedDriverManager.verify(() -> DriverManager.getConnection(
                eq("jdbc:mysql://localhost:3306/mini_app_db"),
                eq("root"),
                eq("password123")));
        }
    }

    @Test
    public void testExecuteQueryWithValidConnection() throws Exception {
        Connection mockConnection = mock(Connection.class);
        PreparedStatement mockStatement = mock(PreparedStatement.class);

        // Setup mock connection and statement
        when(mockConnection.isClosed()).thenReturn(false);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);

        // Use reflection to set the connection field
        Field connectionField = DatabaseService.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(databaseService, mockConnection);

        String testSql = "SELECT * FROM users";
        assertDoesNotThrow(() -> databaseService.executeQuery(testSql));

        verify(mockConnection).prepareStatement(testSql);
        verify(mockStatement).setQueryTimeout(30);
        verify(mockStatement).execute();
        verify(mockStatement).close();
    }

    @Test
    public void testExecuteQueryWithClosedConnection() throws Exception {
        Connection mockConnection = mock(Connection.class);

        // Setup mock connection to be closed
        when(mockConnection.isClosed()).thenReturn(true);

        // Use reflection to set the connection field
        Field connectionField = DatabaseService.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(databaseService, mockConnection);

        String testSql = "SELECT * FROM users";
        assertDoesNotThrow(() -> databaseService.executeQuery(testSql));

        verify(mockConnection).isClosed();
        verify(mockConnection, never()).prepareStatement(anyString());
    }

    @Test
    public void testExecuteQueryWithNullConnection() {
        String testSql = "SELECT * FROM users";
        assertDoesNotThrow(() -> databaseService.executeQuery(testSql));
    }

    @Test
    public void testExecuteQuerySQLException() throws Exception {
        Connection mockConnection = mock(Connection.class);

        // Setup mock connection and statement
        when(mockConnection.isClosed()).thenReturn(false);
        when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("SQL Error"));

        // Use reflection to set the connection field
        Field connectionField = DatabaseService.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(databaseService, mockConnection);

        String testSql = "SELECT * FROM users";
        assertDoesNotThrow(() -> databaseService.executeQuery(testSql));

        verify(mockConnection).prepareStatement(testSql);
    }

    @ParameterizedTest
    @ValueSource(strings = {"SELECT * FROM users", "INSERT INTO users VALUES (1, 'test')", "UPDATE users SET name='test'", "DELETE FROM users WHERE id=1"})
    public void testExecuteQueryWithVariousStatements(String sql) throws Exception {
        Connection mockConnection = mock(Connection.class);
        PreparedStatement mockStatement = mock(PreparedStatement.class);

        // Setup mock connection and statement
        when(mockConnection.isClosed()).thenReturn(false);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);

        // Use reflection to set the connection field
        Field connectionField = DatabaseService.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(databaseService, mockConnection);

        assertDoesNotThrow(() -> databaseService.executeQuery(sql));

        verify(mockConnection).prepareStatement(sql);
        verify(mockStatement).setQueryTimeout(30);
        verify(mockStatement).execute();
        verify(mockStatement).close();
    }

    @Test
    public void testDisconnectSuccessful() throws Exception {
        Connection mockConnection = mock(Connection.class);

        // Setup mock connection
        when(mockConnection.isClosed()).thenReturn(false);

        // Use reflection to set the connection field
        Field connectionField = DatabaseService.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(databaseService, mockConnection);

        assertDoesNotThrow(() -> databaseService.disconnect());

        verify(mockConnection).isClosed();
        verify(mockConnection).close();
    }

    @Test
    public void testDisconnectWithClosedConnection() throws Exception {
        Connection mockConnection = mock(Connection.class);

        // Setup mock connection to be closed
        when(mockConnection.isClosed()).thenReturn(true);

        // Use reflection to set the connection field
        Field connectionField = DatabaseService.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(databaseService, mockConnection);

        assertDoesNotThrow(() -> databaseService.disconnect());

        verify(mockConnection).isClosed();
        verify(mockConnection, never()).close();
    }

    @Test
    public void testDisconnectWithNullConnection() {
        assertDoesNotThrow(() -> databaseService.disconnect());
    }

    @Test
    public void testDisconnectSQLException() throws Exception {
        Connection mockConnection = mock(Connection.class);

        // Setup mock connection to throw SQLException when checking isClosed
        when(mockConnection.isClosed()).thenThrow(new SQLException("Connection error"));

        // Use reflection to set the connection field
        Field connectionField = DatabaseService.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(databaseService, mockConnection);

        assertDoesNotThrow(() -> databaseService.disconnect());

        verify(mockConnection).isClosed();
        verify(mockConnection, never()).close();
    }

    @Test
    public void testDisconnectCloseException() throws Exception {
        Connection mockConnection = mock(Connection.class);

        // Setup mock connection
        when(mockConnection.isClosed()).thenReturn(false);
        doThrow(new SQLException("Close error")).when(mockConnection).close();

        // Use reflection to set the connection field
        Field connectionField = DatabaseService.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(databaseService, mockConnection);

        assertDoesNotThrow(() -> databaseService.disconnect());

        verify(mockConnection).isClosed();
        verify(mockConnection).close();
    }

    @Test
    public void testStaticFieldsValues() throws Exception {
        // Test hardcoded constant values using reflection
        Field dbHostField = DatabaseService.class.getDeclaredField("DB_HOST");
        dbHostField.setAccessible(true);
        assertEquals("localhost", dbHostField.get(null));

        Field dbPortField = DatabaseService.class.getDeclaredField("DB_PORT");
        dbPortField.setAccessible(true);
        assertEquals("3306", dbPortField.get(null));

        Field dbNameField = DatabaseService.class.getDeclaredField("DB_NAME");
        dbNameField.setAccessible(true);
        assertEquals("mini_app_db", dbNameField.get(null));

        Field dbUsernameField = DatabaseService.class.getDeclaredField("DB_USERNAME");
        dbUsernameField.setAccessible(true);
        assertEquals("root", dbUsernameField.get(null));

        Field dbPasswordField = DatabaseService.class.getDeclaredField("DB_PASSWORD");
        dbPasswordField.setAccessible(true);
        assertEquals("password123", dbPasswordField.get(null));

        Field redisHostField = DatabaseService.class.getDeclaredField("REDIS_HOST");
        redisHostField.setAccessible(true);
        assertEquals("127.0.0.1", redisHostField.get(null));

        Field redisPortField = DatabaseService.class.getDeclaredField("REDIS_PORT");
        redisPortField.setAccessible(true);
        assertEquals(6379, redisPortField.getInt(null));
    }
}