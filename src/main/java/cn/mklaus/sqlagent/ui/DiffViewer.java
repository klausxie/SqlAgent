package cn.mklaus.sqlagent.ui;

import cn.mklaus.sqlagent.model.OptimizationResponse;
import com.intellij.diff.DiffContentFactory;
import com.intellij.diff.DiffManager;
import com.intellij.diff.requests.SimpleDiffRequest;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.fileTypes.FileTypes;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * Utility class for showing SQL optimization diff
 */
public class DiffViewer {

    /**
     * Show diff in editor area (non-modal, as a tab)
     */
    public static void showDiffInEditor(@NotNull Project project,
                                         @NotNull String originalSql,
                                         @NotNull String optimizedSql,
                                         @NotNull OptimizationResponse response) {
        // Create documents for diff
        EditorFactory editorFactory = EditorFactory.getInstance();
        Document originalDoc = editorFactory.createDocument(originalSql);
        Document optimizedDoc = editorFactory.createDocument(optimizedSql);

        // Use DiffContentFactory with documents
        DiffContentFactory contentFactory = DiffContentFactory.getInstance();

        SimpleDiffRequest diffRequest = new SimpleDiffRequest(
                "SQL Optimization - " + String.format("%.1f%%", response.getEstimatedImprovement() * 100) + " Improvement",
                contentFactory.create(project, originalDoc, FileTypes.PLAIN_TEXT),
                contentFactory.create(project, optimizedDoc, FileTypes.PLAIN_TEXT),
                "Original SQL",
                "Optimized SQL"
        );

        // Show diff in editor area (non-modal, opens as a tab)
        DiffManager.getInstance().showDiff(project, diffRequest);
    }
}
