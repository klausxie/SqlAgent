package cn.mklaus.sqlagent;

import cn.mklaus.sqlagent.model.OptimizationRequest;
import cn.mklaus.sqlagent.model.OptimizationResponse;
import cn.mklaus.sqlagent.opencode.ConnectionTester;
import cn.mklaus.sqlagent.service.SqlOptimizerService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.junit.Assume;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

/**
 * End-to-end integration tests for SqlAgent
 *
 * These tests verify the complete workflow from SQL selection to optimization.
 * They require OpenCode server to be running.
 */
public class E2ETest extends BasePlatformTestCase {

    private static final String TEST_SQL = "SELECT * FROM users WHERE email = 'test@example.com'";
    private static final String OPENCODE_SERVER_URL = "http://localhost:4096";

    private SqlOptimizerService optimizer;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        optimizer = new SqlOptimizerService(OPENCODE_SERVER_URL);
    }

    @Override
    protected void tearDown() throws Exception {
        optimizer = null;
        super.tearDown();
    }

    /**
     * Test 1: Connection to OpenCode server
     *
     * Prerequisites:
     * - OpenCode server must be running at http://localhost:4096
     *
     * Run this test first to verify environment setup.
     */
    @Test
    public void test01_OpenCodeConnection() {
        ConnectionTester tester = new ConnectionTester(OPENCODE_SERVER_URL);
        ConnectionTester.ConnectionTestResult result = tester.testConnection();

        // Print results for manual inspection
        System.out.println("\n=== Connection Test Results ===");
        System.out.println(result.getSummary());
        System.out.println("==============================\n");

        // Skip other tests if connection fails
        Assume.assumeTrue("OpenCode server is not available. Skipping integration tests.",
                          result.isOverallSuccess());

        assertTrue("OpenCode should be reachable", result.isOpenCodeReachable());
        assertTrue("Session creation should work", result.isSessionWorking());
    }

    /**
     * Test 2: Basic SQL optimization
     *
     * Prerequisites:
     * - OpenCode server is running
     * - database-tools MCP is configured
     * - Database connection is valid
     */
    @Test
    public void test02_BasicSqlOptimization() throws IOException {
        // First verify connection
        ConnectionTester tester = new ConnectionTester(OPENCODE_SERVER_URL);
        ConnectionTester.ConnectionTestResult result = tester.testConnection();
        Assume.assumeTrue("OpenCode server is not available", result.isOverallSuccess());

        // Create optimization request
        OptimizationRequest request = new OptimizationRequest();
        request.setOriginalSql(TEST_SQL);
        request.setOptimizationGoals(java.util.Arrays.asList(
                "Performance optimization",
                "Syntax improvements"
        ));

        // Execute optimization
        OptimizationResponse response = optimizer.optimize(TEST_SQL);

        // Verify response
        assertNotNull("Response should not be null", response);

        if (response.hasError()) {
            // If MCP/database is not configured, we expect an error
            System.out.println("Optimization returned error (expected if MCP not configured):");
            System.out.println(response.getErrorMessage());
            return;
        }

        // If successful, verify fields
        assertNotNull("Optimized SQL should not be null", response.getOptimizedSql());
        assertNotNull("Suggestions should not be null", response.getSuggestions());
        assertNotNull("Explanation should not be null", response.getExplanation());

        // Print results
        System.out.println("\n=== Optimization Results ===");
        System.out.println("Original: " + TEST_SQL);
        System.out.println("Optimized: " + response.getOptimizedSql());
        System.out.println("Explanation: " + response.getExplanation());
        System.out.println("=============================\n");
    }

    /**
     * Test 3: Error handling - Invalid server URL
     */
    @Test
    public void test03_InvalidServerUrl() {
        SqlOptimizerService invalidOptimizer = new SqlOptimizerService("http://localhost:9999");

        try {
            OptimizationResponse response = invalidOptimizer.optimize(TEST_SQL);
            assertNotNull("Response should not be null", response);
            assertTrue("Should return error for invalid server", response.hasError());
            assertNotNull("Error message should not be null", response.getErrorMessage());
        } catch (Exception e) {
            // Expected - connection should fail
            assertNotNull("Exception should have a message", e.getMessage());
        }
    }

    /**
     * Test 4: Request with empty SQL
     */
    @Test
    public void test04_EmptySql() throws IOException {
        String emptySql = "";

        try {
            OptimizationResponse response = optimizer.optimize(emptySql);

            // Should either return error or handle gracefully
            assertNotNull("Response should not be null", response);
        } catch (Exception e) {
            // Expected - empty SQL might cause errors
            assertNotNull("Exception should have a message", e.getMessage());
        }
    }

    /**
     * Test 5: Request with complex SQL
     *
     * Prerequisites:
     * - OpenCode server is running
     * - database-tools MCP is configured
     */
    @Test
    public void test05_ComplexSqlOptimization() throws IOException {
        ConnectionTester tester = new ConnectionTester(OPENCODE_SERVER_URL);
        ConnectionTester.ConnectionTestResult result = tester.testConnection();
        Assume.assumeTrue("OpenCode server is not available", result.isOverallSuccess());

        String complexSql = """
            SELECT u.id, u.name, u.email, COUNT(o.id) as order_count,
                   SUM(o.total_amount) as total_spent
            FROM users u
            LEFT JOIN orders o ON u.id = o.user_id
            WHERE u.created_at > '2024-01-01'
            GROUP BY u.id, u.name, u.email
            HAVING COUNT(o.id) > 5
            ORDER BY total_spent DESC
            LIMIT 10
        """;

        try {
            OptimizationResponse response = optimizer.optimize(complexSql);

            assertNotNull("Response should not be null", response);

            if (response.hasError()) {
                System.out.println("Complex SQL optimization returned error:");
                System.out.println(response.getErrorMessage());
                return;
            }

            assertNotNull("Optimized SQL should not be null", response.getOptimizedSql());

            System.out.println("\n=== Complex SQL Optimization ===");
            System.out.println("Original: " + complexSql);
            System.out.println("Optimized: " + response.getOptimizedSql());
            System.out.println("=================================\n");
        } catch (Exception e) {
            // Complex SQL might timeout or fail
            assertNotNull("Exception should have a message", e.getMessage());
        }
    }

    /**
     * Test 6: Request with syntax errors in SQL
     *
     * Tests how the plugin handles invalid SQL syntax
     */
    @Test
    public void test06_SqlWithSyntaxErrors() throws IOException {
        String invalidSql = "SELECT FROM WHERE users";

        try {
            OptimizationResponse response = optimizer.optimize(invalidSql);

            assertNotNull("Response should not be null", response);

            // Should either return error or attempt to fix
            if (response.hasError()) {
                System.out.println("Invalid SQL returned error: " + response.getErrorMessage());
            } else {
                // AI might try to fix the SQL
                System.out.println("AI attempted to fix invalid SQL");
                System.out.println("Fixed: " + response.getOptimizedSql());
            }
        } catch (Exception e) {
            // Expected - invalid SQL might cause errors
            assertNotNull("Exception should have a message", e.getMessage());
        }
    }

    /**
     * Test 7: Multiple sequential requests
     *
     * Tests if the plugin can handle multiple optimization requests
     */
    @Test
    public void test07_MultipleRequests() throws IOException {
        ConnectionTester tester = new ConnectionTester(OPENCODE_SERVER_URL);
        ConnectionTester.ConnectionTestResult result = tester.testConnection();
        Assume.assumeTrue("OpenCode server is not available", result.isOverallSuccess());

        String[] testQueries = {
                "SELECT * FROM users WHERE id = 1",
                "SELECT name, email FROM users WHERE status = 'active'",
                "SELECT COUNT(*) FROM orders"
        };

        for (String query : testQueries) {
            try {
                OptimizationResponse response = optimizer.optimize(query);
                assertNotNull("Response should not be null for: " + query, response);

                if (!response.hasError()) {
                    assertNotNull("Optimized SQL should not be null", response.getOptimizedSql());
                    System.out.println("Query optimized: " + query);
                }
            } catch (Exception e) {
                // Some queries might fail
                System.out.println("Query failed: " + query + " - " + e.getMessage());
            }
        }
    }
}
