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
 * MCP Tool for explaining SQL execution plans
 */
public class ExplainSqlTool implements McpTool {
    private static final Logger logger = LoggerFactory.getLogger(ExplainSqlTool.class);
    private final DatabaseConfig config;

    public ExplainSqlTool(DatabaseConfig config) {
        this.config = config;
    }

    @Override
    public String getDescription() {
        return "Explain SQL execution plan to identify performance bottlenecks";
    }

    @Override
    public JsonObject getInputSchema() {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");

        JsonObject properties = new JsonObject();
        JsonObject sql = new JsonObject();
        sql.addProperty("type", "string");
        sql.addProperty("description", "SQL query to explain");
        properties.add("sql", sql);

        schema.add("properties", properties);

        JsonArray required = new JsonArray();
        required.add("sql");
        schema.add("required", required);

        return schema;
    }

    @Override
    public JsonObject execute(JsonObject arguments) throws Exception {
        String sql = arguments.get("sql").getAsString();

        logger.info("Explaining SQL: {}", sql.substring(0, Math.min(50, sql.length())));

        try (HikariDataSource dataSource = config.createDataSource();
             Connection conn = dataSource.getConnection()) {

            JsonObject result = new JsonObject();
            result.addProperty("sql", sql);

            // Use appropriate EXPLAIN syntax for database type
            String explainQuery;
            if (config.getType().equalsIgnoreCase("postgresql")) {
                explainQuery = "EXPLAIN (ANALYZE, FORMAT JSON) " + sql;
            } else { // MySQL
                explainQuery = "EXPLAIN FORMAT=JSON " + sql;
            }

            try (PreparedStatement stmt = conn.prepareStatement(explainQuery);
                 ResultSet rs = stmt.executeQuery()) {

                if (rs.next()) {
                    String explainResult = rs.getString(1);
                    result.addProperty("execution_plan", explainResult);
                }
            }

            return result;

        } catch (SQLException e) {
            logger.error("Database error while explaining SQL", e);
            throw new Exception("Failed to explain SQL: " + e.getMessage());
        }
    }
}
