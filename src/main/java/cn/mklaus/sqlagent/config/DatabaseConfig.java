package cn.mklaus.sqlagent.config;

/**
 * Database configuration for MCP server
 */
public class DatabaseConfig {
    private String type = "mysql";           // mysql or postgresql
    private String host = "localhost";
    private int port = 3306;                 // 3306 for MySQL, 5432 for PostgreSQL
    private String database = "";
    private String username = "";
    private String password = "";

    public DatabaseConfig() {
    }

    public DatabaseConfig(String type, String host, int port, String database, String username, String password) {
        this.type = type;
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Validate that all required fields are set
     */
    public boolean isValid() {
        return type != null && !type.isEmpty() &&
               host != null && !host.isEmpty() &&
               port > 0 &&
               database != null && !database.isEmpty() &&
               username != null && !username.isEmpty() &&
               password != null && !password.isEmpty();
    }

    /**
     * Get default port for database type
     */
    public static int getDefaultPort(String type) {
        if (type != null && type.toLowerCase().contains("postgres")) {
            return 5432;
        }
        return 3306; // MySQL default
    }

    @Override
    public String toString() {
        return username + "@" + host + ":" + port + "/" + database;
    }
}
