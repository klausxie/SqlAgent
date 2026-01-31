package cn.mklaus.sqlagent.ui;

import cn.mklaus.sqlagent.config.DatabaseConfigStore;
import cn.mklaus.sqlagent.database.DatabaseConnectionManager;
import cn.mklaus.sqlagent.database.MySQLMetadataAdapter;
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

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Action for optimizing SQL from editor context menu
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

        DatabaseConfig config = DatabaseConfigStore.getInstance().getConfig();
        if (config == null) {
            showWarningDialog(
                    "Please configure a database connection in Settings → Tools → SQL Agent first.",
                    "No Database Configuration"
            );
            return;
        }

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

                    SqlOptimizerService optimizer = new SqlOptimizerService(OPENCODE_SERVER_URL);

                    String tableName = extractTableName(optimizer, selectedSql);
                    displayMetadata(panel, config, tableName);
                    updateProgress(indicator, panel, "Optimizing with AI...", 0.6, 60);

                    response = optimizer.optimize(selectedSql.trim(), config, tableName);

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

            private String extractTableName(SqlOptimizerService optimizer, String sql) {
                updateProgress(null, panel, "Analyzing SQL query...", 0.2, 20);

                String tableName = optimizer.extractTableName(sql);
                if (tableName == null) {
                    throw new IllegalArgumentException("Could not extract table name from SQL");
                }

                if (panel != null) {
                    panel.addMetadata("Table", tableName);
                }
                return tableName;
            }

            private void displayMetadata(OptimizationPanel panel, DatabaseConfig config, String tableName) {
                updateProgress(null, panel, "Fetching metadata...", 0.4, 40);

                DatabaseConnectionManager connectionManager = DatabaseConnectionManager.getInstance();
                try (Connection conn = connectionManager.getConnection(config)) {
                    MySQLMetadataAdapter metadataAdapter = new MySQLMetadataAdapter();
                    TableMetadata metadata = metadataAdapter.extractTableMetadata(conn, tableName);

                    if (panel != null && metadata != null) {
                        panel.addMetadata("Row Count", String.format("%,d", metadata.getRowCount()));
                        panel.addMetadata("Columns", String.valueOf(metadata.getColumns().size()));
                        panel.addMetadata("Indexes", String.valueOf(metadata.getIndexes().size()));
                    }
                } catch (SQLException e) {
                    throw new RuntimeException("Failed to fetch metadata", e);
                }
            }

            private void finalizeOptimization(ProgressIndicator indicator, OptimizationPanel panel,
                                             OptimizationResponse response) {
                updateProgress(indicator, panel, "Processing results...", 0.9, 90);

                if (panel != null) {
                    panel.addSuggestion(response);
                    panel.setStatus("Optimization completed", false);
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
