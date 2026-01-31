package cn.mklaus.sqlagent.service;

import cn.mklaus.sqlagent.database.DatabaseConnectionManager;
import cn.mklaus.sqlagent.database.MetadataAdapter;
import cn.mklaus.sqlagent.database.MetadataAdapterFactory;
import cn.mklaus.sqlagent.model.*;
import cn.mklaus.sqlagent.opencode.OpenCodeClient;
import com.intellij.openapi.diagnostic.Logger;

import java.sql.Connection;
import java.util.Arrays;
import java.util.List;

/**
 * Service for orchestrating SQL optimization
 */
public class SqlOptimizerService {
    private static final Logger LOG = Logger.getInstance(SqlOptimizerService.class);

    private final String openCodeServerUrl;

    public SqlOptimizerService(String openCodeServerUrl) {
        this.openCodeServerUrl = openCodeServerUrl;
    }

    /**
     * Optimize a SQL query
     */
    public OptimizationResponse optimize(
            String originalSql,
            DatabaseConfig databaseConfig,
            String tableName
    ) {
        try {
            LOG.info("Starting optimization for SQL: " + originalSql.substring(0, Math.min(50, originalSql.length())) + "...");

            DatabaseConnectionManager connectionManager = DatabaseConnectionManager.getInstance();
            try (Connection conn = connectionManager.getConnection(databaseConfig)) {

                MetadataAdapter metadataAdapter = MetadataAdapterFactory.getAdapter(databaseConfig);
                TableMetadata metadata = metadataAdapter.extractTableMetadata(conn, tableName);
                metadataAdapter.markPrimaryKeys(conn, metadata);

                LOG.info("Extracted metadata: " + metadata.getColumns().size() + " columns, " +
                        metadata.getIndexes().size() + " indexes");

                String executionPlan = metadataAdapter.getExecutionPlan(conn, originalSql);

                OptimizationRequest request = buildOptimizationRequest(originalSql, databaseConfig, metadata, executionPlan);

                OpenCodeClient client = new OpenCodeClient(openCodeServerUrl);
                OptimizationResponse response = client.optimize(request);

                LOG.info("Optimization completed. Has error: " + response.hasError());

                return response;

            } catch (Exception e) {
                throw new RuntimeException("Optimization failed", e);
            }

        } catch (Exception e) {
            LOG.error("Service error", e);
            return createErrorResponse("Service error: " + e.getMessage());
        }
    }

    /**
     * Build optimization request with all required data
     */
    private OptimizationRequest buildOptimizationRequest(String originalSql, DatabaseConfig databaseConfig,
                                                         TableMetadata metadata, String executionPlan) {
        OptimizationRequest request = new OptimizationRequest();
        request.setOriginalSql(originalSql);
        request.setDatabase(databaseConfig);
        request.setTableMetadata(metadata);
        request.setExecutionPlan(executionPlan);
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

    /**
     * Extract table name from SQL query
     */
    public String extractTableName(String sql) {
        // Handle null or empty input
        if (sql == null || sql.trim().isEmpty()) {
            return null;
        }

        // Simple extraction - in production, use a proper SQL parser
        String lowerSql = sql.toLowerCase();

        // Find FROM clause
        int fromIndex = lowerSql.indexOf("from ");
        if (fromIndex == -1) {
            fromIndex = lowerSql.indexOf("into ");
        }

        if (fromIndex != -1) {
            int start = fromIndex + 5;
            int end = lowerSql.length();

            // Find end of table name
            for (int i = start; i < lowerSql.length(); i++) {
                char c = lowerSql.charAt(i);
                if (c == ' ' || c == '\n' || c == '\t' || c == ',' || c == ';') {
                    end = i;
                    break;
                }
            }

            String tableName = sql.substring(start, end).trim();
            // Remove backticks if present
            tableName = tableName.replace("`", "");
            return tableName;
        }

        return null;
    }
}
