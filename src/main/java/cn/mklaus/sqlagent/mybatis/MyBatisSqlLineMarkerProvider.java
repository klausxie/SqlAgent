package cn.mklaus.sqlagent.mybatis;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.psi.PsiElement;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlFile;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collection;

/**
 * Line marker provider for MyBatis mapper XML files
 * Adds gutter icons next to SQL statements (select, insert, update, delete tags)
 */
public class MyBatisSqlLineMarkerProvider implements LineMarkerProvider {

    private static final Icon SQL_ICON = AllIcons.Actions.Execute;

    @Nullable
    @Override
    public LineMarkerInfo<?> getLineMarkerInfo(@NotNull PsiElement element) {
        // Only process XML tags
        if (!(element instanceof XmlTag)) {
            return null;
        }

        XmlTag tag = (XmlTag) element;
        String tagName = tag.getName();

        // Only process MyBatis SQL tags
        if (!isSqlTag(tagName)) {
            return null;
        }

        // Check if this file is likely a MyBatis mapper
        if (!isMyBatisMapperFile(tag)) {
            return null;
        }

        // Extract SQL information
        MyBatisSqlExtractor extractor = new MyBatisSqlExtractor();
        String statementId = extractor.getStatementId(tag);
        String sql = extractor.extractSql(tag);

        // Skip if no statement ID or no SQL content
        if (statementId == null || statementId.trim().isEmpty()) {
            return null;
        }

        if (sql == null || sql.trim().isEmpty()) {
            return null;
        }

        // Get SQL type for tooltip
        String sqlType = extractor.getSqlType(tagName);
        String fullName = getFullName(tag, statementId);
        String tooltip = buildTooltip(sqlType, fullName);
        Project project = tag.getProject();

        // Create line marker info with click handler
        TextRange range = element.getTextRange();

        return new LineMarkerInfo<>(
                element,
                range,
                SQL_ICON,
                (e) -> tooltip,
                (e, elt) -> {
                    // Handle click - trigger optimization action directly
                    MyBatisOptimizerAction action = new MyBatisOptimizerAction(tag, sql, statementId);
                    action.actionPerformedForProject(project);
                },
                GutterIconRenderer.Alignment.RIGHT
        );
    }

    /**
     * Check if the tag is a MyBatis SQL tag
     */
    private boolean isSqlTag(String tagName) {
        if (tagName == null) {
            return false;
        }
        return "select".equalsIgnoreCase(tagName) ||
               "insert".equalsIgnoreCase(tagName) ||
               "update".equalsIgnoreCase(tagName) ||
               "delete".equalsIgnoreCase(tagName);
    }

    /**
     * Check if this is likely a MyBatis mapper file
     */
    private boolean isMyBatisMapperFile(XmlTag tag) {
        PsiElement containingFile = tag.getContainingFile();
        if (!(containingFile instanceof XmlFile)) {
            return false;
        }

        // Check file name
        String fileName = ((XmlFile) containingFile).getName();
        if (fileName.toLowerCase().contains("mapper")) {
            return true;
        }

        // Check if root element is <mapper>
        XmlTag rootTag = ((XmlFile) containingFile).getRootTag();
        if (rootTag != null && "mapper".equalsIgnoreCase(rootTag.getName())) {
            return true;
        }

        return false;
    }

    /**
     * Build tooltip text for the gutter icon
     */
    private String buildTooltip(String sqlType, String fullName) {
        StringBuilder tooltip = new StringBuilder();
        if (sqlType != null) {
            tooltip.append("SQL Agent: Optimize ").append(sqlType);
        } else {
            tooltip.append("SQL Agent: Optimize");
        }
        tooltip.append(" - ").append(fullName);
        return tooltip.toString();
    }

    /**
     * Get full statement name including namespace
     */
    private String getFullName(XmlTag tag, String statementId) {
        MyBatisSqlExtractor extractor = new MyBatisSqlExtractor();
        String namespace = extractor.getNamespace(tag);

        if (namespace != null && !namespace.isEmpty()) {
            // Extract simple class name from namespace
            String simpleName = namespace;
            if (namespace.contains(".")) {
                simpleName = namespace.substring(namespace.lastIndexOf('.') + 1);
            }
            return simpleName + "." + statementId;
        }

        return statementId;
    }
}
