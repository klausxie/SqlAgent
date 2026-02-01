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
 * MCP Tool for listing all tables in the database
 */
public class ListTablesTool implements McpTool {
    private static final Logger logger = LoggerFactory.getLogger(ListTablesTool.class);
    private final DatabaseConfig config;

    public ListTablesTool(DatabaseConfig config) {
        this.config = config;
    }

    @Override
    public String getDescription() {
        return "List all tables in the database";
    }

    @Override
    public JsonObject getInputSchema() {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");

        // No parameters required
        schema.addProperty("description", "List all tables in the database");

        return schema;
    }

    @Override
    public JsonObject execute(JsonObject arguments) throws Exception {
        logger.info("Listing tables in database: {}", config.getDatabase());

        try (HikariDataSource dataSource = config.createDataSource();
             Connection conn = dataSource.getConnection()) {

            JsonObject result = new JsonObject();
            result.addProperty("database_type", config.getType());
            result.addProperty("database_name", config.getDatabase());

            // Get table list
            JsonArray tables = getTables(conn);
            result.add("tables", tables);
            result.addProperty("table_count", tables.size());

            return result;

        } catch (SQLException e) {
            logger.error("Database error while listing tables", e);
            throw new Exception("Failed to list tables: " + e.getMessage());
        }
    }

    /**
     * Get list of all tables in the database
     */
    private JsonArray getTables(Connection conn) throws SQLException {
        JsonArray tables = new JsonArray();

        String query;
        if (config.getType().equalsIgnoreCase("postgresql")) {
            query = "SELECT table_name FROM information_schema.tables " +
                    "WHERE table_schema = 'public' " +
                    "AND table_type = 'BASE TABLE' " +
                    "ORDER BY table_name";
        } else { // MySQL
            query = "SELECT TABLE_NAME FROM information_schema.tables " +
                    "WHERE TABLE_SCHEMA = DATABASE() " +
                    "AND TABLE_TYPE = 'BASE TABLE' " +
                    "ORDER BY TABLE_NAME";
        }

        try (PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                tables.add(rs.getString(1));
            }
        }

        return tables;
    }
}
