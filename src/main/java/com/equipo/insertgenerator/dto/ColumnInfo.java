package com.equipo.insertgenerator.dto;

public class ColumnInfo {
    private String columnName;
    private String dataType;
    private String isNullable;
    private String columnKey;

    // Constructor
    public ColumnInfo(String columnName, String dataType, String isNullable, String columnKey) {
        this.columnName = columnName;
        this.dataType = dataType;
        this.isNullable = isNullable;
        this.columnKey = columnKey;
    }

    // Getters y Setters
    public String getColumnName() { return columnName; }
    public void setColumnName(String columnName) { this.columnName = columnName; }

    public String getDataType() { return dataType; }
    public void setDataType(String dataType) { this.dataType = dataType; }

    public String getIsNullable() { return isNullable; }
    public void setIsNullable(String isNullable) { this.isNullable = isNullable; }

    public String getColumnKey() { return columnKey; }
    public void setColumnKey(String columnKey) { this.columnKey = columnKey; }
}
