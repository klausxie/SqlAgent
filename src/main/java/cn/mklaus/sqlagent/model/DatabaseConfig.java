package cn.mklaus.sqlagent.model;

import java.util.Objects;

/**
 * Database connection configuration model
 */
public class DatabaseConfig {
    private String id;
    private String name;
    private DatabaseType type;
    private String host;
    private int port;
    private String database;
    private String username;
    private String password;
    private boolean isDefault;

    public DatabaseConfig() {
        this.type = DatabaseType.MYSQL;
        this.port = 3306;
        this.isDefault = false;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public DatabaseType getType() {
        return type;
    }

    public void setType(DatabaseType type) {
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

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean aDefault) {
        isDefault = aDefault;
    }

    public String getJdbcUrl() {
        return String.format("jdbc:%s://%s:%d/%s",
            type.name().toLowerCase(),
            host,
            port,
            database
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DatabaseConfig that = (DatabaseConfig) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "DatabaseConfig{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", type=" + type +
                ", host='" + host + '\'' +
                ", port=" + port +
                ", database='" + database + '\'' +
                '}';
    }
}
