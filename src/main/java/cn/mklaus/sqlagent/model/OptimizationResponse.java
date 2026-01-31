package cn.mklaus.sqlagent.model;

import java.util.List;

/**
 * Optimization response model from OpenCode API
 */
public class OptimizationResponse {
    private String optimizedSql;
    private String explanation;
    private List<OptimizationSuggestion> suggestions;
    private double estimatedImprovement;
    private String errorMessage;

    public OptimizationResponse() {
        this.estimatedImprovement = 0.0;
    }

    public String getOptimizedSql() {
        return optimizedSql;
    }

    public void setOptimizedSql(String optimizedSql) {
        this.optimizedSql = optimizedSql;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public List<OptimizationSuggestion> getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(List<OptimizationSuggestion> suggestions) {
        this.suggestions = suggestions;
    }

    public double getEstimatedImprovement() {
        return estimatedImprovement;
    }

    public void setEstimatedImprovement(double estimatedImprovement) {
        this.estimatedImprovement = estimatedImprovement;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public boolean hasError() {
        return errorMessage != null && !errorMessage.isEmpty();
    }
}
