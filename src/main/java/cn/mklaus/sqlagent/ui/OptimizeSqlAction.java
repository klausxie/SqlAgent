package cn.mklaus.sqlagent.ui;

import cn.mklaus.sqlagent.config.SqlAgentConfigurable;
import cn.mklaus.sqlagent.config.SqlAgentSettingsService;
import cn.mklaus.sqlagent.model.*;
import cn.mklaus.sqlagent.service.SqlOptimizerService;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import org.jetbrains.annotations.NotNull;

/**
 * Action for optimizing SQL from editor context menu
 *
 * Simplified version - database operations (metadata, execution plan) are now
 * handled by OpenCode MCP tools. This action only sends SQL and database config.
 */
public class OptimizeSqlAction extends AnAction {

    private static final String OPENCODE_SERVER_URL = "http://localhost:4096";
    private static final String NOTIFICATION_GROUP_ID = "SQL Agent";
    private static final String TOOL_WINDOW_ID = "SQL Agent";

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        Editor editor = event.getData(CommonDataKeys.EDITOR);

        if (project == null || editor == null) {
            return;
        }

        SelectionModel selectionModel = editor.getSelectionModel();
        String selectedSql = selectionModel.getSelectedText();

        if (selectedSql == null || selectedSql.trim().isEmpty()) {
            showWarningDialog("Please select the SQL query you want to optimize.", "No SQL Selected");
            return;
        }

        // Note: Database configuration is now managed by OpenCode MCP tools
        // The plugin only sends SQL; OpenCode uses database-tools MCP to connect
        // Database connection is configured in ~/.opencode/config.json

        showToolWindow(project);

        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Optimizing SQL", true) {
            private OptimizationResponse response;
            private Exception error;
            private OptimizationPanel panel;

            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    panel = getToolWindowPanel(project);
                    initializePanel(panel);

                    // Get settings
                    SqlAgentConfigurable.State state = SqlAgentSettingsService.getInstance().getState();

                    SqlOptimizerService optimizer = new SqlOptimizerService(
                            state.serverUrl != null ? state.serverUrl : OPENCODE_SERVER_URL,
                            state);

                    updateProgress(indicator, panel, "Optimizing with AI...", 0.3, 30);
                    panel.log("Starting optimization...");

                    // Simplified: Only send SQL, database config is managed by OpenCode MCP
                    response = optimizer.optimize(selectedSql.trim());

                    finalizeOptimization(indicator, panel, response);

                } catch (Exception e) {
                    error = e;
                    handleOptimizationError(panel, e);
                }
            }

            private void initializePanel(OptimizationPanel panel) {
                if (panel != null) {
                    panel.clear();
                    panel.setStatus("Optimizing SQL...", true);
                }
            }

            private void finalizeOptimization(ProgressIndicator indicator, OptimizationPanel panel,
                                             OptimizationResponse response) {
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

            private void handleOptimizationError(OptimizationPanel panel, Exception e) {
                if (panel != null) {
                    panel.log("Error: " + e.getMessage());
                    panel.setStatus("Optimization failed", false);
                }
            }

            @Override
            public void onSuccess() {
                runOnEdt(() -> {
                    if (error != null) {
                        showError("Optimization failed: " + error.getMessage());
                        return;
                    }

                    if (response == null || response.hasError()) {
                        String errorMsg = response != null ? response.getErrorMessage() : "Unknown error";
                        showError("Optimization failed: " + errorMsg);
                        return;
                    }

                    showDiffAndApply(project, editor, selectedSql, response.getOptimizedSql(), response, panel);
                });
            }

            private OptimizationPanel getToolWindowPanel(Project project) {
                return OptimizationToolWindowFactory.getPanel(project);
            }

            private void showError(String message) {
                Notification notification = new Notification(
                        NOTIFICATION_GROUP_ID,
                        "SQL Optimization Failed",
                        message,
                        NotificationType.ERROR
                );
                Notifications.Bus.notify(notification, project);
            }

            private void showDiffAndApply(Project project, Editor editor,
                                          String originalSql, String optimizedSql,
                                          OptimizationResponse response, OptimizationPanel panel) {
                if (panel != null) {
                    panel.setOptimizationResult(editor, originalSql, optimizedSql, response, () -> {
                        applyOptimizedSql(project, editor, optimizedSql, panel);
                    });
                }

                DiffViewer.showDiffInEditor(project, originalSql, optimizedSql, response);
            }

            private void applyOptimizedSql(Project project, Editor editor,
                                           String optimizedSql, OptimizationPanel panel) {
                SelectionModel selectionModel = editor.getSelectionModel();
                int startOffset = selectionModel.getSelectionStart();
                int endOffset = selectionModel.getSelectionEnd();

                WriteCommandAction.runWriteCommandAction(project, () -> {
                    Document document = editor.getDocument();
                    document.replaceString(startOffset, endOffset, optimizedSql);

                    int newEndOffset = startOffset + optimizedSql.length();
                    selectionModel.setSelection(startOffset, newEndOffset);
                });

                if (panel != null) {
                    panel.setStatus("Applied optimized SQL", false);
                    panel.clearOptimizationResult();
                }
            }
        });
    }

    private void showWarningDialog(String message, String title) {
        runOnEdt(() -> Messages.showWarningDialog(message, title));
    }

    private void showToolWindow(Project project) {
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(TOOL_WINDOW_ID);
        if (toolWindow != null) {
            toolWindow.show();
        }
    }

    private void runOnEdt(Runnable action) {
        ApplicationManager.getApplication().invokeLater(action);
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        Editor editor = event.getData(CommonDataKeys.EDITOR);

        boolean enabled = project != null && editor != null &&
                editor.getSelectionModel().hasSelection();

        event.getPresentation().setEnabledAndVisible(enabled);
    }
}
