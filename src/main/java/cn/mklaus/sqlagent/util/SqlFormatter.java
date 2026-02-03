package cn.mklaus.sqlagent.util;

import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.util.SelectUtils;

import java.util.regex.Pattern;

/**
 * Utility class for formatting SQL queries
 */
public class SqlFormatter {

    private static final Pattern MULTIPLE_SPACES = Pattern.compile("\\s+");

    /**
     * Format SQL query with proper indentation and line breaks
     *
     * @param sql The SQL query to format
     * @return Formatted SQL query
     */
    public static String format(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return sql;
        }

        // Check if original SQL is multi-line
        boolean isMultiLine = sql.contains("\n") && !sql.trim().matches("^[^\n]+$");

        // Clean up the SQL first
        String cleaned = cleanSql(sql);

        // Try to parse and format using JSqlParser
        try {
            Statement statement = CCJSqlParserUtil.parse(cleaned);
            String formatted = statement.toString();

            // Post-process formatting for better readability
            formatted = postProcessFormat(formatted);

            // Ensure multi-line SQL ends with newline
            if (isMultiLine && !formatted.endsWith("\n")) {
                formatted += "\n";
            }

            return formatted;
        } catch (Exception e) {
            // If parsing fails, use simple formatting
            return simpleFormat(cleaned);
        }
    }

    /**
     * Clean up SQL string
     */
    private static String cleanSql(String sql) {
        // Remove leading/trailing whitespace
        String cleaned = sql.trim();

        // Replace multiple spaces with single space
        cleaned = MULTIPLE_SPACES.matcher(cleaned).replaceAll(" ");

        return cleaned;
    }

    /**
     * Post-process the formatted SQL for better readability
     * Adds newlines before major keywords but no extra indentation
     * Indentation will be applied later based on the original code context
     */
    private static String postProcessFormat(String sql) {
        // Add newlines before major keywords if they're on the same line
        String result = sql;

        // Add newlines before these keywords (without indentation)
        String[] keywords = {"FROM", "WHERE", "AND", "OR", "JOIN", "LEFT JOIN", "RIGHT JOIN",
                            "INNER JOIN", "ON", "GROUP BY", "ORDER BY", "HAVING", "LIMIT",
                            "UNION", "VALUES", "SET"};

        for (String keyword : keywords) {
            // Look for the keyword not preceded by a newline
            result = result.replaceAll(
                "(?<!\\n)\\b" + keyword + "\\b",
                "\n" + keyword
            );
        }

        // Clean up multiple consecutive newlines
        result = result.replaceAll("\\n\\s*\\n+", "\n");

        return result.trim();
    }

    /**
     * Simple SQL formatting as fallback when parser fails
     * Adds line breaks before major SQL keywords (without indentation)
     */
    private static String simpleFormat(String sql) {
        StringBuilder formatted = new StringBuilder();
        String[] lines = sql.split("\\r?\\n");

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            // Add line breaks before major keywords (without indentation)
            formatted.append(trimmed
                .replaceAll("(?i)\\b(SELECT|FROM|WHERE|AND|OR|JOIN|LEFT\\s+JOIN|RIGHT\\s+JOIN|INNER\\s+JOIN|ON|GROUP\\s+BY|ORDER\\s+BY|HAVING|LIMIT)\\b", "\n$1"));
            formatted.append("\n");
        }

        return formatted.toString().trim();
    }

    /**
     * Check if a string is a valid SQL query
     */
    public static boolean isValidSql(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return false;
        }

        try {
            CCJSqlParserUtil.parse(sql);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
