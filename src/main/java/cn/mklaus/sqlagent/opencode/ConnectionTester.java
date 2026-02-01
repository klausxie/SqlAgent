package cn.mklaus.sqlagent.opencode;

import com.intellij.openapi.diagnostic.Logger;
import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Tests OpenCode and database-tools MCP connection
 */
public class ConnectionTester {
    private static final Logger LOG = Logger.getInstance(ConnectionTester.class);

    private final String serverUrl;
    private final OkHttpClient httpClient;

    public ConnectionTester(String serverUrl) {
        this.serverUrl = serverUrl.endsWith("/") ? serverUrl.substring(0, serverUrl.length() - 1) : serverUrl;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build();
    }

    /**
     * Test connection and return diagnostic result
     */
    public ConnectionTestResult testConnection() {
        ConnectionTestResult result = new ConnectionTestResult();

        // Test 1: OpenCode server reachability
        testOpenCodeServer(result);

        // Test 2: Session creation
        if (result.isOpenCodeReachable()) {
            testSessionCreation(result);
        }

        return result;
    }

    /**
     * Test if OpenCode server is running
     */
    private void testOpenCodeServer(ConnectionTestResult result) {
        try {
            Request request = new Request.Builder()
                    .url(serverUrl + "/health")
                    .get()
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    result.setOpenCodeReachable(true);
                    result.addSuccess("✓ OpenCode server is running at " + serverUrl);
                } else {
                    result.setOpenCodeReachable(false);
                    result.addError("✗ OpenCode server returned status " + response.code());
                }
            }
        } catch (IOException e) {
            result.setOpenCodeReachable(false);
            result.addError("✗ Cannot connect to OpenCode server at " + serverUrl);
            result.addSolution("Please check:");
            result.addSolution("1. OpenCode is installed: run 'opencode server'");
            result.addSolution("2. Server is running at " + serverUrl);
            result.addSolution("3. Port 4096 is not blocked by firewall");
        }
    }

    /**
     * Test session creation
     */
    private void testSessionCreation(ConnectionTestResult result) {
        try {
            RequestBody body = RequestBody.create(
                    MediaType.parse("application/json"),
                    "{}"
            );

            Request request = new Request.Builder()
                    .url(serverUrl + "/session")
                    .post(body)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    result.setSessionWorking(true);
                    result.addSuccess("✓ Session creation works");

                    // Clean up test session
                    String sessionId = response.header("Location");
                    if (sessionId != null) {
                        deleteSession(sessionId);
                    }
                } else {
                    result.setSessionWorking(false);
                    result.addError("✗ Session creation failed with status " + response.code());
                    result.addSolution("Check OpenCode server logs: ~/.opencode/logs/server.log");
                }
            }
        } catch (Exception e) {
            result.setSessionWorking(false);
            result.addError("✗ Session creation error: " + e.getMessage());
        }
    }

    /**
     * Delete test session
     */
    private void deleteSession(String sessionId) {
        try {
            Request request = new Request.Builder()
                    .url(serverUrl + "/session/" + sessionId)
                    .delete()
                    .build();

            httpClient.newCall(request).execute();
        } catch (Exception e) {
            LOG.warn("Failed to delete test session: " + e.getMessage());
        }
    }

    /**
     * Connection test result
     */
    public static class ConnectionTestResult {
        private boolean openCodeReachable = false;
        private boolean sessionWorking = false;
        private final java.util.List<String> successes = new java.util.ArrayList<>();
        private final java.util.List<String> errors = new java.util.ArrayList<>();
        private final java.util.List<String> solutions = new java.util.ArrayList<>();

        public boolean isOpenCodeReachable() {
            return openCodeReachable;
        }

        public void setOpenCodeReachable(boolean openCodeReachable) {
            this.openCodeReachable = openCodeReachable;
        }

        public boolean isSessionWorking() {
            return sessionWorking;
        }

        public void setSessionWorking(boolean sessionWorking) {
            this.sessionWorking = sessionWorking;
        }

        public java.util.List<String> getSuccesses() {
            return successes;
        }

        public java.util.List<String> getErrors() {
            return errors;
        }

        public java.util.List<String> getSolutions() {
            return solutions;
        }

        public void addSuccess(String message) {
            successes.add(message);
        }

        public void addError(String message) {
            errors.add(message);
        }

        public void addSolution(String message) {
            solutions.add(message);
        }

        public boolean isOverallSuccess() {
            return openCodeReachable && sessionWorking;
        }

        public String getSummary() {
            StringBuilder sb = new StringBuilder();

            if (isOverallSuccess()) {
                sb.append("✓ All tests passed! Connection is working.\n\n");
            } else {
                sb.append("✗ Connection test failed. Please fix the issues below.\n\n");
            }

            if (!successes.isEmpty()) {
                sb.append("Successes:\n");
                for (String success : successes) {
                    sb.append("  ").append(success).append("\n");
                }
                sb.append("\n");
            }

            if (!errors.isEmpty()) {
                sb.append("Errors:\n");
                for (String error : errors) {
                    sb.append("  ").append(error).append("\n");
                }
                sb.append("\n");
            }

            if (!solutions.isEmpty()) {
                sb.append("Suggested solutions:\n");
                for (String solution : solutions) {
                    sb.append("  ").append(solution).append("\n");
                }
            }

            return sb.toString();
        }
    }
}
