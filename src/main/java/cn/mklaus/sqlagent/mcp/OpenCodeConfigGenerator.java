package cn.mklaus.sqlagent.mcp;

import cn.mklaus.sqlagent.config.DatabaseConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.intellij.openapi.diagnostic.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collections;

/**
 * Generates and updates OpenCode configuration with MCP server
 */
public class OpenCodeConfigGenerator {
    private static final Logger LOG = Logger.getInstance(OpenCodeConfigGenerator.class);
    private static final String OPENCODE_CONFIG_PATH = ".opencode/opencode.json";
    private static final String MCP_SERVER_NAME = "database-tools";

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    /**
     * Update OpenCode configuration to include MCP server
     * @return true if successful
     */
    public boolean updateConfig(DatabaseConfig dbConfig, String mcpServerJarPath) {
        try {
            Path configPath = getConfigPath();

            // Read existing config or create new
            JsonObject config;
            if (Files.exists(configPath)) {
                String content = Files.readString(configPath);
                config = JsonParser.parseString(content).getAsJsonObject();
            } else {
                config = new JsonObject();
                Files.createDirectories(configPath.getParent());
            }

            // Add or update MCP configuration (OpenCode uses 'mcp' key)
            JsonObject mcpConfig;
            if (config.has("mcp")) {
                mcpConfig = config.getAsJsonObject("mcp");
            } else {
                mcpConfig = new JsonObject();
                config.add("mcp", mcpConfig);
            }

            // Add database-tools MCP server
            JsonObject dbToolsMcp = createMcpServerConfig(dbConfig, mcpServerJarPath);
            mcpConfig.add(MCP_SERVER_NAME, dbToolsMcp);

            // Write config
            String json = gson.toJson(config);
            Files.writeString(configPath, json, StandardOpenOption.CREATE,
                             StandardOpenOption.TRUNCATE_EXISTING);

            LOG.info("OpenCode configuration updated: " + configPath);
            return true;

        } catch (Exception e) {
            LOG.error("Failed to update OpenCode configuration", e);
            return false;
        }
    }

    /**
     * Create MCP server configuration JSON (OpenCode format)
     */
    private JsonObject createMcpServerConfig(DatabaseConfig dbConfig, String mcpServerJarPath) {
        JsonObject config = new JsonObject();

        // Type: local MCP server
        config.addProperty("type", "local");

        // Command: array format ["java", "-jar", "<jar-path>"]
        String javaHome = System.getProperty("java.home");
        String osName = System.getProperty("os.name").toLowerCase();
        String javaBin = osName.contains("win") ? "java.exe" : "java";
        String javaExec = Paths.get(javaHome, "bin", javaBin).toString();

        config.add("command", gson.toJsonTree(new String[]{javaExec, "-jar", mcpServerJarPath}));

        // Enabled
        config.addProperty("enabled", true);

        // Environment variables for database connection
        JsonObject env = new JsonObject();
        env.addProperty("DB_TYPE", dbConfig.getType());
        env.addProperty("DB_HOST", dbConfig.getHost());
        env.addProperty("DB_PORT", String.valueOf(dbConfig.getPort()));
        env.addProperty("DB_NAME", dbConfig.getDatabase());
        env.addProperty("DB_USER", dbConfig.getUsername());
        env.addProperty("DB_PASSWORD", dbConfig.getPassword());
        config.add("environment", env);

        return config;
    }

    /**
     * Get OpenCode config path
     */
    private Path getConfigPath() {
        String userHome = System.getProperty("user.home");
        return Paths.get(userHome, OPENCODE_CONFIG_PATH);
    }
}
