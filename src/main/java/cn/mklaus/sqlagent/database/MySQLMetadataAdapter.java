package cn.mklaus.sqlagent.database;

import cn.mklaus.sqlagent.model.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * MySQL metadata adapter for extracting table metadata
 *
 * @deprecated Database metadata operations are now handled by OpenCode MCP tools.
 *             This adapter is kept for potential future use but is no longer used by the plugin.
 */
@Deprecated
public class MySQLMetadataAdapter implements MetadataAdapter {

    /**
     * Extract complete table metadata including columns, indexes, and statistics
     */
    @Override
    public TableMetadata extractTableMetadata(Connection conn, String tableName) throws SQLException {
        TableMetadata metadata = new TableMetadata();
        metadata.setTableName(tableName);

        // Extract column information
        extractColumns(conn, metadata);

        // Extract index information
        extractIndexes(conn, metadata);

        // Extract row count
        extractRowCount(conn, metadata);

        return metadata;
    }

    /**
     * Extract column information from database metadata
     */
    private void extractColumns(Connection conn, TableMetadata metadata) throws SQLException {
        DatabaseMetaData dbMeta = conn.getMetaData();

        try (ResultSet rs = dbMeta.getColumns(
                conn.getCatalog(),
                null,
                metadata.getTableName(),
                null
        )) {
            while (rs.next()) {
                ColumnInfo column = new ColumnInfo();
                column.setColumnName(rs.getString("COLUMN_NAME"));
                column.setDataType(rs.getInt("DATA_TYPE"));
                column.setTypeName(rs.getString("TYPE_NAME"));
                column.setColumnSize(rs.getInt("COLUMN_SIZE"));
                column.setNullable(rs.getInt("NULLABLE") == DatabaseMetaData.columnNullable);
                column.setColumnDefault(rs.getString("COLUMN_DEF"));
                column.setRemark(rs.getString("REMARKS"));
                column.setAutoIncrement("YES".equalsIgnoreCase(rs.getString("IS_AUTOINCREMENT")));

                metadata.addColumn(column);
            }
        }
    }

    /**
     * Extract index information from database metadata
     */
    private void extractIndexes(Connection conn, TableMetadata metadata) throws SQLException {
        DatabaseMetaData dbMeta = conn.getMetaData();

        try (ResultSet rs = dbMeta.getIndexInfo(
                conn.getCatalog(),
                null,
                metadata.getTableName(),
                false,
                false
        )) {
            String currentIndexName = null;
            IndexInfo currentIndex = null;

            while (rs.next()) {
                String indexName = rs.getString("INDEX_NAME");
                String columnName = rs.getString("COLUMN_NAME");
                boolean unique = !rs.getBoolean("NON_UNIQUE");

                // Group columns by index
                if (!indexName.equals(currentIndexName)) {
                    if (currentIndex != null) {
                        metadata.addIndex(currentIndex);
                    }

                    currentIndex = new IndexInfo();
                    currentIndex.setIndexName(indexName);
                    currentIndex.setUnique(unique);
                    currentIndex.setPrimary("PRIMARY".equalsIgnoreCase(indexName));
                    currentIndex.setIndexType(rs.getString("TYPE"));

                    currentIndexName = indexName;
                }

                if (currentIndex != null) {
                    currentIndex.addColumnName(columnName);
                }
            }

            // Add the last index
            if (currentIndex != null) {
                metadata.addIndex(currentIndex);
            }
        }
    }

    /**
     * Extract row count for the table
     */
    private void extractRowCount(Connection conn, TableMetadata metadata) {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + metadata.getTableName())) {
            if (rs.next()) {
                metadata.setRowCount(rs.getLong(1));
            }
        } catch (SQLException e) {
            // Row count extraction failed, set to unknown (-1)
            metadata.setRowCount(-1);
        }
    }

    /**
     * Get execution plan for a SQL query using EXPLAIN
     */
    @Override
    public String getExecutionPlan(Connection conn, String sql) throws SQLException {
        StringBuilder plan = new StringBuilder();

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("EXPLAIN " + sql)) {

            ResultSetMetaData rsMeta = rs.getMetaData();
            int columnCount = rsMeta.getColumnCount();

            appendExecutionPlanHeader(plan, rsMeta, columnCount);
            appendExecutionPlanSeparator(plan, columnCount);
            appendExecutionPlanRows(plan, rs, columnCount);
        }

        return plan.toString();
    }

    /**
     * Append execution plan header row
     */
    private void appendExecutionPlanHeader(StringBuilder plan, ResultSetMetaData rsMeta, int columnCount)
            throws SQLException {
        for (int i = 1; i <= columnCount; i++) {
            if (i > 1) plan.append(" | ");
            plan.append(String.format("%-20s", rsMeta.getColumnLabel(i)));
        }
        plan.append("\n");
    }

    /**
     * Append execution plan separator line
     */
    private void appendExecutionPlanSeparator(StringBuilder plan, int columnCount) {
        plan.append("-".repeat(20 * columnCount + (columnCount - 1) * 3)).append("\n");
    }

    /**
     * Append execution plan data rows
     */
    private void appendExecutionPlanRows(StringBuilder plan, ResultSet rs, int columnCount) throws SQLException {
        while (rs.next()) {
            for (int i = 1; i <= columnCount; i++) {
                if (i > 1) plan.append(" | ");
                String value = rs.getString(i);
                plan.append(String.format("%-20s", value != null ? value : "NULL"));
            }
            plan.append("\n");
        }
    }

    /**
     * Mark primary key columns in metadata
     */
    @Override
    public void markPrimaryKeys(Connection conn, TableMetadata metadata) throws SQLException {
        DatabaseMetaData dbMeta = conn.getMetaData();

        try (ResultSet rs = dbMeta.getPrimaryKeys(
                conn.getCatalog(),
                null,
                metadata.getTableName()
        )) {
            while (rs.next()) {
                String pkColumnName = rs.getString("COLUMN_NAME");

                // Find and mark the column as primary key
                for (ColumnInfo column : metadata.getColumns()) {
                    if (column.getColumnName().equalsIgnoreCase(pkColumnName)) {
                        column.setPrimaryKey(true);
                        break;
                    }
                }
            }
        }
    }
}
