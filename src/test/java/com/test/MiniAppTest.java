package com.test;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class MiniAppTest {

    @Test
    public void testMiniAppInstantiation() {
        MiniApp app = new MiniApp();
        assertNotNull(app);
    }
}
