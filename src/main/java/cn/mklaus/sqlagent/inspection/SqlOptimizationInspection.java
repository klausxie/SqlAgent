package cn.mklaus.sqlagent.inspection;

import cn.mklaus.sqlagent.config.SqlAgentConfigurable;
import cn.mklaus.sqlagent.config.SqlAgentSettingsService;
import cn.mklaus.sqlagent.service.SqlOptimizerService;
import com.intellij.codeInspection.*;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlText;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Inspection for SQL optimization in MyBatis mapper XML files
 *
 * Similar to ESLint, provides real-time hints for SQL optimization.
 * Shows warnings when SQL can be improved.
 */
public class SqlOptimizationInspection extends LocalInspectionTool {

    private static final String OPENCODE_SERVER_URL = "http://localhost:4096";
    private static final String NOTIFICATION_GROUP_ID = "SQL Agent";

    @NotNull
    @Override
    public String getDisplayName() {
        return "SQL Optimization Inspection";
    }

    @Nls
    @NotNull
    @Override
    public String getGroupDisplayName() {
        return "SqlAgent";
    }

    @Nls
    @NotNull
    @Override
    public String getShortName() {
        return "SqlOptimization";
    }

    @Override
    public boolean isEnabledByDefault() {
        return true;
    }

    @Nullable
    @Override
    public String getStaticDescription() {
        return "Analyzes SQL queries in MyBatis mapper files and suggests optimizations.";
    }

    @Override
    public ProblemDescriptor[] checkFile(@NotNull PsiFile file, @NotNull InspectionManager manager, boolean isOnTheFly) {
        // Only process MyBatis mapper XML files
        if (!(file instanceof XmlFile)) {
            return ProblemDescriptor.EMPTY_ARRAY;
        }

        List<ProblemDescriptor> problems = new ArrayList<>();

        // Find all SQL statements in MyBatis tags
        file.accept(new com.intellij.psi.XmlElementVisitor() {
            @Override
            public void visitXmlTag(XmlTag tag) {
                // Check if this is a MyBatis SQL tag (select, insert, update, delete)
                String tagName = tag.getName();
                if (isMyBatisSqlTag(tagName)) {
                    // Extract SQL from the tag
                    String sql = extractSqlFromTag(tag);
                    if (sql != null && !sql.trim().isEmpty()) {
                        // Create problem descriptor with quick fix
                        ProblemDescriptor descriptor = createSqlProblem(
                                tag,
                                sql,
                                manager,
                                isOnTheFly
                        );
                        if (descriptor != null) {
                            problems.add(descriptor);
                        }
                    }
                }
                super.visitXmlTag(tag);
            }
        });

        return problems.toArray(ProblemDescriptor.EMPTY_ARRAY);
    }

    /**
     * Check if tag is a MyBatis SQL tag
     */
    private boolean isMyBatisSqlTag(String tagName) {
        return tagName.equals("select") ||
                tagName.equals("insert") ||
                tagName.equals("update") ||
                tagName.equals("delete");
    }

    /**
     * Extract SQL from MyBatis XML tag
     */
    private String extractSqlFromTag(XmlTag tag) {
        StringBuilder sql = new StringBuilder();

        // Find XmlText children
        for (PsiElement child : tag.getChildren()) {
            if (child instanceof XmlText) {
                sql.append(child.getText());
            }
        }

        return sql.toString().trim();
    }

    /**
     * Create problem descriptor for SQL
     */
    private ProblemDescriptor createSqlProblem(
            XmlTag tag,
            String sql,
            InspectionManager manager,
            boolean isOnTheFly) {

        String message = "SQL can be optimized";

        // Create quick fix
        OptimizeSqlQuickFix fix = new OptimizeSqlQuickFix(sql, tag);

        return manager.createProblemDescriptor(
                tag,
                message,
                new LocalQuickFix[]{fix},
                ProblemHighlightType.WEAK_WARNING,
                isOnTheFly,
                false
        );
    }

    /**
     * Quick fix for SQL optimization
     */
    private static class OptimizeSqlQuickFix implements LocalQuickFix {
        private final String originalSql;
        private final XmlTag xmlTag;

        public OptimizeSqlQuickFix(String originalSql, XmlTag xmlTag) {
            this.originalSql = originalSql;
            this.xmlTag = xmlTag;
        }

        @Nls(capitalization = Nls.Capitalization.Sentence)
        @NotNull
        @Override
        public String getName() {
            return "Optimize with AI";
        }

        @Nls(capitalization = Nls.Capitalization.Sentence)
        @NotNull
        @Override
        public String getFamilyName() {
            return "SqlAgent Optimization";
        }

        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Show notification
            Notification notification = new Notification(
                    NOTIFICATION_GROUP_ID,
                    "SQL Optimization",
                    "Analyzing SQL with AI...",
                    NotificationType.INFORMATION
            );
            Notifications.Bus.notify(notification, project);

            // Run optimization in background
            ProgressManager.getInstance().run(new com.intellij.openapi.progress.Task.Backgroundable(
                    project,
                    "Optimizing SQL",
                    true
            ) {
                @Override
                public void run(@NotNull com.intellij.openapi.progress.ProgressIndicator indicator) {
                    try {
                        // Get settings
                        SqlAgentConfigurable.State state = SqlAgentSettingsService.getInstance().getState();

                        SqlOptimizerService optimizer = new SqlOptimizerService(
                                state.serverUrl != null ? state.serverUrl : OPENCODE_SERVER_URL,
                                state);
                        cn.mklaus.sqlagent.model.OptimizationResponse response = optimizer.optimize(originalSql);

                        if (response.hasError()) {
                            // Show error notification
                            Notification errorNotification = new Notification(
                                    NOTIFICATION_GROUP_ID,
                                    "Optimization Failed",
                                    response.getErrorMessage(),
                                    NotificationType.ERROR
                            );
                            Notifications.Bus.notify(errorNotification, project);
                        } else {
                            // Show success notification with diff
                            showOptimizationResult(project, response);
                        }
                    } catch (Exception e) {
                        Notification errorNotification = new Notification(
                                NOTIFICATION_GROUP_ID,
                                "Optimization Failed",
                                "Error: " + e.getMessage(),
                                NotificationType.ERROR
                        );
                        Notifications.Bus.notify(errorNotification, project);
                    }
                }
            });
        }

        /**
         * Show optimization result with diff
         */
        private void showOptimizationResult(Project project, cn.mklaus.sqlagent.model.OptimizationResponse response) {
            javax.swing.SwingUtilities.invokeLater(() -> {
                // Show diff viewer
                cn.mklaus.sqlagent.ui.DiffViewer.showDiffInEditor(
                        project,
                        originalSql,
                        response.getOptimizedSql(),
                        response
                );

                // Show success notification
                Notification notification = new Notification(
                        NOTIFICATION_GROUP_ID,
                        "SQL Optimized",
                        "View changes in the diff viewer",
                        NotificationType.INFORMATION
                );
                Notifications.Bus.notify(notification, project);
            });
        }
    }
}
