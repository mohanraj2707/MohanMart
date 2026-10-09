package com.mohan.mohanmart.listener;

import com.mohan.mohanmart.util.DatabaseConfigResolver;
import com.mohan.mohanmart.util.DatabaseMigrationRunner;
import com.mohan.mohanmart.util.DatabaseSeeder;
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
 * connection pool, executing versioned database schema migrations, and running idempotent
 * startup seeding at application startup and shutdown.
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
                    logger.info("config.properties not found on classpath; using environment variables / defaults");
                }
            }

            DatabaseConfigResolver dbConfig = DatabaseConfigResolver.resolve(props);
            String jdbcUrl = dbConfig.getJdbcUrl();

            logger.info("Active Database Profile: {} | Driver: {} | URL: {}",
                    dbConfig.getDatabaseProfile(), dbConfig.getDriverClassName(), dbConfig.getMaskedJdbcUrl());

            // When running on local H2 (and not disabled), start H2 TCP Server before pool init
            // so jdbc:h2:tcp://localhost:9092/./data/mohanmart connections succeed automatically
            boolean h2ConsoleEnabled = !"false".equalsIgnoreCase(System.getenv("H2_CONSOLE_ENABLED"));
            if (!dbConfig.isPostgres() && jdbcUrl.startsWith("jdbc:h2:") && h2ConsoleEnabled) {
                try {
                    h2TcpServer = org.h2.tools.Server.createTcpServer(
                            "-tcp", "-tcpAllowOthers", "-tcpPort", "9092", "-ifNotExists").start();
                    logger.info(">>> H2 TCP Server started on port 9092");
                } catch (Exception e) {
                    logger.debug("H2 TCP Server on port 9092 already running or unavailable: {}", e.getMessage());
                }

                try {
                    h2WebServer = org.h2.tools.Server.createWebServer(
                            "-web", "-webAllowOthers", "-webPort", "8082").start();
                    logger.info(">>> H2 Web Console started at: http://localhost:8082");
                } catch (Exception e) {
                    logger.debug("H2 Web Console on port 8082 already running or unavailable: {}", e.getMessage());
                }
            }

            HikariConfig config = new HikariConfig();
            config.setDriverClassName(dbConfig.getDriverClassName());
            config.setJdbcUrl(jdbcUrl);
            config.setUsername(dbConfig.getUsername());
            config.setPassword(dbConfig.getPassword());
            config.setMaximumPoolSize(Integer.parseInt(props.getProperty("db.pool.maxSize", "10")));
            config.setMinimumIdle(Integer.parseInt(props.getProperty("db.pool.minIdle", "2")));
            config.setIdleTimeout(Long.parseLong(props.getProperty("db.pool.idleTimeout", "300000")));
            config.setConnectionTimeout(Long.parseLong(props.getProperty("db.pool.connectionTimeout", "20000")));
            config.setMaxLifetime(Long.parseLong(props.getProperty("db.pool.maxLifetime", "1800000")));
            config.setPoolName("MohanMartHikariPool");

            dataSource = new HikariDataSource(config);
            DatabaseUtil.setDataSource(dataSource);
            sce.getServletContext().setAttribute("dataSource", dataSource);

            logger.info("HikariCP connection pool initialized successfully ({})", dbConfig.getDatabaseProfile());

            // 1. Execute versioned schema migrations (V1 - V4)
            DatabaseMigrationRunner migrationRunner = new DatabaseMigrationRunner(dataSource);
            migrationRunner.runMigrations();

            // 2. Execute idempotent database seeder (admin password configuration + catalog & sample orders)
            DatabaseSeeder seeder = new DatabaseSeeder(dataSource, System.getenv(), dbConfig.isPostgres());
            seeder.seedIfNeeded();

        } catch (Exception e) {
            logger.error("Failed to initialize HikariCP connection pool, migrations, or seeding", e);
            throw new RuntimeException("Application startup failed due to database initialization error", e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        logger.info("Shutting down MohanMart Application Context...");
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("HikariCP connection pool closed cleanly.");
        }
        if (h2WebServer != null && h2WebServer.isRunning(false)) {
            h2WebServer.stop();
            logger.info("H2 Web Console stopped.");
        }
        if (h2TcpServer != null && h2TcpServer.isRunning(false)) {
            h2TcpServer.stop();
            logger.info("H2 TCP Server stopped.");
        }
    }
}
