package com.mohan.mohanmart.util;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;

/**
 * Resolves JDBC connection parameters from environment variables (Render / Docker / Cloud)
 * and fallback classpath properties (local H2 development).
 *
 * <p>Supports Render's {@code DATABASE_URL} format
 * ({@code postgres://user:password@host:port/database} or {@code postgresql://user:password@host/database}),
 * explicit {@code JDBC_URL}, {@code DB_USERNAME}/{@code JDBC_USER}, and {@code DB_PASSWORD}/{@code JDBC_PASSWORD}.
 */
public final class DatabaseConfigResolver {

    private static final String DEFAULT_H2_URL = "jdbc:h2:mem:mohanmart;DB_CLOSE_DELAY=-1;MODE=PostgreSQL";
    private static final int DEFAULT_POSTGRES_PORT = 5432;

    private final String jdbcUrl;
    private final String username;
    private final String password;
    private final String driverClassName;
    private final boolean postgres;

    private DatabaseConfigResolver(String jdbcUrl, String username, String password, String driverClassName) {
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
        this.driverClassName = driverClassName;
        this.postgres = jdbcUrl != null && jdbcUrl.startsWith("jdbc:postgresql:");
    }

    /**
     * Resolves database configuration using {@link System#getenv()} and the supplied properties.
     *
     * @param props fallback properties loaded from {@code config.properties}
     * @return resolved {@link DatabaseConfigResolver}
     */
    public static DatabaseConfigResolver resolve(Properties props) {
        return resolve(System.getenv(), props);
    }

    /**
     * Resolves database configuration from the provided environment map and fallback properties.
     *
     * @param env   environment variable map
     * @param props fallback properties
     * @return resolved {@link DatabaseConfigResolver}
     */
    public static DatabaseConfigResolver resolve(Map<String, String> env, Properties props) {
        Properties safeProps = (props != null) ? props : new Properties();

        String rawUrl = firstNonBlank(
                env.get("JDBC_URL"),
                env.get("JDBC_DATABASE_URL"),
                env.get("SPRING_DATASOURCE_URL"),
                env.get("DATABASE_URL")
        );

        String parsedUriUser = null;
        String parsedUriPassword = null;
        String resolvedUrl;

        if (rawUrl != null) {
            String trimmed = rawUrl.trim();
            if (trimmed.startsWith("postgres://") || trimmed.startsWith("postgresql://")
                    || (trimmed.startsWith("jdbc:postgresql://") && trimmed.contains("@"))) {
                String uriCandidate = trimmed.startsWith("jdbc:") ? trimmed.substring(5) : trimmed;
                URI uri = URI.create(uriCandidate);
                String userInfo = uri.getRawUserInfo();
                if (userInfo != null && !userInfo.isEmpty()) {
                    int colonIdx = userInfo.indexOf(':');
                    if (colonIdx >= 0) {
                        parsedUriUser = URLDecoder.decode(userInfo.substring(0, colonIdx), StandardCharsets.UTF_8);
                        parsedUriPassword = URLDecoder.decode(userInfo.substring(colonIdx + 1), StandardCharsets.UTF_8);
                    } else {
                        parsedUriUser = URLDecoder.decode(userInfo, StandardCharsets.UTF_8);
                    }
                }
                String host = uri.getHost();
                int port = (uri.getPort() > 0) ? uri.getPort() : DEFAULT_POSTGRES_PORT;
                String path = (uri.getRawPath() != null) ? uri.getRawPath() : "";
                String query = uri.getRawQuery();
                StringBuilder sb = new StringBuilder("jdbc:postgresql://")
                        .append(host)
                        .append(':')
                        .append(port)
                        .append(path);
                if (query != null && !query.isEmpty()) {
                    sb.append('?').append(query);
                }
                resolvedUrl = sb.toString();
            } else {
                resolvedUrl = trimmed;
            }
        } else {
            resolvedUrl = safeProps.getProperty("db.url", DEFAULT_H2_URL).trim();
        }

        boolean isPg = resolvedUrl.startsWith("jdbc:postgresql:");

        String resolvedUser = firstNonBlank(
                env.get("DB_USERNAME"),
                env.get("JDBC_USER"),
                env.get("SPRING_DATASOURCE_USERNAME"),
                parsedUriUser,
                isPg ? null : safeProps.getProperty("db.user"),
                isPg ? "postgres" : "sa"
        );

        String resolvedPassword = firstNonNull(
                nonBlankOrNull(env.get("DB_PASSWORD")),
                nonBlankOrNull(env.get("JDBC_PASSWORD")),
                nonBlankOrNull(env.get("SPRING_DATASOURCE_PASSWORD")),
                parsedUriPassword,
                isPg ? "" : safeProps.getProperty("db.password", "")
        );

        String resolvedDriver = firstNonBlank(
                env.get("JDBC_DRIVER"),
                env.get("SPRING_DATASOURCE_DRIVER_CLASS_NAME"),
                isPg ? "org.postgresql.Driver" : safeProps.getProperty("db.driver", "org.h2.Driver")
        );

        return new DatabaseConfigResolver(resolvedUrl, resolvedUser, resolvedPassword, resolvedDriver);
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getDriverClassName() {
        return driverClassName;
    }

    public boolean isPostgres() {
        return postgres;
    }

    public String getDatabaseProfile() {
        return postgres ? "PostgreSQL (Cloud/Render)" : "H2 (Local/Embedded)";
    }

    /**
     * Returns a sanitized JDBC URL safe for startup logging with any query-string credentials redacted.
     */
    public String getMaskedJdbcUrl() {
        if (jdbcUrl == null) {
            return "null";
        }
        return jdbcUrl
                .replaceAll("(?i)(password=)[^&;]+", "$1****")
                .replaceAll("://[^/@]+@", "://****@");
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

    private static String nonBlankOrNull(String value) {
        return (value != null && !value.trim().isEmpty()) ? value : null;
    }

    private static String firstNonNull(String... values) {
        if (values == null) {
            return "";
        }
        for (String v : values) {
            if (v != null) {
                return v;
            }
        }
        return "";
    }
}
