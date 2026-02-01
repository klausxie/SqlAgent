package cn.mklaus.sqlagent.database;

import cn.mklaus.sqlagent.model.ColumnInfo;
import cn.mklaus.sqlagent.model.IndexInfo;
import cn.mklaus.sqlagent.model.TableMetadata;

import java.sql.*;

/**
 * PostgreSQL metadata adapter for extracting table metadata
 *
 * @deprecated Database metadata operations are now handled by OpenCode MCP tools.
 *             This adapter is kept for potential future use but is no longer used by the plugin.
 */
@Deprecated
public class PostgreSQLMetadataAdapter implements MetadataAdapter {

    private static final String EXPLAIN_FORMAT = "TEXT";

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

    @Override
    public void markPrimaryKeys(Connection conn, TableMetadata metadata) throws SQLException {
        DatabaseMetaData dbMeta = conn.getMetaData();

        try (ResultSet rs = dbMeta.getPrimaryKeys(
                conn.getSchema(),
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

    @Override
    public String getExecutionPlan(Connection conn, String sql) throws SQLException {
        StringBuilder plan = new StringBuilder();

        // PostgreSQL uses EXPLAIN (FORMAT TEXT) for execution plans
        String explainSql = "EXPLAIN (FORMAT " + EXPLAIN_FORMAT + ") " + sql;

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(explainSql)) {

            while (rs.next()) {
                plan.append(rs.getString(1)).append("\n");
            }
        }

        return plan.toString();
    }

    /**
     * Extract column information from database metadata
     */
    private void extractColumns(Connection conn, TableMetadata metadata) throws SQLException {
        DatabaseMetaData dbMeta = conn.getMetaData();

        try (ResultSet rs = dbMeta.getColumns(
                conn.getSchema(),
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

                // PostgreSQL specific: auto-increment is determined via DEFAULT nextval()
                String columnDefault = column.getColumnDefault();
                column.setAutoIncrement(columnDefault != null &&
                        columnDefault.toLowerCase().contains("nextval("));

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
                conn.getSchema(),
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
                if (indexName == null) {
                    continue;
                }

                if (!indexName.equals(currentIndexName)) {
                    if (currentIndex != null) {
                        metadata.addIndex(currentIndex);
                    }

                    currentIndex = new IndexInfo();
                    currentIndex.setIndexName(indexName);
                    currentIndex.setUnique(unique);

                    // PostgreSQL typically identifies primary keys by name (e.g., table_pkey)
                    currentIndex.setPrimary(indexName != null && indexName.endsWith("_pkey"));
                    currentIndex.setIndexType("btree");

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
}
