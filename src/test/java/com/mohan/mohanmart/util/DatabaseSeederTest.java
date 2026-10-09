package com.mohan.mohanmart.util;

import com.mohan.mohanmart.dao.BaseDAOTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseSeederTest extends BaseDAOTest {

    @Test
    @DisplayName("Should seed empty database and remain 100% idempotent across repeated invocations")
    void testIdempotentDatabaseSeeding() throws Exception {
        DatabaseSeeder seeder = new DatabaseSeeder(DatabaseUtil.getDataSource(), new HashMap<>(), false);

        // First run on empty schema
        seeder.seedIfNeeded();

        long usersAfterFirst = countRows("users");
        long productsAfterFirst = countRows("products");
        long ordersAfterFirst = countRows("orders");
        long reviewsAfterFirst = countRows("reviews");

        assertTrue(usersAfterFirst >= 6, "Should seed admin, sellers, and buyers");
        assertTrue(productsAfterFirst >= 16, "Should seed at least 16 catalog products");
        assertEquals(3, ordersAfterFirst, "Should seed 3 initial orders");
        assertEquals(4, reviewsAfterFirst, "Should seed 4 initial reviews");

        // Second & third runs (simulating Render container restarts) must not duplicate rows
        seeder.seedIfNeeded();
        seeder.seedIfNeeded();

        assertEquals(usersAfterFirst, countRows("users"), "Users must not be duplicated on restart");
        assertEquals(productsAfterFirst, countRows("products"), "Products must not be duplicated on restart");
        assertEquals(ordersAfterFirst, countRows("orders"), "Orders must not be duplicated on restart");
        assertEquals(reviewsAfterFirst, countRows("reviews"), "Reviews must not be duplicated on restart");
    }

    @Test
    @DisplayName("Should hash ADMIN_INITIAL_PASSWORD with BCrypt when provided in environment")
    void testAdminInitialPasswordFromEnvironment() throws Exception {
        Map<String, String> env = new HashMap<>();
        env.put("ADMIN_EMAIL", "admin@mohanmart.com");
        env.put("ADMIN_INITIAL_PASSWORD", "RenderStrongAdmin#2026");

        DatabaseSeeder seeder = new DatabaseSeeder(DatabaseUtil.getDataSource(), env, true);
        seeder.seedIfNeeded();

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT password_hash, role FROM users WHERE email = ?")) {
            ps.setString(1, "admin@mohanmart.com");
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next(), "Admin user must be seeded");
                assertEquals("ADMIN", rs.getString("role"));
                String hash = rs.getString("password_hash");
                assertTrue(PasswordUtil.checkPassword("RenderStrongAdmin#2026", hash),
                        "Admin password must match ADMIN_INITIAL_PASSWORD");
                assertFalse(PasswordUtil.checkPassword("admin123", hash),
                        "Admin password must not accept default weak password");
            }
        }
    }

    @Test
    @DisplayName("Should lock default weak admin password in production mode when ADMIN_INITIAL_PASSWORD is unset")
    void testProductionModeLocksDefaultAdminPasswordWhenUnset() throws Exception {
        DatabaseSeeder seeder = new DatabaseSeeder(DatabaseUtil.getDataSource(), new HashMap<>(), true);
        seeder.seedIfNeeded();

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT password_hash FROM users WHERE email = ?")) {
            ps.setString(1, "admin@mohanmart.com");
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                String hash = rs.getString("password_hash");
                assertFalse(PasswordUtil.checkPassword("admin123", hash),
                        "Production mode must never leave default weak password active");
            }
        }
    }

    @Test
    @DisplayName("Should skip seeding when SEED_DATABASE is false")
    void testSeedingDisabledViaEnv() throws Exception {
        Map<String, String> env = new HashMap<>();
        env.put("SEED_DATABASE", "false");

        DatabaseSeeder seeder = new DatabaseSeeder(DatabaseUtil.getDataSource(), env, false);
        seeder.seedIfNeeded();

        assertEquals(0, countRows("users"), "Users table should remain empty when SEED_DATABASE=false");
        assertEquals(0, countRows("products"), "Products table should remain empty when SEED_DATABASE=false");
    }

    private long countRows(String table) throws Exception {
        try (Connection conn = DatabaseUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + table)) {
            return rs.next() ? rs.getLong(1) : 0L;
        }
    }
}
