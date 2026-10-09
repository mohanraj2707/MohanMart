package com.mohan.mohanmart.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.UUID;

/**
 * Idempotent database seeder for MohanMart.
 *
 * <p>Safely populates initial marketplace categories, products, sample orders, reviews, and
 * demo/admin accounts when the application starts on an empty or partially seeded database.
 * Running {@link #seedIfNeeded()} multiple times across container restarts never creates
 * duplicate rows or triggers unique constraint violations.
 */
public class DatabaseSeeder {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseSeeder.class);

    // Pre-computed BCrypt (cost=12) hashes for local dev defaults
    private static final String DEFAULT_DEV_ADMIN_HASH =
            "$2a$12$qKzk6H7WGdb2WVpX3bKQmOG9DhO7DnpWKM/Aapv1MW1ylFiwrQkre";
    private static final String DEFAULT_DEV_SELLER_HASH =
            "$2a$12$M6a7NKDtDAoxaoohwBHYOuQTz0NU7PTZYrMPx17Zg2iUVXhM6b0SW";
    private static final String DEFAULT_DEV_BUYER_HASH =
            "$2a$12$2PLRursg3L62LU/8ALbIauXz7bAZM6jlXfqiXq33VO1C1ttFavB6u";

    private final DataSource dataSource;
    private final Map<String, String> env;
    private final boolean productionMode;

    public DatabaseSeeder(DataSource dataSource) {
        this(dataSource, System.getenv(), false);
    }

    public DatabaseSeeder(DataSource dataSource, Map<String, String> env, boolean productionMode) {
        this.dataSource = dataSource;
        this.env = (env != null) ? env : System.getenv();
        this.productionMode = productionMode;
    }

    /**
     * Checks whether initial seed data is needed and applies it idempotently.
     */
    public void seedIfNeeded() {
        String seedFlag = firstNonBlank(env.get("SEED_DATABASE"), env.get("APP_SEED_ENABLED"), "true");
        if ("false".equalsIgnoreCase(seedFlag)) {
            logger.info("[DatabaseSeeder] Database seeding is disabled via SEED_DATABASE=false. Skipping.");
            return;
        }

        logger.info("[DatabaseSeeder] Starting idempotent database seed check (productionMode={})...", productionMode);

        try (Connection conn = dataSource.getConnection()) {
            boolean previousAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                int insertedUsers = 0;
                int insertedProducts = 0;
                int insertedOrders = 0;
                int insertedReviews = 0;

                // 1. Seed / configure Admin user safely
                String adminEmail = firstNonBlank(env.get("ADMIN_EMAIL"), "admin@mohanmart.com");
                String adminPasswordEnv = firstNonBlank(env.get("ADMIN_INITIAL_PASSWORD"), env.get("ADMIN_PASSWORD"));
                insertedUsers += ensureAdminUser(conn, adminEmail, adminPasswordEnv);

                // 2. Seed demo sellers & buyers idempotently
                String sellerHash = resolveRoleHash(env.get("DEMO_SELLER_PASSWORD"), DEFAULT_DEV_SELLER_HASH);
                String buyerHash = resolveRoleHash(env.get("DEMO_BUYER_PASSWORD"), DEFAULT_DEV_BUYER_HASH);

                insertedUsers += ensureUser(conn, "Mohan Electronics", "seller1@mohanmart.com", sellerHash, "SELLER");
                insertedUsers += ensureUser(conn, "BookWorld Store", "seller2@mohanmart.com", sellerHash, "SELLER");
                insertedUsers += ensureUser(conn, "Kaveri Living & Apparel", "seller3@mohanmart.com", sellerHash, "SELLER");
                insertedUsers += ensureUser(conn, "Alice Buyer", "buyer1@mohanmart.com", buyerHash, "BUYER");
                insertedUsers += ensureUser(conn, "Bob Buyer", "buyer2@mohanmart.com", buyerHash, "BUYER");

                long seller1Id = findUserIdByEmail(conn, "seller1@mohanmart.com");
                long seller2Id = findUserIdByEmail(conn, "seller2@mohanmart.com");
                long seller3Id = findUserIdByEmail(conn, "seller3@mohanmart.com");
                long buyer1Id = findUserIdByEmail(conn, "buyer1@mohanmart.com");
                long buyer2Id = findUserIdByEmail(conn, "buyer2@mohanmart.com");

                // 3. Seed realistic catalog products across Electronics, Books, Home, Clothing, Accessories
                insertedProducts += ensureProduct(conn, seller1Id,
                        "Mechanical Keyboard",
                        "Compact TKL mechanical keyboard with tactile switches and per-key RGB backlighting.",
                        new BigDecimal("89.99"), 25, "Electronics",
                        "https://images.unsplash.com/photo-1587829741301-dc798b83add3?w=500");

                insertedProducts += ensureProduct(conn, seller1Id,
                        "Wireless Noise-Cancelling Headphones",
                        "Over-ear Bluetooth headphones. 40 mm drivers, 30 h battery, foldable design.",
                        new BigDecimal("149.50"), 15, "Electronics",
                        "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=500");

                insertedProducts += ensureProduct(conn, seller1Id,
                        "USB-C Hub (7-in-1)",
                        "4K HDMI, 3x USB-A 3.0, SD card reader, 100 W PD pass-through charging.",
                        new BigDecimal("45.00"), 50, "Electronics",
                        "https://images.unsplash.com/photo-1593642632559-0c6d3fc62b89?w=500");

                insertedProducts += ensureProduct(conn, seller1Id,
                        "Portable Bluetooth Speaker",
                        "IPX7 waterproof, 360 degree surround sound, 20 h playtime.",
                        new BigDecimal("59.99"), 30, "Electronics",
                        "https://images.unsplash.com/photo-1608043152269-423dbba4e7e1?w=500");

                insertedProducts += ensureProduct(conn, seller1Id,
                        "Laptop Stand (Aluminium)",
                        "Adjustable ergonomic stand compatible with 10-17 inch laptops.",
                        new BigDecimal("34.99"), 60, "Electronics",
                        "https://images.unsplash.com/photo-1593642634491-a5fa8eb5b1ac?w=500");

                insertedProducts += ensureProduct(conn, seller1Id,
                        "4K Ultra HD Webcam with Dual Mic",
                        "Auto-focus 4K streaming webcam with privacy shutter and noise-reducing stereo microphones.",
                        new BigDecimal("74.99"), 40, "Electronics",
                        "https://images.unsplash.com/photo-1587826080692-f439cd0b70da?w=500");

                insertedProducts += ensureProduct(conn, seller2Id,
                        "Clean Code",
                        "By Robert C. Martin - A handbook of agile software craftsmanship.",
                        new BigDecimal("39.95"), 50, "Books",
                        "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=500");

                insertedProducts += ensureProduct(conn, seller2Id,
                        "Effective Java (3rd Edition)",
                        "By Joshua Bloch - The definitive guide to best-practice Java programming.",
                        new BigDecimal("44.99"), 35, "Books",
                        "https://images.unsplash.com/photo-1524995997946-a1c2e315a42f?w=500");

                insertedProducts += ensureProduct(conn, seller2Id,
                        "Designing Data-Intensive Applications",
                        "By Martin Kleppmann - Deep dive into distributed systems and databases.",
                        new BigDecimal("49.99"), 20, "Books",
                        "https://images.unsplash.com/photo-1510172951991-856a654063f9?w=500");

                insertedProducts += ensureProduct(conn, seller2Id,
                        "System Design Interview - An Insider's Guide",
                        "By Alex Xu - Step-by-step framework and real-world distributed system architectures.",
                        new BigDecimal("36.50"), 45, "Books",
                        "https://images.unsplash.com/photo-1532012197267-da84d127e765?w=500");

                insertedProducts += ensureProduct(conn, seller2Id,
                        "Ceramic Coffee Mug (400 ml)",
                        "Handcrafted matte-finish ceramic mug. Microwave and dishwasher safe.",
                        new BigDecimal("18.00"), 80, "Home",
                        "https://images.unsplash.com/photo-1514432324607-a09d9b4aefdd?w=500");

                insertedProducts += ensureProduct(conn, seller2Id,
                        "Bamboo Desk Organiser",
                        "Eco-friendly 5-compartment desk organiser made from sustainable bamboo.",
                        new BigDecimal("27.50"), 45, "Home",
                        "https://images.unsplash.com/photo-1593642634315-48f5414c3ad9?w=500");

                insertedProducts += ensureProduct(conn, seller3Id,
                        "Insulated Stainless Steel Flask (1 Litre)",
                        "Double-wall vacuum insulated bottle keeping beverages hot for 12h or cold for 24h.",
                        new BigDecimal("29.99"), 70, "Home",
                        "https://images.unsplash.com/photo-1602143407151-7111542de6e8?w=500");

                insertedProducts += ensureProduct(conn, seller1Id,
                        "Cotton Graphic Tee - Developer Edition",
                        "100% combed cotton. Available in S / M / L / XL. Pre-shrunk comfort fit.",
                        new BigDecimal("22.00"), 100, "Clothing",
                        "https://images.unsplash.com/photo-1583743814966-8936f5b7be1a?w=500");

                insertedProducts += ensureProduct(conn, seller3Id,
                        "Merino Wool Everyday Hoodie",
                        "Breathable temperature-regulating merino blend pullover hoodie for year-round comfort.",
                        new BigDecimal("64.00"), 35, "Clothing",
                        "https://images.unsplash.com/photo-1556821840-3a63f95609a7?w=500");

                insertedProducts += ensureProduct(conn, seller3Id,
                        "Water-Resistant Laptop Backpack (16-inch)",
                        "Padded laptop compartment, luggage pass-through strap, and hidden anti-theft pocket.",
                        new BigDecimal("54.99"), 40, "Accessories",
                        "https://images.unsplash.com/photo-1553062407-98eeb64c6a62?w=500");

                // 4. Seed sample orders & order items if orders table is empty
                long keyboardId = findProductIdByName(conn, "Mechanical Keyboard");
                long headphonesId = findProductIdByName(conn, "Wireless Noise-Cancelling Headphones");
                long hubId = findProductIdByName(conn, "USB-C Hub (7-in-1)");
                long standId = findProductIdByName(conn, "Laptop Stand (Aluminium)");
                long cleanCodeId = findProductIdByName(conn, "Clean Code");
                long effectiveJavaId = findProductIdByName(conn, "Effective Java (3rd Edition)");
                long mugId = findProductIdByName(conn, "Ceramic Coffee Mug (400 ml)");
                long organiserId = findProductIdByName(conn, "Bamboo Desk Organiser");

                if (countTableRows(conn, "orders") == 0 && buyer1Id > 0 && buyer2Id > 0) {
                    long order1 = insertOrder(conn, buyer1Id, "DELIVERED", new BigDecimal("129.94"));
                    insertOrderItem(conn, order1, keyboardId, 1, new BigDecimal("89.99"));
                    insertOrderItem(conn, order1, cleanCodeId, 1, new BigDecimal("39.95"));

                    long order2 = insertOrder(conn, buyer2Id, "SHIPPED", new BigDecimal("194.49"));
                    insertOrderItem(conn, order2, headphonesId, 1, new BigDecimal("149.50"));
                    insertOrderItem(conn, order2, effectiveJavaId, 1, new BigDecimal("44.99"));

                    long order3 = insertOrder(conn, buyer1Id, "PENDING", new BigDecimal("63.00"));
                    insertOrderItem(conn, order3, hubId, 1, new BigDecimal("45.00"));
                    insertOrderItem(conn, order3, mugId, 1, new BigDecimal("18.00"));
                    insertedOrders = 3;
                }

                // 5. Seed sample cart items idempotently
                if (buyer2Id > 0 && standId > 0 && organiserId > 0) {
                    ensureCartItem(conn, buyer2Id, standId, 1);
                    ensureCartItem(conn, buyer2Id, organiserId, 2);
                }

                // 6. Seed verified reviews idempotently
                insertedReviews += ensureReview(conn, keyboardId, buyer1Id, 5,
                        "Excellent keyboard! The typing feel is superb and build quality is top-notch.");
                insertedReviews += ensureReview(conn, cleanCodeId, buyer1Id, 5,
                        "Best programming book I have ever read. A must-have for every developer.");
                insertedReviews += ensureReview(conn, headphonesId, buyer2Id, 4,
                        "Great sound quality and very comfortable. Battery life is impressive.");
                insertedReviews += ensureReview(conn, effectiveJavaId, buyer2Id, 5,
                        "Bloch explains Java best-practices with clear real-world examples.");

                conn.commit();

                long totalUsers = countTableRows(conn, "users");
                long totalProducts = countTableRows(conn, "products");
                long totalOrders = countTableRows(conn, "orders");

                if (insertedUsers == 0 && insertedProducts == 0 && insertedOrders == 0 && insertedReviews == 0) {
                    logger.info("[DatabaseSeeder] Seed check complete: database already populated "
                            + "({} users, {} products, {} orders). Skipped duplicate seeding.",
                            totalUsers, totalProducts, totalOrders);
                } else {
                    logger.info("[DatabaseSeeder] Seeding completed successfully: inserted {} users, "
                            + "{} products, {} orders, {} reviews (total: {} users, {} products, {} orders).",
                            insertedUsers, insertedProducts, insertedOrders, insertedReviews,
                            totalUsers, totalProducts, totalOrders);
                }
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(previousAutoCommit);
            }
        } catch (Exception e) {
            logger.error("[DatabaseSeeder] Database seeding failed", e);
            throw new RuntimeException("Failed to execute idempotent database seeding", e);
        }
    }

    private int ensureAdminUser(Connection conn, String adminEmail, String adminPasswordEnv) throws SQLException {
        long existingId = findUserIdByEmail(conn, adminEmail);

        if (adminPasswordEnv != null && !adminPasswordEnv.trim().isEmpty()) {
            String customHash = PasswordUtil.hashPassword(adminPasswordEnv.trim());
            if (existingId > 0) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE users SET password_hash = ?, role = 'ADMIN', is_active = TRUE WHERE id = ?")) {
                    ps.setString(1, customHash);
                    ps.setLong(2, existingId);
                    ps.executeUpdate();
                }
                logger.info("[DatabaseSeeder] Updated admin account ({}) password from environment variable.", adminEmail);
                return 0;
            } else {
                insertUser(conn, "Admin Mohan", adminEmail, customHash, "ADMIN");
                logger.info("[DatabaseSeeder] Created admin account ({}) with password from environment variable.", adminEmail);
                return 1;
            }
        }

        // No ADMIN_INITIAL_PASSWORD / ADMIN_PASSWORD set in environment
        if (productionMode) {
            String randomLockedHash = PasswordUtil.hashPassword(UUID.randomUUID().toString() + "-ProdLock!");
            if (existingId <= 0) {
                insertUser(conn, "Admin Mohan", adminEmail, randomLockedHash, "ADMIN");
                logger.warn("[DatabaseSeeder] ADMIN_INITIAL_PASSWORD not set in production mode - created admin ({}) "
                        + "with locked random hash. Set ADMIN_INITIAL_PASSWORD on Render to enable admin sign-in.", adminEmail);
                return 1;
            } else if (hasDefaultDevAdminHash(conn, existingId)) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE users SET password_hash = ? WHERE id = ?")) {
                    ps.setString(1, randomLockedHash);
                    ps.setLong(2, existingId);
                    ps.executeUpdate();
                }
                logger.warn("[DatabaseSeeder] Replaced default dev admin hash on production database for {}. "
                        + "Set ADMIN_INITIAL_PASSWORD on Render to configure admin sign-in.", adminEmail);
            }
            return 0;
        }

        // Local H2 development mode fallback
        if (existingId <= 0) {
            insertUser(conn, "Admin Mohan", adminEmail, DEFAULT_DEV_ADMIN_HASH, "ADMIN");
            return 1;
        }
        return 0;
    }

    private boolean hasDefaultDevAdminHash(Connection conn, long userId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT password_hash FROM users WHERE id = ?")) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return DEFAULT_DEV_ADMIN_HASH.equals(rs.getString("password_hash"));
                }
            }
        }
        return false;
    }

    private String resolveRoleHash(String rawPasswordEnv, String fallbackHash) {
        if (rawPasswordEnv != null && !rawPasswordEnv.trim().isEmpty()) {
            return PasswordUtil.hashPassword(rawPasswordEnv.trim());
        }
        return fallbackHash;
    }

    private int ensureUser(Connection conn, String name, String email, String passwordHash, String role)
            throws SQLException {
        if (findUserIdByEmail(conn, email) > 0) {
            return 0;
        }
        insertUser(conn, name, email, passwordHash, role);
        return 1;
    }

    private void insertUser(Connection conn, String name, String email, String passwordHash, String role)
            throws SQLException {
        String sql = "INSERT INTO users (name, email, password_hash, role) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, email);
            ps.setString(3, passwordHash);
            ps.setString(4, role);
            ps.executeUpdate();
        }
    }

    private long findUserIdByEmail(Connection conn, String email) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT id FROM users WHERE email = ?")) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("id");
                }
            }
        }
        return -1L;
    }

    private int ensureProduct(Connection conn, long sellerId, String name, String description,
                              BigDecimal price, int stockQty, String category, String imageUrl)
            throws SQLException {
        if (sellerId <= 0 || findProductIdByName(conn, name) > 0) {
            return 0;
        }
        String sql = "INSERT INTO products (seller_id, name, description, price, stock_qty, category, image_url) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, sellerId);
            ps.setString(2, name);
            ps.setString(3, description);
            ps.setBigDecimal(4, price);
            ps.setInt(5, stockQty);
            ps.setString(6, category);
            ps.setString(7, imageUrl);
            ps.executeUpdate();
        }
        return 1;
    }

    private long findProductIdByName(Connection conn, String name) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT id FROM products WHERE name = ?")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("id");
                }
            }
        }
        return -1L;
    }

    private long insertOrder(Connection conn, long buyerId, String status, BigDecimal totalAmount)
            throws SQLException {
        String sql = "INSERT INTO orders (buyer_id, status, total_amount) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, buyerId);
            ps.setString(2, status);
            ps.setBigDecimal(3, totalAmount);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        }
        return -1L;
    }

    private void insertOrderItem(Connection conn, long orderId, long productId, int quantity, BigDecimal unitPrice)
            throws SQLException {
        if (orderId <= 0 || productId <= 0) {
            return;
        }
        String sql = "INSERT INTO order_items (order_id, product_id, quantity, unit_price) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, orderId);
            ps.setLong(2, productId);
            ps.setInt(3, quantity);
            ps.setBigDecimal(4, unitPrice);
            ps.executeUpdate();
        }
    }

    private void ensureCartItem(Connection conn, long userId, long productId, int quantity) throws SQLException {
        try (PreparedStatement check = conn.prepareStatement(
                "SELECT 1 FROM cart_items WHERE user_id = ? AND product_id = ?")) {
            check.setLong(1, userId);
            check.setLong(2, productId);
            try (ResultSet rs = check.executeQuery()) {
                if (rs.next()) {
                    return;
                }
            }
        }
        try (PreparedStatement insert = conn.prepareStatement(
                "INSERT INTO cart_items (user_id, product_id, quantity) VALUES (?, ?, ?)")) {
            insert.setLong(1, userId);
            insert.setLong(2, productId);
            insert.setInt(3, quantity);
            insert.executeUpdate();
        }
    }

    private int ensureReview(Connection conn, long productId, long userId, int rating, String comment)
            throws SQLException {
        if (productId <= 0 || userId <= 0) {
            return 0;
        }
        try (PreparedStatement check = conn.prepareStatement(
                "SELECT 1 FROM reviews WHERE user_id = ? AND product_id = ?")) {
            check.setLong(1, userId);
            check.setLong(2, productId);
            try (ResultSet rs = check.executeQuery()) {
                if (rs.next()) {
                    return 0;
                }
            }
        }
        try (PreparedStatement insert = conn.prepareStatement(
                "INSERT INTO reviews (product_id, user_id, rating, comment) VALUES (?, ?, ?, ?)")) {
            insert.setLong(1, productId);
            insert.setLong(2, userId);
            insert.setInt(3, rating);
            insert.setString(4, comment);
            insert.executeUpdate();
        }
        return 1;
    }

    private long countTableRows(Connection conn, String tableName) throws SQLException {
        String sql;
        switch (tableName) {
            case "users":
                sql = "SELECT COUNT(*) FROM users";
                break;
            case "products":
                sql = "SELECT COUNT(*) FROM products";
                break;
            case "orders":
                sql = "SELECT COUNT(*) FROM orders";
                break;
            default:
                throw new IllegalArgumentException("Unsupported table: " + tableName);
        }
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0L;
        }
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String v : values) {
            if (v != null && !v.trim().isEmpty()) {
                return v.trim();
            }
        }
        return null;
    }
}
