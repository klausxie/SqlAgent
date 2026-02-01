package cn.mklaus.sqlagent.mcp;

import cn.mklaus.sqlagent.mcp.config.DatabaseConfig;
import cn.mklaus.sqlagent.mcp.tools.*;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Database MCP Server - Main server class
 *
 * Implements MCP (Model Context Protocol) server for database operations.
 * Communicates via STDIO using JSON-RPC 2.0 protocol.
 */
public class DatabaseMcpServer {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseMcpServer.class);
    private static final Gson GSON = new Gson();

    private final DatabaseConfig config;
    private final Map<String, McpTool> tools;
    private final Map<String, Object> toolContexts; // Context for each tool instance

    public DatabaseMcpServer(DatabaseConfig config) {
        this.config = config;
        this.tools = new ConcurrentHashMap<>();
        this.toolContexts = new ConcurrentHashMap<>();
        initializeTools();
    }

    /**
     * Initialize all MCP tools
     */
    private void initializeTools() {
        try {
            // Initialize tools with database context
            tools.put("get_table_metadata", new GetTableMetadataTool(config));
            tools.put("explain_sql", new ExplainSqlTool(config));
            tools.put("parse_sql", new ParseSqlTool());
            tools.put("list_tables", new ListTablesTool(config));

            logger.info("Initialized {} MCP tools", tools.size());
        } catch (Exception e) {
            logger.error("Failed to initialize tools", e);
            throw new RuntimeException("Tool initialization failed", e);
        }
    }

    /**
     * Start the MCP server
     * Reads JSON-RPC requests from stdin and writes responses to stdout
     */
    public void start() {
        logger.info("Starting MCP server on STDIO...");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }

                try {
                    JsonObject request = GSON.fromJson(line, JsonObject.class);
                    JsonObject response = handleRequest(request);
                    String responseJson = GSON.toJson(response);
                    System.out.println(responseJson);
                    System.out.flush();
                } catch (Exception e) {
                    logger.error("Error processing request: {}", line, e);
                    sendError(-1, "Request processing error: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            logger.error("Server error", e);
            throw new RuntimeException("Server failed", e);
        }
    }

    /**
     * Handle incoming JSON-RPC request
     */
    private JsonObject handleRequest(JsonObject request) {
        String method = request.has("method") ? request.get("method").getAsString() : null;

        if (method == null) {
            return sendError(null, "Missing 'method' field");
        }

        // Handle different method types
        switch (method) {
            case "initialize":
                return handleInitialize(request);
            case "tools/list":
                return handleListTools(request);
            case "tools/call":
                return handleToolCall(request);
            case "ping":
                return handlePing(request);
            default:
                return sendError(request.get("id"), "Unknown method: " + method);
        }
    }

    /**
     * Handle initialize request
     */
    private JsonObject handleInitialize(JsonObject request) {
        JsonObject response = new JsonObject();
        response.addProperty("jsonrpc", "2.0");
        response.add("id", request.get("id"));

        JsonObject result = new JsonObject();
        result.addProperty("name", "sqlagent-database-tools");
        result.addProperty("version", "1.0.0");
        result.addProperty("protocolVersion", "2024-11-05");

        JsonObject serverInfo = new JsonObject();
        serverInfo.addProperty("name", "SqlAgent Database Tools");
        serverInfo.addProperty("version", "1.0.0");
        result.add("serverInfo", serverInfo);

        JsonObject capabilities = new JsonObject();
        JsonObject tools = new JsonObject();
        capabilities.add("tools", tools);
        result.add("capabilities", capabilities);

        response.add("result", result);
        return response;
    }

    /**
     * Handle tools/list request
     */
    private JsonObject handleListTools(JsonObject request) {
        JsonObject response = new JsonObject();
        response.addProperty("jsonrpc", "2.0");
        response.add("id", request.get("id"));

        JsonObject result = new JsonObject();
        result.add("tools", GSON.toJsonTree(getToolDescriptions()));

        response.add("result", result);
        return response;
    }

    /**
     * Handle tools/call request
     */
    private JsonObject handleToolCall(JsonObject request) {
        JsonObject params = request.getAsJsonObject("params");
        String toolName = params.get("name").getAsString();
        JsonObject arguments = params.getAsJsonObject("arguments");

        McpTool tool = tools.get(toolName);
        if (tool == null) {
            return sendError(request.get("id"), "Unknown tool: " + toolName);
        }

        try {
            JsonObject result = tool.execute(arguments);
            return sendSuccess(request.get("id"), result);
        } catch (Exception e) {
            logger.error("Tool execution error: {}", toolName, e);
            return sendError(request.get("id"),
                "Tool execution failed: " + e.getMessage());
        }
    }

    /**
     * Handle ping request
     */
    private JsonObject handlePing(JsonObject request) {
        return sendSuccess(request.get("id"), new JsonObject());
    }

    /**
     * Get descriptions of all available tools
     */
    private Map<String, JsonObject> getToolDescriptions() {
        Map<String, JsonObject> descriptions = new HashMap<>();

        for (Map.Entry<String, McpTool> entry : tools.entrySet()) {
            JsonObject desc = new JsonObject();
            desc.addProperty("name", entry.getKey());
            desc.addProperty("description", entry.getValue().getDescription());
            desc.add("inputSchema", GSON.toJsonTree(entry.getValue().getInputSchema()));
            descriptions.put(entry.getKey(), desc);
        }

        return descriptions;
    }

    /**
     * Send successful response
     */
    private JsonObject sendSuccess(Object id, JsonObject result) {
        JsonObject response = new JsonObject();
        response.addProperty("jsonrpc", "2.0");
        if (id != null && !id.equals(JsonNull.INSTANCE)) {
            response.add("id", GSON.toJsonTree(id));
        }
        response.add("result", result);
        return response;
    }

    /**
     * Send error response
     */
    private JsonObject sendError(Object id, String message) {
        JsonObject response = new JsonObject();
        response.addProperty("jsonrpc", "2.0");
        if (id != null && !id.equals(JsonNull.INSTANCE)) {
            response.add("id", GSON.toJsonTree(id));
        }

        JsonObject error = new JsonObject();
        error.addProperty("code", -32000);
        error.addProperty("message", message);
        response.add("error", error);

        return response;
    }
}
