package cn.mklaus.sqlagent.mcp.tools;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.util.TablesNamesFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

/**
 * MCP Tool for parsing SQL queries
 */
public class ParseSqlTool implements McpTool {
    private static final Logger logger = LoggerFactory.getLogger(ParseSqlTool.class);

    public ParseSqlTool() {
    }

    @Override
    public String getDescription() {
        return "Parse SQL to extract table names, column names, and query type";
    }

    @Override
    public JsonObject getInputSchema() {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");

        JsonObject properties = new JsonObject();
        JsonObject sql = new JsonObject();
        sql.addProperty("type", "string");
        sql.addProperty("description", "SQL query to parse");
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

        logger.debug("Parsing SQL: {}", sql.substring(0, Math.min(50, sql.length())));

        try {
            Statement statement = CCJSqlParserUtil.parse(sql);

            JsonObject result = new JsonObject();
            result.addProperty("sql", sql);
            result.addProperty("query_type", getQueryType(statement));

            // Extract table names
            Set<String> tables = extractTables(statement);
            JsonArray tableArray = new JsonArray();
            for (String table : tables) {
                tableArray.add(table);
            }
            result.add("tables", tableArray);

            return result;

        } catch (Exception e) {
            logger.error("Failed to parse SQL", e);
            throw new Exception("Failed to parse SQL: " + e.getMessage());
        }
    }

    /**
     * Get the type of SQL query
     */
    private String getQueryType(Statement statement) {
        if (statement instanceof Select) {
            return "SELECT";
        } else if (statement instanceof Insert) {
            return "INSERT";
        } else if (statement instanceof Update) {
            return "UPDATE";
        } else if (statement instanceof Delete) {
            return "DELETE";
        } else {
            return statement.getClass().getSimpleName().toUpperCase();
        }
    }

    /**
     * Extract table names from SQL statement
     */
    private Set<String> extractTables(Statement statement) {
        TablesNamesFinder tablesNamesFinder = new TablesNamesFinder();
        return new HashSet<>(tablesNamesFinder.getTableList(statement));
    }
}
