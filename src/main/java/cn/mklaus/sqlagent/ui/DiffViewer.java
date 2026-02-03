package cn.mklaus.sqlagent.ui;

import cn.mklaus.sqlagent.model.OptimizationResponse;
import cn.mklaus.sqlagent.util.SqlFormatter;
import com.intellij.diff.DiffContentFactory;
import com.intellij.diff.DiffManager;
import com.intellij.diff.requests.SimpleDiffRequest;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

/**
 * Utility class for showing SQL optimization diff
 */
public class DiffViewer {

    /**
     * Show diff in editor area (non-modal, as a tab)
     * Shows only the SQL text diff (legacy method for backward compatibility)
     */
    public static void showDiffInEditor(@NotNull Project project,
                                         @NotNull String originalSql,
                                         @NotNull String optimizedSql,
                                         @NotNull OptimizationResponse response) {
        EditorFactory editorFactory = EditorFactory.getInstance();
        Document originalDoc = editorFactory.createDocument(originalSql);
        Document optimizedDoc = editorFactory.createDocument(optimizedSql);

        DiffContentFactory contentFactory = DiffContentFactory.getInstance();

        SimpleDiffRequest diffRequest = new SimpleDiffRequest(
                "SQL Optimization - " + String.format("%.1f%%", response.getEstimatedImprovement() * 100) + " Improvement",
                contentFactory.create(project, originalDoc, FileTypes.PLAIN_TEXT),
                contentFactory.create(project, optimizedDoc, FileTypes.PLAIN_TEXT),
                "Original SQL",
                "Optimized SQL"
        );

        DiffManager.getInstance().showDiff(project, diffRequest);
    }

    /**
     * Show file-level diff with SQL replaced - Editor context
     * Displays the complete file with optimized SQL replacing the original
     *
     * @param project Current project
     * @param editor The editor containing the SQL
     * @param originalSql Original SQL text
     * @param optimizedSql Optimized SQL text
     * @param response Optimization response
     */
    public static void showFileDiffWithReplacement(@NotNull Project project,
                                                     @NotNull Editor editor,
                                                     @NotNull String originalSql,
                                                     @NotNull String optimizedSql,
                                                     @NotNull OptimizationResponse response) {
        Document document = editor.getDocument();
        String fullOriginalContent = document.getText();

        SelectionModel selectionModel = editor.getSelectionModel();
        int startOffset = selectionModel.getSelectionStart();
        int endOffset = selectionModel.getSelectionEnd();
        TextRange sqlRange = new TextRange(startOffset, endOffset);

        FileType fileType = getFileTypeFromEditor(editor);
        showFileDiffInternal(project, fullOriginalContent, sqlRange, optimizedSql, fileType, response);
    }

    /**
     * Show file-level diff with SQL replaced - PSI context
     * Displays the complete file with optimized SQL replacing the original
     *
     * @param project Current project
     * @param psiFile The file containing the SQL
     * @param sqlRange Text range of the SQL in the file
     * @param originalSql Original SQL text
     * @param optimizedSql Optimized SQL text
     * @param response Optimization response
     */
    public static void showFileDiffWithReplacement(@NotNull Project project,
                                                     @NotNull PsiFile psiFile,
                                                     @NotNull TextRange sqlRange,
                                                     @NotNull String originalSql,
                                                     @NotNull String optimizedSql,
                                                     @NotNull OptimizationResponse response) {
        String fullOriginalContent = psiFile.getText();
        FileType fileType = psiFile.getFileType();

        if (fileType == null) {
            fileType = FileTypes.PLAIN_TEXT;
        }

        showFileDiffInternal(project, fullOriginalContent, sqlRange, optimizedSql, fileType, response);
    }

    /**
     * Internal method to show file-level diff
     */
    private static void showFileDiffInternal(@NotNull Project project,
                                              @NotNull String fullOriginalContent,
                                              @NotNull TextRange sqlRange,
                                              @NotNull String optimizedSql,
                                              @NotNull FileType fileType,
                                              @NotNull OptimizationResponse response) {
        // Validate range
        if (sqlRange.getStartOffset() < 0 || sqlRange.getEndOffset() > fullOriginalContent.length()) {
            // Fallback to SQL-level diff if range is invalid
            showDiffInEditor(project,
                           fullOriginalContent.substring(sqlRange.getStartOffset(), sqlRange.getEndOffset()),
                           optimizedSql, response);
            return;
        }

        // Create modified content with formatted SQL
        String originalSql = fullOriginalContent.substring(sqlRange.getStartOffset(), sqlRange.getEndOffset());
        String fullModifiedContent = createModifiedContent(fullOriginalContent, sqlRange, originalSql, optimizedSql);

        // Create documents
        EditorFactory editorFactory = EditorFactory.getInstance();
        Document originalDoc = editorFactory.createDocument(fullOriginalContent);
        Document modifiedDoc = editorFactory.createDocument(fullModifiedContent);

        // Build diff request
        DiffContentFactory contentFactory = DiffContentFactory.getInstance();
        String title = "File Diff - " +
                String.format("%.1f%%", response.getEstimatedImprovement() * 100) +
                " Improvement";

        SimpleDiffRequest diffRequest = new SimpleDiffRequest(
                title,
                contentFactory.create(project, originalDoc, fileType),
                contentFactory.create(project, modifiedDoc, fileType),
                "Original File",
                "Modified File (SQL Optimized)"
        );

        DiffManager.getInstance().showDiff(project, diffRequest);
    }

