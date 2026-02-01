package cn.mklaus.sqlagent.model;

import java.time.LocalDateTime;

/**
 * Optimization history entry
 */
public class OptimizationHistory {
    private String id;  // Unique ID (SQL hash)
    private String originalSql;
    private String optimizedSql;
    private String explanation;
    private double improvementScore;  // Performance improvement score (0-100)
    private LocalDateTime timestamp;
    private boolean applied;  // Whether user applied the optimization

    public OptimizationHistory() {
    }

    public OptimizationHistory(String id, String originalSql, String optimizedSql,
                               String explanation, double improvementScore) {
        this.id = id;
        this.originalSql = originalSql;
        this.optimizedSql = optimizedSql;
        this.explanation = explanation;
        this.improvementScore = improvementScore;
        this.timestamp = LocalDateTime.now();
        this.applied = false;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOriginalSql() {
        return originalSql;
    }

    public void setOriginalSql(String originalSql) {
        this.originalSql = originalSql;
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

    public double getImprovementScore() {
        return improvementScore;
    }

    public void setImprovementScore(double improvementScore) {
        this.improvementScore = improvementScore;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isApplied() {
        return applied;
    }

    public void setApplied(boolean applied) {
        this.applied = applied;
    }
}
