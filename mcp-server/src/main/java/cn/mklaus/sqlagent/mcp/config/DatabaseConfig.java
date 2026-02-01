package cn.mklaus.sqlagent.mcp.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Database configuration for MCP server
 */
public class DatabaseConfig {
    private final String type;
    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;

    public DatabaseConfig(String type, String host, int port, String database,
                          String username, String password) {
        this.type = type;
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
    }

    public String getType() {
        return type;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getDatabase() {
        return database;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    /**
     * Create HikariDataSource from this configuration
     */
    public HikariDataSource createDataSource() throws SQLException {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(getJdbcUrl());
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(3); // Small pool for MCP server
        config.setMinimumIdle(1);
        config.setConnectionTimeout(30000); // 30 seconds
        config.setIdleTimeout(600000); // 10 minutes
        config.setMaxLifetime(1800000); // 30 minutes

        return new HikariDataSource(config);
    }

    /**
     * Get JDBC URL based on database type
     */
    private String getJdbcUrl() {
        switch (type.toLowerCase()) {
            case "mysql":
                return String.format("jdbc:mysql://%s:%d/%s?useSSL=false&serverTimezone=UTC",
                    host, port, database);
            case "postgresql":
            case "postgres":
                return String.format("jdbc:postgresql://%s:%d/%s",
                    host, port, database);
            default:
                throw new IllegalArgumentException("Unsupported database type: " + type);
        }
    }

    /**
     * Create DatabaseConfig from environment variables
     */
    public static DatabaseConfig fromEnvironment() {
        String type = getEnvOrDefault("DB_TYPE", "mysql");
        String host = getEnvOrDefault("DB_HOST", "localhost");
        int port = Integer.parseInt(getEnvOrDefault("DB_PORT", getDefaultPort(type)));
        String database = getEnvOrDefault("DB_NAME", "");
        String username = getEnvOrDefault("DB_USER", "");
        String password = getEnvOrDefault("DB_PASSWORD", "");

        if (database.isEmpty()) {
            throw new IllegalArgumentException("DB_NAME environment variable is required");
        }
        if (username.isEmpty()) {
            throw new IllegalArgumentException("DB_USER environment variable is required");
        }

        return new DatabaseConfig(type, host, port, database, username, password);
    }

    private static String getEnvOrDefault(String key, String defaultValue) {
        String value = System.getenv(key);
        return value != null ? value : defaultValue;
    }

    private static String getDefaultPort(String type) {
        switch (type.toLowerCase()) {
            case "mysql":
                return "3306";
            case "postgresql":
            case "postgres":
                return "5432";
            default:
                return "3306";
        }
    }
}
