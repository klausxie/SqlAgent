package cn.mklaus.sqlagent.model;

import java.util.List;

/**
 * Optimization request model for OpenCode API
 */
public class OptimizationRequest {
    private String originalSql;
    private DatabaseConfig database;
    private TableMetadata tableMetadata;
    private String executionPlan;
    private List<String> optimizationGoals;

    public OptimizationRequest() {
    }

    public String getOriginalSql() {
        return originalSql;
    }

    public void setOriginalSql(String originalSql) {
        this.originalSql = originalSql;
    }

    public DatabaseConfig getDatabase() {
        return database;
    }

    public void setDatabase(DatabaseConfig database) {
        this.database = database;
    }

    public TableMetadata getTableMetadata() {
        return tableMetadata;
    }

    public void setTableMetadata(TableMetadata tableMetadata) {
        this.tableMetadata = tableMetadata;
    }

    public String getExecutionPlan() {
        return executionPlan;
    }

    public void setExecutionPlan(String executionPlan) {
        this.executionPlan = executionPlan;
    }

    public List<String> getOptimizationGoals() {
        return optimizationGoals;
    }

    public void setOptimizationGoals(List<String> optimizationGoals) {
        this.optimizationGoals = optimizationGoals;
    }
}
