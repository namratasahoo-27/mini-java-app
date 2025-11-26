package com.test;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class DatabaseServiceTest {

    @Test
    public void testDatabaseServiceInstantiation() {
        DatabaseService service = new DatabaseService();
        assertNotNull(service);
    }
}
