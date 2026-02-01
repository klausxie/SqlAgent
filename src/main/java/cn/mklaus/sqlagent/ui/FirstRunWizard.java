package cn.mklaus.sqlagent.ui;

import cn.mklaus.sqlagent.opencode.ConnectionTester;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * First-run setup wizard for SqlAgent
 *
 * Guides users through:
 * 1. OpenCode installation check
 * 2. MCP configuration check
 * 3. Database connection setup
 * 4. Test connection
 */
public class FirstRunWizard {
    private JDialog dialog;
    private CardLayout cardLayout;
    private JPanel mainPanel;
    private int currentStep = 0;

    private static final int TOTAL_STEPS = 4;

    public FirstRunWizard() {
        createWizard();
    }

    /**
     * Show the wizard
     */
    public void show() {
        dialog.setVisible(true);
    }

    /**
     * Create wizard UI
     */
    private void createWizard() {
        dialog = new JDialog();
        dialog.setTitle("SqlAgent Setup Wizard");
        dialog.setModal(true);
        dialog.setSize(700, 500);
        dialog.setLocationRelativeTo(null);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Header
        JLabel headerLabel = new JLabel("Welcome to SqlAgent!", SwingConstants.CENTER);
        headerLabel.setFont(new Font("Arial", Font.BOLD, 24));
        contentPanel.add(headerLabel, BorderLayout.NORTH);

        // Main content with card layout
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        // Add all steps
        mainPanel.add(createWelcomeStep(), "welcome");
        mainPanel.add(createOpenCodeStep(), "opencode");
        mainPanel.add(createMcpStep(), "mcp");
        mainPanel.add(createDatabaseStep(), "database");
        mainPanel.add(createCompleteStep(), "complete");

        contentPanel.add(mainPanel, BorderLayout.CENTER);

        // Navigation buttons
        JPanel buttonPanel = createButtonPanel();
        contentPanel.add(buttonPanel, BorderLayout.SOUTH);

        dialog.add(contentPanel);
    }

    /**
     * Create button panel with navigation
     */
    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton cancelButton = new JButton("Cancel");
        JButton backButton = new JButton("< Back");
        JButton nextButton = new JButton("Next >");
        JButton finishButton = new JButton("Finish");

        cancelButton.addActionListener(e -> dialog.dispose());
        backButton.addActionListener(e -> previousStep());
        nextButton.addActionListener(e -> nextStep());
        finishButton.addActionListener(e -> {
            dialog.dispose();
        });

        panel.add(cancelButton);
        panel.add(backButton);
        panel.add(nextButton);
        panel.add(finishButton);

