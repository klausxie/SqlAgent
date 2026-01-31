package cn.mklaus.sqlagent.config;

import cn.mklaus.sqlagent.model.DatabaseConfig;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Configurable for database settings
 */
public class DatabaseConfigurable implements Configurable {

    private DatabaseConfigForm form;

    @Override
    public @Nls(capitalization = Nls.Capitalization.Title) String getDisplayName() {
        return "SQL Agent";
    }

    @Override
    public @Nullable JComponent createComponent() {
        if (form == null) {
            form = new DatabaseConfigForm();
        }
        return form.getPanel();
    }

    @Override
    public boolean isModified() {
        DatabaseConfigStore store = DatabaseConfigStore.getInstance();
        DatabaseConfig currentConfig = store.getConfig();
        DatabaseConfig formConfig = form.getConfig();

        if (currentConfig == null) {
            // Always modified if no config exists
            return !formConfig.getHost().isEmpty();
        }

        return !currentConfig.getHost().equals(formConfig.getHost()) ||
               !currentConfig.getDatabase().equals(formConfig.getDatabase()) ||
               !currentConfig.getUsername().equals(formConfig.getUsername());
    }

    @Override
    public void apply() throws ConfigurationException {
        DatabaseConfig config = form.getConfig();
        DatabaseConfigStore.getInstance().setConfig(config);
    }

    @Override
    public void reset() {
        DatabaseConfigStore store = DatabaseConfigStore.getInstance();
        DatabaseConfig config = store.getConfig();
        if (config != null) {
            form.setConfig(config);
        }
    }
}
