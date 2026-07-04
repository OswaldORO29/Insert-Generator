package com.equipo.insertgenerator.dto;

import java.util.Map;

public class ColumnInfo {
    private String name;
    private String type;
    private boolean isPrimaryKey;
    private boolean isForeignKey;
    private boolean isAutoIncrement;
    private boolean isNullable;
    private Integer maxLength;
    private Map<String, Object> restrictions; // maneja los CHECKs, ENUMs o info de FK

    // Constructor
    public ColumnInfo(String name, String type, boolean isPrimaryKey, boolean isForeignKey,
                      boolean isAutoIncrement, boolean isNullable, Integer maxLength, Map<String, Object> restrictions) {
        this.name = name;
        this.type = type;
        this.isPrimaryKey = isPrimaryKey;
        this.isForeignKey = isForeignKey;
        this.isAutoIncrement = isAutoIncrement;
        this.isNullable = isNullable;
        this.maxLength = maxLength;
        this.restrictions = restrictions;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public boolean getIsPrimaryKey() { return isPrimaryKey; }
    public void setIsPrimaryKey(boolean isPrimaryKey) { this.isPrimaryKey = isPrimaryKey; }

    public boolean getIsForeignKey() { return isForeignKey; }
    public void setIsForeignKey(boolean isForeignKey) { this.isForeignKey = isForeignKey; }

    public boolean getIsAutoIncrement() { return isAutoIncrement; }
    public void setIsAutoIncrement(boolean isAutoIncrement) { this.isAutoIncrement = isAutoIncrement; }

    public boolean getIsNullable() { return isNullable; }
    public void setIsNullable(boolean isNullable) { this.isNullable = isNullable; }

    public Integer getMaxLength() { return maxLength; }
    public void setMaxLength(Integer maxLength) { this.maxLength = maxLength; }

    public Map<String, Object> getRestrictions() { return restrictions; }
    public void setRestrictions(Map<String, Object> restrictions) { this.restrictions = restrictions; }
}
