package cn.mklaus.sqlagent.service;

import cn.mklaus.sqlagent.model.*;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * Unit tests for PromptBuilder
 */
public class PromptBuilderTest {

    private PromptBuilder promptBuilder;
    private OptimizationRequest request;

    @Before
    public void setUp() {
        promptBuilder = new PromptBuilder();

        // Create a test request
        request = new OptimizationRequest();

        // Set database config
        DatabaseConfig dbConfig = new DatabaseConfig();
        dbConfig.setType(DatabaseType.MYSQL);
        dbConfig.setHost("localhost");
        dbConfig.setPort(3306);
        dbConfig.setDatabase("testdb");
        request.setDatabase(dbConfig);

        // Set SQL
        request.setOriginalSql("SELECT * FROM users WHERE email = 'test@example.com'");

        // Set table metadata
        TableMetadata metadata = new TableMetadata();
        metadata.setTableName("users");
        metadata.setRowCount(1000000);

        // Add columns
        ColumnInfo idColumn = new ColumnInfo();
        idColumn.setColumnName("id");
        idColumn.setTypeName("BIGINT");
        idColumn.setPrimaryKey(true);

        ColumnInfo emailColumn = new ColumnInfo();
        emailColumn.setColumnName("email");
        emailColumn.setTypeName("VARCHAR(255)");

        metadata.setColumns(Arrays.asList(idColumn, emailColumn));

        // Add indexes
        IndexInfo index = new IndexInfo();
        index.setIndexName("idx_email");
        index.addColumnName("email");
        index.setUnique(true);
        metadata.setIndexes(Arrays.asList(index));

        request.setTableMetadata(metadata);

        // Set execution plan
        request.setExecutionPlan(
                "-> Filter: (email = 'test@example.com')  (cost=100050.00 rows=100000)\n" +
                "   -> Table scan on users  (cost=100050.00 rows=100000)"
        );

        // Set optimization goals
        request.setOptimizationGoals(Arrays.asList(
                "Performance optimization",
                "Index usage"
        ));
    }

    @Test
    public void testBuildPrompt_BasicStructure() {
        String prompt = promptBuilder.buildPrompt(request);

        assertNotNull(prompt);
        assertFalse(prompt.isEmpty());

        // Verify prompt contains key sections
        assertTrue(prompt.contains("Original Query"));
        assertTrue(prompt.contains("Database Context"));
        assertTrue(prompt.contains("Table Schema"));
        assertTrue(prompt.contains("Indexes"));
        assertTrue(prompt.contains("Current Execution Plan"));
        assertTrue(prompt.contains("Optimization Goals"));
    }

    @Test
    public void testBuildPrompt_ContainsOriginalSql() {
        String prompt = promptBuilder.buildPrompt(request);

        assertTrue(prompt.contains("SELECT * FROM users WHERE email = 'test@example.com'"));
    }

    @Test
    public void testBuildPrompt_ContainsDatabaseInfo() {
        String prompt = promptBuilder.buildPrompt(request);

        assertTrue(prompt.contains("MySQL"));
        assertTrue(prompt.contains("testdb"));
        assertTrue(prompt.contains("users"));
    }

    @Test
    public void testBuildPrompt_ContainsMetadata() {
        String prompt = promptBuilder.buildPrompt(request);

        // formatNumber converts 1000000 to "1.0M"
        assertTrue(prompt.contains("1.0M") || prompt.contains("1000000")); // row count formatted
        assertTrue(prompt.contains("id"));
        assertTrue(prompt.contains("email"));
        assertTrue(prompt.contains("idx_email"));
    }

    @Test
    public void testBuildPrompt_ContainsExecutionPlan() {
        String prompt = promptBuilder.buildPrompt(request);

        assertTrue(prompt.contains("Filter: (email ="));
        assertTrue(prompt.contains("Table scan"));
    }

    @Test
    public void testBuildPrompt_WithEmptyMetadata() {
        TableMetadata emptyMetadata = new TableMetadata();
        emptyMetadata.setTableName("test_table");
        emptyMetadata.setRowCount(0);
        request.setTableMetadata(emptyMetadata);

        String prompt = promptBuilder.buildPrompt(request);

        assertNotNull(prompt);
        assertTrue(prompt.contains("test_table"));
    }

    @Test
    public void testBuildPrompt_WithPostgreSQL() {
        request.getDatabase().setType(DatabaseType.POSTGRESQL);
        request.getDatabase().setPort(5432);

        String prompt = promptBuilder.buildPrompt(request);

        assertTrue(prompt.contains("PostgreSQL"));
    }

    @Test
    public void testBuildPrompt_ContainsJsonFormatRequest() {
        String prompt = promptBuilder.buildPrompt(request);

        // Verify prompt requests JSON response format
        assertTrue(prompt.contains("JSON") || prompt.contains("json") || prompt.contains("format"));
    }
}
