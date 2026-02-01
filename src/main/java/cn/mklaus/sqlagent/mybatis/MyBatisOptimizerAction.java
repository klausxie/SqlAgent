package cn.mklaus.sqlagent.mybatis;

import cn.mklaus.sqlagent.config.SqlAgentConfigurable;
import cn.mklaus.sqlagent.config.SqlAgentSettingsService;
import cn.mklaus.sqlagent.model.OptimizationResponse;
import cn.mklaus.sqlagent.service.SqlOptimizerService;
import cn.mklaus.sqlagent.ui.DiffViewer;
import cn.mklaus.sqlagent.ui.OptimizationPanel;
import cn.mklaus.sqlagent.ui.OptimizationToolWindowFactory;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlTagValue;
import com.intellij.psi.xml.XmlText;
import com.intellij.psi.PsiElement;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;

/**
 * Action triggered by clicking gutter icon in MyBatis mapper XML files
 *
 * Simplified version - database operations (metadata, execution plan) are now
 * handled by OpenCode MCP tools. This action only sends SQL and database config.
 */
public class MyBatisOptimizerAction extends AnAction {
    private static final String OPENCODE_SERVER_URL = "http://localhost:4096";
    private static final String NOTIFICATION_GROUP_ID = "SQL Agent";
    private static final String TOOL_WINDOW_ID = "SQL Agent";

    private final XmlTag xmlTag;
    private final String originalSql;
    private final String statementId;
    private OptimizationPanel panel; // Field to be accessible in applyOptimizedSql

    public MyBatisOptimizerAction(XmlTag xmlTag, String originalSql, String statementId) {
        super("Optimize SQL with AI");
        this.xmlTag = xmlTag;
        this.originalSql = originalSql;
        this.statementId = statementId;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        if (project == null) {
            return;
        }
        performOptimization(project);
    }

    /**
     * Perform optimization with a specific project (for gutter icon clicks)
     */
    public void actionPerformedForProject(Project project) {
        if (project == null) {
            return;
        }
        performOptimization(project);
    }

    /**
     * Common optimization logic
     */
    private void performOptimization(Project project) {
        // Note: Database configuration is now managed by OpenCode MCP tools
        // Database connection is configured in ~/.opencode/config.json

        // Show tool window
        showToolWindow(project);

        // Run optimization in background
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Optimizing SQL", true) {
            private OptimizationResponse response;
            private Exception error;

            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    panel = getToolWindowPanel(project);
                    if (panel != null) {
                        panel.clear();
                        panel.setStatus("Optimizing: " + statementId, true);
                        panel.log("Starting optimization for: " + statementId);
                    }

                    // Get settings
                    SqlAgentConfigurable.State state = SqlAgentSettingsService.getInstance().getState();

                    SqlOptimizerService optimizer = new SqlOptimizerService(
                            state.serverUrl != null ? state.serverUrl : OPENCODE_SERVER_URL,
                            state);

                    updateProgress(indicator, panel, "Optimizing with AI...", 0.5, 50);

                    // Simplified: Only send SQL, database config managed by MCP
                    response = optimizer.optimize(originalSql);

                    updateProgress(indicator, panel, "Processing results...", 0.9, 90);

                    if (panel != null) {
                        panel.addSuggestion(response);
                        panel.setStatus("Optimization completed", false);
                        panel.log("Optimization completed successfully");
                    }

                    if (indicator != null) {
                        indicator.setText("Optimization completed");
                        indicator.setFraction(1.0);
                    }

                } catch (Exception e) {
                    error = e;
                    if (panel != null) {
                        panel.log("Error: " + e.getMessage());
                        panel.setStatus("Optimization failed", false);
                    }
                    LOG.error("Optimization failed for: " + statementId, e);
                }
            }

            @Override
            public void onSuccess() {
                runOnEdt(() -> {
                    if (error != null) {
                        showError(project, "Optimization failed: " + error.getMessage());
                        return;
                    }

                    if (response == null || response.hasError()) {
                        String errorMsg = response != null ? response.getErrorMessage() : "Unknown error";
                        showError(project, "Optimization failed: " + errorMsg);
                        return;
                    }

                    // Show diff view
                    DiffViewer.showDiffInEditor(project, originalSql, response.getOptimizedSql(), response);

                    // Set up apply callback
                    if (panel != null) {
                        panel.setOptimizationResult(originalSql,
                                response.getOptimizedSql(), response,
                                () -> applyOptimizedSql(response.getOptimizedSql()));
                    }
                });
            }

            private void updateProgress(ProgressIndicator indicator, OptimizationPanel panel,
                                       String text, double fraction, int percent) {
                if (indicator != null) {
                    indicator.setText(text);
                    indicator.setFraction(fraction);
                }
                if (panel != null) {
                    panel.setProgress(percent);
                    panel.log(text);
                }
            }
        });
    }

    /**
     * Apply the optimized SQL back to the XML file
     */
    private void applyOptimizedSql(String optimizedSql) {
        Project project = xmlTag.getProject();
        PsiFile psiFile = xmlTag.getContainingFile();

        WriteCommandAction.runWriteCommandAction(project, () -> {
            Document document = PsiDocumentManager.getInstance(project).getDocument(psiFile);
            if (document == null) {
                if (panel != null) {
                    panel.log("Error: Could not get document for file");
                }
                return;
            }

            TextRange range = findSqlTextRange(xmlTag);
            if (range == null) {
                if (panel != null) {
                    panel.log("Error: Could not find SQL text range");
                }
                return;
            }

            document.replaceString(range.getStartOffset(), range.getEndOffset(), optimizedSql);

            if (panel != null) {
                panel.setStatus("Applied optimized SQL", false);
                panel.log("Optimized SQL has been applied to the file");
                panel.clearOptimizationResult();
            }
        });
    }

    /**
     * Find the text range of the SQL content within the XML tag
     */
    private TextRange findSqlTextRange(XmlTag tag) {
        // First, try to find XmlText child elements
        PsiElement[] children = tag.getChildren();
        for (PsiElement child : children) {
            if (child instanceof XmlText) {
                return child.getTextRange();
            }
        }

        // Fallback to tag value
        XmlTagValue value = tag.getValue();
        return value.getTextRange();
    }

    private OptimizationPanel getToolWindowPanel(Project project) {
        return OptimizationToolWindowFactory.getPanel(project);
    }

    private void showToolWindow(Project project) {
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(TOOL_WINDOW_ID);
        if (toolWindow != null) {
            toolWindow.show();
        }
    }

    private void showError(Project project, String message) {
        Notification notification = new Notification(
                NOTIFICATION_GROUP_ID,
                "SQL Optimization Failed",
                message,
                NotificationType.ERROR
        );
        Notifications.Bus.notify(notification, project);
    }

    private void runOnEdt(Runnable action) {
        ApplicationManager.getApplication().invokeLater(action);
    }

    private static final com.intellij.openapi.diagnostic.Logger LOG =
            com.intellij.openapi.diagnostic.Logger.getInstance(MyBatisOptimizerAction.class);
}
