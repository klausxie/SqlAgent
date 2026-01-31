package cn.mklaus.sqlagent.opencode;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.intellij.openapi.diagnostic.Logger;
import okhttp3.*;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Manages OpenCode session lifecycle
 */
public class SessionManager {
    private static final Logger LOG = Logger.getInstance(SessionManager.class);

    private String currentSessionId;
    private final String serverUrl;
    private final OkHttpClient httpClient;
    private final Gson gson;

    public SessionManager(String serverUrl) {
        this.serverUrl = serverUrl.endsWith("/") ? serverUrl.substring(0, serverUrl.length() - 1) : serverUrl;
        this.gson = new Gson();

        // Configure HTTP client with timeouts
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .build();
    }

    /**
     * Get or create a session
     */
    public String getSessionId() throws IOException {
        if (currentSessionId == null || !isSessionValid()) {
            createNewSession();
        }
        return currentSessionId;
    }

    /**
     * Create a new session
     */
    private void createNewSession() throws IOException {
        String url = buildUrl("/session");

        Request request = new Request.Builder()
                .url(url)
                .post(createEmptyJsonBody())
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to create session: " + response.code());
            }

            JsonObject json = parseJsonResponse(response);
            this.currentSessionId = extractSessionId(json);
            LOG.info("Created new session: " + currentSessionId);
        }
    }

    /**
     * Extract session ID from JSON response
     */
    private String extractSessionId(JsonObject json) throws IOException {
        if (!json.has("id")) {
            throw new IOException("Session response does not contain 'id' field");
        }
        return json.get("id").getAsString();
    }

    /**
     * Parse JSON response
     */
    private JsonObject parseJsonResponse(Response response) throws IOException {
        String responseBody = response.body().string();
        return gson.fromJson(responseBody, JsonObject.class);
    }

    /**
     * Create empty JSON request body
     */
    private RequestBody createEmptyJsonBody() {
        return RequestBody.create(MediaType.parse("application/json"), "{}");
    }

    /**
     * Build URL from endpoint
     */
    private String buildUrl(String endpoint) {
        return serverUrl + endpoint;
    }

    /**
     * Check if current session is valid
     */
    private boolean isSessionValid() {
        if (currentSessionId == null) {
            return false;
        }

        try {
            String url = buildUrl("/session/" + currentSessionId);
            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                return response.isSuccessful();
            }
        } catch (Exception e) {
            LOG.warn("Session validation failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Close current session
     */
    public void closeSession() {
        if (currentSessionId == null) {
            return;
        }

        try {
            String url = buildUrl("/session/" + currentSessionId);
            Request request = new Request.Builder()
                    .url(url)
                    .delete()
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                LOG.info("Closed session: " + currentSessionId);
            }
        } catch (Exception e) {
            LOG.error("Failed to close session: " + e.getMessage());
        } finally {
            currentSessionId = null;
        }
    }

    /**
     * Get HTTP client for making requests
     */
    public OkHttpClient getHttpClient() {
        return httpClient;
    }

    /**
     * Get server URL
     */
    public String getServerUrl() {
        return serverUrl;
    }
}
