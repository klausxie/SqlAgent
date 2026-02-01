package cn.mklaus.sqlagent.config;

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

    public static class State {
        public String serverUrl = "http://localhost:4096";
        public int timeout = 300;
        public boolean autoStartServer = true;
        public String openCodeExecutablePath = "";
        public boolean stopServerOnExit = false;
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

        // Test Connection Button
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.CENTER;
        testConnectionButton = new JButton("Test Connection");
        testConnectionButton.addActionListener(e -> testConnection());
        mainPanel.add(testConnectionButton, gbc);

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

    @Override
    public boolean isModified() {
        State state = getState();
        return !serverUrlField.getText().trim().equals(state.serverUrl) ||
                !timeoutField.getText().trim().equals(String.valueOf(state.timeout)) ||
                autoStartServerCheckBox.isSelected() != state.autoStartServer ||
                !openCodeExecutablePathField.getText().trim().equals(state.openCodeExecutablePath) ||
                stopServerOnExitCheckBox.isSelected() != state.stopServerOnExit;
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
    }

    @Override
    public void reset() {
        State state = getState();
        serverUrlField.setText(state.serverUrl);
        timeoutField.setText(String.valueOf(state.timeout));
        autoStartServerCheckBox.setSelected(state.autoStartServer);
        openCodeExecutablePathField.setText(state.openCodeExecutablePath);
        stopServerOnExitCheckBox.setSelected(state.stopServerOnExit);
    }
}
