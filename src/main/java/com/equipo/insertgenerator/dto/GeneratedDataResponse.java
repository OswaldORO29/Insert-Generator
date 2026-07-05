package com.equipo.insertgenerator.dto;

import java.util.List;
import java.util.Map;

/**
 * que se envía al Frontend para la vista previa, y que luego el
 * Frontend reenvía tal cual al endpoint de inserción.
 */
public class GeneratedDataResponse {

    private Summary summary;
    private List<Map<String, Object>> records;

    public GeneratedDataResponse() {
    }

    public GeneratedDataResponse(Summary summary, List<Map<String, Object>> records) {
        this.summary = summary;
        this.records = records;
    }

    public Summary getSummary() {
        return summary;
    }

    public void setSummary(Summary summary) {
        this.summary = summary;
    }

    public List<Map<String, Object>> getRecords() {
        return records;
    }

    public void setRecords(List<Map<String, Object>> records) {
        this.records = records;
    }

    /**
     * Sub-objeto con las métricas de generación (cuántos hizo Faker,
     * cuántos ajustó/generó la IA).
     */
    public static class Summary {
        private int totalRequested;
        private int fakerGenerated;
        private int aiGenerated;

        public Summary() {
        }

        public Summary(int totalRequested, int fakerGenerated, int aiGenerated) {
            this.totalRequested = totalRequested;
            this.fakerGenerated = fakerGenerated;
            this.aiGenerated = aiGenerated;
        }

        public int getTotalRequested() {
            return totalRequested;
        }

        public void setTotalRequested(int totalRequested) {
            this.totalRequested = totalRequested;
        }

        public int getFakerGenerated() {
            return fakerGenerated;
        }

        public void setFakerGenerated(int fakerGenerated) {
            this.fakerGenerated = fakerGenerated;
        }

        public int getAiGenerated() {
            return aiGenerated;
        }

        public void setAiGenerated(int aiGenerated) {
            this.aiGenerated = aiGenerated;
        }
    }
}