package cn.mklaus.sqlagent.database;

import cn.mklaus.sqlagent.model.TableMetadata;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Interface for database metadata adapters
 * Provides abstraction for extracting metadata from different database types
 */
public interface MetadataAdapter {

    /**
     * Extract complete table metadata including columns, indexes, and statistics
     *
     * @param conn Database connection
     * @param tableName Name of the table
     * @return Table metadata
     * @throws SQLException if extraction fails
     */
    TableMetadata extractTableMetadata(Connection conn, String tableName) throws SQLException;

    /**
     * Mark primary key columns in metadata
     *
     * @param conn Database connection
     * @param metadata Table metadata to mark
     * @throws SQLException if marking fails
     */
    void markPrimaryKeys(Connection conn, TableMetadata metadata) throws SQLException;

    /**
     * Get execution plan for a SQL query
     *
     * @param conn Database connection
     * @param sql SQL query
     * @return Formatted execution plan
     * @throws SQLException if execution plan retrieval fails
     */
    String getExecutionPlan(Connection conn, String sql) throws SQLException;
}
