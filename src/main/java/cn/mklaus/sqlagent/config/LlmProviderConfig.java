package cn.mklaus.sqlagent.config;

/**
 * LLM Provider configuration for OpenCode
 */
public class LlmProviderConfig {
    private String providerType = "anthropic";  // anthropic, openai, gemini
    private String apiKey = "";
    private String baseUrl = "";  // Optional: custom endpoint
    private String model = "";    // Optional: custom model (e.g., "anthropic/claude-sonnet-4-5")

    public LlmProviderConfig() {
    }

    public LlmProviderConfig(String providerType, String apiKey, String baseUrl, String model) {
        this.providerType = providerType;
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.model = model;
    }

    public String getProviderType() {
        return providerType;
    }

    public void setProviderType(String providerType) {
        this.providerType = providerType;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    /**
     * Get display name for UI (e.g., "Anthropic", "OpenAI", "Google Gemini")
     */
    public String getProviderDisplayName() {
        if (providerType == null) {
            return "Anthropic";
        }
        switch (providerType.toLowerCase()) {
            case "openai":
                return "OpenAI";
            case "gemini":
                return "Google Gemini";
            case "anthropic":
            default:
                return "Anthropic";
        }
    }

    /**
     * Get provider type from display name
     */
    public static String getProviderTypeFromDisplayName(String displayName) {
        if (displayName == null) {
            return "anthropic";
        }
        switch (displayName) {
            case "OpenAI":
                return "openai";
            case "Google Gemini":
                return "gemini";
            case "Anthropic":
            default:
                return "anthropic";
        }
    }

    /**
     * Get default base URL for provider
     */
    public static String getDefaultBaseUrl(String providerType) {
        if (providerType == null) {
            return "https://api.anthropic.com";
        }
        switch (providerType.toLowerCase()) {
            case "openai":
                return "https://api.openai.com/v1";
            case "gemini":
                return "https://generativelanguage.googleapis.com/v1beta";
            case "anthropic":
            default:
                return "https://api.anthropic.com";
        }
    }

    /**
     * Get default model for provider
     */
    public static String getDefaultModel(String providerType) {
        if (providerType == null) {
            return "anthropic/claude-sonnet-4-5";
        }
        switch (providerType.toLowerCase()) {
            case "openai":
                return "openai/gpt-4o";
            case "gemini":
                return "gemini/gemini-2.5-flash";
            case "anthropic":
            default:
                return "anthropic/claude-sonnet-4-5";
        }
    }

    /**
     * Validate that required fields are set
     */
    public boolean isValid() {
        return providerType != null && !providerType.isEmpty() &&
               apiKey != null && !apiKey.isEmpty();
    }

    @Override
    public String toString() {
        return providerType + " (key: " + (apiKey.isEmpty() ? "not set" : "set") +
               ", model: " + (model.isEmpty() ? "default" : model) + ")";
    }
}
