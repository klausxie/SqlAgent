package cn.mklaus.sqlagent.mcp.tools;

import com.google.gson.JsonObject;

/**
 * Interface for MCP tools
 */
public interface McpTool {
    /**
     * Get tool description
     */
    String getDescription();

    /**
     * Get input schema (JSON Schema format)
     */
    JsonObject getInputSchema();

    /**
     * Execute the tool with given arguments
     */
    JsonObject execute(JsonObject arguments) throws Exception;
}
