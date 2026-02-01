package cn.mklaus.sqlagent;

import cn.mklaus.sqlagent.ui.FirstRunWizard;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.startup.StartupActivity;
import org.jetbrains.annotations.NotNull;

/**
 * Startup activity to show first-run wizard
 *
 * Shows the setup wizard on first launch if:
 * - User hasn't completed setup before
 * - OpenCode is not configured
 */
public class FirstRunStartupActivity implements StartupActivity {

    @Override
    public void runActivity(@NotNull com.intellij.openapi.project.Project project) {
        // Get settings to check if this is first run
        SqlAgentSettings settings = SqlAgentSettings.getInstance();

        if (settings == null) {
            // Service not available, skip wizard
            return;
        }

        if (settings.isFirstRun()) {
            // Show wizard with a slight delay to ensure IDE is fully loaded
            javax.swing.SwingUtilities.invokeLater(() -> {
                FirstRunWizard wizard = new FirstRunWizard();
                wizard.show();

                // Mark as shown
                settings.setFirstRun(false);
            });
        }
    }
}
