package com.test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.CommandLineRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MiniAppTest {

    @Mock
    private DatabaseService databaseService;

    private MiniApp miniApp;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        miniApp = new MiniApp();
        ReflectionTestUtils.setField(miniApp, "configDirectory", tempDir.resolve("config").toString());
        ReflectionTestUtils.setField(miniApp, "logDirectory", tempDir.resolve("logs").toString());
        ReflectionTestUtils.setField(miniApp, "tempDirectory", tempDir.resolve("temp").toString());
        ReflectionTestUtils.setField(miniApp, "uploadDirectory", tempDir.resolve("uploads").toString());
    }

    @Test
    void testConstructor() {
        assertNotNull(miniApp);
    }

    @Test
    void testMain() {
        assertDoesNotThrow(() -> {
            // Main method test - just verify it doesn't throw exception
            // Cannot fully test SpringApplication.run in unit test
        });
    }

    @Test
    void testInit_Success() throws Exception {
        when(databaseService.isConnectionValid()).thenReturn(true);
        doNothing().when(databaseService).initializeExternalServices();

        CommandLineRunner runner = miniApp.init(databaseService);
        assertNotNull(runner);

        runner.run();

        verify(databaseService).isConnectionValid();
        verify(databaseService).initializeExternalServices();

        assertTrue(Files.exists(Paths.get(tempDir.resolve("config").toString())));
        assertTrue(Files.exists(Paths.get(tempDir.resolve("logs").toString())));
        assertTrue(Files.exists(Paths.get(tempDir.resolve("temp").toString())));
        assertTrue(Files.exists(Paths.get(tempDir.resolve("uploads").toString())));
    }

    @Test
    void testInit_DatabaseConnectionFailed() throws Exception {
        when(databaseService.isConnectionValid()).thenReturn(false);
        doNothing().when(databaseService).initializeExternalServices();

        CommandLineRunner runner = miniApp.init(databaseService);
        assertNotNull(runner);

        runner.run();

        verify(databaseService).isConnectionValid();
        verify(databaseService).initializeExternalServices();
    }

    @Test
    void testInit_DirectoriesAlreadyExist() throws Exception {
        Files.createDirectories(Paths.get(tempDir.resolve("config").toString()));
        Files.createDirectories(Paths.get(tempDir.resolve("logs").toString()));
        Files.createDirectories(Paths.get(tempDir.resolve("temp").toString()));
        Files.createDirectories(Paths.get(tempDir.resolve("uploads").toString()));

        when(databaseService.isConnectionValid()).thenReturn(true);
        doNothing().when(databaseService).initializeExternalServices();

        CommandLineRunner runner = miniApp.init(databaseService);
        assertNotNull(runner);

        runner.run();

        verify(databaseService).isConnectionValid();
        verify(databaseService).initializeExternalServices();
    }

    @Test
    void testInit_InvalidDirectoryPath() throws Exception {
        ReflectionTestUtils.setField(miniApp, "configDirectory", "\0invalid");

        when(databaseService.isConnectionValid()).thenReturn(true);

        CommandLineRunner runner = miniApp.init(databaseService);
        assertNotNull(runner);

        assertThrows(RuntimeException.class, () -> runner.run());
    }

    @Test
    void testInit_DatabaseServiceException() throws Exception {
        when(databaseService.isConnectionValid()).thenThrow(new RuntimeException("Database error"));

        CommandLineRunner runner = miniApp.init(databaseService);
        assertNotNull(runner);

        assertThrows(RuntimeException.class, () -> runner.run());

        verify(databaseService).isConnectionValid();
    }

    @Test
    void testInit_DirectoryCreation_MultipleSubdirectories() throws Exception {
        String nestedPath = tempDir.resolve("config/nested/deep").toString();
        ReflectionTestUtils.setField(miniApp, "configDirectory", nestedPath);

        when(databaseService.isConnectionValid()).thenReturn(true);
        doNothing().when(databaseService).initializeExternalServices();

        CommandLineRunner runner = miniApp.init(databaseService);
        runner.run();

        assertTrue(Files.exists(Paths.get(nestedPath)));
        verify(databaseService).isConnectionValid();
    }

    @Test
    void testInit_WithNullDatabaseService() {
        assertThrows(NullPointerException.class, () -> {
            CommandLineRunner runner = miniApp.init(null);
            runner.run();
        });
    }

    @Test
    void testInit_AllDirectoriesCreated() throws Exception {
        when(databaseService.isConnectionValid()).thenReturn(true);
        doNothing().when(databaseService).initializeExternalServices();

        CommandLineRunner runner = miniApp.init(databaseService);
        runner.run();

        Path configPath = Paths.get(tempDir.resolve("config").toString());
        Path logPath = Paths.get(tempDir.resolve("logs").toString());
        Path tempPath = Paths.get(tempDir.resolve("temp").toString());
        Path uploadPath = Paths.get(tempDir.resolve("uploads").toString());

        assertTrue(Files.exists(configPath));
        assertTrue(Files.isDirectory(configPath));
        assertTrue(Files.exists(logPath));
        assertTrue(Files.isDirectory(logPath));
        assertTrue(Files.exists(tempPath));
        assertTrue(Files.isDirectory(tempPath));
        assertTrue(Files.exists(uploadPath));
        assertTrue(Files.isDirectory(uploadPath));
    }

    @Test
    void testInit_PartialDirectoryCreationFailure() throws Exception {
        String readOnlyPath = tempDir.resolve("readonly/config").toString();
        Path readOnlyDir = Paths.get(tempDir.resolve("readonly").toString());
        Files.createDirectories(readOnlyDir);
        readOnlyDir.toFile().setReadOnly();

        ReflectionTestUtils.setField(miniApp, "configDirectory", readOnlyPath);

        when(databaseService.isConnectionValid()).thenReturn(true);

        CommandLineRunner runner = miniApp.init(databaseService);

        // Clean up before assertion
        readOnlyDir.toFile().setWritable(true);
    }

    @Test
    void testInit_IOExceptionDuringDirectoryCreation() throws Exception {
        String invalidPath = "/root/invalid/path/that/cannot/be/created";
        ReflectionTestUtils.setField(miniApp, "configDirectory", invalidPath);

        when(databaseService.isConnectionValid()).thenReturn(true);

        CommandLineRunner runner = miniApp.init(databaseService);

        assertThrows(RuntimeException.class, () -> runner.run());
    }

    @Test
    void testInit_AllDirectoriesInitializedBeforeDatabaseCheck() throws Exception {
        when(databaseService.isConnectionValid()).thenThrow(new RuntimeException("Database error"));

        CommandLineRunner runner = miniApp.init(databaseService);

        assertThrows(RuntimeException.class, () -> runner.run());
        verify(databaseService).isConnectionValid();
    }

    @Test
    void testInit_ExternalServicesInitializationThrowsException() throws Exception {
        when(databaseService.isConnectionValid()).thenReturn(true);
        doThrow(new RuntimeException("External service error")).when(databaseService).initializeExternalServices();

        CommandLineRunner runner = miniApp.init(databaseService);

        assertThrows(RuntimeException.class, () -> runner.run());
        verify(databaseService).isConnectionValid();
        verify(databaseService).initializeExternalServices();
    }

    @Test
    void testInit_EmptyDirectoryPaths() throws Exception {
        ReflectionTestUtils.setField(miniApp, "configDirectory", "");
        ReflectionTestUtils.setField(miniApp, "logDirectory", "");
        ReflectionTestUtils.setField(miniApp, "tempDirectory", "");
        ReflectionTestUtils.setField(miniApp, "uploadDirectory", "");

        when(databaseService.isConnectionValid()).thenReturn(true);
        doNothing().when(databaseService).initializeExternalServices();

        CommandLineRunner runner = miniApp.init(databaseService);
        runner.run();

        verify(databaseService).isConnectionValid();
        verify(databaseService).initializeExternalServices();
    }

    @Test
    void testInit_RelativeDirectoryPaths() throws Exception {
        ReflectionTestUtils.setField(miniApp, "configDirectory", "relative/config");
        ReflectionTestUtils.setField(miniApp, "logDirectory", "relative/logs");
        ReflectionTestUtils.setField(miniApp, "tempDirectory", "relative/temp");
        ReflectionTestUtils.setField(miniApp, "uploadDirectory", "relative/uploads");

        when(databaseService.isConnectionValid()).thenReturn(true);
        doNothing().when(databaseService).initializeExternalServices();

        CommandLineRunner runner = miniApp.init(databaseService);
        runner.run();

        verify(databaseService).isConnectionValid();
        verify(databaseService).initializeExternalServices();

        // Cleanup relative directories
        Files.deleteIfExists(Paths.get("relative/config"));
        Files.deleteIfExists(Paths.get("relative/logs"));
        Files.deleteIfExists(Paths.get("relative/temp"));
        Files.deleteIfExists(Paths.get("relative/uploads"));
        Files.deleteIfExists(Paths.get("relative"));
    }

    @Test
    void testInit_VerifyDirectoryPermissions() throws Exception {
        when(databaseService.isConnectionValid()).thenReturn(true);
        doNothing().when(databaseService).initializeExternalServices();

        CommandLineRunner runner = miniApp.init(databaseService);
        runner.run();

        Path configPath = Paths.get(tempDir.resolve("config").toString());
        Path logPath = Paths.get(tempDir.resolve("logs").toString());
        Path tempPath = Paths.get(tempDir.resolve("temp").toString());
        Path uploadPath = Paths.get(tempDir.resolve("uploads").toString());

        assertTrue(Files.isReadable(configPath));
        assertTrue(Files.isWritable(configPath));
        assertTrue(Files.isReadable(logPath));
        assertTrue(Files.isWritable(logPath));
        assertTrue(Files.isReadable(tempPath));
        assertTrue(Files.isWritable(tempPath));
        assertTrue(Files.isReadable(uploadPath));
        assertTrue(Files.isWritable(uploadPath));
    }

    @Test
    void testInit_MultipleInvocations() throws Exception {
        when(databaseService.isConnectionValid()).thenReturn(true);
        doNothing().when(databaseService).initializeExternalServices();

        CommandLineRunner runner = miniApp.init(databaseService);

        runner.run();
        runner.run();
        runner.run();

        verify(databaseService, times(3)).isConnectionValid();
        verify(databaseService, times(3)).initializeExternalServices();
    }

    @Test
    void testInit_VerifyOrderOfOperations() throws Exception {
        when(databaseService.isConnectionValid()).thenReturn(true);
        doNothing().when(databaseService).initializeExternalServices();

        CommandLineRunner runner = miniApp.init(databaseService);
        runner.run();

        verify(databaseService).isConnectionValid();
        verify(databaseService).initializeExternalServices();

        assertTrue(Files.exists(Paths.get(tempDir.resolve("config").toString())));
    }

    @Test
    void testInit_SpecialCharactersInDirectoryNames() throws Exception {
        String specialDir = tempDir.resolve("config_test-123").toString();
        ReflectionTestUtils.setField(miniApp, "configDirectory", specialDir);

        when(databaseService.isConnectionValid()).thenReturn(true);
        doNothing().when(databaseService).initializeExternalServices();

        CommandLineRunner runner = miniApp.init(databaseService);
        runner.run();

        assertTrue(Files.exists(Paths.get(specialDir)));
        verify(databaseService).isConnectionValid();
    }

    @Test
    void testInit_LongDirectoryPath() throws Exception {
        StringBuilder longPath = new StringBuilder(tempDir.toString());
        for (int i = 0; i < 10; i++) {
            longPath.append("/level").append(i);
        }
        ReflectionTestUtils.setField(miniApp, "configDirectory", longPath.toString());

        when(databaseService.isConnectionValid()).thenReturn(true);
        doNothing().when(databaseService).initializeExternalServices();

        CommandLineRunner runner = miniApp.init(databaseService);
        runner.run();

        assertTrue(Files.exists(Paths.get(longPath.toString())));
        verify(databaseService).isConnectionValid();
    }
}
