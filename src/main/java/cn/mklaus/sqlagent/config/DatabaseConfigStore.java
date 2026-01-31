package cn.mklaus.sqlagent.config;

import cn.mklaus.sqlagent.model.DatabaseConfig;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Persistent store for database configuration
 */
@Service
@State(
        name = "SqlAgentDatabaseConfig",
        storages = @Storage("SqlAgentSettings.xml")
)
public final class DatabaseConfigStore implements PersistentStateComponent<DatabaseConfigStore.State> {

    private State state = new State();

    public static class State {
        public String name = "";
        public String type = "MYSQL";
        public String host = "";
        public int port = 3306;
        public String database = "";
        public String username = "";
        public String password = "";
    }

    public static DatabaseConfigStore getInstance() {
        return com.intellij.openapi.application.ApplicationManager.getApplication().getService(DatabaseConfigStore.class);
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

    /**
     * Get database config from state
     */
    @Nullable
    public DatabaseConfig getConfig() {
        if (StringUtil.isEmpty(state.host)) {
            return null;
        }

        DatabaseConfig config = new DatabaseConfig();
        config.setName(state.name);
        config.setType(cn.mklaus.sqlagent.model.DatabaseType.valueOf(state.type));
        config.setHost(state.host);
        config.setPort(state.port);
        config.setDatabase(state.database);
        config.setUsername(state.username);
        config.setPassword(state.password);

        return config;
    }

    /**
     * Save database config to state
     */
    public void setConfig(DatabaseConfig config) {
        state.name = config.getName();
        state.type = config.getType().name();
        state.host = config.getHost();
        state.port = config.getPort();
        state.database = config.getDatabase();
        state.username = config.getUsername();
        state.password = config.getPassword();
    }
}
