package cn.mklaus.sqlagent.model;

import java.util.List;

/**
 * Optimization request model for OpenCode API
 *
 * Simplified version - only contains SQL and optimization goals.
 * Database metadata is retrieved by OpenCode MCP tools.
 */
public class OptimizationRequest {
    private String originalSql;
    private List<String> optimizationGoals;

    public OptimizationRequest() {
    }

    public String getOriginalSql() {
        return originalSql;
    }

    public void setOriginalSql(String originalSql) {
        this.originalSql = originalSql;
    }

    public List<String> getOptimizationGoals() {
        return optimizationGoals;
    }

    public void setOptimizationGoals(List<String> optimizationGoals) {
        this.optimizationGoals = optimizationGoals;
    }
}