    /**
     * Create modified file content by replacing SQL at specific range
     * Formats the optimized SQL and maintains consistent indentation
     */
    private static String createModifiedContent(@NotNull String fullFileContent,
                                                 @NotNull TextRange sqlRange,
                                                 @NotNull String originalSql,
                                                 @NotNull String optimizedSql) {
        // Get the original SQL with surrounding whitespace to understand the context
        String originalWithContext = fullFileContent.substring(sqlRange.getStartOffset(), sqlRange.getEndOffset());

        // Extract leading context (whitespace before first non-whitespace)
        LeadingInfo leadingInfo = extractLeadingInfo(originalWithContext);

        // Detect trailing content - check what comes after the SQL in the file
        String afterSql = fullFileContent.substring(sqlRange.getEndOffset(),
                                                     Math.min(sqlRange.getEndOffset() + 20, fullFileContent.length()));
        boolean needsTrailingNewline = afterSql.startsWith("\n") || afterSql.startsWith("\r\n") ||
                                       afterSql.startsWith("</") || afterSql.trim().startsWith("</");

        // Format the optimized SQL and trim trailing whitespace
        String formattedSql = SqlFormatter.format(optimizedSql).trim();

        // Remove any trailing newlines
        while (formattedSql.endsWith("\n")) {
            formattedSql = formattedSql.substring(0, formattedSql.length() - 1).trim();
        }

        // Apply indentation to each line
        String indentedSql = applyIndentation(formattedSql, leadingInfo.indentation);

        StringBuilder sb = new StringBuilder();
        sb.append(fullFileContent.substring(0, sqlRange.getStartOffset()));

        // Add newline before SQL if original had one
        if (leadingInfo.hasLeadingNewline) {
            sb.append("\n");
        }

        // Add indentation before SQL
        if (!leadingInfo.indentation.isEmpty()) {
            sb.append(leadingInfo.indentation);
        }

        sb.append(indentedSql);

        // Add trailing newline if needed
        if (needsTrailingNewline) {
            sb.append("\n");
        }

        sb.append(fullFileContent.substring(sqlRange.getEndOffset()));

        return sb.toString();
    }

    /**
     * Helper to show string representation with visible special characters
     */
    private static String repr(String s) {
        if (s == null) return "null";
        return s.replace("\n", "\\n")
                 .replace("\r", "\\r")
                 .replace("\t", "\\t")
                 .replace(" ", "·");
    }

    /**
     * Information about leading whitespace
     */
    private static class LeadingInfo {
        boolean hasLeadingNewline = false;
        String indentation = "";
    }

    /**
     * Extract leading whitespace information from SQL text
     * Handles cases like "\n        SELECT..." where newline precedes indentation
     * Only captures indentation AFTER the last newline
     */
    private static LeadingInfo extractLeadingInfo(String sql) {
        LeadingInfo info = new LeadingInfo();
        int lastNewlineIndex = -1;

        // Find the last newline
        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);
            if (c == '\n' || c == '\r') {
                lastNewlineIndex = i;
                // Skip \r\n sequences
                if (c == '\r' && i + 1 < sql.length() && sql.charAt(i + 1) == '\n') {
                    i++;
                }
            }
        }

        // No newline found, return empty info
        if (lastNewlineIndex == -1) {
            return info;
        }

        info.hasLeadingNewline = true;

        // Extract indentation after the last newline
        for (int i = lastNewlineIndex + 1; i < sql.length(); i++) {
            char c = sql.charAt(i);
            if (c == ' ' || c == '\t') {
                info.indentation += c;
            } else {
                // Found non-whitespace character
                break;
            }
        }

        return info;
    }

    /**
     * Apply indentation to each line of the formatted SQL
     * Does NOT add trailing newline (caller controls that)
     */
    private static String applyIndentation(String sql, String indentation) {
        if (indentation == null || indentation.isEmpty()) {
            return sql;
        }

        // Split into lines
        String[] lines = sql.split("\\r?\\n");
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < lines.length; i++) {
            if (i > 0) {
                result.append("\n");
            }
            // Trim each line to ensure consistent indentation
            String line = lines[i].trim();
            if (!line.isEmpty()) {
                result.append(indentation);
                result.append(line);
            }
        }

        return result.toString();
    }

    /**
     * Get FileType from Editor for syntax highlighting
     */
    private static FileType getFileTypeFromEditor(@NotNull Editor editor) {
        Project project = editor.getProject();
        if (project == null) {
            return FileTypes.PLAIN_TEXT;
        }

        Document document = editor.getDocument();
        PsiDocumentManager psiDocManager = PsiDocumentManager.getInstance(project);
        PsiFile psiFile = psiDocManager.getPsiFile(document);

        if (psiFile != null) {
            FileType fileType = psiFile.getFileType();
            return fileType != null ? fileType : FileTypes.PLAIN_TEXT;
        }

        return FileTypes.PLAIN_TEXT;
    }
}
