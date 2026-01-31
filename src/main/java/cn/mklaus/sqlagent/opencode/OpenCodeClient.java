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

            // Send message to session
            String responseJson = sendMessage(sessionId, prompt);

            // Parse response
            return parseOptimizationResponse(responseJson);

        } catch (Exception e) {
            LOG.error("Optimization failed", e);
            OptimizationResponse errorResponse = new OptimizationResponse();
            errorResponse.setErrorMessage("Optimization failed: " + e.getMessage());
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
                return createErrorResponse("Response does not contain 'parts'");
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

            // If no JSON found, return error
            return createErrorResponse("Could not extract optimization result from response");

        } catch (Exception e) {
            LOG.error("Failed to parse optimization response", e);
            return createErrorResponse("Failed to parse response: " + e.getMessage());
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
        OptimizationResponse response = new OptimizationResponse();
        response.setErrorMessage(errorMessage);
        return response;
    }

    /**
     * Close the client and cleanup resources
     */
    public void close() {
        sessionManager.closeSession();
    }
}
