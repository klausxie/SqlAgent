package cn.mklaus.sqlagent.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Table metadata model
 */
public class TableMetadata {
    private String catalog;
    private String schema;
    private String tableName;
    private String tableType;
    private String remark;
    private long rowCount;
    private List<ColumnInfo> columns;
    private List<IndexInfo> indexes;
    private Map<String, Object> statistics;

    public TableMetadata() {
        this.columns = new ArrayList<>();
        this.indexes = new ArrayList<>();
        this.tableType = "TABLE";
        this.rowCount = -1; // -1 means unknown
    }

    public String getCatalog() {
        return catalog;
    }

    public void setCatalog(String catalog) {
        this.catalog = catalog;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getTableType() {
        return tableType;
    }

    public void setTableType(String tableType) {
        this.tableType = tableType;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public long getRowCount() {
        return rowCount;
    }

    public void setRowCount(long rowCount) {
        this.rowCount = rowCount;
    }

    public List<ColumnInfo> getColumns() {
        return columns;
    }

    public void setColumns(List<ColumnInfo> columns) {
        this.columns = columns;
    }

    public void addColumn(ColumnInfo column) {
        this.columns.add(column);
    }

    public List<IndexInfo> getIndexes() {
        return indexes;
    }

    public void setIndexes(List<IndexInfo> indexes) {
        this.indexes = indexes;
    }

    public void addIndex(IndexInfo index) {
        this.indexes.add(index);
    }

    public Map<String, Object> getStatistics() {
        return statistics;
    }

    public void setStatistics(Map<String, Object> statistics) {
        this.statistics = statistics;
    }

    @Override
    public String toString() {
        return "TableMetadata{" +
                "tableName='" + tableName + '\'' +
                ", rowCount=" + rowCount +
                ", columns=" + columns.size() +
                ", indexes=" + indexes.size() +
                '}';
    }
}
