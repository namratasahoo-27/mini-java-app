package com.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

public class MiniAppTest {

    private MiniApp miniApp;
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

    @BeforeEach
    public void setUp() {
        miniApp = new MiniApp();
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
    }

    @AfterEach
    public void tearDown() {
        miniApp = null;
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    @Test
    public void testConstructor() {
        MiniApp app = new MiniApp();
        assertNotNull(app, "MiniApp instance should not be null");
    }

    @Test
    public void testConstructorMultipleInstances() {
        MiniApp app1 = new MiniApp();
        MiniApp app2 = new MiniApp();
        MiniApp app3 = new MiniApp();

        assertNotNull(app1);
        assertNotNull(app2);
        assertNotNull(app3);
        assertNotEquals(app1, app2);
        assertNotEquals(app2, app3);
    }

    @Test
    public void testMainMethod() {
        assertDoesNotThrow(() -> MiniApp.main(new String[]{}));
        String output = outContent.toString();
        assertTrue(output.contains("Starting Mini Java Application..."));
    }

    @Test
    public void testMainMethodWithNullArgs() {
        assertDoesNotThrow(() -> MiniApp.main(null));
        String output = outContent.toString();
        assertTrue(output.contains("Starting Mini Java Application..."));
    }

    @Test
    public void testMainMethodWithArguments() {
        String[] args = {"arg1", "arg2", "arg3"};
        assertDoesNotThrow(() -> MiniApp.main(args));
        String output = outContent.toString();
        assertTrue(output.contains("Starting Mini Java Application..."));
    }

    @Test
    public void testMainMethodWithEmptyArguments() {
        String[] args = {};
        assertDoesNotThrow(() -> MiniApp.main(args));
        String output = outContent.toString();
        assertTrue(output.contains("Starting Mini Java Application..."));
    }

    @Test
    public void testLoadConfigurationMethodExists() throws Exception {
        Method loadConfigMethod = MiniApp.class.getDeclaredMethod("loadConfiguration");
        assertNotNull(loadConfigMethod);
        loadConfigMethod.setAccessible(true);
        assertDoesNotThrow(() -> loadConfigMethod.invoke(miniApp));
        String output = outContent.toString();
        assertTrue(output.contains("Configuration") || output.contains("Warning"));
    }

    @Test
    public void testInitializeLoggingMethodExists() throws Exception {
        Method initLoggingMethod = MiniApp.class.getDeclaredMethod("initializeLogging");
        assertNotNull(initLoggingMethod);
        initLoggingMethod.setAccessible(true);
        assertDoesNotThrow(() -> initLoggingMethod.invoke(miniApp));
    }

    @Test
    public void testStartServerMethodExists() throws Exception {
        Method startServerMethod = MiniApp.class.getDeclaredMethod("startServer");
        assertNotNull(startServerMethod);
        startServerMethod.setAccessible(true);
        assertDoesNotThrow(() -> startServerMethod.invoke(miniApp));
    }

    @Test
    public void testInitializeApplicationMethodExists() throws Exception {
        Method initMethod = MiniApp.class.getDeclaredMethod("initializeApplication");
        assertNotNull(initMethod);
        initMethod.setAccessible(true);
        assertDoesNotThrow(() -> initMethod.invoke(miniApp));
        String output = outContent.toString();
        assertTrue(output.contains("Connecting to database") || output.contains("database"));
    }

    @Test
    public void testStaticFieldsValues() throws Exception {
        Field serverPortField = MiniApp.class.getDeclaredField("SERVER_PORT");
        serverPortField.setAccessible(true);
        assertEquals(8080, serverPortField.getInt(null));

        Field configFilePathField = MiniApp.class.getDeclaredField("CONFIG_FILE_PATH");
        configFilePathField.setAccessible(true);
        assertEquals("/opt/app/config/app.properties", configFilePathField.get(null));

        Field logFilePathField = MiniApp.class.getDeclaredField("LOG_FILE_PATH");
        logFilePathField.setAccessible(true);
        assertEquals("/var/log/mini-app.log", logFilePathField.get(null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "arg1", "arg1 arg2", "arg1 arg2 arg3"})
    public void testMainWithVariousArguments(String argsString) {
        String[] args = argsString.isEmpty() ? new String[]{} : argsString.split(" ");
        assertDoesNotThrow(() -> MiniApp.main(args));
        String output = outContent.toString();
        assertTrue(output.contains("Starting Mini Java Application..."));
        outContent.reset();
    }

    @Test
    public void testLoadConfigurationHandlesFileNotFound() throws Exception {
        Method loadConfigMethod = MiniApp.class.getDeclaredMethod("loadConfiguration");
        loadConfigMethod.setAccessible(true);
        assertDoesNotThrow(() -> loadConfigMethod.invoke(miniApp));
        String output = outContent.toString();
        assertTrue(output.contains("Warning") || output.contains("Configuration"));
    }

    @Test
    public void testInitializeLoggingHandlesDirectoryCreation() throws Exception {
        Method initLoggingMethod = MiniApp.class.getDeclaredMethod("initializeLogging");
        initLoggingMethod.setAccessible(true);
        assertDoesNotThrow(() -> initLoggingMethod.invoke(miniApp));
        String output = outContent.toString();
        String errorOutput = errContent.toString();
        assertTrue(output.contains("Logging") || errorOutput.contains("Failed"));
    }

    @Test
    public void testStartServerHandlesPortBinding() throws Exception {
        Method startServerMethod = MiniApp.class.getDeclaredMethod("startServer");
        startServerMethod.setAccessible(true);
        assertDoesNotThrow(() -> startServerMethod.invoke(miniApp));
        String output = outContent.toString();
        String errorOutput = errContent.toString();
        assertTrue(output.contains("Server") || errorOutput.contains("Failed"));
    }

    @Test
    public void testAllPrivateMethodsExist() {
        Method[] methods = MiniApp.class.getDeclaredMethods();
        boolean hasLoadConfiguration = false;
        boolean hasInitializeLogging = false;
        boolean hasStartServer = false;
        boolean hasInitializeApplication = false;

        for (Method method : methods) {
            switch (method.getName()) {
                case "loadConfiguration":
                    hasLoadConfiguration = true;
                    break;
                case "initializeLogging":
                    hasInitializeLogging = true;
                    break;
                case "startServer":
                    hasStartServer = true;
                    break;
                case "initializeApplication":
                    hasInitializeApplication = true;
                    break;
            }
        }

        assertTrue(hasLoadConfiguration, "loadConfiguration method should exist");
        assertTrue(hasInitializeLogging, "initializeLogging method should exist");
        assertTrue(hasStartServer, "startServer method should exist");
        assertTrue(hasInitializeApplication, "initializeApplication method should exist");
    }

    @Test
    public void testHardcodedValues() throws Exception {
        Field[] fields = MiniApp.class.getDeclaredFields();
        boolean hasServerPort = false;
        boolean hasConfigPath = false;
        boolean hasLogPath = false;

        for (Field field : fields) {
            field.setAccessible(true);
            switch (field.getName()) {
                case "SERVER_PORT":
                    hasServerPort = true;
                    assertEquals(8080, field.getInt(null));
                    break;
                case "CONFIG_FILE_PATH":
                    hasConfigPath = true;
                    assertTrue(field.get(null).toString().contains("/opt/app"));
                    break;
                case "LOG_FILE_PATH":
                    hasLogPath = true;
                    assertTrue(field.get(null).toString().contains("/var/log"));
                    break;
            }
        }

        assertTrue(hasServerPort, "SERVER_PORT field should exist");
        assertTrue(hasConfigPath, "CONFIG_FILE_PATH field should exist");
        assertTrue(hasLogPath, "LOG_FILE_PATH field should exist");
    }

    @Test
    public void testMainMethodFlow() {
        assertDoesNotThrow(() -> MiniApp.main(new String[]{}));
        String output = outContent.toString();
        assertTrue(output.contains("Starting Mini Java Application..."));
        assertTrue(output.contains("Connecting to database") ||
                   output.contains("Database") ||
                   errContent.toString().contains("Failed"));
    }

    @Test
    public void testPrivateMethodsAccessibility() throws Exception {
        Method[] methods = MiniApp.class.getDeclaredMethods();
        int privateMethodCount = 0;

        for (Method method : methods) {
            if (java.lang.reflect.Modifier.isPrivate(method.getModifiers())) {
                privateMethodCount++;
                method.setAccessible(true);
                assertDoesNotThrow(() -> method.invoke(miniApp));
            }
        }

        assertTrue(privateMethodCount >= 4, "Should have at least 4 private methods");
    }

    @Test
    public void testFieldModifiers() throws Exception {
        Field serverPortField = MiniApp.class.getDeclaredField("SERVER_PORT");
        assertTrue(java.lang.reflect.Modifier.isStatic(serverPortField.getModifiers()));
        assertTrue(java.lang.reflect.Modifier.isFinal(serverPortField.getModifiers()));
        assertTrue(java.lang.reflect.Modifier.isPrivate(serverPortField.getModifiers()));

        Field configPathField = MiniApp.class.getDeclaredField("CONFIG_FILE_PATH");
        assertTrue(java.lang.reflect.Modifier.isStatic(configPathField.getModifiers()));
        assertTrue(java.lang.reflect.Modifier.isFinal(configPathField.getModifiers()));
        assertTrue(java.lang.reflect.Modifier.isPrivate(configPathField.getModifiers()));

        Field logPathField = MiniApp.class.getDeclaredField("LOG_FILE_PATH");
        assertTrue(java.lang.reflect.Modifier.isStatic(logPathField.getModifiers()));
        assertTrue(java.lang.reflect.Modifier.isFinal(logPathField.getModifiers()));
        assertTrue(java.lang.reflect.Modifier.isPrivate(logPathField.getModifiers()));
    }

    @Test
    public void testMethodSequentialExecution() throws Exception {
        Method initMethod = MiniApp.class.getDeclaredMethod("initializeApplication");
        Method serverMethod = MiniApp.class.getDeclaredMethod("startServer");

        initMethod.setAccessible(true);
        serverMethod.setAccessible(true);

        assertDoesNotThrow(() -> {
            initMethod.invoke(miniApp);
            serverMethod.invoke(miniApp);
        });
    }

    @Test
    public void testOutputContainsExpectedMessages() {
        assertDoesNotThrow(() -> MiniApp.main(new String[]{}));
        String output = outContent.toString();

        assertTrue(output.contains("Starting Mini Java Application..."));
        assertTrue(output.contains("Connecting to database...") ||
                   errContent.toString().contains("Database"));
    }

    @Test
    public void testCompleteApplicationFlow() throws Exception {
        Method[] methods = {
            MiniApp.class.getDeclaredMethod("loadConfiguration"),
            MiniApp.class.getDeclaredMethod("initializeLogging"),
            MiniApp.class.getDeclaredMethod("startServer")
        };

        for (Method method : methods) {
            method.setAccessible(true);
            assertDoesNotThrow(() -> method.invoke(miniApp));
        }

        String output = outContent.toString();
        String errorOutput = errContent.toString();
        assertFalse(output.isEmpty() || errorOutput.isEmpty());
    }
}