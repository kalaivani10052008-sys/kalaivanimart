package com.kalaivanimart;

import com.kalaivanimart.config.DatabaseConfig;
import com.kalaivanimart.dao.UserDao;
import com.kalaivanimart.model.User;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class UserDaoTest {

    private static UserDao userDao;

    @BeforeAll
    static void setUp() {
        DatabaseConfig.initializeDatabase();
        userDao = new UserDao();
    }

    @Test
    @DisplayName("Should retrieve seeded admin by email")
    void testGetAdminByEmail() {
        User admin = userDao.getUserByEmail("admin@kalaivanimart.com");
        assertNotNull(admin, "Admin account should exist in database");
        assertEquals("admin", admin.getRole().toLowerCase());
        assertNotNull(admin.getPassword());
    }

    @Test
    @DisplayName("Should retrieve seeded customer by email")
    void testGetCustomerByEmail() {
        User customer = userDao.getUserByEmail("customer@kalaivanimart.com");
        assertNotNull(customer, "Customer account should exist in database");
        assertEquals("buyer", customer.getRole().toLowerCase());
    }

    @Test
    @DisplayName("Should return null for non-existent user email")
    void testNonExistentUser() {
        User notFound = userDao.getUserByEmail("nonexistent_user_999@example.com");
        assertNull(notFound);
    }

    @Test
    @DisplayName("Should retrieve list of all registered users")
    void testGetAllUsers() {
        List<User> users = userDao.getAllUsers();
        assertNotNull(users);
        assertTrue(users.size() >= 3, "Should have admin, buyer, and seller registered");
        System.out.println(">>> Total Registered Accounts in DB: " + users.size());
    }
}
