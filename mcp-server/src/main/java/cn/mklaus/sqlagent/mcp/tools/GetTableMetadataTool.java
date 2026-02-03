package cn.mklaus.sqlagent.mcp.tools;

import cn.mklaus.sqlagent.mcp.config.DatabaseConfig;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * MCP Tool for getting table metadata
 */
public class GetTableMetadataTool implements McpTool {
    private static final Logger logger = LoggerFactory.getLogger(GetTableMetadataTool.class);
    private final DatabaseConfig config;

    public GetTableMetadataTool(DatabaseConfig config) {
        this.config = config;
    }

    @Override
    public String getDescription() {
        return "Get detailed metadata for a database table including columns, indexes, and row count";
    }

    @Override
    public JsonObject getInputSchema() {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");

        JsonObject properties = new JsonObject();
        JsonObject tableName = new JsonObject();
        tableName.addProperty("type", "string");
        tableName.addProperty("description", "Name of the table");
        properties.add("table_name", tableName);

        schema.add("properties", properties);

        JsonArray required = new JsonArray();
        required.add("table_name");
        schema.add("required", required);

        return schema;
    }

    @Override
    public JsonObject execute(JsonObject arguments) throws Exception {
        String tableName = arguments.get("table_name").getAsString();

        logger.info("Getting metadata for table: {}", tableName);

        try (HikariDataSource dataSource = config.createDataSource();
             Connection conn = dataSource.getConnection()) {

            JsonObject result = new JsonObject();
            result.addProperty("table_name", tableName);
            result.add("columns", getColumns(conn, tableName));
            result.add("indexes", getIndexes(conn, tableName));
            result.addProperty("row_count", getRowCount(conn, tableName));

            return result;

        } catch (SQLException e) {
            logger.error("Database error while getting metadata for table: {}", tableName, e);
            throw new Exception("Failed to get table metadata: " + e.getMessage());
        }
    }

    /**
     * Get column information for table
     */
    private JsonArray getColumns(Connection conn, String tableName) throws SQLException {
        JsonArray columns = new JsonArray();

        String query;
        String nameCol, typeCol, nullableCol, defaultCol;

        if (config.getType().equalsIgnoreCase("postgresql")) {
            nameCol = "column_name";
            typeCol = "data_type";
            nullableCol = "is_nullable";
            defaultCol = "column_default";
            query = "SELECT column_name, data_type, is_nullable, column_default " +
                    "FROM information_schema.columns " +
                    "WHERE table_name = ? " +
                    "ORDER BY ordinal_position";
        } else { // MySQL
            nameCol = "COLUMN_NAME";
            typeCol = "DATA_TYPE";
            nullableCol = "IS_NULLABLE";
            defaultCol = "COLUMN_DEFAULT";
            query = "SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE, COLUMN_DEFAULT " +
                    "FROM INFORMATION_SCHEMA.COLUMNS " +
                    "WHERE TABLE_NAME = ? " +
                    "ORDER BY ORDINAL_POSITION";
        }

        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, tableName);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    JsonObject column = new JsonObject();
                    column.addProperty("name", rs.getString(nameCol));
                    column.addProperty("type", rs.getString(typeCol));
                    column.addProperty("nullable", rs.getString(nullableCol));

                    String defaultValue = rs.getString(defaultCol);
                    if (defaultValue != null && !rs.wasNull()) {
                        column.addProperty("default_value", defaultValue);
                    }

                    columns.add(column);
                }
            }
        }

        return columns;
    }

    /**
     * Get index information for table
     */
    private JsonArray getIndexes(Connection conn, String tableName) throws SQLException {
        JsonArray indexes = new JsonArray();

        String query;
        if (config.getType().equalsIgnoreCase("postgresql")) {
            query = "SELECT indexname, indexdef " +
                    "FROM pg_indexes " +
                    "WHERE tablename = ?";
        } else { // MySQL
            query = "SELECT INDEX_NAME as index_name, " +
                    "GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) as columns, " +
                    "NOT NON_UNIQUE as is_unique " +
                    "FROM INFORMATION_SCHEMA.STATISTICS " +
                    "WHERE TABLE_NAME = ? AND TABLE_SCHEMA = DATABASE() " +
                    "GROUP BY INDEX_NAME, NON_UNIQUE";
        }

        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, tableName);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    JsonObject index = new JsonObject();

                    if (config.getType().equalsIgnoreCase("postgresql")) {
                        index.addProperty("name", rs.getString("indexname"));
                        index.addProperty("definition", rs.getString("indexdef"));
                    } else {
                        index.addProperty("name", rs.getString("index_name"));
                        index.addProperty("columns", rs.getString("columns"));
                        index.addProperty("unique", rs.getBoolean("is_unique"));
                    }

                    indexes.add(index);
                }
            }
        }

        return indexes;
    }

    /**
     * Get row count for table
     */
    private long getRowCount(Connection conn, String tableName) throws SQLException {
        String query = "SELECT COUNT(*) FROM " + tableName;

        try (PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getLong(1);
            }
        }

        return 0;
    }
}
