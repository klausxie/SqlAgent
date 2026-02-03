package cn.mklaus.sqlagent.ui;

import cn.mklaus.sqlagent.config.SqlAgentConfigurable;
import cn.mklaus.sqlagent.model.OptimizationResponse;

/**
 * Utility class for logging optimization-related messages
 */
public class OptimizationLogger {

    private static final int MAX_STACK_TRACE_LINES = 10;

    /**
     * Log configuration information to the panel
     */
    public static void logConfiguration(OptimizationPanel panel, SqlAgentConfigurable.State state, String defaultServerUrl) {
        if (panel == null || state == null) return;

        panel.log("Configuration:");
        panel.log("  Server URL: " + (state.serverUrl != null ? state.serverUrl : defaultServerUrl));
        panel.log("  Auto-start server: " + state.autoStartServer);
        panel.log("");
    }

    /**
     * Log exception with stack trace to the panel
     */
    public static void logError(OptimizationPanel panel, Exception e) {
        if (panel == null || e == null) return;

        panel.log("Error: " + e.getMessage());
        panel.log("Stack trace:");

        StackTraceElement[] stackTrace = e.getStackTrace();
        int linesToLog = Math.min(stackTrace.length, MAX_STACK_TRACE_LINES);

        for (int i = 0; i < linesToLog; i++) {
            panel.log("  at " + stackTrace[i].toString());
        }

        if (stackTrace.length > MAX_STACK_TRACE_LINES) {
            panel.log("  ... " + (stackTrace.length - MAX_STACK_TRACE_LINES) + " more");
        }
    }

    /**
     * Log raw AI response when parsing fails
     */
    public static void logRawResponse(OptimizationPanel panel, OptimizationResponse response) {
        if (panel == null || response == null || response.getRawResponse() == null) return;

        panel.log("");
        panel.log("=== Raw AI Response (for debugging) ===");
        panel.log(response.getRawResponse());
        panel.log("=== End of Raw Response ===");
        panel.log("");
        panel.log("Please check:");
        panel.log("  1. Is the sql-optimizer skill installed?");
        panel.log("  2. Check OpenCode logs: ~/.opencode/logs/server.log");
        panel.log("  3. Database configuration in ~/.opencode/opencode.json");
    }
}
