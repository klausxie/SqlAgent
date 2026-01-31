package cn.mklaus.sqlagent.model;

/**
 * Database type enumeration
 */
public enum DatabaseType {
    MYSQL("MySQL", "com.mysql.cj.jdbc.Driver"),
    POSTGRESQL("PostgreSQL", "org.postgresql.Driver");

    private final String displayName;
    private final String driverClassName;

    DatabaseType(String displayName, String driverClassName) {
        this.displayName = displayName;
        this.driverClassName = driverClassName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDriverClassName() {
        return driverClassName;
    }
}
