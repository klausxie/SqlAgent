package cn.mklaus.sqlagent.service;

import cn.mklaus.sqlagent.model.*;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * Unit tests for PromptBuilder
 *
 * Tests for the simplified prompt builder that delegates metadata retrieval
 * to OpenCode MCP tools.
 */
public class PromptBuilderTest {

    private PromptBuilder promptBuilder;
    private OptimizationRequest request;

    @Before
    public void setUp() {
        promptBuilder = new PromptBuilder();

        // Create a test request (simplified - no database config)
        request = new OptimizationRequest();

        // Set SQL
        request.setOriginalSql("SELECT * FROM users WHERE email = 'test@example.com'");

        // Set optimization goals
        request.setOptimizationGoals(Arrays.asList(
                "Performance optimization (index usage, execution plan)",
                "Cost optimization (resource usage)",
                "Syntax corrections and best practices"
        ));
    }

    @Test
    public void testBuildPrompt_BasicStructure() {
        String prompt = promptBuilder.buildPrompt(request);

        assertNotNull(prompt);
        assertFalse(prompt.isEmpty());

        // Verify prompt contains key sections
        assertTrue(prompt.contains("Original Query"));
        assertTrue(prompt.contains("Optimization Goals"));

        // Verify prompt references MCP tools and Skill
        assertTrue(prompt.contains("sql-optimizer") || prompt.contains("database-tools"));
        assertTrue(prompt.contains("MCP") || prompt.contains("tools"));
    }

    @Test
    public void testBuildPrompt_ContainsOriginalSql() {
        String prompt = promptBuilder.buildPrompt(request);

        assertTrue(prompt.contains("SELECT * FROM users WHERE email = 'test@example.com'"));
    }

    @Test
    public void testBuildPrompt_ContainsDatabaseInfo() {
        // Note: Database info is no longer included in prompt
        // It's configured in MCP server environment variables
        String prompt = promptBuilder.buildPrompt(request);

        // Should mention MCP server has the database config
        assertTrue(prompt.contains("MCP server") || prompt.contains("database-tools"));
    }

    @Test
    public void testBuildPrompt_ReferencesMCPTools() {
        String prompt = promptBuilder.buildPrompt(request);

        // Verify prompt mentions MCP tools for metadata retrieval
        assertTrue(prompt.contains("database-tools") || prompt.contains("MCP"));
    }

    @Test
    public void testBuildPrompt_WithPostgreSQL() {
        // Note: Database type is no longer included in prompt
        // MCP server determines the database type from its environment
        String prompt = promptBuilder.buildPrompt(request);

        assertNotNull(prompt);
        assertTrue(prompt.contains("Original Query"));
    }

    @Test
    public void testBuildPrompt_ContainsJsonFormatRequest() {
        String prompt = promptBuilder.buildPrompt(request);

        // Verify prompt requests JSON response format
        assertTrue(prompt.contains("JSON") || prompt.contains("json") || prompt.contains("format"));
    }

    @Test
    public void testBuildPrompt_ContainsOptimizationGoals() {
        String prompt = promptBuilder.buildPrompt(request);

        assertTrue(prompt.contains("Performance optimization"));
        assertTrue(prompt.contains("Cost optimization"));
    }

    @Test
    public void testBuildPrompt_WithoutMetadata() {
        // In the new implementation, metadata is not included in the prompt
        // It's retrieved by OpenCode MCP tools
        String prompt = promptBuilder.buildPrompt(request);

        // Prompt should not contain metadata sections
        assertFalse(prompt.contains("Table Schema"));
        assertFalse(prompt.contains("Indexes"));
        assertFalse(prompt.contains("Current Execution Plan"));

        // But should mention using MCP tools to get this info
        assertTrue(prompt.contains("database-tools") || prompt.contains("MCP"));
    }

    @Test
    public void testBuildPrompt_EmptyGoals() {
        request.setOptimizationGoals(null);

        String prompt = promptBuilder.buildPrompt(request);

        assertNotNull(prompt);
        // Should still contain essential parts even without goals
        assertTrue(prompt.contains("Original Query"));
    }
}
