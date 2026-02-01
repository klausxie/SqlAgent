package cn.mklaus.sqlagent;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Persistent settings for SqlAgent plugin
 */
@State(
        name = "SqlAgentSettings",
        storages = @Storage("sqlAgentSettings.xml")
)
public class SqlAgentSettings implements PersistentStateComponent<SqlAgentSettings.State> {

    public static class State {
        public boolean firstRun = true;
        public String serverUrl = "http://localhost:4096";
        public int timeout = 300;
        public boolean showWelcomeOnStartup = true;
    }

    private State state = new State();

    public static SqlAgentSettings getInstance() {
        return ApplicationManager.getApplication().getService(SqlAgentSettings.class);
    }

    @Nullable
    @Override
    public State getState() {
        return state;
    }

    @Override
    public void loadState(@NotNull State state) {
        this.state = state;
    }

    public boolean isFirstRun() {
        return state.firstRun;
    }

    public void setFirstRun(boolean firstRun) {
        state.firstRun = firstRun;
    }

    public String getServerUrl() {
        return state.serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        state.serverUrl = serverUrl;
    }

    public int getTimeout() {
        return state.timeout;
    }

    public void setTimeout(int timeout) {
        state.timeout = timeout;
    }

    public boolean isShowWelcomeOnStartup() {
        return state.showWelcomeOnStartup;
    }

    public void setShowWelcomeOnStartup(boolean showWelcomeOnStartup) {
        state.showWelcomeOnStartup = showWelcomeOnStartup;
    }
}
