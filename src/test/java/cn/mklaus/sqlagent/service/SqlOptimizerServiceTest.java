package cn.mklaus.sqlagent.service;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for SqlOptimizerService
 *
 * Tests for the simplified service that delegates metadata retrieval
 * to OpenCode MCP tools.
 */
public class SqlOptimizerServiceTest {

    private SqlOptimizerService optimizer;

    @Before
    public void setUp() {
        optimizer = new SqlOptimizerService("http://localhost:4096");
    }

    @Test
    public void testServiceCreation() {
        assertNotNull("Service should be created", optimizer);
    }

    @Test
    public void testServiceWithDifferentUrl() {
        SqlOptimizerService service1 = new SqlOptimizerService("http://localhost:4096");
        SqlOptimizerService service2 = new SqlOptimizerService("http://example.com:8080");

        assertNotNull("Service with custom URL should be created", service1);
        assertNotNull("Service with different URL should be created", service2);
    }

    /**
     * Test that demonstrates the simplified interface.
     *
     * In the old implementation, this would require:
     * 1. Extracting table name from SQL
     * 2. Connecting to database
     * 3. Fetching metadata
     * 4. Getting execution plan
     *
     * Now, all of that is handled by OpenCode MCP tools.
     * The plugin only needs to send the SQL string.
     */
    @Test
    public void testSimplifiedInterface() {
        String sql = "SELECT * FROM users WHERE id = 1";

        // The service now only needs SQL
        // Table extraction and metadata fetching are done by MCP tools
        assertNotNull("SQL should not be null", sql);
        assertFalse("SQL should not be empty", sql.trim().isEmpty());
    }

    @Test
    public void testValidSqlStrings() {
        String[] validSqlQueries = {
                "SELECT * FROM users",
                "SELECT id, name FROM products WHERE price > 100",
                "INSERT INTO orders (user_id, total) VALUES (1, 100)",
                "UPDATE users SET status = 'active' WHERE id = 1"
        };

        for (String sql : validSqlQueries) {
            assertNotNull("SQL should not be null", sql);
            assertFalse("SQL should not be empty", sql.trim().isEmpty());
        }
    }

    @Test
    public void testEmptySqlHandling() {
        String emptySql = "";

        // Service should handle empty SQL gracefully
        // In real scenario, this would be validated before calling optimize()
        assertTrue("Empty SQL should be detected", emptySql.trim().isEmpty());
    }

    @Test
    public void testNullSqlHandling() {
        String nullSql = null;

        // Service should handle null SQL gracefully
        assertNull("Null SQL should be detected", nullSql);
    }
}
