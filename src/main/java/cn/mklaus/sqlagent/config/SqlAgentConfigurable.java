package cn.mklaus.sqlagent.config;

import cn.mklaus.sqlagent.mcp.McpServerLifecycleService;
import cn.mklaus.sqlagent.opencode.ConnectionTester;
import cn.mklaus.sqlagent.opencode.OpenCodeLocator;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.io.File;

/**
 * SqlAgent plugin settings UI
 * Delegates to SqlAgentSettingsService for actual state storage
 */
public class SqlAgentConfigurable implements Configurable {

    private JPanel mainPanel;
    private JTextField serverUrlField;
    private JTextField timeoutField;
    private JButton testConnectionButton;
    private JLabel serverUrlLabel;
    private JLabel timeoutLabel;
    private JCheckBox autoStartServerCheckBox;
    private JTextField openCodeExecutablePathField;
    private JButton autoDetectButton;
    private JCheckBox stopServerOnExitCheckBox;

    // Database configuration fields
    private JComboBox<String> dbTypeComboBox;
    private JTextField dbHostField;
    private JTextField dbPortField;
    private JTextField dbDatabaseField;
    private JTextField dbUsernameField;
    private JPasswordField dbPasswordField;

    // LLM Provider configuration fields
    private JComboBox<String> llmProviderComboBox;
    private JPasswordField llmApiKeyField;
    private JTextField llmBaseUrlField;
    private JTextField llmModelField;
    private JLabel llmSecurityWarningLabel;

    public static class State {
        public String serverUrl = "http://localhost:4096";
        public int timeout = 300;
        public boolean autoStartServer = true;
        public String openCodeExecutablePath = "";
        public boolean stopServerOnExit = false;

        // Database configuration for MCP server
        public DatabaseConfig databaseConfig = new DatabaseConfig();

        // LLM Provider configuration for OpenCode
        public LlmProviderConfig llmProviderConfig = new LlmProviderConfig();
    }

    private State getState() {
        return SqlAgentSettingsService.getInstance().getState();
    }

    @Override
    public @Nls(capitalization = Nls.Capitalization.Title) String getDisplayName() {
        return "SqlAgent";
    }

