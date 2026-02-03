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
            LOG.info("Server: " + openCodeServerUrl + ", auto-start: " + settings.autoStartServer);

            boolean isServerRunning = serverManager.isServerRunning();
            LOG.info("Server running: " + isServerRunning);

            if (settings.autoStartServer && !isServerRunning) {
                LOG.info("Attempting to auto-start OpenCode server...");
                if (!serverManager.ensureServerRunning()) {
                    return createDetailedErrorResponse("OpenCode server not available",
                        serverManager.getDetailedErrorMessage());
                }
                LOG.info("OpenCode server started successfully");
            } else if (!isServerRunning) {
                return createDetailedErrorResponse("Cannot connect to OpenCode server",
                    "Please start OpenCode server:\n  Run: opencode server\n  Or enable auto-start in plugin settings");
            }

            OptimizationRequest request = buildOptimizationRequest(originalSql);
            OptimizationResponse response = new OpenCodeClient(openCodeServerUrl).optimize(request);

            LOG.info("Optimization completed. Has error: " + response.hasError());
            if (response.hasError()) {
                LOG.error("Optimization error: " + response.getErrorMessage());
            }

            return response;

        } catch (Exception e) {
            LOG.error("Service error", e);
            return createDetailedErrorResponse("Service error: " + e.getMessage(),
                "Please check:\n  1. OpenCode server is running\n  2. Server URL: " + openCodeServerUrl);
        }
    }

    /**
     * Create detailed error response with suggestions
     */
    private OptimizationResponse createDetailedErrorResponse(String title, String details) {
        OptimizationResponse response = new OptimizationResponse();
        response.setErrorMessage(title + "\n\nDetails:\n" + details);
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
