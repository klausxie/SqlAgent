package cn.mklaus.sqlagent.opencode;

import com.intellij.openapi.diagnostic.Logger;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manages OpenCode server lifecycle (startup, shutdown, health checks)
 */
public class OpenCodeServerManager {
    private static final Logger LOG = Logger.getInstance(OpenCodeServerManager.class);
    private static final int MAX_STARTUP_WAIT_SECONDS = 10;

    private final String serverUrl;
    private final OpenCodeLocator locator;
    private final OkHttpClient httpClient;
    private final AtomicBoolean isStarting = new AtomicBoolean(false);

    private Process serverProcess;

    public OpenCodeServerManager(String serverUrl, String customExecutablePath) {
        this.serverUrl = serverUrl.endsWith("/") ? serverUrl.substring(0, serverUrl.length() - 1) : serverUrl;
        this.locator = new OpenCodeLocator(customExecutablePath);
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(2, TimeUnit.SECONDS)
                .readTimeout(2, TimeUnit.SECONDS)
                .build();
    }

    /**
     * Ensure OpenCode server is running
     * @return true if server is running or was successfully started
     */
    public boolean ensureServerRunning() {
        // Check if server is already running
        if (isServerRunning()) {
            LOG.info("OpenCode server is already running");
            return true;
        }

        // Prevent concurrent startup attempts
        if (!isStarting.compareAndSet(false, true)) {
            LOG.info("Server startup already in progress, waiting...");
            return waitForStartup();
        }

        try {
            // Find OpenCode executable
            File executable = locator.findOpenCodeExecutable();
            if (executable == null) {
                LOG.error("OpenCode executable not found");
                return false;
            }

            LOG.info("Starting OpenCode server from: " + executable.getAbsolutePath());
            return startServer(executable);

        } finally {
            isStarting.set(false);
        }
    }