    @Override
    public @Nullable JComponent createComponent() {
        State state = getState();

        mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;

        // Server URL
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        serverUrlLabel = new JLabel("OpenCode Server URL:");
        mainPanel.add(serverUrlLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        serverUrlField = new JTextField(state.serverUrl);
        mainPanel.add(serverUrlField, gbc);

        row++;

        // Timeout
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        timeoutLabel = new JLabel("Request Timeout (seconds):");
        mainPanel.add(timeoutLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        timeoutField = new JTextField(String.valueOf(state.timeout));
        mainPanel.add(timeoutField, gbc);

        row++;

        // Separator
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        mainPanel.add(new JSeparator(), gbc);

        row++;

        // Auto-start server checkbox
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        autoStartServerCheckBox = new JCheckBox("Auto-start OpenCode server", state.autoStartServer);
        autoStartServerCheckBox.setToolTipText("Automatically start OpenCode server if not running");
        mainPanel.add(autoStartServerCheckBox, gbc);

        row++;

        // Stop server on exit checkbox
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        stopServerOnExitCheckBox = new JCheckBox("Stop server on IDE exit", state.stopServerOnExit);
        stopServerOnExitCheckBox.setToolTipText("Stop OpenCode server when IDE closes (default: keep running)");
        mainPanel.add(stopServerOnExitCheckBox, gbc);

        row++;

        // OpenCode executable path
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        JLabel executableLabel = new JLabel("OpenCode Executable:");
        mainPanel.add(executableLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;

        JPanel pathPanel = new JPanel(new BorderLayout(5, 0));
        openCodeExecutablePathField = new JTextField(state.openCodeExecutablePath);
        openCodeExecutablePathField.setToolTipText("Leave empty to auto-detect");
        pathPanel.add(openCodeExecutablePathField, BorderLayout.CENTER);

        autoDetectButton = new JButton("Auto-detect");
        autoDetectButton.addActionListener(e -> autoDetectExecutable());
        pathPanel.add(autoDetectButton, BorderLayout.EAST);

        mainPanel.add(pathPanel, gbc);

        row++;

        // Separator for database configuration
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(new JSeparator(), gbc);

        row++;

        // Database Configuration Section Label
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        JLabel dbSectionLabel = new JLabel("<html><b>Database Configuration (for SQL Analysis)</b></html>");
        mainPanel.add(dbSectionLabel, gbc);

        row++;

        // Database Type
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        JLabel dbTypeLabel = new JLabel("Database Type:");
        mainPanel.add(dbTypeLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        dbTypeComboBox = new JComboBox<>(new String[]{"MySQL", "PostgreSQL"});
        dbTypeComboBox.setSelectedItem(state.databaseConfig.getType().equalsIgnoreCase("postgresql") ? "PostgreSQL" : "MySQL");
        dbTypeComboBox.addActionListener(e -> updateDefaultPort());
        mainPanel.add(dbTypeComboBox, gbc);

        row++;

        // Database Host
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        JLabel dbHostLabel = new JLabel("Host:");
        mainPanel.add(dbHostLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        dbHostField = new JTextField(state.databaseConfig.getHost());
        mainPanel.add(dbHostField, gbc);

        row++;

        // Database Port
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        JLabel dbPortLabel = new JLabel("Port:");
        mainPanel.add(dbPortLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        dbPortField = new JTextField(String.valueOf(state.databaseConfig.getPort()));
        mainPanel.add(dbPortField, gbc);

        row++;

        // Database Name
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        JLabel dbDatabaseLabel = new JLabel("Database:");
        mainPanel.add(dbDatabaseLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        dbDatabaseField = new JTextField(state.databaseConfig.getDatabase());
        mainPanel.add(dbDatabaseField, gbc);

        row++;

        // Database Username
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        JLabel dbUsernameLabel = new JLabel("Username:");
        mainPanel.add(dbUsernameLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        dbUsernameField = new JTextField(state.databaseConfig.getUsername());
        mainPanel.add(dbUsernameField, gbc);

        row++;

        // Database Password
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        JLabel dbPasswordLabel = new JLabel("Password:");
        mainPanel.add(dbPasswordLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        dbPasswordField = new JPasswordField(state.databaseConfig.getPassword());
        mainPanel.add(dbPasswordField, gbc);

        row++;

        // Test Connection Button
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.CENTER;
        testConnectionButton = new JButton("Test Connection");
        testConnectionButton.addActionListener(e -> testConnection());
        mainPanel.add(testConnectionButton, gbc);

        row++;

        // Separator for LLM Provider Configuration
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(new JSeparator(), gbc);

        row++;

        // LLM Provider Configuration Section Label
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        JLabel llmSectionLabel = new JLabel("<html><b>LLM Provider Configuration (for OpenCode)</b></html>");
        mainPanel.add(llmSectionLabel, gbc);

        row++;

        // LLM Provider Type
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        JLabel llmProviderLabel = new JLabel("Provider:");
        mainPanel.add(llmProviderLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        llmProviderComboBox = new JComboBox<>(new String[]{"Anthropic", "OpenAI", "Google Gemini"});
        llmProviderComboBox.setSelectedItem(state.llmProviderConfig.getProviderDisplayName());
        llmProviderComboBox.addActionListener(e -> updateLlmPlaceholderUrl());
        llmProviderComboBox.setToolTipText("Select the LLM provider for AI-powered SQL optimization");
        mainPanel.add(llmProviderComboBox, gbc);

        row++;

        // LLM API Key
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        JLabel llmApiKeyLabel = new JLabel("API Key:");
        mainPanel.add(llmApiKeyLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        llmApiKeyField = new JPasswordField(state.llmProviderConfig.getApiKey());
        llmApiKeyField.setToolTipText("Enter your API key for the selected provider");
        mainPanel.add(llmApiKeyField, gbc);

        row++;

        // LLM Base URL (Optional)
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        JLabel llmBaseUrlLabel = new JLabel("Base URL (Optional):");
        mainPanel.add(llmBaseUrlLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        // Display the saved value, or show default as placeholder
        String savedBaseUrl = state.llmProviderConfig.getBaseUrl();
        llmBaseUrlField = new JTextField(savedBaseUrl);
        llmBaseUrlField.setToolTipText("Custom API endpoint (leave empty to use default)");
        mainPanel.add(llmBaseUrlField, gbc);

        row++;

        // LLM Model (Optional)
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        JLabel llmModelLabel = new JLabel("Model (Optional):");
        mainPanel.add(llmModelLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        // Display the saved value, or show default as placeholder
        String savedModel = state.llmProviderConfig.getModel();
        llmModelField = new JTextField(savedModel);
        llmModelField.setToolTipText("Custom model (e.g., anthropic/claude-sonnet-4-5, openai/gpt-4o). Leave empty to use provider default.");
        mainPanel.add(llmModelField, gbc);

        row++;

        // Security Warning
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        llmSecurityWarningLabel = new JLabel("<html><div style='color:#E65100;'>⚠️ Security Notice: API keys will be stored in plain text in ~/.opencode/opencode.json</div></html>");
        llmSecurityWarningLabel.setToolTipText("Ensure your system is secure and don't share configuration files");
        mainPanel.add(llmSecurityWarningLabel, gbc);

        return mainPanel;
    }

    /**
     * Test connection to OpenCode server
     */
    private void testConnection() {
        String serverUrl = serverUrlField.getText().trim();

        if (serverUrl.isEmpty()) {
            Messages.showErrorDialog(mainPanel, "Please enter a server URL", "Server URL Required");
            return;
        }

        // Show testing dialog
        JDialog progressDialog = new JDialog();
        progressDialog.setTitle("Testing Connection");
        progressDialog.setModal(false);
        progressDialog.setSize(300, 100);

        JPanel panel = new JPanel(new BorderLayout());
        JLabel label = new JLabel("Testing connection to " + serverUrl + "...", SwingConstants.CENTER);
        panel.add(label, BorderLayout.CENTER);
        progressDialog.add(panel);

        // Center on screen
        progressDialog.setLocationRelativeTo(mainPanel);

        // Run test in background
        new SwingWorker<ConnectionTester.ConnectionTestResult, Void>() {
            @Override
            protected ConnectionTester.ConnectionTestResult doInBackground() {
                ConnectionTester tester = new ConnectionTester(serverUrl);
                return tester.testConnection();
            }

            @Override
            protected void done() {
                progressDialog.dispose();
                try {
                    ConnectionTester.ConnectionTestResult result = get();

                    // Show result dialog
                    if (result.isOverallSuccess()) {
                        Messages.showMessageDialog(
                                mainPanel,
                                result.getSummary(),
                                "Connection Test Successful",
                                Messages.getInformationIcon()
                        );
                    } else {
                        Messages.showWarningDialog(
                                mainPanel,
                                result.getSummary(),
                                "Connection Test Failed"
                        );
                    }
                } catch (Exception e) {
                    Messages.showErrorDialog(
                            mainPanel,
                            "Test failed: " + e.getMessage(),
                            "Connection Test Error"
                    );
                }
            }
        }.execute();

        progressDialog.setVisible(true);
    }

    /**
     * Auto-detect OpenCode executable path
     */
    private void autoDetectExecutable() {
        OpenCodeLocator locator = new OpenCodeLocator(null);
        File executable = locator.findOpenCodeExecutable();

        if (executable != null) {
            openCodeExecutablePathField.setText(executable.getAbsolutePath());

            // Show version
            String version = locator.getOpenCodeVersion();
            String message = "Found OpenCode at: " + executable.getAbsolutePath();
            if (version != null) {
                message += "\nVersion: " + version;
            }
            Messages.showMessageDialog(
                    mainPanel,
                    message,
                    "OpenCode Detected",
                    Messages.getInformationIcon()
            );
        } else {
            Messages.showWarningDialog(
                    mainPanel,
                    "Could not find OpenCode executable in common locations.\n\n" +
                    "Please install OpenCode or specify the path manually.",
                    "OpenCode Not Found"
            );
        }
    }

    /**
     * Update port when database type changes
     */
    private void updateDefaultPort() {
        String selectedType = (String) dbTypeComboBox.getSelectedItem();
        int defaultPort = "PostgreSQL".equals(selectedType) ? 5432 : 3306;
        dbPortField.setText(String.valueOf(defaultPort));
    }

    /**
     * Update LLM base URL and model placeholders when provider changes
     */
    private void updateLlmPlaceholderUrl() {
        String selectedProvider = (String) llmProviderComboBox.getSelectedItem();
        String providerType = LlmProviderConfig.getProviderTypeFromDisplayName(selectedProvider);
        String defaultUrl = LlmProviderConfig.getDefaultBaseUrl(providerType);
        String defaultModel = LlmProviderConfig.getDefaultModel(providerType);

        // Only update URL if the field is empty
        String currentUrl = llmBaseUrlField.getText().trim();
        if (currentUrl.isEmpty()) {
            llmBaseUrlField.setText(defaultUrl);
        }

        // Only update model if the field is empty
        String currentModel = llmModelField.getText().trim();
        if (currentModel.isEmpty()) {
            llmModelField.setText(defaultModel);
        }
    }

    /**
     * Check if the model is a default model
     */
    private boolean isDefaultModel(String model) {
        return model.equals("anthropic/claude-sonnet-4-5") ||
               model.equals("openai/gpt-4o") ||
               model.equals("gemini/gemini-2.5-flash");
    }

    /**
     * Check if the URL is a default URL
     */
    private boolean isDefaultUrl(String url) {
        return url.equals("https://api.anthropic.com") ||
               url.equals("https://api.openai.com/v1") ||
               url.equals("https://generativelanguage.googleapis.com/v1beta");
    }

    /**
     * Get LLM provider type from combo box
     */
    private String getLlmProviderType() {
        return LlmProviderConfig.getProviderTypeFromDisplayName(
            (String) llmProviderComboBox.getSelectedItem()
        );
    }

    /**
     * Get database type from combo box
     */
    private String getTypeFromComboBox() {
        String selectedType = (String) dbTypeComboBox.getSelectedItem();
        return "PostgreSQL".equals(selectedType) ? "postgresql" : "mysql";
    }

    @Override
    public boolean isModified() {
        State state = getState();
        DatabaseConfig dbConfig = state.databaseConfig;
        LlmProviderConfig llmConfig = state.llmProviderConfig;

        return !serverUrlField.getText().trim().equals(state.serverUrl) ||
                !timeoutField.getText().trim().equals(String.valueOf(state.timeout)) ||
                autoStartServerCheckBox.isSelected() != state.autoStartServer ||
                !openCodeExecutablePathField.getText().trim().equals(state.openCodeExecutablePath) ||
                stopServerOnExitCheckBox.isSelected() != state.stopServerOnExit ||
                !getTypeFromComboBox().equals(dbConfig.getType()) ||
                !dbHostField.getText().trim().equals(dbConfig.getHost()) ||
                !dbPortField.getText().trim().equals(String.valueOf(dbConfig.getPort())) ||
                !dbDatabaseField.getText().trim().equals(dbConfig.getDatabase()) ||
                !dbUsernameField.getText().trim().equals(dbConfig.getUsername()) ||
                !String.valueOf(dbPasswordField.getPassword()).equals(dbConfig.getPassword()) ||
                !getLlmProviderType().equals(llmConfig.getProviderType()) ||
                !String.valueOf(llmApiKeyField.getPassword()).equals(llmConfig.getApiKey()) ||
                !llmBaseUrlField.getText().trim().equals(llmConfig.getBaseUrl()) ||
                !llmModelField.getText().trim().equals(llmConfig.getModel());
    }

    @Override
    public void apply() throws ConfigurationException {
        State state = getState();
        state.serverUrl = serverUrlField.getText().trim();
        try {
            state.timeout = Integer.parseInt(timeoutField.getText().trim());
            if (state.timeout < 10 || state.timeout > 600) {
                throw new ConfigurationException("Timeout must be between 10 and 600 seconds");
            }
        } catch (NumberFormatException e) {
            throw new ConfigurationException("Invalid timeout value");
        }
        state.autoStartServer = autoStartServerCheckBox.isSelected();
        state.openCodeExecutablePath = openCodeExecutablePathField.getText().trim();
        state.stopServerOnExit = stopServerOnExitCheckBox.isSelected();

        // Save database configuration
        state.databaseConfig.setType(getTypeFromComboBox());
        state.databaseConfig.setHost(dbHostField.getText().trim());
        try {
            state.databaseConfig.setPort(Integer.parseInt(dbPortField.getText().trim()));
        } catch (NumberFormatException e) {
            throw new ConfigurationException("Invalid port value");
        }
        state.databaseConfig.setDatabase(dbDatabaseField.getText().trim());
        state.databaseConfig.setUsername(dbUsernameField.getText().trim());
        state.databaseConfig.setPassword(String.valueOf(dbPasswordField.getPassword()));

        // Save LLM provider configuration
        state.llmProviderConfig.setProviderType(getLlmProviderType());
        state.llmProviderConfig.setApiKey(String.valueOf(llmApiKeyField.getPassword()));

        // Save base URL (if empty, will use provider default)
        state.llmProviderConfig.setBaseUrl(llmBaseUrlField.getText().trim());

        // Save model (if empty, will use provider default)
        state.llmProviderConfig.setModel(llmModelField.getText().trim());

        // After saving configurations, update OpenCode config
        if (state.databaseConfig.isValid() || state.llmProviderConfig.isValid()) {
            try {
                // Start MCP server (will also update OpenCode config)
                McpServerLifecycleService lifecycleService = McpServerLifecycleService.getInstance();
                boolean started = lifecycleService.startMcpServer();

                if (started) {
                    // Show success notification
                    showNotification("Configuration Saved",
                            "MCP server started and OpenCode configuration updated at ~/.opencode/opencode.json",
                            NotificationType.INFORMATION);
                } else {
                    showNotification("Configuration Warning",
                            "Settings saved, but MCP server failed to start. Check logs for details.",
                            NotificationType.WARNING);
                }
            } catch (Exception e) {
                showNotification("MCP Server Error",
                        "Failed to start MCP server: " + e.getMessage(),
                        NotificationType.ERROR);
            }
        }
    }

    /**
     * Show notification to user
     */
    private void showNotification(String title, String content, NotificationType type) {
        Notification notification = new Notification("SqlAgent", title, content, type);
        Notifications.Bus.notify(notification);
    }

    @Override
    public void reset() {
        State state = getState();
        serverUrlField.setText(state.serverUrl);
        timeoutField.setText(String.valueOf(state.timeout));
        autoStartServerCheckBox.setSelected(state.autoStartServer);
        openCodeExecutablePathField.setText(state.openCodeExecutablePath);
        stopServerOnExitCheckBox.setSelected(state.stopServerOnExit);

        // Reset database configuration
        DatabaseConfig dbConfig = state.databaseConfig;
        dbTypeComboBox.setSelectedItem(dbConfig.getType().equalsIgnoreCase("postgresql") ? "PostgreSQL" : "MySQL");
        dbHostField.setText(dbConfig.getHost());
        dbPortField.setText(String.valueOf(dbConfig.getPort()));
        dbDatabaseField.setText(dbConfig.getDatabase());
        dbUsernameField.setText(dbConfig.getUsername());
        dbPasswordField.setText(dbConfig.getPassword());

        // Reset LLM provider configuration
        LlmProviderConfig llmConfig = state.llmProviderConfig;
        llmProviderComboBox.setSelectedItem(llmConfig.getProviderDisplayName());
        llmApiKeyField.setText(llmConfig.getApiKey());
        llmBaseUrlField.setText(llmConfig.getBaseUrl());
        llmModelField.setText(llmConfig.getModel());
    }
}
