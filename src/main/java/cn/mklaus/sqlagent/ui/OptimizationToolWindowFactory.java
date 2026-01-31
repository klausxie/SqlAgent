package cn.mklaus.sqlagent.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Factory for creating SQL Agent tool window
 */
public class OptimizationToolWindowFactory implements ToolWindowFactory {

    // Store panel references per project
    private static final Map<String, OptimizationPanel> panelMap = new HashMap<>();

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        OptimizationPanel panel = new OptimizationPanel(project);
        panelMap.put(project.getLocationHash(), panel);

        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(panel.getPanel(), "", false);
        toolWindow.getContentManager().addContent(content);
    }

    /**
     * Get the panel for a specific project
     */
    public static OptimizationPanel getPanel(Project project) {
        return panelMap.get(project.getLocationHash());
    }

    /**
     * Remove panel reference when project is disposed
     */
    public static void removePanel(Project project) {
        panelMap.remove(project.getLocationHash());
    }
}
