package cn.mklaus.sqlagent.opencode;

import cn.mklaus.sqlagent.model.OptimizationRequest;
import cn.mklaus.sqlagent.model.OptimizationResponse;
import cn.mklaus.sqlagent.service.PromptBuilder;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.intellij.openapi.diagnostic.Logger;
import okhttp3.*;

import java.io.IOException;
import java.util.UUID;

/**
 * HTTP client for communicating with OpenCode Server
 */
public class OpenCodeClient {
    private static final Logger LOG = Logger.getInstance(OpenCodeClient.class);

    private final SessionManager sessionManager;
    private final Gson gson;

    public OpenCodeClient(String serverUrl) {
        this.sessionManager = new SessionManager(serverUrl);
        this.gson = new Gson();
    }

    /**
     * Send optimization request to OpenCode Server
     */
    public OptimizationResponse optimize(OptimizationRequest request) throws IOException {
        try {
            String sessionId = sessionManager.getSessionId();

            // Build the prompt
            PromptBuilder promptBuilder = new PromptBuilder();
            String prompt = promptBuilder.buildPrompt(request);
            LOG.info(prompt);

            // Send message to session
            String responseJson = sendMessage(sessionId, prompt);

            // Parse response
            return parseOptimizationResponse(responseJson);

        } catch (Exception e) {
            LOG.error("Optimization failed", e);

            OptimizationResponse errorResponse = new OptimizationResponse();
            errorResponse.setErrorMessage(buildDetailedErrorMessage(e));
            return errorResponse;
        }
    }

    /**
     * Send a message to the session
     */
    private String sendMessage(String sessionId, String prompt) throws IOException {
        String url = sessionManager.getServerUrl() + "/session/" + sessionId + "/message";

        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("messageID", generateMessageId());
        requestBody.add("parts", createTextPart(prompt));

        LOG.info("Sending request to: " + url);
        LOG.debug("Request body: " + gson.toJson(requestBody));

        RequestBody body = RequestBody.create(
                MediaType.parse("application/json"),
                gson.toJson(requestBody)
        );

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        try (Response response = sessionManager.getHttpClient().newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "No response body";
                LOG.error("Request failed with status " + response.code() + " " + response.message());
                LOG.error("Response body: " + errorBody);
                throw new IOException("Request failed: " + response.code() + " " + response.message() + ". Response: " + errorBody);
            }

            String responseBody = response.body().string();
            LOG.info("Response received successfully");
            return responseBody;
        }
    }

    /**
     * Generate message ID (must start with "msg" according to OpenCode API spec)
     */
    private String generateMessageId() {
        return "msg-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    /**
     * Create JSON array with text part
     */
    private JsonArray createTextPart(String text) {
        JsonObject textPart = new JsonObject();
        textPart.addProperty("type", "text");
        textPart.addProperty("text", text);

        JsonArray parts = new JsonArray();
        parts.add(textPart);
        return parts;
    }

    /**
     * Parse optimization response from JSON
     */
    private OptimizationResponse parseOptimizationResponse(String jsonStr) {
        try {
            JsonObject jsonResponse = gson.fromJson(jsonStr, JsonObject.class);

            // Extract parts array
            if (!jsonResponse.has("parts")) {
                return createErrorResponse("Response does not contain 'parts'", jsonStr);
            }

            JsonArray parts = jsonResponse.getAsJsonArray("parts");

            // Find text part with optimization result
            for (int i = 0; i < parts.size(); i++) {
                JsonObject part = parts.get(i).getAsJsonObject();
                if (part.has("text")) {
                    String text = part.get("text").getAsString();

                    // Try to extract JSON from the text response
                    OptimizationResponse response = extractJsonFromText(text);
                    if (response != null) {
                        return response;
                    }
                }
            }

            // If no JSON found, return error with raw response
            return createErrorResponse("Could not extract optimization result from response", jsonStr);

        } catch (Exception e) {
            LOG.error("Failed to parse optimization response", e);
            return createErrorResponse("Failed to parse response: " + e.getMessage(), jsonStr);
        }
    }

    /**
     * Extract JSON from text response
     */
    private OptimizationResponse extractJsonFromText(String text) {
        try {
            int[] jsonBounds = findJsonBounds(text);
            if (jsonBounds == null) {
                return null;
            }

            String jsonStr = text.substring(jsonBounds[0], jsonBounds[1] + 1);
            return gson.fromJson(jsonStr, OptimizationResponse.class);
        } catch (Exception e) {
            LOG.warn("Failed to extract JSON from text", e);
            return null;
        }
    }

    /**
     * Find JSON bounds in text (supports markdown code blocks)
     */
    private int[] findJsonBounds(String text) {
        int jsonStart = text.indexOf("```json");
        if (jsonStart != -1) {
            jsonStart = text.indexOf("{", jsonStart);
        } else {
            jsonStart = text.indexOf("{");
        }

        int jsonEnd = text.lastIndexOf("}");

        if (jsonStart != -1 && jsonEnd != -1 && jsonStart < jsonEnd) {
            return new int[]{jsonStart, jsonEnd};
        }
        return null;
    }

    /**
     * Create error response
     */
    private OptimizationResponse createErrorResponse(String errorMessage) {
        return createErrorResponse(errorMessage, null);
    }

    /**
     * Create error response with raw response for debugging
     */
    private OptimizationResponse createErrorResponse(String errorMessage, String rawResponse) {
        OptimizationResponse response = new OptimizationResponse();
        response.setErrorMessage(errorMessage);
        if (rawResponse != null) {
            response.setRawResponse(rawResponse);
        }
        return response;
    }

    /**
     * Build detailed error message with actionable solutions
     */
    private String buildDetailedErrorMessage(Exception e) {
        String errorMsg = e.getMessage();
        if (errorMsg == null) {
            errorMsg = "Unknown error: " + e.getClass().getSimpleName();
        }

        // Connection errors
        if (errorMsg.contains("Failed to create session") || errorMsg.contains("Connection refused")) {
            return formatError("Cannot connect to OpenCode server",
                "OpenCode is running: opencode server",
                "Server is accessible at: " + sessionManager.getServerUrl(),
                "Port 4096 is not blocked by firewall",
                "Quick fix: Run 'opencode server' in terminal");
        }

        // Timeout errors
        if (errorMsg.contains("timeout") || errorMsg.contains("timed out")) {
            return formatError("Request timed out",
                "database-tools MCP is configured in ~/.opencode/opencode.json",
                "Database is accessible and responding",
                "Try simplifying the SQL query",
                "Quick fix: Test connection in SqlAgent settings");
        }

        // Parsing errors
        if (errorMsg.contains("Could not extract optimization result")) {
            return formatError("AI response parsing failed",
                "~/.claude/skills/sql-optimizer/SKILL.md exists",
                "OpenCode logs: ~/.opencode/logs/server.log",
                "Try asking a simpler SQL query");
        }

        // Generic errors
        return "Optimization error\n\nError details: " + errorMsg + "\n\n" +
               "Please check:\n  1. OpenCode logs: ~/.opencode/logs/server.log\n" +
               "  2. Plugin logs: Help → Show Log in Explorer\n" +
               "  3. Report issue: https://github.com/your-org/sqlagent/issues";
    }

    private String formatError(String title, String... checks) {
        StringBuilder sb = new StringBuilder(title).append("\n\nPlease check:\n");
        for (int i = 0; i < checks.length; i++) {
            sb.append("  ").append(i + 1).append(". ").append(checks[i]).append("\n");
        }
        return sb.toString();
    }

    /**
     * Close the client and cleanup resources
     */
    public void close() {
        sessionManager.closeSession();
    }
}
