package cn.mklaus.sqlagent.service;

import cn.mklaus.sqlagent.model.DatabaseConfig;
import cn.mklaus.sqlagent.model.DatabaseType;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for SqlOptimizerService
 */
public class SqlOptimizerServiceTest {

    private SqlOptimizerService optimizer;

    @Before
    public void setUp() {
        optimizer = new SqlOptimizerService("http://localhost:4096");
    }

    @Test
    public void testExtractTableName_SimpleSelect() {
        String sql = "SELECT * FROM users WHERE id = 1";
        String tableName = optimizer.extractTableName(sql);

        assertEquals("users", tableName);
    }

    @Test
    public void testExtractTableName_WithBackticks() {
        String sql = "SELECT * FROM `users` WHERE id = 1";
        String tableName = optimizer.extractTableName(sql);

        assertEquals("users", tableName);
    }

    @Test
    public void testExtractTableName_InsertInto() {
        String sql = "INSERT INTO users (name, email) VALUES ('John', 'john@example.com')";
        String tableName = optimizer.extractTableName(sql);

        assertEquals("users", tableName);
    }

    @Test
    public void testExtractTableName_WithSchemaPrefix() {
        String sql = "SELECT * FROM mydb.users WHERE id = 1";
        String tableName = optimizer.extractTableName(sql);

        // Should extract the table name after schema
        assertEquals("mydb.users", tableName);
    }

    @Test
    public void testExtractTableName_ComplexQuery() {
        String sql = "SELECT u.*, p.name FROM users u JOIN profiles p ON u.id = p.user_id WHERE u.id = 1";
        String tableName = optimizer.extractTableName(sql);

        // Should extract the first table
        assertEquals("users", tableName);
    }

    @Test
    public void testExtractTableName_WithNewlines() {
        String sql = "SELECT *\nFROM users\nWHERE id = 1";
        String tableName = optimizer.extractTableName(sql);

        assertEquals("users", tableName);
    }

    @Test
    public void testExtractTableName_WithComments() {
        String sql = "/* comment */ SELECT * FROM users WHERE id = 1";
        String tableName = optimizer.extractTableName(sql);

        assertEquals("users", tableName);
    }

    @Test
    public void testExtractTableName_NoFromClause() {
        String sql = "SELECT 1";
        String tableName = optimizer.extractTableName(sql);

        assertNull(tableName);
    }

    @Test
    public void testExtractTableName_NullInput() {
        String tableName = optimizer.extractTableName(null);
        assertNull(tableName);
    }

    @Test
    public void testExtractTableName_EmptyInput() {
        String tableName = optimizer.extractTableName("");
        assertNull(tableName);
    }

    @Test
    public void testExtractTableName_CaseInsensitive() {
        String sql = "select * from users where id = 1";
        String tableName = optimizer.extractTableName(sql);

        assertEquals("users", tableName);
    }

    @Test
    public void testExtractTableName_WithAlias() {
        String sql = "SELECT * FROM users u WHERE u.id = 1";
        String tableName = optimizer.extractTableName(sql);

        assertEquals("users", tableName);
    }

    @Test
    public void testExtractTableName_WithWhereContainingFrom() {
        String sql = "SELECT * FROM users WHERE email LIKE 'FROM@example.com'";
        String tableName = optimizer.extractTableName(sql);

        assertEquals("users", tableName);
    }
}
