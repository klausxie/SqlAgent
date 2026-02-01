package cn.mklaus.sqlagent.config;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.State;
import org.jetbrains.annotations.NotNull;

/**
 * Application-level service for managing SqlAgent settings
 * This is the actual persistent state component that stores settings
 */
@Service(Service.Level.APP)
@State(
    name = "SqlAgentSettings",
    storages = @Storage("sqlAgent.xml")
)
public final class SqlAgentSettingsService implements PersistentStateComponent<SqlAgentConfigurable.State> {

    private SqlAgentConfigurable.State state = new SqlAgentConfigurable.State();

    public static SqlAgentSettingsService getInstance() {
        return com.intellij.openapi.application.ApplicationManager.getApplication().getService(SqlAgentSettingsService.class);
    }

    @NotNull
    @Override
    public SqlAgentConfigurable.State getState() {
        return state;
    }

    @Override
    public void loadState(@NotNull SqlAgentConfigurable.State state) {
        this.state = state;
    }
}
