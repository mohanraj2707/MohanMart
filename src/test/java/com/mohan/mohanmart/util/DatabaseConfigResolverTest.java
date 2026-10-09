package com.mohan.mohanmart.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseConfigResolverTest {

    @Test
    @DisplayName("Should parse Render DATABASE_URL with postgres:// scheme and embedded credentials")
    void testRenderDatabaseUrlWithPort() {
        Map<String, String> env = new HashMap<>();
        env.put("DATABASE_URL", "postgres://mohan_user:s3cr3t%21pass@dpg-xyz.oregon-postgres.render.com:5432/mohanmart");

        DatabaseConfigResolver resolver = DatabaseConfigResolver.resolve(env, new Properties());

        assertEquals("jdbc:postgresql://dpg-xyz.oregon-postgres.render.com:5432/mohanmart", resolver.getJdbcUrl());
        assertEquals("mohan_user", resolver.getUsername());
        assertEquals("s3cr3t!pass", resolver.getPassword());
        assertEquals("org.postgresql.Driver", resolver.getDriverClassName());
        assertTrue(resolver.isPostgres());
        assertEquals("PostgreSQL (Cloud/Render)", resolver.getDatabaseProfile());
        assertFalse(resolver.getMaskedJdbcUrl().contains("s3cr3t"));
    }

    @Test
    @DisplayName("Should parse Render internal postgresql:// URL without explicit port (defaulting to 5432)")
    void testRenderInternalDatabaseUrlWithoutPort() {
        Map<String, String> env = new HashMap<>();
        env.put("DATABASE_URL", "postgresql://mohanmart_user:renderPass123@dpg-internal-host/mohanmart_db");

        DatabaseConfigResolver resolver = DatabaseConfigResolver.resolve(env, new Properties());

        assertEquals("jdbc:postgresql://dpg-internal-host:5432/mohanmart_db", resolver.getJdbcUrl());
        assertEquals("mohanmart_user", resolver.getUsername());
        assertEquals("renderPass123", resolver.getPassword());
        assertEquals("org.postgresql.Driver", resolver.getDriverClassName());
        assertTrue(resolver.isPostgres());
    }

    @Test
    @DisplayName("Should allow explicit DB_USERNAME and DB_PASSWORD env vars to override URI credentials")
    void testExplicitDbUsernameAndPasswordOverride() {
        Map<String, String> env = new HashMap<>();
        env.put("DATABASE_URL", "postgresql://old_user:old_pass@db.example.com:5432/mohanmart");
        env.put("DB_USERNAME", "override_user");
        env.put("DB_PASSWORD", "override_pass");

        DatabaseConfigResolver resolver = DatabaseConfigResolver.resolve(env, new Properties());

        assertEquals("jdbc:postgresql://db.example.com:5432/mohanmart", resolver.getJdbcUrl());
        assertEquals("override_user", resolver.getUsername());
        assertEquals("override_pass", resolver.getPassword());
    }

    @Test
    @DisplayName("Should fall back to local H2 configuration when cloud env vars are absent")
    void testLocalH2Fallback() {
        Map<String, String> env = new HashMap<>();
        Properties props = new Properties();
        props.setProperty("db.url", "jdbc:h2:tcp://localhost:9092/./data/mohanmart");
        props.setProperty("db.user", "sa");
        props.setProperty("db.password", "");

        DatabaseConfigResolver resolver = DatabaseConfigResolver.resolve(env, props);

        assertEquals("jdbc:h2:tcp://localhost:9092/./data/mohanmart", resolver.getJdbcUrl());
        assertEquals("sa", resolver.getUsername());
        assertEquals("", resolver.getPassword());
        assertEquals("org.h2.Driver", resolver.getDriverClassName());
        assertFalse(resolver.isPostgres());
        assertEquals("H2 (Local/Embedded)", resolver.getDatabaseProfile());
    }
}
