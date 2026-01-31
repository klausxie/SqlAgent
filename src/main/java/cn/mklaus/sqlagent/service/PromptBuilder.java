package cn.mklaus.sqlagent.service;

import cn.mklaus.sqlagent.model.*;

import java.util.stream.Collectors;

/**
 * Builds optimization prompts for OpenCode AI
 */
public class PromptBuilder {

    private static final String SYSTEM_PROMPT = """
            You are an expert SQL optimization assistant. Your task is to analyze SQL queries
            and provide optimized versions along with detailed explanations.

            Consider the following aspects:
            1. Performance: Execution time, resource usage, index utilization
            2. Index optimization: Missing or redundant indexes
            3. Query rewriting: More efficient SQL patterns
            4. Best practices: Follow database-specific conventions

            Always provide:
            - Optimized SQL query
            - Explanation of changes
            - Expected performance improvement
            - Specific index recommendations (if applicable)

            Format your response as JSON:
            {
              "optimizedSql": "...",
              "explanation": "...",
              "suggestions": [
                {
                  "type": "INDEX_ADDITION|QUERY_REWRITE|JOIN_OPTIMIZATION|SYNTAX_CORRECTION",
                  "title": "...",
                  "description": "...",
                  "sqlSnippet": "...",
                  "impact": "HIGH|MEDIUM|LOW",
                  "complexity": "EASY|MEDIUM|HARD"
                }
              ],
              "estimatedImprovement": 0.95
            }
            """;

    public String buildPrompt(OptimizationRequest request) {
        StringBuilder prompt = new StringBuilder();

        prompt.append(SYSTEM_PROMPT).append("\n\n");

        prompt.append("# SQL Optimization Request\n\n");

        // Original Query
        prompt.append("## Original Query\n");
        prompt.append("```sql\n");
        prompt.append(request.getOriginalSql());
        prompt.append("\n```\n\n");

        // Database Context
        prompt.append("## Database Context\n");
        prompt.append("- Database Type: ").append(request.getDatabase().getType().getDisplayName()).append("\n");
        prompt.append("- Database Name: ").append(request.getDatabase().getDatabase()).append("\n");

        if (request.getTableMetadata() != null) {
            TableMetadata metadata = request.getTableMetadata();
            prompt.append("- Table: ").append(metadata.getTableName());
            if (metadata.getRowCount() >= 0) {
                prompt.append(" (").append(formatNumber(metadata.getRowCount())).append(" rows)");
            }
            prompt.append("\n\n");

            // Table Schema
            appendTableSchema(prompt, metadata);

            // Indexes
            appendIndexes(prompt, metadata);
        }

        // Execution Plan
        if (request.getExecutionPlan() != null && !request.getExecutionPlan().isEmpty()) {
            prompt.append("## Current Execution Plan\n");
            prompt.append("```\n");
            prompt.append(request.getExecutionPlan());
            prompt.append("\n```\n\n");
        }

        // Optimization Goals
        if (request.getOptimizationGoals() != null && !request.getOptimizationGoals().isEmpty()) {
            prompt.append("## Optimization Goals\n");
            for (String goal : request.getOptimizationGoals()) {
                prompt.append("- ").append(goal).append("\n");
            }
            prompt.append("\n");
        }

        prompt.append("Please provide the optimized SQL query in JSON format as specified above.");

        return prompt.toString();
    }

    private void appendTableSchema(StringBuilder prompt, TableMetadata metadata) {
        prompt.append("## Table Schema\n\n");
        prompt.append("**Columns:**\n");

        for (ColumnInfo column : metadata.getColumns()) {
            prompt.append(String.format("- `%s`: %s",
                    column.getColumnName(),
                    column.getTypeName()));

            if (column.isPrimaryKey()) {
                prompt.append(" (PK)");
            }
            if (column.isForeignKey()) {
                prompt.append(" (FK)");
            }
            if (!column.isNullable()) {
                prompt.append(" NOT NULL");
            }
            prompt.append("\n");
        }
        prompt.append("\n");
    }

    private void appendIndexes(StringBuilder prompt, TableMetadata metadata) {
        if (metadata.getIndexes().isEmpty()) {
            return;
        }

        prompt.append("**Indexes:**\n");
        for (IndexInfo index : metadata.getIndexes()) {
            prompt.append(String.format("- **%s** on (`%s`)",
                    index.getIndexName(),
                    String.join("`, `", index.getColumnNames())));

            if (index.isUnique()) {
                prompt.append(" UNIQUE");
            }
            if (index.isPrimary()) {
                prompt.append(" PRIMARY");
            }
            prompt.append("\n");
        }
        prompt.append("\n");
    }

    private String formatNumber(long number) {
        if (number >= 1_000_000) {
            return String.format("%.1fM", number / 1_000_000.0);
        } else if (number >= 1_000) {
            return String.format("%.1fK", number / 1_000.0);
        } else {
            return String.valueOf(number);
        }
    }
}
