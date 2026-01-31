package cn.mklaus.sqlagent.database;

import cn.mklaus.sqlagent.model.DatabaseConfig;
import cn.mklaus.sqlagent.model.DatabaseType;

/**
 * Factory for creating appropriate metadata adapters based on database type
 */
public class MetadataAdapterFactory {

    /**
     * Get a metadata adapter for the specified database configuration
     *
     * @param config Database configuration
     * @return Appropriate metadata adapter
     * @throws IllegalArgumentException if database type is not supported
     */
    public static MetadataAdapter getAdapter(DatabaseConfig config) {
        if (config == null || config.getType() == null) {
            throw new IllegalArgumentException("Database configuration or type cannot be null");
        }

        return getAdapter(config.getType());
    }

    /**
     * Get a metadata adapter for the specified database type
     *
     * @param type Database type
     * @return Appropriate metadata adapter
     * @throws IllegalArgumentException if database type is not supported
     */
    public static MetadataAdapter getAdapter(DatabaseType type) {
        if (type == null) {
            throw new IllegalArgumentException("Database type cannot be null");
        }

        switch (type) {
            case MYSQL:
                return new MySQLMetadataAdapter();
            case POSTGRESQL:
                return new PostgreSQLMetadataAdapter();
            default:
                throw new IllegalArgumentException("Unsupported database type: " + type);
        }
    }
}
