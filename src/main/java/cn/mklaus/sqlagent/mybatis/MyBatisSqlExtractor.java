package cn.mklaus.sqlagent.mybatis;

import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlTagValue;
import com.intellij.psi.xml.XmlText;
import com.intellij.psi.PsiElement;
import com.intellij.openapi.diagnostic.Logger;

/**
 * Extracts SQL from MyBatis mapper XML tags
 */
public class MyBatisSqlExtractor {
    private static final Logger LOG = Logger.getInstance(MyBatisSqlExtractor.class);

    /**
     * Extract SQL content from a MyBatis XML tag (select, insert, update, delete)
     *
     * @param sqlTag The XML tag containing SQL
     * @return Cleaned SQL string, or null if no SQL found
     */
    public String extractSql(XmlTag sqlTag) {
        if (sqlTag == null) {
            return null;
        }

        // Get the tag value
        XmlTagValue tagValue = sqlTag.getValue();
        String sql = tagValue.getTrimmedText();

        if (sql.isEmpty()) {
            // Check for CDATA or nested text elements
            sql = extractTextFromChildren(sqlTag);
        }

        if (sql == null || sql.trim().isEmpty()) {
            return null;
        }

        // Normalize the SQL
        sql = normalizeParameters(sql);
        sql = handleDynamicSql(sql);

        return sql.trim();
    }

    /**
     * Get the statement ID from the tag's 'id' attribute
     *
     * @param sqlTag The XML tag
     * @return The statement ID, or null if not found
     */
    public String getStatementId(XmlTag sqlTag) {
        if (sqlTag == null) {
            return null;
        }
        return sqlTag.getAttributeValue("id");
    }

    /**
     * Get the namespace from the root mapper tag
     *
     * @param sqlTag Any tag within the mapper file
     * @return The namespace, or null if not found
     */
    public String getNamespace(XmlTag sqlTag) {
        if (sqlTag == null) {
            return null;
        }

        // Navigate to the root mapper tag
        XmlTag current = sqlTag;
        while (current != null) {
            if ("mapper".equals(current.getName())) {
                return current.getAttributeValue("namespace");
            }
            PsiElement parent = current.getParent();
            current = parent instanceof XmlTag ? (XmlTag) parent : null;
        }

        return null;
    }

    /**
     * Extract text from child elements (handles CDATA)
     */
    private String extractTextFromChildren(XmlTag tag) {
        StringBuilder text = new StringBuilder();
        PsiElement[] children = tag.getChildren();

        for (PsiElement child : children) {
            if (child instanceof XmlText) {
                String content = ((XmlText) child).getText();
                if (content != null && !content.trim().isEmpty()) {
                    text.append(content).append("\n");
                }
            }
        }

        return text.length() > 0 ? text.toString() : null;
    }

    /**
     * Normalize MyBatis parameter placeholders to standard SQL
     * - #{param} -> ?
     * - ${param} -> 'value' (placeholder)
     */
    private String normalizeParameters(String sql) {
        if (sql == null) {
            return null;
        }

        // Replace #{param} with ?
        sql = sql.replaceAll("#\\{[^}]+\\}", "?");

        // Replace ${param} with 'value' placeholder
        sql = sql.replaceAll("\\$\\{[^}]+\\}", "'value'");

        return sql;
    }

    /**
     * Handle dynamic SQL elements by converting them to comments
     * This is a conservative approach - keeps the SQL structure valid
     *
     * Common MyBatis dynamic elements:
     * <if test="">...</if>
     * <choose>...</choose>
     * <when test="">...</when>
     * <otherwise>...</otherwise>
     * <where>...</where>
     * <trim>...</trim>
     * <foreach>...</foreach>
     */
    private String handleDynamicSql(String sql) {
        if (sql == null) {
            return null;
        }

        // Remove dynamic SQL tags, keep inner content as comment
        // This is a simple approach - could be enhanced

        // <if test="condition">content</if> -> /* if: condition */ content
        sql = sql.replaceAll("<if[^>]*test=\"([^\"]+)\"[^>]*>", "/* if: $1 */");
        sql = sql.replaceAll("</if>", "/* end if */");

        // <choose>, <when>, <otherwise>
        sql = sql.replaceAll("<choose>", "/* choose */");
        sql = sql.replaceAll("</choose>", "/* end choose */");
        sql = sql.replaceAll("<when[^>]*test=\"([^\"]+)\"[^>]*>", "/* when: $1 */");
        sql = sql.replaceAll("</when>", "/* end when */");
        sql = sql.replaceAll("<otherwise>", "/* otherwise */");
        sql = sql.replaceAll("</otherwise>", "/* end otherwise */");

        // <where>, <trim>
        sql = sql.replaceAll("<where[^>]*>", "WHERE ");
        sql = sql.replaceAll("</where>", "");
        sql = sql.replaceAll("<trim[^>]*/>", "");
        sql = sql.replaceAll("<trim[^>]*>", "");
        sql = sql.replaceAll("</trim>", "");

        // <foreach>
        sql = sql.replaceAll("<foreach[^>]*/>", "");
        sql = sql.replaceAll("<foreach[^>]*>", "/* foreach */");
        sql = sql.replaceAll("</foreach>", "/* end foreach */");

        // <include refid="...">
        sql = sql.replaceAll("<include[^>]*/>", "/* include */");

        // Remove any remaining XML tags
        sql = sql.replaceAll("<[^>]+>", "");

        // Clean up extra whitespace
        sql = sql.replaceAll("\\s+", " ").trim();

        return sql;
    }

    /**
     * Get SQL type from tag name
     *
     * @param tagName The XML tag name
     * @return SQL type (SELECT, INSERT, UPDATE, DELETE) or null
     */
    public String getSqlType(String tagName) {
        if (tagName == null) {
            return null;
        }

        switch (tagName.toLowerCase()) {
            case "select":
                return "SELECT";
            case "insert":
                return "INSERT";
            case "update":
                return "UPDATE";
            case "delete":
                return "DELETE";
            default:
                return null;
        }
    }
}
