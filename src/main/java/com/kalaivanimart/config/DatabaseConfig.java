package com.kalaivanimart.config;

import com.kalaivanimart.util.PasswordUtil;

import java.io.InputStream;
import java.sql.*;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseConfig {

    private static final Logger LOGGER = Logger.getLogger(DatabaseConfig.class.getName());

    private static String dbUrl;
    private static String dbUser;
    private static String dbPassword;
    private static String dbType;
    private static boolean isInitialized = false;
    // Once primary DB fails, skip retrying it on every request to avoid log spam
    private static volatile boolean primaryDbFailed = false;

    static {
        loadConfiguration();
    }

    private static void loadConfiguration() {
        Properties props = new Properties();
        try (InputStream is = DatabaseConfig.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (is != null) {
                props.load(is);
            }
        } catch (Exception ignored) {}

        // Read from Environment variables first, then System properties, then properties file, then defaults
        dbUrl = getEnvOrProp("DB_URL", props.getProperty("db.url", null));
        dbUser = getEnvOrProp("DB_USERNAME", props.getProperty("db.username", "root"));
        dbPassword = getEnvOrProp("DB_PASSWORD", props.getProperty("db.password", ""));
        dbType = getEnvOrProp("DB_TYPE", props.getProperty("db.type", "mysql"));

        LOGGER.info("DatabaseConfig loaded. DB_TYPE: " + dbType + ", Target URL configured: " + (dbUrl != null));
    }

    private static String getEnvOrProp(String key, String defaultVal) {
        String val = System.getenv(key);
        if (val == null || val.trim().isEmpty()) {
            val = System.getProperty(key);
        }
        if (val == null || val.trim().isEmpty()) {
            return defaultVal;
        }
        return val.trim();
    }

    public static Connection getConnection() throws SQLException {
        synchronized (DatabaseConfig.class) {
            if (!isInitialized) {
                initializeDatabase();
                isInitialized = true;
            }
        }

        // Skip retrying primary DB if it already failed (avoids log spam per request)
        if (primaryDbFailed) {
            return getFallbackConnection();
        }

        // Try primary configured database
        if (dbUrl != null && !dbUrl.trim().isEmpty()) {
            try {
                if ("mysql".equalsIgnoreCase(dbType) || dbUrl.contains("mysql")) {
                    Class.forName("com.mysql.cj.jdbc.Driver");
                }
                return DriverManager.getConnection(dbUrl, dbUser != null ? dbUser : "", dbPassword != null ? dbPassword : "");
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Primary DB unavailable (" + e.getMessage().split("\n")[0] + "). Using embedded H2 fallback.");
                primaryDbFailed = true;
                return getFallbackConnection();
            }
        }

        // Default local MySQL try
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            String defaultLocalUrl = "jdbc:mysql://localhost:3306/kalaivanimart?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
            return DriverManager.getConnection(defaultLocalUrl, dbUser != null ? dbUser : "root", dbPassword != null ? dbPassword : "");
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Local MySQL unavailable. Switching to H2 embedded fallback.");
            primaryDbFailed = true;
            return getFallbackConnection();
        }
    }

    private static Connection getFallbackConnection() throws SQLException {
        try {
            Class.forName("org.h2.Driver");
            // File-persisted H2 with MySQL syntax compatibility
            String home = System.getProperty("user.home");
            String h2Url = "jdbc:h2:" + home.replace("\\", "/") + "/.kalaivanimart_db;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1;AUTO_SERVER=TRUE";
            return DriverManager.getConnection(h2Url, "sa", "");
        } catch (ClassNotFoundException e) {
            throw new SQLException("Database driver not found", e);
        }
    }

    public static synchronized void initializeDatabase() {
        try (Connection conn = getConnectionInternal()) {
            LOGGER.info("Connected to database. Verifying and migrating KalaivaniMart schema...");
            createTables(conn);
            seedInitialData(conn);
            LOGGER.info("KalaivaniMart database schema ready.");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Database initialization error: " + e.getMessage(), e);
        }
    }

    private static Connection getConnectionInternal() throws SQLException {
        if (dbUrl != null && !dbUrl.trim().isEmpty()) {
            try {
                if ("mysql".equalsIgnoreCase(dbType) || dbUrl.contains("mysql")) {
                    Class.forName("com.mysql.cj.jdbc.Driver");
                }
                return DriverManager.getConnection(dbUrl, dbUser != null ? dbUser : "", dbPassword != null ? dbPassword : "");
            } catch (Exception e) {
                return getFallbackConnection();
            }
        }
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            String defaultLocalUrl = "jdbc:mysql://localhost:3306/kalaivanimart?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
            return DriverManager.getConnection(defaultLocalUrl, dbUser != null ? dbUser : "root", dbPassword != null ? dbPassword : "");
        } catch (Exception ex) {
            return getFallbackConnection();
        }
    }

    private static void createTables(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            // Users table
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS users (" +
                "  id INT AUTO_INCREMENT PRIMARY KEY," +
                "  name VARCHAR(150) NOT NULL," +
                "  email VARCHAR(191) NOT NULL UNIQUE," +
                "  password VARCHAR(255) NOT NULL," +
                "  phone VARCHAR(30)," +
                "  address TEXT," +
                "  city VARCHAR(100)," +
                "  state VARCHAR(100)," +
                "  pincode VARCHAR(20)," +
                "  role VARCHAR(50) DEFAULT 'buyer'," +
                "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")"
            );

            // Categories table
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS categories (" +
                "  id INT AUTO_INCREMENT PRIMARY KEY," +
                "  name VARCHAR(100) NOT NULL UNIQUE," +
                "  slug VARCHAR(100) NOT NULL UNIQUE," +
                "  icon VARCHAR(50)," +
                "  description TEXT" +
                ")"
            );

            // Products table
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS products (" +
                "  id INT AUTO_INCREMENT PRIMARY KEY," +
                "  name VARCHAR(255) NOT NULL," +
                "  description TEXT," +
                "  category VARCHAR(100) NOT NULL," +
                "  price DECIMAL(10,2) NOT NULL," +
                "  old_price DECIMAL(10,2) DEFAULT 0.00," +
                "  stock INT NOT NULL DEFAULT 0," +
                "  image TEXT," +
                "  seller_id INT DEFAULT 1," +
                "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")"
            );

            // Cart table
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS cart_items (" +
                "  id INT AUTO_INCREMENT PRIMARY KEY," +
                "  user_id INT NOT NULL," +
                "  product_id INT NOT NULL," +
                "  quantity INT NOT NULL DEFAULT 1," +
                "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "  CONSTRAINT uk_user_product UNIQUE (user_id, product_id)" +
                ")"
            );

            // Orders table
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS orders (" +
                "  id INT AUTO_INCREMENT PRIMARY KEY," +
                "  user_id INT NOT NULL," +
                "  total DECIMAL(10,2) NOT NULL," +
                "  status VARCHAR(50) DEFAULT 'Pending'," +
                "  address TEXT NOT NULL," +
                "  payment_method VARCHAR(50) DEFAULT 'COD'," +
                "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")"
            );

            // Order items table
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS order_items (" +
                "  id INT AUTO_INCREMENT PRIMARY KEY," +
                "  order_id INT NOT NULL," +
                "  product_id INT NOT NULL," +
                "  product_name VARCHAR(255) NOT NULL," +
                "  product_image TEXT," +
                "  price DECIMAL(10,2) NOT NULL," +
                "  quantity INT NOT NULL" +
                ")"
            );

            // Wishlist table
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS wishlist (" +
                "  id INT AUTO_INCREMENT PRIMARY KEY," +
                "  user_id INT NOT NULL," +
                "  product_id INT NOT NULL," +
                "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "  CONSTRAINT uk_wishlist UNIQUE (user_id, product_id)" +
                ")"
            );

            // Reviews table
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS reviews (" +
                "  id INT AUTO_INCREMENT PRIMARY KEY," +
                "  product_id INT NOT NULL," +
                "  user_id INT NOT NULL," +
                "  rating INT NOT NULL," +
                "  comment TEXT," +
                "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")"
            );
        }
    }

    private static void seedInitialData(Connection conn) throws SQLException {
        // Seed Admin user if no admin exists
        try (PreparedStatement checkAdmin = conn.prepareStatement("SELECT COUNT(*) FROM users WHERE role = 'admin'")) {
            ResultSet rs = checkAdmin.executeQuery();
            if (rs.next() && rs.getInt(1) == 0) {
                String adminPass = PasswordUtil.hashPassword("Admin@123");
                try (PreparedStatement insertAdmin = conn.prepareStatement(
                    "INSERT INTO users (name, email, password, role, city, state) VALUES (?, ?, ?, 'admin', 'Chennai', 'Tamil Nadu')"
                )) {
                    insertAdmin.setString(1, "Kalaivani Admin");
                    insertAdmin.setString(2, "admin@kalaivanimart.com");
                    insertAdmin.setString(3, adminPass);
                    insertAdmin.executeUpdate();
                    LOGGER.info("Default KalaivaniMart admin account created: admin@kalaivanimart.com");
                }
            }
        }

        // Seed Demo Buyer if no buyers exist
        try (PreparedStatement checkBuyer = conn.prepareStatement("SELECT COUNT(*) FROM users WHERE email = 'customer@kalaivanimart.com'")) {
            ResultSet rs = checkBuyer.executeQuery();
            if (rs.next() && rs.getInt(1) == 0) {
                String buyerPass = PasswordUtil.hashPassword("Customer@123");
                try (PreparedStatement insertBuyer = conn.prepareStatement(
                    "INSERT INTO users (name, email, password, role, phone, address, city, state, pincode) VALUES (?, ?, ?, 'buyer', '9876543210', '124 South Car Street, Mylapore', 'Chennai', 'Tamil Nadu', '600004')"
                )) {
                    insertBuyer.setString(1, "Priya Sundaram");
                    insertBuyer.setString(2, "customer@kalaivanimart.com");
                    insertBuyer.setString(3, buyerPass);
                    insertBuyer.executeUpdate();
                }
            }
        }

        // Seed Demo Seller if not exists
        try (PreparedStatement checkSeller = conn.prepareStatement("SELECT COUNT(*) FROM users WHERE email = 'seller@kalaivanimart.com'")) {
            ResultSet rs = checkSeller.executeQuery();
            if (rs.next() && rs.getInt(1) == 0) {
                String sellerPass = PasswordUtil.hashPassword("Seller@123");
                try (PreparedStatement insertSeller = conn.prepareStatement(
                    "INSERT INTO users (name, email, password, role, phone, address, city, state, pincode) VALUES (?, ?, ?, 'seller', '9123456780', '45 Cross Cut Road, Gandhipuram', 'Coimbatore', 'Tamil Nadu', '641012')"
                )) {
                    insertSeller.setString(1, "Kalaivani Retailers");
                    insertSeller.setString(2, "seller@kalaivanimart.com");
                    insertSeller.setString(3, sellerPass);
                    insertSeller.executeUpdate();
                }
            }
        }

        // Seed Categories
        String[][] categories = {
            {"Electronics", "electronics", "fas fa-laptop", "Smartphones, Audio, Laptops and Home Appliances"},
            {"Fashion", "fashion", "fas fa-tshirt", "Men, Women, Kids traditional and contemporary clothing"},
            {"Home & Kitchen", "home", "fas fa-couch", "Cookware, Decor, Furnishings, and Dining"},
            {"Beauty", "beauty", "fas fa-spa", "Ayurvedic Skincare, Haircare and Personal Grooming"},
            {"Grocery", "grocery", "fas fa-shopping-basket", "Daily essentials, Organic Spices, Grains & Oils"},
            {"Sports", "sports", "fas fa-running", "Fitness Gear, Sporting Goods and Gym Equipment"},
            {"Toys", "toys", "fas fa-gamepad", "Handcrafted Wooden Toys, Puzzles and Board Games"}
        };

        for (String[] cat : categories) {
            try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO categories (name, slug, icon, description) SELECT ?, ?, ?, ? WHERE NOT EXISTS (SELECT 1 FROM categories WHERE slug = ?)"
            )) {
                ps.setString(1, cat[0]);
                ps.setString(2, cat[1]);
                ps.setString(3, cat[2]);
                ps.setString(4, cat[3]);
                ps.setString(5, cat[1]);
                ps.executeUpdate();
            }
        }

        // Seed Curated Products if products table is empty
        try (PreparedStatement checkProd = conn.prepareStatement("SELECT COUNT(*) FROM products")) {
            ResultSet rs = checkProd.executeQuery();
            if (rs.next() && rs.getInt(1) == 0) {
                insertInitialProducts(conn);
                LOGGER.info("Sample high-quality Indian e-commerce products seeded successfully.");
            }
        }
    }

    private static void insertInitialProducts(Connection conn) throws SQLException {
        Object[][] products = {
            {
                "Boat Rockerz 550 Wireless Bluetooth Headphones",
                "High-definition sound with 50mm dynamic drivers, 20 hours battery backup, and plush padded earcups designed for all-day comfort.",
                "electronics", 1799.00, 4999.00, 45,
                "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=700&auto=format&fit=crop&q=80"
            },
            {
                "OnePlus Nord Smart Watch with AMOLED Display",
                "1.78-inch vibrant AMOLED display, 105 sports modes, SpO2 blood oxygen monitoring, and 10 days battery life with IP68 water resistance.",
                "electronics", 2999.00, 4999.00, 32,
                "https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=700&auto=format&fit=crop&q=80"
            },
            {
                "Dell 15.6-inch Core i5 Laptop (16GB RAM, 512GB SSD)",
                "Fast, reliable computing for work, education, and entertainment. Equipped with full HD anti-glare display and fast charging.",
                "electronics", 48999.00, 62000.00, 15,
                "https://images.unsplash.com/photo-1496181133206-80ce9b88a853?w=700&auto=format&fit=crop&q=80"
            },
            {
                "Pure Kanchipuram Silk Saree with Gold Zari Border",
                "Handcrafted authentic Kanchipuram silk saree with rich pallu design and matching blouse piece, ideal for weddings and celebrations.",
                "fashion", 6499.00, 12999.00, 20,
                "https://images.unsplash.com/photo-1610030469983-98e550d6193c?w=700&auto=format&fit=crop&q=80"
            },
            {
                "Men's Pure Khadi Cotton Kurta Pajama Set",
                "Breathable, lightweight handspun Khadi cotton ethnic wear for festive occasions, pujas, and traditional gatherings.",
                "fashion", 1499.00, 2999.00, 50,
                "https://images.unsplash.com/photo-1602810318383-e386cc2a3ccf?w=700&auto=format&fit=crop&q=80"
            },
            {
                "Woodland Men's Genuine Leather Casual Shoes",
                "Durable outdoor shoes crafted with water-resistant nubuck leather and rugged rubber grip sole.",
                "fashion", 3299.00, 4995.00, 25,
                "https://images.unsplash.com/photo-1549298916-b41d501d3772?w=700&auto=format&fit=crop&q=80"
            },
            {
                "Traditional Brass Peacock Diya / Kuthuvilakku (Pair)",
                "Artisan hand-casted brass oil lamps bringing auspicious warmth and timeless grace to pooja rooms and festive celebrations.",
                "home", 2199.00, 3999.00, 18,
                "https://images.unsplash.com/photo-1578749556568-bc2c40e68b61?w=700&auto=format&fit=crop&q=80"
            },
            {
                "Handcrafted Ceramic Dinner Set (16 Pieces)",
                "Microwave and dishwasher-safe studio pottery dinnerware with rich indigo glazed finish. Includes plates, bowls, and mugs.",
                "home", 2899.00, 5499.00, 30,
                "https://images.unsplash.com/photo-1517256064527-09c73fc73e38?w=700&auto=format&fit=crop&q=80"
            },
            {
                "Forest Essentials Ayurvedic Kumkumadi Face Glow Serum",
                "Enriched with pure Kashmiri saffron, sandalwood, and 24K gold bhasma for radiant, even-toned skin.",
                "beauty", 1850.00, 2400.00, 40,
                "https://images.unsplash.com/photo-1608248597359-5f284b3d39f7?w=700&auto=format&fit=crop&q=80"
            },
            {
                "Kama Ayurveda Pure Rose Water Facial Mist (200ml)",
                "Steam-distilled from luxurious Kannauj roses to naturally tone, hydrate, and refresh skin throughout the day.",
                "beauty", 975.00, 1250.00, 60,
                "https://images.unsplash.com/photo-1598440947619-2c35fc9aa908?w=700&auto=format&fit=crop&q=80"
            },
            {
                "Daawat Traditional Basmati Rice (5kg Bag)",
                "Aged long-grain aromatic Basmati rice, carefully selected for royal biryanis and everyday traditional dishes.",
                "grocery", 625.00, 795.00, 100,
                "https://images.unsplash.com/photo-1586201375761-83865001e31c?w=700&auto=format&fit=crop&q=80"
            },
            {
                "South Indian Filter Coffee Decoction Powder (500g)",
                "80:20 Arabica, Robusta and Chicory blend roasted to perfection for the authentic tumbler-davarah filter kaapi experience.",
                "grocery", 380.00, 480.00, 85,
                "https://images.unsplash.com/photo-1514432324607-a09d9b4aefdd?w=700&auto=format&fit=crop&q=80"
            },
            {
                "Premium Anti-Skid Eco-Friendly Yoga Mat (6mm)",
                "High-density TPE cushioned mat with alignment lines, sweat-resistant texture, and complimentary carry strap.",
                "sports", 899.00, 1999.00, 50,
                "https://images.unsplash.com/photo-1601925260368-ae2f83cf8b7f?w=700&auto=format&fit=crop&q=80"
            },
            {
                "Kashmir Willow English Style Cricket Bat",
                "Full-sized adult tennis and leather ball cricket bat with thick edges, balanced sweet spot, and cane handle.",
                "sports", 1599.00, 2999.00, 25,
                "https://images.unsplash.com/photo-1531415074968-036ba1b575da?w=700&auto=format&fit=crop&q=80"
            },
            {
                "Handcrafted Channapatna Wooden Stacking Toy",
                "Eco-friendly non-toxic vegetable-dye colored wooden stacking rings, safe for toddlers and promoting motor skills.",
                "toys", 499.00, 899.00, 40,
                "https://images.unsplash.com/photo-1596461404969-9ae70f2830c1?w=700&auto=format&fit=crop&q=80"
            }
        };

        String insertSql = "INSERT INTO products (name, description, category, price, old_price, stock, image, seller_id) VALUES (?, ?, ?, ?, ?, ?, ?, 1)";
        try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
            for (Object[] prod : products) {
                ps.setString(1, (String) prod[0]);
                ps.setString(2, (String) prod[1]);
                ps.setString(3, (String) prod[2]);
                ps.setDouble(4, (Double) prod[3]);
                ps.setDouble(5, (Double) prod[4]);
                ps.setInt(6, (Integer) prod[5]);
                ps.setString(7, (String) prod[6]);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    public static boolean checkHealth() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT 1")) {
            return rs.next();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Database health check failed: " + e.getMessage());
            return false;
        }
    }

    public static String getDatabaseProductName() {
        try (Connection conn = getConnection()) {
            return conn.getMetaData().getDatabaseProductName() + " " + conn.getMetaData().getDatabaseProductVersion();
        } catch (Exception e) {
            return "Unknown";
        }
    }
}
