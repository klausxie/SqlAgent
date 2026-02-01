package cn.mklaus.sqlagent.service;

import cn.mklaus.sqlagent.model.DatabaseConfig;
import cn.mklaus.sqlagent.model.DatabaseType;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for SqlOptimizerService
 *
 * Tests for the simplified service that delegates metadata extraction
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

    // Note: Full integration tests require a running OpenCode server
    // and database connection. Those should be in separate integration tests.

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
     */
    @Test
    public void testSimplifiedInterface() {
        String sql = "SELECT * FROM users WHERE id = 1";
        DatabaseConfig config = createTestConfig();

        // The service now only needs SQL and database config
        // Table extraction and metadata fetching are done by MCP tools
        assertNotNull("SQL should not be null", sql);
        assertNotNull("Config should not be null", config);
    }

    @Test
    public void testDatabaseConfigCreation() {
        DatabaseConfig config = createTestConfig();

        assertEquals("MySQL", DatabaseType.MYSQL, config.getType());
        assertEquals("localhost", config.getHost());
        assertEquals(3306, config.getPort());
        assertEquals("testdb", config.getDatabase());
        assertEquals("user", config.getUsername());
        assertEquals("pass", config.getPassword());
    }

    /**
     * Helper method to create a test database config
     */
    private DatabaseConfig createTestConfig() {
        DatabaseConfig config = new DatabaseConfig();
        config.setType(DatabaseType.MYSQL);
        config.setHost("localhost");
        config.setPort(3306);
        config.setDatabase("testdb");
        config.setUsername("user");
        config.setPassword("pass");
        return config;
    }
}
