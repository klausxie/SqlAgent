package cn.mklaus.sqlagent.model;

import com.intellij.psi.xml.XmlTag;

/**
 * MyBatis SQL statement information extracted from mapper XML files
 */
public class MyBatisSqlInfo {
    private String namespace;      // Mapper namespace (e.g., "com.example.mapper.UserMapper")
    private String statementId;    // SQL statement ID from 'id' attribute (e.g., "findUserById")
    private String sqlType;        // SQL type: SELECT, INSERT, UPDATE, DELETE
    private String originalSql;    // The extracted SQL statement
    private XmlTag xmlTag;         // Reference to the XML tag containing this SQL

    public MyBatisSqlInfo() {
    }

    public MyBatisSqlInfo(String namespace, String statementId, String sqlType, String originalSql, XmlTag xmlTag) {
        this.namespace = namespace;
        this.statementId = statementId;
        this.sqlType = sqlType;
        this.originalSql = originalSql;
        this.xmlTag = xmlTag;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getStatementId() {
        return statementId;
    }

    public void setStatementId(String statementId) {
        this.statementId = statementId;
    }

    public String getSqlType() {
        return sqlType;
    }

    public void setSqlType(String sqlType) {
        this.sqlType = sqlType;
    }

    public String getOriginalSql() {
        return originalSql;
    }

    public void setOriginalSql(String originalSql) {
        this.originalSql = originalSql;
    }

    public XmlTag getXmlTag() {
        return xmlTag;
    }

    public void setXmlTag(XmlTag xmlTag) {
        this.xmlTag = xmlTag;
    }

    /**
     * Get the full statement name including namespace
     * e.g., "UserMapper.findUserById"
     */
    public String getFullName() {
        String mapperName = namespace != null ? namespace : "";
        if (mapperName.contains(".")) {
            mapperName = mapperName.substring(mapperName.lastIndexOf('.') + 1);
        }
        return statementId != null ? mapperName + "." + statementId : mapperName;
    }

    @Override
    public String toString() {
        return "MyBatisSqlInfo{" +
                "fullName='" + getFullName() + '\'' +
                ", sqlType='" + sqlType + '\'' +
                ", hasSql=" + (originalSql != null && !originalSql.trim().isEmpty()) +
                '}';
    }
}
