package cn.mklaus.sqlagent.mcp;

import com.intellij.openapi.diagnostic.Logger;
import cn.mklaus.sqlagent.config.DatabaseConfig;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Manages the Java MCP server lifecycle
 */
public class McpServerManager {
    private static final Logger LOG = Logger.getInstance(McpServerManager.class);
    private static final int MAX_STARTUP_WAIT_SECONDS = 5;

    private Process mcpServerProcess;

    /**
     * Start the MCP server
     * @param config Database configuration
     * @return true if started successfully
     */
    public boolean startServer(DatabaseConfig config) {
        try {
            // Extract JAR from plugin resources
            Path jarPath = extractMcpServerJar();
            if (jarPath == null) {
                LOG.error("Failed to extract MCP server JAR");
                return false;
            }

            // Find Java executable
            String javaExec = findJavaExecutable();
            if (javaExec == null) {
                LOG.error("Failed to find Java executable");
                return false;
            }

            // Build process
            ProcessBuilder pb = new ProcessBuilder(
                javaExec,
                "-jar",
                jarPath.toString()
            );

            // Set environment variables for database connection
            pb.environment().put("DB_TYPE", config.getType());
            pb.environment().put("DB_HOST", config.getHost());
            pb.environment().put("DB_PORT", String.valueOf(config.getPort()));
            pb.environment().put("DB_NAME", config.getDatabase());
            pb.environment().put("DB_USER", config.getUsername());
            pb.environment().put("DB_PASSWORD", config.getPassword());

            pb.redirectErrorStream(true);

            LOG.info("Starting MCP server: " + javaExec + " -jar " + jarPath);
            mcpServerProcess = pb.start();

            // Start output logger
            startOutputLogger();

            // Wait a bit to ensure it starts
            Thread.sleep(1000);

            if (mcpServerProcess.isAlive()) {
                LOG.info("MCP server started successfully, PID: " + mcpServerProcess.pid());
                return true;
            } else {
                int exitCode = mcpServerProcess.exitValue();
                LOG.error("MCP server failed to start, exit code: " + exitCode);
                mcpServerProcess = null;
                return false;
            }

        } catch (Exception e) {
            LOG.error("Failed to start MCP server", e);
            mcpServerProcess = null;
            return false;
        }
    }

    /**
     * Stop the MCP server
     */
    public void stopServer() {
        if (mcpServerProcess != null && mcpServerProcess.isAlive()) {
            LOG.info("Stopping MCP server");
            mcpServerProcess.destroy();
            try {
                if (!mcpServerProcess.waitFor(5, TimeUnit.SECONDS)) {
                    LOG.warn("MCP server did not stop gracefully, forcing termination");
                    mcpServerProcess.destroyForcibly();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                mcpServerProcess.destroyForcibly();
            }
            mcpServerProcess = null;
        }
    }

    /**
     * Check if MCP server is running
     */
    public boolean isServerRunning() {
        return mcpServerProcess != null && mcpServerProcess.isAlive();
    }

    /**
     * Extract MCP server JAR from plugin resources to temp directory
     */
    private Path extractMcpServerJar() throws IOException {
        // Get temp directory
        Path tempDir = Path.of(System.getProperty("java.io.tmpdir"), "sqlagent", "mcp");
        Files.createDirectories(tempDir);

        Path jarPath = tempDir.resolve("sqlagent-mcp-server.jar");

        // Try to extract from resources
        try (var in = getClass().getResourceAsStream("/mcp/sqlagent-mcp-server.jar")) {
            if (in == null) {
                LOG.error("MCP server JAR not found in resources: /mcp/sqlagent-mcp-server.jar");
                return null;
            }

            Files.copy(in, jarPath, StandardCopyOption.REPLACE_EXISTING);
            LOG.info("Extracted MCP server JAR to: " + jarPath);
            return jarPath;

        } catch (IOException e) {
            LOG.error("Failed to extract MCP server JAR", e);
            return null;
        }
    }

    /**
     * Find Java executable
     */
    private String findJavaExecutable() {
        // Use current Java
        String javaHome = System.getProperty("java.home");
        String osName = System.getProperty("os.name").toLowerCase();
        String javaBin = osName.contains("win") ? "java.exe" : "java";

        Path javaPath = Path.of(javaHome, "bin", javaBin);
        if (Files.exists(javaPath)) {
            return javaPath.toString();
        }

        LOG.error("Java executable not found at: " + javaPath);
        return null;
    }

    /**
     * Start logging server output
     */
    private void startOutputLogger() {
        Thread loggerThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(mcpServerProcess.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    LOG.info("MCP server: " + line);
                }
            } catch (IOException e) {
                if (mcpServerProcess.isAlive()) {
                    LOG.warn("Error reading MCP server output: " + e.getMessage());
                }
            }
        });
        loggerThread.setDaemon(true);
        loggerThread.start();
    }
}