        return panel;
    }

    /**
     * Navigate to next step
     */
    private void nextStep() {
        if (currentStep < TOTAL_STEPS) {
            currentStep++;
            showStep(currentStep);
        }
    }

    /**
     * Navigate to previous step
     */
    private void previousStep() {
        if (currentStep > 0) {
            currentStep--;
            showStep(currentStep);
        }
    }

    /**
     * Show specific step
     */
    private void showStep(int step) {
        String[] steps = {"welcome", "opencode", "mcp", "database", "complete"};
        if (step >= 0 && step < steps.length) {
            cardLayout.show(mainPanel, steps[step]);
        }
    }

    /**
     * Step 0: Welcome
     */
    private JPanel createWelcomeStep() {
        JPanel panel = new JPanel(new BorderLayout(20, 20));

        JTextArea textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setOpaque(false);
        textArea.setText("""
                This wizard will help you set up SqlAgent for AI-powered SQL optimization.

                You will need:
                • OpenCode server installed and running
                • database-tools MCP configured
                • Database connection information

                Click 'Next' to get started.
                """);
        textArea.setFont(new Font("Arial", Font.PLAIN, 14));

        panel.add(textArea, BorderLayout.CENTER);

        // Illustration
        JLabel iconLabel = new JLabel("🤖", SwingConstants.CENTER);
        iconLabel.setFont(new Font("Arial", Font.PLAIN, 80));
        panel.add(iconLabel, BorderLayout.NORTH);

        return panel;
    }

    /**
     * Step 1: OpenCode Installation
     */
    private JPanel createOpenCodeStep() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        JLabel titleLabel = new JLabel("Step 1: Install OpenCode");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        panel.add(titleLabel, BorderLayout.NORTH);

        JTextArea instructions = new JTextArea();
        instructions.setEditable(false);
        instructions.setOpaque(false);
        instructions.setFont(new Font("Arial", Font.PLAIN, 13));
        instructions.setText("""
                OpenCode is the AI assistant server that powers SqlAgent.

                Install using Homebrew (macOS/Linux):
                ┌────────────────────────────────────┐
                │ brew install opencode              │
                │ opencode server                    │
                └────────────────────────────────────┘

                Or download from: https://github.com/anthropics/opencode

                After installation, verify OpenCode is running:
                ┌────────────────────────────────────┐
                │ open http://localhost:4096         │
                └────────────────────────────────────┘
                """);

        panel.add(instructions, BorderLayout.CENTER);

        // Test button
        JButton testButton = new JButton("🔍 Test OpenCode Connection");
        testButton.addActionListener(e -> testOpenCodeConnection());
        panel.add(testButton, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * Step 2: MCP Configuration
     */
    private JPanel createMcpStep() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        JLabel titleLabel = new JLabel("Step 2: Configure database-tools MCP");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        panel.add(titleLabel, BorderLayout.NORTH);

        JTextArea instructions = new JTextArea();
        instructions.setEditable(false);
        instructions.setOpaque(false);
        instructions.setFont(new Font("Monaco", Font.PLAIN, 12));
        instructions.setText("""
                Create or edit ~/.opencode/opencode.json:

                {
                  "mcpServers": {
                    "database-tools": {
                      "command": "uv",
                      "args": [
                        "--directory",
                        "/path/to/database_tools_mcp",
                        "run",
                        "python",
                        "/path/to/database_tools_mcp/main.py"
                      ],
                      "env": {
                        "DB_TYPE": "mysql",
                        "DB_HOST": "localhost",
                        "DB_PORT": "3306",
                        "DB_USER": "your_username",
                        "DB_PASSWORD": "your_password",
                        "DB_NAME": "your_database"
                      }
                    }
                  }
                }

                IMPORTANT: Replace the paths and credentials with your actual values.
                """);

        JScrollPane scrollPane = new JScrollPane(instructions);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Open config file button
        JButton openButton = new JButton("📂 Open Configuration File");
        openButton.addActionListener(e -> openConfigFile());
        panel.add(openButton, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * Step 3: Database Connection
     */
    private JPanel createDatabaseStep() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        JLabel titleLabel = new JLabel("Step 3: Verify Database Connection");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        panel.add(titleLabel, BorderLayout.NORTH);

        JTextArea instructions = new JTextArea();
        instructions.setEditable(false);
        instructions.setOpaque(false);
        instructions.setFont(new Font("Arial", Font.PLAIN, 13));
        instructions.setText("""
                Ensure your database is accessible:

                MySQL:
                ┌────────────────────────────────────┐
                │ mysql -h localhost -u user -p      │
                └────────────────────────────────────┘

                PostgreSQL:
                ┌────────────────────────────────────┐
                │ psql -h localhost -U user -d db    │
                └────────────────────────────────────┘

                If the connection fails:
                1. Check database is running
                2. Verify credentials in opencode.json
                3. Ensure user has SELECT permissions
                """);

        panel.add(instructions, BorderLayout.CENTER);

        // Test connection button
        JButton testButton = new JButton("🔍 Test Full Connection");
        testButton.addActionListener(e -> testFullConnection());
        panel.add(testButton, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * Step 4: Complete
     */
    private JPanel createCompleteStep() {
        JPanel panel = new JPanel(new BorderLayout(20, 20));

        JLabel successLabel = new JLabel("✅ Setup Complete!", SwingConstants.CENTER);
        successLabel.setFont(new Font("Arial", Font.BOLD, 24));
        panel.add(successLabel, BorderLayout.NORTH);

        JTextArea instructions = new JTextArea();
        instructions.setEditable(false);
        instructions.setOpaque(false);
        instructions.setFont(new Font("Arial", Font.PLAIN, 14));
        instructions.setText("""
                SqlAgent is now ready to use!

                How to optimize SQL:

                1. Select SQL in your editor
                2. Right-click → "Optimize SQL with AI"
                3. View suggestions in the SQL Agent tool window

                Or click the 💡 icon in MyBatis mapper XML files.

                For more help, see: Help → SQL Agent → User Guide
                """);

        panel.add(instructions, BorderLayout.CENTER);

        return panel;
    }

    /**
     * Test OpenCode connection
     */
    private void testOpenCodeConnection() {
        ConnectionTester tester = new ConnectionTester("http://localhost:4096");

        JOptionPane.showMessageDialog(
                dialog,
                "Testing OpenCode connection...",
                "Testing",
                JOptionPane.INFORMATION_MESSAGE
        );

        // Run test in background
        SwingWorker<ConnectionTester.ConnectionTestResult, Void> worker = new SwingWorker<>() {
            @Override
            protected ConnectionTester.ConnectionTestResult doInBackground() {
                return tester.testConnection();
            }

            @Override
            protected void done() {
                try {
                    ConnectionTester.ConnectionTestResult result = get();
                    if (result.isOverallSuccess()) {
                        JOptionPane.showMessageDialog(
                                dialog,
                                "✅ OpenCode is running and accessible!",
                                "Connection Successful",
                                JOptionPane.INFORMATION_MESSAGE
                        );
                    } else {
                        JOptionPane.showMessageDialog(
                                dialog,
                                result.getSummary(),
                                "Connection Failed",
                                JOptionPane.ERROR_MESSAGE
                        );
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(
                            dialog,
                            "Error: " + e.getMessage(),
                            "Test Error",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        };
        worker.execute();
    }

    /**
     * Test full connection (OpenCode + MCP + Database)
     */
    private void testFullConnection() {
        ConnectionTester tester = new ConnectionTester("http://localhost:4096");

        JOptionPane.showMessageDialog(
                dialog,
                "Testing OpenCode, MCP, and database connections...",
                "Testing",
                JOptionPane.INFORMATION_MESSAGE
        );

        SwingWorker<ConnectionTester.ConnectionTestResult, Void> worker = new SwingWorker<>() {
            @Override
            protected ConnectionTester.ConnectionTestResult doInBackground() {
                return tester.testConnection();
            }

            @Override
            protected void done() {
                try {
                    ConnectionTester.ConnectionTestResult result = get();
                    if (result.isOverallSuccess()) {
                        JOptionPane.showMessageDialog(
                                dialog,
                                "✅ All connections successful!\n\n" +
                                        "You can now use SqlAgent to optimize your SQL queries.",
                                "Setup Complete",
                                JOptionPane.INFORMATION_MESSAGE
                        );
                    } else {
                        JOptionPane.showMessageDialog(
                                dialog,
                                result.getSummary(),
                                "Connection Test Failed",
                                JOptionPane.ERROR_MESSAGE
                        );
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(
                            dialog,
                            "Error: " + e.getMessage(),
                            "Test Error",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        };
        worker.execute();
    }

    /**
     * Open configuration file
     */
    private void openConfigFile() {
        try {
            String home = System.getProperty("user.home");
            java.io.File configFile = new java.io.File(home + "/.opencode/opencode.json");

            if (!configFile.exists()) {
                int result = JOptionPane.showConfirmDialog(
                        dialog,
                        "Configuration file does not exist:\n" + configFile.getPath() +
                                "\n\nWould you like to create it?",
                        "File Not Found",
                        JOptionPane.YES_NO_OPTION
                );

                if (result == JOptionPane.YES_OPTION) {
                    configFile.getParentFile().mkdirs();
                    configFile.createNewFile();

                    // Write default template
                    java.nio.file.Files.writeString(configFile.toPath(), """
                            {
                              "mcpServers": {
                                "database-tools": {
                                  "command": "uv",
                                  "args": [
                                    "--directory",
                                    "/path/to/database_tools_mcp",
                                    "run",
                                    "python",
                                    "/path/to/database_tools_mcp/main.py"
                                  ],
                                  "env": {
                                    "DB_TYPE": "mysql",
                                    "DB_HOST": "localhost",
                                    "DB_PORT": "3306",
                                    "DB_USER": "your_username",
                                    "DB_PASSWORD": "your_password",
                                    "DB_NAME": "your_database"
                                  }
                                }
                              }
                            }
                            """);
                }
            }

            // Open with default editor
            if (configFile.exists()) {
                java.awt.Desktop.getDesktop().open(configFile);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(
                    dialog,
                    "Could not open configuration file:\n" + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }
}
