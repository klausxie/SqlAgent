package cn.mklaus.sqlagent.mcp;

import cn.mklaus.sqlagent.mcp.config.DatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main entry point for Database MCP Server
 *
 * This server provides MCP (Model Context Protocol) tools for database operations.
 * It communicates via STDIO using JSON-RPC 2.0 protocol.
 *
 * Environment variables required:
 * - DB_TYPE: mysql or postgresql
 * - DB_HOST: database host
 * - DB_PORT: database port
 * - DB_NAME: database name
 * - DB_USER: database username
 * - DB_PASSWORD: database password
 */
public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        try {
            logger.info("Starting SqlAgent Database MCP Server...");

            // Load configuration from environment variables
            DatabaseConfig config = DatabaseConfig.fromEnvironment();
            logger.info("Loaded configuration: {}@{}:{}/{}",
                config.getUsername(), config.getHost(), config.getPort(), config.getDatabase());

            // Create and start the MCP server
            DatabaseMcpServer server = new DatabaseMcpServer(config);
            server.start();

            logger.info("Database MCP Server stopped gracefully");

        } catch (Exception e) {
            System.err.println("Failed to start MCP server: " + e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }
}
