package com.equipo.insertgenerator.dto;

import java.util.List;
import java.util.Map;

public class DataInsertionDTO {
    private Map<String, Object> summary;
    private List<Map<String, Object>> records;


    public Map<String, Object> getSummary() { return summary; }
    public void setSummary(Map<String, Object> summary) { this.summary = summary; }

    public List<Map<String, Object>> getRecords() { return records; }
    public void setRecords(List<Map<String, Object>> records) { this.records = records; }
}
