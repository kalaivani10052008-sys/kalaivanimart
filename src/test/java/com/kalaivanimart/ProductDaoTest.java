package com.kalaivanimart;

import com.kalaivanimart.config.DatabaseConfig;
import com.kalaivanimart.dao.ProductDao;
import com.kalaivanimart.model.Product;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ProductDaoTest {

    private static ProductDao productDao;

    @BeforeAll
    static void setUp() {
        DatabaseConfig.initializeDatabase();
        productDao = new ProductDao();
    }

    @Test
    @DisplayName("Should retrieve seeded products count greater than zero")
    void testTotalProductsCount() {
        int count = productDao.getTotalProductsCount();
        System.out.println(">>> Total Products in Catalog: " + count);
        assertTrue(count >= 15, "Should have at least 15 seeded products in catalog");
    }

    @Test
    @DisplayName("Should filter products by category accurately")
    void testGetProductsByCategory() {
        List<Product> electronics = productDao.getAllProducts("electronics", null);
        assertNotNull(electronics);
        assertFalse(electronics.isEmpty());
        for (Product p : electronics) {
            assertEquals("electronics", p.getCategory().toLowerCase());
        }
    }

    @Test
    @DisplayName("Should search products by keyword in name or description")
    void testSearchProducts() {
        List<Product> results = productDao.getAllProducts(null, "headphones");
        assertNotNull(results);
        assertFalse(results.isEmpty());
        assertTrue(results.get(0).getName().toLowerCase().contains("headphones") ||
                   results.get(0).getDescription().toLowerCase().contains("headphones"));
    }

    @Test
    @DisplayName("Should retrieve single product by valid ID")
    void testGetProductById() {
        List<Product> all = productDao.getAllProducts(null, null);
        assertFalse(all.isEmpty());
        int firstId = all.get(0).getId();

        Product p = productDao.getProductById(firstId);
        assertNotNull(p);
        assertEquals(firstId, p.getId());
        assertTrue(p.getPrice() > 0);
    }
}
