package cn.mklaus.sqlagent.config;

import cn.mklaus.sqlagent.opencode.ConnectionTester;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * SqlAgent plugin settings
 */
@State(
        name = "SqlAgentSettings",
        storages = @Storage("sqlAgent.xml")
)
public class SqlAgentConfigurable implements Configurable, PersistentStateComponent<SqlAgentConfigurable.State> {

    private JPanel mainPanel;
    private JTextField serverUrlField;
    private JTextField timeoutField;
    private JButton testConnectionButton;
    private JLabel serverUrlLabel;
    private JLabel timeoutLabel;

    public static class State {
        public String serverUrl = "http://localhost:4096";
        public int timeout = 300;
    }

    private State state = new State();

    @Override
    public @Nls(capitalization = Nls.Capitalization.Title) String getDisplayName() {
        return "SqlAgent";
    }

    @Override
    public @Nullable JComponent createComponent() {
        mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Server URL
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        serverUrlLabel = new JLabel("OpenCode Server URL:");
        mainPanel.add(serverUrlLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        serverUrlField = new JTextField(state.serverUrl);
        mainPanel.add(serverUrlField, gbc);

        // Timeout
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        timeoutLabel = new JLabel("Request Timeout (seconds):");
        mainPanel.add(timeoutLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        timeoutField = new JTextField(String.valueOf(state.timeout));
        mainPanel.add(timeoutField, gbc);

        // Test Connection Button
        gbc.gridx = 0;
        gbc.gridy = 2;
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

    @Override
    public boolean isModified() {
        return !serverUrlField.getText().trim().equals(state.serverUrl) ||
                !timeoutField.getText().trim().equals(String.valueOf(state.timeout));
    }

    @Override
    public void apply() throws ConfigurationException {
        state.serverUrl = serverUrlField.getText().trim();
        try {
            state.timeout = Integer.parseInt(timeoutField.getText().trim());
            if (state.timeout < 10 || state.timeout > 600) {
                throw new ConfigurationException("Timeout must be between 10 and 600 seconds");
            }
        } catch (NumberFormatException e) {
            throw new ConfigurationException("Invalid timeout value");
        }
    }

    @Override
    public void reset() {
        serverUrlField.setText(state.serverUrl);
        timeoutField.setText(String.valueOf(state.timeout));
    }

    @Nullable
    @Override
    public State getState() {
        return state;
    }

    @Override
    public void loadState(State state) {
        this.state = state;
        if (serverUrlField != null) {
            serverUrlField.setText(state.serverUrl);
            timeoutField.setText(String.valueOf(state.timeout));
        }
    }
}
