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

            ## Use Available Tools

            You have access to MCP tools and skills to help with optimization:

            1. **Load the sql-optimizer skill**: Use the `skill` tool to load "sql-optimizer"
               - This skill provides detailed workflow for SQL optimization

            2. **Use database-tools MCP**: Available tools:
               - `parse_sql`: Extract table names and query structure
               - `get_table_metadata`: Get columns, indexes, and row counts
               - `explain_sql`: Get the execution plan
               - `list_tables`: List all tables in the database

            3. **Follow the sql-optimizer workflow**:
               - Parse the SQL to identify tables
               - Get metadata for each table using MCP tools
               - Analyze the execution plan using MCP tools
               - Generate specific optimization recommendations

            ## Response Format

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

            IMPORTANT: Always use the available MCP tools to gather real database metadata
            and execution plans before making recommendations. Do not guess about the database
            structure - use the tools!
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

        // Note: Database connection info is configured in MCP server environment
        prompt.append("<!-- Database connection is configured in MCP server -->\n");
        prompt.append("<!-- Use database-tools MCP to get table metadata and execution plan -->\n\n");

        // Optimization Goals
        if (request.getOptimizationGoals() != null && !request.getOptimizationGoals().isEmpty()) {
            prompt.append("## Optimization Goals\n");
            for (String goal : request.getOptimizationGoals()) {
                prompt.append("- ").append(goal).append("\n");
            }
            prompt.append("\n");
        }

        prompt.append("Please use the sql-optimizer skill and database-tools MCP to analyze this query ");
        prompt.append("and provide the optimized SQL query in JSON format as specified above.");

        return prompt.toString();
    }
}
