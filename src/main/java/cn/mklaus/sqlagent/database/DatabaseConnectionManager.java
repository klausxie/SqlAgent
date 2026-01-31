package cn.mklaus.sqlagent.database;

import cn.mklaus.sqlagent.model.DatabaseConfig;
import cn.mklaus.sqlagent.model.DatabaseType;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.intellij.openapi.diagnostic.Logger;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Database connection manager using HikariCP connection pool
 */
public class DatabaseConnectionManager {
    private static final Logger LOG = Logger.getInstance(DatabaseConnectionManager.class);

    private static DatabaseConnectionManager instance;
    private final Map<String, HikariDataSource> dataSourceMap;

    private DatabaseConnectionManager() {
        this.dataSourceMap = new ConcurrentHashMap<>();
    }

    public static synchronized DatabaseConnectionManager getInstance() {
        if (instance == null) {
            instance = new DatabaseConnectionManager();
        }
        return instance;
    }

    /**
     * Get a connection from the pool
     */
    public Connection getConnection(DatabaseConfig config) throws SQLException {
        String key = getDataSourceKey(config);

        HikariDataSource dataSource = dataSourceMap.computeIfAbsent(key, k -> {
            try {
                return createDataSource(config);
            } catch (Exception e) {
                LOG.error("Failed to create data source for " + config.getName(), e);
                throw new RuntimeException("Failed to create data source", e);
            }
        });

        return dataSource.getConnection();
    }

    /**
     * Create a new HikariCP data source
     */
    private HikariDataSource createDataSource(DatabaseConfig config) {
        HikariConfig hikariConfig = new HikariConfig();

        hikariConfig.setJdbcUrl(config.getJdbcUrl());
        hikariConfig.setUsername(config.getUsername());
        hikariConfig.setPassword(config.getPassword());

        // Database driver configuration
        try {
            hikariConfig.setDriverClassName(config.getType().getDriverClassName());
        } catch (Exception e) {
            LOG.error("Failed to load driver: " + config.getType().getDriverClassName(), e);
            throw new RuntimeException("Failed to load database driver", e);
        }

        // Connection pool settings
        hikariConfig.setMinimumIdle(2);
        hikariConfig.setMaximumPoolSize(10);
        hikariConfig.setConnectionTimeout(30000); // 30 seconds
        hikariConfig.setIdleTimeout(600000); // 10 minutes
        hikariConfig.setMaxLifetime(1800000); // 30 minutes

        // MySQL specific optimizations
        if (config.getType() == DatabaseType.MYSQL) {
            hikariConfig.addDataSourceProperty("useUnicode", "true");
            hikariConfig.addDataSourceProperty("characterEncoding", "UTF-8");
            hikariConfig.addDataSourceProperty("useSSL", "false");
            hikariConfig.addDataSourceProperty("allowPublicKeyRetrieval", "true");
            hikariConfig.addDataSourceProperty("serverTimezone", "UTC");
        }
        // PostgreSQL specific optimizations
        else if (config.getType() == DatabaseType.POSTGRESQL) {
            hikariConfig.addDataSourceProperty("ssl", "false");
            hikariConfig.addDataSourceProperty("prepareThreshold", "0");
        }

        hikariConfig.setPoolName("SQLAgent-" + config.getName());

        LOG.info("Created data source for " + config.getName() + " (" + config.getJdbcUrl() + ")");

        return new HikariDataSource(hikariConfig);
    }

    /**
     * Test database connection
     */
    public boolean testConnection(DatabaseConfig config) {
        try (Connection conn = getConnection(config)) {
            return conn.isValid(5); // 5 second timeout
        } catch (Exception e) {
            LOG.error("Connection test failed for " + config.getName(), e);
            return false;
        }
    }

    /**
     * Close and remove a data source
     */
    public void closeDataSource(DatabaseConfig config) {
        String key = getDataSourceKey(config);
        HikariDataSource dataSource = dataSourceMap.remove(key);
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            LOG.info("Closed data source for " + config.getName());
        }
    }

    /**
     * Close all data sources
     */
    public void closeAll() {
        dataSourceMap.forEach((key, dataSource) -> {
            if (!dataSource.isClosed()) {
                dataSource.close();
            }
        });
        dataSourceMap.clear();
        LOG.info("Closed all data sources");
    }

    /**
     * Generate unique key for data source map
     */
    private String getDataSourceKey(DatabaseConfig config) {
        return config.getType().name() + ":" +
               config.getHost() + ":" +
               config.getPort() + "/" +
               config.getDatabase();
    }
}
