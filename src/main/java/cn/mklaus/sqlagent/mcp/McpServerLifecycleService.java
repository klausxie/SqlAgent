package cn.mklaus.sqlagent.mcp;

import cn.mklaus.sqlagent.config.DatabaseConfig;
import cn.mklaus.sqlagent.config.LlmProviderConfig;
import cn.mklaus.sqlagent.config.SqlAgentSettingsService;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * Application service that manages MCP server lifecycle
 */
@Service(Service.Level.APP)
public final class McpServerLifecycleService implements Disposable {
    private static final Logger LOG = Logger.getInstance(McpServerLifecycleService.class);

    private final McpServerManager mcpServerManager;
    private final OpenCodeConfigGenerator configGenerator;
    private boolean isRunning = false;

    public McpServerLifecycleService() {
        this.mcpServerManager = new McpServerManager();
        this.configGenerator = new OpenCodeConfigGenerator();
    }

    public static McpServerLifecycleService getInstance() {
        return ApplicationManager.getApplication().getService(McpServerLifecycleService.class);
    }

    /**
     * Start MCP server with current database configuration
     * @return true if started successfully
     */
    public synchronized boolean startMcpServer() {
        if (isRunning) {
            LOG.info("MCP server is already running");
            return true;
        }

        var state = SqlAgentSettingsService.getInstance().getState();
        DatabaseConfig dbConfig = state.databaseConfig;
        LlmProviderConfig llmConfig = state.llmProviderConfig;

        // Update OpenCode configuration even if database config is not valid
        // (LLM provider config alone is useful)
        if (!dbConfig.isValid()) {
            LOG.warn("Database configuration is not valid, not starting MCP server");
            // Still update OpenCode config for LLM provider
            String jarPath = System.getProperty("java.io.tmpdir") + "/sqlagent/mcp/sqlagent-mcp-server.jar";
            LOG.info("Updating OpenCode configuration with LLM provider config");
            configGenerator.updateConfig(dbConfig, llmConfig, jarPath);
            return false;
        }

        LOG.info("Starting MCP server for database: " + dbConfig);
        isRunning = mcpServerManager.startServer(dbConfig);

        if (isRunning) {
            // Update OpenCode configuration with the extracted JAR path and LLM provider config
            String jarPath = System.getProperty("java.io.tmpdir") + "/sqlagent/mcp/sqlagent-mcp-server.jar";
            LOG.info("Updating OpenCode configuration with MCP server JAR: " + jarPath);
            configGenerator.updateConfig(dbConfig, llmConfig, jarPath);
        }

        return isRunning;
    }

    /**
     * Stop MCP server
     */
    public synchronized void stopMcpServer() {
        if (!isRunning) {
            return;
        }

        LOG.info("Stopping MCP server");
        mcpServerManager.stopServer();
        isRunning = false;
    }

    /**
     * Check if MCP server is running
     */
    public boolean isMcpServerRunning() {
        return isRunning && mcpServerManager.isServerRunning();
    }

    /**
     * Restart MCP server with new configuration
     */
    public boolean restartMcpServer() {
        stopMcpServer();
        return startMcpServer();
    }

    @Override
    public void dispose() {
        stopMcpServer();
    }
}
