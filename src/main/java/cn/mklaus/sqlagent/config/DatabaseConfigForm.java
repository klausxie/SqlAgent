package cn.mklaus.sqlagent.config;

import cn.mklaus.sqlagent.model.DatabaseConfig;
import cn.mklaus.sqlagent.model.DatabaseType;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.UIUtil;

import javax.swing.*;
import java.awt.*;

/**
 * Form component for database configuration UI
 */
public class DatabaseConfigForm {
    private JPanel mainPanel;
    private JBTextField nameField;
    private JComboBox<DatabaseType> typeCombo;
    private JBTextField hostField;
    private JBTextField portField;
    private JBTextField databaseField;
    private JBTextField usernameField;
    private JBPasswordField passwordField;
    private JButton testButton;

    public DatabaseConfigForm() {
        initUI();
    }

    private void initUI() {
        // Create form
        FormBuilder formBuilder = FormBuilder.createFormBuilder()
                .setAlignLabelOnRight(true)
                .setHorizontalGap(UIUtil.DEFAULT_HGAP)
                .setVerticalGap(UIUtil.DEFAULT_VGAP);

        // Name field
        nameField = new JBTextField();
        nameField.setPreferredSize(new Dimension(200, nameField.getPreferredSize().height));

        // Type combo
        typeCombo = new JComboBox<>(DatabaseType.values());
        typeCombo.addActionListener(e -> updateDefaultPort());

        // Host field
        hostField = new JBTextField();
        hostField.setPreferredSize(new Dimension(200, hostField.getPreferredSize().height));

        // Port field
        portField = new JBTextField();
        portField.setPreferredSize(new Dimension(100, portField.getPreferredSize().height));

        // Database field
        databaseField = new JBTextField();
        databaseField.setPreferredSize(new Dimension(200, databaseField.getPreferredSize().height));

        // Username field
        usernameField = new JBTextField();
        usernameField.setPreferredSize(new Dimension(200, usernameField.getPreferredSize().height));

        // Password field
        passwordField = new JBPasswordField();
        passwordField.setPreferredSize(new Dimension(200, passwordField.getPreferredSize().height));

        // Test button
        testButton = new JButton("Test Connection");
        testButton.addActionListener(e -> testConnection());

        // Build form
        mainPanel = formBuilder
                .addLabeledComponent("Configuration Name:", nameField)
                .addVerticalGap(10)
                .addLabeledComponent("Database Type:", typeCombo)
                .addVerticalGap(10)
                .addComponent(createHostPortPanel(), 0)
                .addVerticalGap(10)
                .addLabeledComponent("Database Name:", databaseField)
                .addVerticalGap(10)
                .addLabeledComponent("Username:", usernameField)
                .addVerticalGap(10)
                .addLabeledComponent("Password:", passwordField)
                .addVerticalGap(15)
                .addComponent(testButton, 0)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();
    }

    private JPanel createHostPortPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 0, 10);
        gbc.anchor = GridBagConstraints.WEST;

        // Host label
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JBLabel("Host:"), gbc);

        // Host field
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(hostField, gbc);

        // Port label
        gbc.gridx = 2;
        gbc.weightx = 0.0;
        gbc.insets = new Insets(0, 10, 0, 5);
        panel.add(new JBLabel("Port:"), gbc);

        // Port field
        gbc.gridx = 3;
        gbc.insets = new Insets(0, 0, 0, 0);
        panel.add(portField, gbc);

        return panel;
    }

    private void updateDefaultPort() {
        DatabaseType type = (DatabaseType) typeCombo.getSelectedItem();
        if (type == DatabaseType.MYSQL) {
            portField.setText("3306");
        } else if (type == DatabaseType.POSTGRESQL) {
            portField.setText("5432");
        }
    }

    private void testConnection() {
        DatabaseConfig config = getConfig();

        // Validate required fields
        if (config.getHost().isEmpty() || config.getDatabase().isEmpty() ||
            config.getUsername().isEmpty()) {
            Messages.showWarningDialog(
                    "Please fill in all required fields (Host, Database, Username).",
                    "Incomplete Configuration"
            );
            return;
        }

        // Test connection
        try {
            cn.mklaus.sqlagent.database.DatabaseConnectionManager connectionManager =
                    cn.mklaus.sqlagent.database.DatabaseConnectionManager.getInstance();

            boolean success = connectionManager.testConnection(config);

            if (success) {
                Messages.showInfoMessage(
                        "Successfully connected to database!\n\n" +
                        "Host: " + config.getHost() + "\n" +
                        "Port: " + config.getPort() + "\n" +
                        "Database: " + config.getDatabase(),
                        "Connection Test Successful"
                );
            } else {
                Messages.showErrorDialog(
                        "Failed to connect to database.\n\n" +
                        "Please check:\n" +
                        "1. Database server is running\n" +
                        "2. Host and port are correct\n" +
                        "3. Username and password are correct\n" +
                        "4. Database exists\n" +
                        "5. User has proper permissions",
                        "Connection Test Failed"
                );
            }
        } catch (Exception e) {
            Messages.showErrorDialog(
                    "Connection test failed with error:\n" + e.getMessage(),
                    "Connection Test Error"
            );
        }
    }

    public JPanel getPanel() {
        return mainPanel;
    }

    public void setConfig(DatabaseConfig config) {
        nameField.setText(config.getName());
        typeCombo.setSelectedItem(config.getType());
        hostField.setText(config.getHost());
        portField.setText(String.valueOf(config.getPort()));
        databaseField.setText(config.getDatabase());
        usernameField.setText(config.getUsername());
        passwordField.setText(config.getPassword());
    }

    public DatabaseConfig getConfig() {
        DatabaseConfig config = new DatabaseConfig();
        config.setName(nameField.getText().trim());
        config.setType((DatabaseType) typeCombo.getSelectedItem());
        config.setHost(hostField.getText().trim());

        try {
            config.setPort(Integer.parseInt(portField.getText().trim()));
        } catch (NumberFormatException e) {
            config.setPort(3306);
        }

        config.setDatabase(databaseField.getText().trim());
        config.setUsername(usernameField.getText().trim());
        config.setPassword(new String(passwordField.getPassword()));

        return config;
    }

    public boolean isModified(DatabaseConfig config) {
        return !nameField.getText().trim().equals(config.getName()) ||
                !hostField.getText().trim().equals(config.getHost()) ||
                !databaseField.getText().trim().equals(config.getDatabase()) ||
                !usernameField.getText().trim().equals(config.getUsername());
    }
}
