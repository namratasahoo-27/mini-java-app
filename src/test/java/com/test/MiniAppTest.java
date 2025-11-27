package com.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

class MiniAppTest {

    private MiniApp miniApp;

    @BeforeEach
    void setUp() {
        miniApp = new MiniApp();
    }

    @Test
    void testMiniAppCreation() {
        assertNotNull(miniApp);
    }

    @Test
    void testMainMethodDoesNotThrow() {
        assertDoesNotThrow(() -> {
            String[] args = {};
            MiniApp.main(args);
        });
    }
}
