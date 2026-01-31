package cn.mklaus.sqlagent.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Index information model
 */
public class IndexInfo {
    private String indexName;
    private boolean unique;
    private String indexType;
    private List<String> columnNames;
    private int cardinality;
    private boolean isPrimary;
    private String indexComment;

    public IndexInfo() {
        this.unique = false;
        this.columnNames = new ArrayList<>();
        this.cardinality = 0;
        this.isPrimary = false;
        this.indexType = "BTREE";
    }

    public String getIndexName() {
        return indexName;
    }

    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }

    public boolean isUnique() {
        return unique;
    }

    public void setUnique(boolean unique) {
        this.unique = unique;
    }

    public String getIndexType() {
        return indexType;
    }

    public void setIndexType(String indexType) {
        this.indexType = indexType;
    }

    public List<String> getColumnNames() {
        return columnNames;
    }

    public void setColumnNames(List<String> columnNames) {
        this.columnNames = columnNames;
    }

    public void addColumnName(String columnName) {
        this.columnNames.add(columnName);
    }

    public int getCardinality() {
        return cardinality;
    }

    public void setCardinality(int cardinality) {
        this.cardinality = cardinality;
    }

    public boolean isPrimary() {
        return isPrimary;
    }

    public void setPrimary(boolean primary) {
        isPrimary = primary;
    }

    public String getIndexComment() {
        return indexComment;
    }

    public void setIndexComment(String indexComment) {
        this.indexComment = indexComment;
    }

    @Override
    public String toString() {
        return "IndexInfo{" +
                "indexName='" + indexName + '\'' +
                ", unique=" + unique +
                ", columnNames=" + columnNames +
                ", isPrimary=" + isPrimary +
                '}';
    }
}
