package cn.mklaus.sqlagent.service;

import cn.mklaus.sqlagent.config.SqlAgentConfigurable;
import cn.mklaus.sqlagent.model.*;
import cn.mklaus.sqlagent.opencode.OpenCodeClient;
import cn.mklaus.sqlagent.opencode.OpenCodeServerManager;
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
    private final OpenCodeServerManager serverManager;
    private final SqlAgentConfigurable.State settings;

    public SqlOptimizerService(String openCodeServerUrl, SqlAgentConfigurable.State settings) {
        this.openCodeServerUrl = openCodeServerUrl;
        this.settings = settings;
        this.serverManager = new OpenCodeServerManager(openCodeServerUrl, settings.openCodeExecutablePath);
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
     * Database configuration is managed by OpenCode MCP tools in ~/.opencode/opencode.json
     *
     * @param originalSql The SQL query to optimize
     * @return Optimization response with optimized SQL and suggestions
     */
    public OptimizationResponse optimize(String originalSql) {
        try {
            LOG.info("Starting optimization for SQL: " + originalSql.substring(0, Math.min(50, originalSql.length())) + "...");
            LOG.info("OpenCode server URL: " + openCodeServerUrl);
            LOG.info("Auto-start server: " + settings.autoStartServer);

            // Check if server is running
            boolean isServerRunning = serverManager.isServerRunning();
            LOG.info("Server running: " + isServerRunning);

            // Auto-start OpenCode server if enabled and not running
            if (settings.autoStartServer && !isServerRunning) {
                LOG.info("OpenCode server not running, attempting to auto-start...");
                if (!serverManager.ensureServerRunning()) {
                    LOG.error("Failed to start OpenCode server");
                    String errorMsg = serverManager.getDetailedErrorMessage();
                    LOG.error("Error details: " + errorMsg);
                    return createDetailedErrorResponse("OpenCode server not available", errorMsg);
                }
                LOG.info("OpenCode server started successfully");
            } else if (!isServerRunning) {
                LOG.warn("OpenCode server is not running and auto-start is disabled");
                return createDetailedErrorResponse(
                    "Cannot connect to OpenCode server",
                    "Please start OpenCode server:\n" +
                    "  Run: opencode server\n" +
                    "  Or enable auto-start in plugin settings"
                );
            }

            // Build simplified request - just SQL, no database config
            OptimizationRequest request = buildOptimizationRequest(originalSql);
            LOG.info("Sending optimization request...");

            // Send to OpenCode - AI will use MCP tools to gather metadata and execution plan
            OpenCodeClient client = new OpenCodeClient(openCodeServerUrl);
            OptimizationResponse response = client.optimize(request);

            LOG.info("Optimization completed. Has error: " + response.hasError());

            if (response.hasError()) {
                LOG.error("Optimization error: " + response.getErrorMessage());
            }

            return response;

        } catch (Exception e) {
            LOG.error("Service error", e);
            return createDetailedErrorResponse(
                "Service error: " + e.getMessage(),
                "Please check:\n" +
                "  1. OpenCode server is running\n" +
                "  2. Server URL is correct: " + openCodeServerUrl + "\n" +
                "  3. Plugin logs: Help → Show Log in Explorer"
            );
        }
    }

    /**
     * Create detailed error response with suggestions
     */
    private OptimizationResponse createDetailedErrorResponse(String title, String suggestions) {
        OptimizationResponse response = new OptimizationResponse();
        StringBuilder errorMsg = new StringBuilder();
        errorMsg.append(title).append("\n\n");
        errorMsg.append("Details:\n");
        errorMsg.append(suggestions);
        response.setErrorMessage(errorMsg.toString());
        return response;
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
