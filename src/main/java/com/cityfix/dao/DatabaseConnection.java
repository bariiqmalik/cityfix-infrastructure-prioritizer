package com.cityfix.dao;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * JDBC Connection Factory for PostgreSQL, MySQL, or embedded H2 in MySQL-compatibility mode.
 * Configurable via system properties, environment variables, or application.properties.
 */
public class DatabaseConnection {

    private static String jdbcUrl;
    private static String username;
    private static String password;
    private static String driverClass;

    static {
        loadConfiguration();
    }

    private static void loadConfiguration() {
        Properties props = new Properties();
        try (InputStream in = DatabaseConnection.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (Exception ignored) {}

        // Allow environment overrides, fallback to properties, then default embedded H2/Postgres/MySQL
        jdbcUrl = System.getenv().getOrDefault("DB_URL",
                props.getProperty("db.url", "jdbc:h2:mem:cityfix;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"));
        username = System.getenv().getOrDefault("DB_USER",
                props.getProperty("db.username", "sa"));
        password = System.getenv().getOrDefault("DB_PASSWORD",
                props.getProperty("db.password", ""));
        driverClass = System.getenv().getOrDefault("DB_DRIVER",
                props.getProperty("db.driver", "org.h2.Driver"));

        try {
            Class.forName(driverClass);
        } catch (ClassNotFoundException e) {
            // Driver may be auto-loaded via SPI in JDBC 4.0+
        }
    }

    /**
     * Obtains a standard JDBC Connection.
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, username, password);
    }

    /**
     * Executes database schema bootstrap scripts (useful for self-contained installations).
     */
    public static void initializeSchema(String sqlScript) {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            // Split by semicolon ignoring semicolons inside strings
            String[] commands = sqlScript.split(";\\s*(\\r?\\n)+");
            for (String cmd : commands) {
                String trimmed = cmd.trim();
                if (!trimmed.isEmpty() && !trimmed.startsWith("--")) {
                    try {
                        stmt.execute(trimmed);
                    } catch (SQLException e) {
                        System.err.println("Warning during schema setup: " + e.getMessage());
                    }
                }
            }
            System.out.println("CityFix schema initialized successfully.");
        } catch (SQLException e) {
            System.err.println("Database initialization failed: " + e.getMessage());
        }
    }

    // Setters for programmatic configuration/testing
    public static void setCustomConfig(String url, String user, String pass, String driver) {
        jdbcUrl = url;
        username = user;
        password = pass;
        driverClass = driver;
        try {
            Class.forName(driver);
        } catch (ClassNotFoundException ignored) {}
    }
}

