package com.mohan.mohanmart.listener;

import com.mohan.mohanmart.util.DatabaseMigrationRunner;
import com.mohan.mohanmart.util.DatabaseUtil;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.io.InputStream;
import java.util.Properties;

/**
 * ServletContextListener responsible for initializing and terminating the HikariCP
 * connection pool and executing database schema migrations at application startup and shutdown.
 */
@WebListener
public class AppContextListener implements ServletContextListener {

    private static final Logger logger = LoggerFactory.getLogger(AppContextListener.class);
    private HikariDataSource dataSource;
    private org.h2.tools.Server h2WebServer;
    private org.h2.tools.Server h2TcpServer;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        logger.info("Initializing MohanMart Application Context and HikariCP connection pool...");
        try {
            Properties props = new Properties();
            try (InputStream in = getClass().getClassLoader().getResourceAsStream("config.properties")) {
                if (in != null) {
                    props.load(in);
                } else {
                    logger.warn("config.properties not found on classpath, checking environment defaults");
                }
            }

            String jdbcUrl = System.getenv("JDBC_URL");
            if (jdbcUrl == null || jdbcUrl.trim().isEmpty()) {
                String renderDbUrl = System.getenv("DATABASE_URL");
                if (renderDbUrl != null && !renderDbUrl.trim().isEmpty()) {
                    jdbcUrl = renderDbUrl.startsWith("postgres://")
                            ? renderDbUrl.replaceFirst("^postgres://", "jdbc:postgresql://")
                            : (renderDbUrl.startsWith("postgresql://")
                                ? renderDbUrl.replaceFirst("^postgresql://", "jdbc:postgresql://")
                                : renderDbUrl);
                } else {
                    jdbcUrl = props.getProperty("db.url", "jdbc:h2:mem:mohanmart;DB_CLOSE_DELAY=-1;MODE=PostgreSQL");
                }
            }

            String jdbcUser = System.getenv("JDBC_USER");
            if (jdbcUser == null || jdbcUser.trim().isEmpty()) {
                jdbcUser = props.getProperty("db.user", "sa");
            }

            String jdbcPassword = System.getenv("JDBC_PASSWORD");
            if (jdbcPassword == null) {
                jdbcPassword = props.getProperty("db.password", "");
            }

            String driverClassName = System.getenv("JDBC_DRIVER");
            if (driverClassName == null || driverClassName.trim().isEmpty()) {
                if (jdbcUrl.startsWith("jdbc:postgresql:")) {
                    driverClassName = "org.postgresql.Driver";
                } else {
                    driverClassName = props.getProperty("db.driver", "org.h2.Driver");
                }
            }

            HikariConfig config = new HikariConfig();
            config.setDriverClassName(driverClassName);
            config.setJdbcUrl(jdbcUrl);
            config.setUsername(jdbcUser);
            config.setPassword(jdbcPassword);
            config.setMaximumPoolSize(Integer.parseInt(props.getProperty("db.pool.maxSize", "10")));
            config.setMinimumIdle(Integer.parseInt(props.getProperty("db.pool.minIdle", "2")));
            config.setIdleTimeout(Long.parseLong(props.getProperty("db.pool.idleTimeout", "300000")));
            config.setConnectionTimeout(Long.parseLong(props.getProperty("db.pool.connectionTimeout", "20000")));
            config.setMaxLifetime(Long.parseLong(props.getProperty("db.pool.maxLifetime", "1800000")));
            config.setPoolName("MohanMartHikariPool");

            dataSource = new HikariDataSource(config);
            DatabaseUtil.setDataSource(dataSource);
            sce.getServletContext().setAttribute("dataSource", dataSource);

            logger.info("HikariCP connection pool initialized successfully with URL: {}", jdbcUrl);

            // Execute versioned schema migrations
            DatabaseMigrationRunner migrationRunner = new DatabaseMigrationRunner(dataSource);
            migrationRunner.runMigrations();

            // Start H2 Web Console on port 8082 and TCP Server on 9092 when running on H2
            if (jdbcUrl.startsWith("jdbc:h2:")) {
                try {
                    h2WebServer = org.h2.tools.Server.createWebServer("-web", "-webAllowOthers", "-webPort", "8082").start();
                    logger.info(">>> H2 Web Console started at: http://localhost:8082 (connect to {})", jdbcUrl);
                } catch (Exception e) {
                    logger.warn("Could not start H2 Web Console on port 8082: {}", e.getMessage());
                }

                try {
                    h2TcpServer = org.h2.tools.Server.createTcpServer("-tcp", "-tcpAllowOthers", "-tcpPort", "9092").start();
                    logger.info(">>> H2 TCP Server started on port 9092");
                } catch (Exception e) {
                    logger.warn("Could not start H2 TCP Server on port 9092: {}", e.getMessage());
                }
            }

        } catch (Exception e) {
            logger.error("Failed to initialize HikariCP connection pool or run database migrations", e);
            throw new RuntimeException("Application startup failed due to database pool/migration error", e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (h2WebServer != null && h2WebServer.isRunning(false)) {
            h2WebServer.stop();
            logger.info("H2 Web Console stopped.");
        }
        if (h2TcpServer != null && h2TcpServer.isRunning(false)) {
            h2TcpServer.stop();
            logger.info("H2 TCP Server stopped.");
        }
        logger.info("Closing MohanMart HikariCP connection pool...");
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("HikariCP connection pool closed.");
        }
    }
}
