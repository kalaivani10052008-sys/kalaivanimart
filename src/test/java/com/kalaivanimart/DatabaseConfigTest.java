package com.kalaivanimart;

import com.kalaivanimart.config.DatabaseConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.*;

public class DatabaseConfigTest {

    @BeforeAll
    static void setUpDatabase() {
        System.out.println(">>> Testing KalaivaniMart Database Connection & Health...");
        DatabaseConfig.initializeDatabase();
    }

    @Test
    @DisplayName("Should establish database connection and pass health check")
    void testDatabaseHealth() {
        boolean healthy = DatabaseConfig.checkHealth();
        System.out.println(">>> DB Health: " + (healthy ? "HEALTHY (SUCCESS)" : "FAILED"));
        assertTrue(healthy, "Database health check should return true");
    }

    @Test
    @DisplayName("Should return valid database product name")
    void testDatabaseProductName() {
        String name = DatabaseConfig.getDatabaseProductName();
        System.out.println(">>> DB Engine: " + name);
        assertNotNull(name);
        assertNotEquals("Unknown", name);
    }

    @Test
    @DisplayName("Should retrieve a valid open JDBC Connection")
    void testGetConnection() throws Exception {
        try (Connection conn = DatabaseConfig.getConnection()) {
            assertNotNull(conn);
            assertFalse(conn.isClosed());
        }
    }
}
