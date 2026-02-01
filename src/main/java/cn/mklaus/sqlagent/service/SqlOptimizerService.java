package cn.mklaus.sqlagent.service;

import cn.mklaus.sqlagent.model.*;
import cn.mklaus.sqlagent.opencode.OpenCodeClient;
import com.intellij.openapi.diagnostic.Logger;

import java.util.Arrays;

/**
 * Service for orchestrating SQL optimization
 *
 * Simplified version - database operations are now handled by OpenCode MCP tools.
 * The plugin only sends SQL; database configuration is managed by OpenCode MCP server.
 */
public class SqlOptimizerService {
    private static final Logger LOG = Logger.getInstance(SqlOptimizerService.class);

    private final String openCodeServerUrl;

    public SqlOptimizerService(String openCodeServerUrl) {
        this.openCodeServerUrl = openCodeServerUrl;
    }

    /**
     * Optimize a SQL query
     *
     * This method now only sends the SQL to OpenCode.
     * OpenCode will use the database-tools MCP server to:
     * - Parse the SQL to extract table names
     * - Get table metadata (columns, indexes, row counts)
     * - Get the execution plan
     *
     * Database configuration is managed by OpenCode MCP tools in ~/.opencode/config.json
     *
     * @param originalSql The SQL query to optimize
     * @return Optimization response with optimized SQL and suggestions
     */
    public OptimizationResponse optimize(String originalSql) {
        try {
            LOG.info("Starting optimization for SQL: " + originalSql.substring(0, Math.min(50, originalSql.length())) + "...");

            // Build simplified request - just SQL, no database config
            OptimizationRequest request = buildOptimizationRequest(originalSql);

            // Send to OpenCode - AI will use MCP tools to gather metadata and execution plan
            OpenCodeClient client = new OpenCodeClient(openCodeServerUrl);
            OptimizationResponse response = client.optimize(request);

            LOG.info("Optimization completed. Has error: " + response.hasError());

            return response;

        } catch (Exception e) {
            LOG.error("Service error", e);
            return createErrorResponse("Service error: " + e.getMessage());
        }
    }

    /**
     * Build optimization request with SQL only
     *
     * Note: Metadata and execution plan are retrieved by OpenCode using MCP tools.
     * Database configuration is managed by MCP server environment variables.
     */
    private OptimizationRequest buildOptimizationRequest(String originalSql) {
        OptimizationRequest request = new OptimizationRequest();
        request.setOriginalSql(originalSql);
        request.setOptimizationGoals(Arrays.asList(
                "Performance optimization (index usage, execution plan)",
                "Cost optimization (resource usage)",
                "Syntax corrections and best practices"
        ));
        return request;
    }

    /**
     * Create error response with message
     */
    private OptimizationResponse createErrorResponse(String errorMessage) {
        OptimizationResponse response = new OptimizationResponse();
        response.setErrorMessage(errorMessage);
        return response;
    }
}