    /**
     * Check if server is running
     */
    public boolean isServerRunning() {
        try {
            Request request = new Request.Builder()
                    .url(serverUrl + "/health")
                    .get()
                    .build();

            try (okhttp3.Response response = httpClient.newCall(request).execute()) {
                return response.isSuccessful();
            }
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Create ProcessBuilder with platform-specific command
     * On Windows, use "cmd /c" to execute batch files
     */
    private ProcessBuilder createProcessBuilder(File executable, String... args) {
        String osName = System.getProperty("os.name").toLowerCase();
        String executablePath = executable.getAbsolutePath();

        if (osName.contains("win") && (executablePath.endsWith(".bat") || executablePath.endsWith(".cmd"))) {
            // Windows batch file: use cmd /c
            List<String> command = new ArrayList<>();
            command.add("cmd");
            command.add("/c");
            command.add(executablePath);
            command.addAll(Arrays.asList(args));
            return new ProcessBuilder(command);
        } else {
            // Direct execution (Linux/macOS or .exe on Windows)
            List<String> command = new ArrayList<>();
            command.add(executablePath);
            command.addAll(Arrays.asList(args));
            return new ProcessBuilder(command);
        }
    }

    /**
     * Start OpenCode server process
     */
    private boolean startServer(File executable) {
        try {
            ProcessBuilder pb = createProcessBuilder(executable, "serve");
            pb.redirectErrorStream(true);
            pb.environment().putAll(System.getenv());

            serverProcess = pb.start();
            LOG.info("OpenCode server process started, PID: " + serverProcess.pid());

            startServerOutputLogger();

            if (waitForServerReady()) {
                LOG.info("OpenCode server started successfully");
                return true;
            } else {
                LOG.error("OpenCode server failed to start within timeout");
                stopServer();
                return false;
            }

        } catch (IOException e) {
            LOG.error("Failed to start OpenCode server process", e);
            return false;
        }
    }

    /**
     * Wait for server to be ready
     */
    private boolean waitForServerReady() {
        int attempts = 0;
        int maxAttempts = MAX_STARTUP_WAIT_SECONDS * 2; // Check every 0.5 seconds

        while (attempts < maxAttempts) {
            try {
                Thread.sleep(500);
                if (isServerRunning()) {
                    return true;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
            attempts++;
        }

        return false;
    }

    /**
     * Wait for startup in progress (for concurrent calls)
     */
    private boolean waitForStartup() {
        int attempts = 0;
        int maxAttempts = MAX_STARTUP_WAIT_SECONDS * 2; // Check every 0.5 seconds

        while (attempts < maxAttempts) {
            try {
                Thread.sleep(500);
                if (!isStarting.get() && isServerRunning()) {
                    return true;
                }
                if (!isStarting.get()) {
                    // Startup finished but failed
                    return false;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
            attempts++;
        }

        return false;
    }

    /**
     * Start logging server output
     */
    private void startServerOutputLogger() {
        Thread loggerThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(serverProcess.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    LOG.info("OpenCode server: " + line);
                }
            } catch (IOException e) {
                if (serverProcess.isAlive()) {
                    LOG.warn("Error reading server output: " + e.getMessage());
                }
            }
        });
        loggerThread.setDaemon(true);
        loggerThread.start();
    }

    /**
     * Stop OpenCode server
     */
    public void stopServer() {
        if (serverProcess != null && serverProcess.isAlive()) {
            LOG.info("Stopping OpenCode server");
            serverProcess.destroy();
            try {
                if (!serverProcess.waitFor(5, TimeUnit.SECONDS)) {
                    LOG.warn("Server did not stop gracefully, forcing termination");
                    serverProcess.destroyForcibly();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                serverProcess.destroyForcibly();
            }
            serverProcess = null;
        }
    }

    /**
     * Get detailed error message for startup failure
     */
    public String getDetailedErrorMessage() {
        File executable = locator.findOpenCodeExecutable();
        if (executable == null) {
            return buildNotInstalledMessage();
        }

        if (serverProcess != null && !serverProcess.isAlive()) {
            int exitCode = serverProcess.exitValue();
            return buildStartupFailedMessage(executable, exitCode);
        }

        return buildGenericErrorMessage(executable);
    }

    private String buildNotInstalledMessage() {
        String osName = System.getProperty("os.name").toLowerCase();

        StringBuilder sb = new StringBuilder();
        sb.append("未找到 OpenCode 可执行文件。\n\n");
        sb.append("插件应包含 OpenCode 可执行文件，但似乎未正确安装。\n\n");
        sb.append("建议操作:\n");
        sb.append("  1. 重新安装插件\n");
        sb.append("  2. 检查插件完整性\n");
        sb.append("  3. 或手动安装 OpenCode：\n\n");

        if (osName.contains("mac")) {
            sb.append("    macOS:\n");
            sb.append("    brew install opencode\n\n");
        } else if (osName.contains("linux")) {
            sb.append("    Linux:\n");
            sb.append("    npm install -g opencode-ai\n\n");
        } else if (osName.contains("win")) {
            sb.append("    Windows:\n");
            sb.append("    npm install -g opencode-ai\n\n");
        }

        sb.append("或在设置中指定自定义 OpenCode 可执行文件路径。");
        return sb.toString();
    }

    private String buildStartupFailedMessage(File executable, int exitCode) {
        StringBuilder sb = new StringBuilder();
        sb.append("OpenCode 服务器启动失败。\n\n");
        sb.append("可执行文件: ").append(executable.getAbsolutePath()).append("\n");
        sb.append("退出代码: ").append(exitCode).append("\n\n");
        sb.append("可能原因:\n");
        sb.append("  1. 端口 4096 已被占用\n");
        sb.append("  2. OpenCode 配置错误\n");
        sb.append("  3. 权限不足\n\n");
        sb.append("建议操作:\n");
        sb.append("  1. 检查端口占用: lsof -i :4096 (macOS/Linux)\n");
        sb.append("  2. 手动启动查看日志: opencode server\n");
        sb.append("  3. 检查配置文件: ~/.opencode/opencode.json");
        return sb.toString();
    }

    private String buildGenericErrorMessage(File executable) {
        StringBuilder sb = new StringBuilder();
        sb.append("OpenCode 服务器启动超时。\n\n");
        sb.append("可执行文件: ").append(executable.getAbsolutePath()).append("\n\n");
        sb.append("可能原因:\n");
        sb.append("  1. 服务器启动时间过长\n");
        sb.append("  2. MCP 配置问题\n");
        sb.append("  3. 网络配置问题\n\n");
        sb.append("建议操作:\n");
        sb.append("  1. 手动启动查看日志: opencode server\n");
        sb.append("  2. 检查服务器日志: ~/.opencode/logs/server.log");
        return sb.toString();
    }
}
