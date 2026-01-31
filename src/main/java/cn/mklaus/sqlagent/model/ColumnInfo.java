package cn.mklaus.sqlagent.model;

/**
 * Column information model
 */
public class ColumnInfo {
    private String columnName;
    private int dataType;
    private String typeName;
    private int columnSize;
    private boolean nullable;
    private boolean autoIncrement;
    private String columnDefault;
    private String remark;
    private boolean isPrimaryKey;
    private boolean isForeignKey;
    private String referencedTable;

    public ColumnInfo() {
        this.nullable = true;
        this.autoIncrement = false;
        this.isPrimaryKey = false;
        this.isForeignKey = false;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public int getDataType() {
        return dataType;
    }

    public void setDataType(int dataType) {
        this.dataType = dataType;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public int getColumnSize() {
        return columnSize;
    }

    public void setColumnSize(int columnSize) {
        this.columnSize = columnSize;
    }

    public boolean isNullable() {
        return nullable;
    }

    public void setNullable(boolean nullable) {
        this.nullable = nullable;
    }

    public boolean isAutoIncrement() {
        return autoIncrement;
    }

    public void setAutoIncrement(boolean autoIncrement) {
        this.autoIncrement = autoIncrement;
    }

    public String getColumnDefault() {
        return columnDefault;
    }

    public void setColumnDefault(String columnDefault) {
        this.columnDefault = columnDefault;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public boolean isPrimaryKey() {
        return isPrimaryKey;
    }

    public void setPrimaryKey(boolean primaryKey) {
        isPrimaryKey = primaryKey;
    }

    public boolean isForeignKey() {
        return isForeignKey;
    }

    public void setForeignKey(boolean foreignKey) {
        isForeignKey = foreignKey;
    }

    public String getReferencedTable() {
        return referencedTable;
    }

    public void setReferencedTable(String referencedTable) {
        this.referencedTable = referencedTable;
    }

    @Override
    public String toString() {
        return "ColumnInfo{" +
                "columnName='" + columnName + '\'' +
                ", typeName='" + typeName + '\'' +
                ", nullable=" + nullable +
                ", isPrimaryKey=" + isPrimaryKey +
                '}';
    }
}
