package com.equipo.insertgenerator.service;

import com.equipo.insertgenerator.dto.DataInsertionDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DataInsertionService {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    public Map<String, Object> insertarRegistrosMasivos(String tableName, DataInsertionDTO insertionData) {
        long startTime = System.currentTimeMillis();

        List<Map<String, Object>> records = insertionData.getRecords();
        int insertedCount = 0;
        int failedCount = 0;
        List<Map<String, Object>> errors = new ArrayList<>();

        if (records == null || records.isEmpty()) {
            Map<String, Object> respuestaVacia = new HashMap<>();
            respuestaVacia.put("status", "completed");
            respuestaVacia.put("insertedCount", 0);
            respuestaVacia.put("failedCount", 0);
            respuestaVacia.put("executionTimeMs", 0);
            respuestaVacia.put("errors", errors);
            return respuestaVacia;
        }

        Map<String, Object> primerRegistro = records.get(0);
        List<String> columnas = new ArrayList<>(primerRegistro.keySet());

        StringBuilder sql = new StringBuilder("INSERT INTO ").append(tableName).append(" (");
        StringBuilder valoresComodines = new StringBuilder();

        for (int i = 0; i < columnas.size(); i++) {
            sql.append(columnas.get(i));
            valoresComodines.append("?");
            if (i < columnas.size() - 1) {
                sql.append(", ");
                valoresComodines.append(", ");
            }
        }
        sql.append(") VALUES (").append(valoresComodines).append(")");

        int numeroFila = 1;
        for (Map<String, Object> registro : records) {
            try {
                Object[] valores = new Object[columnas.size()];
                for (int i = 0; i < columnas.size(); i++) {
                    valores[i] = registro.get(columnas.get(i));
                }

                jdbcTemplate.update(sql.toString(), valores);
                insertedCount++;

            } catch (Exception e) {
                failedCount++;
                Map<String, Object> errorDetalle = new HashMap<>();
                errorDetalle.put("row", numeroFila);
                errorDetalle.put("reason", e.getMessage());
                errors.add(errorDetalle);
            }
            numeroFila++;
        }

        long endTime = System.currentTimeMillis();

        Map<String, Object> resultadoFinal = new HashMap<>();
        resultadoFinal.put("status", failedCount == 0 ? "completed" : "completed_with_errors");
        resultadoFinal.put("insertedCount", insertedCount);
        resultadoFinal.put("failedCount", failedCount);
        resultadoFinal.put("executionTimeMs", (endTime - startTime));
        resultadoFinal.put("errors", errors);

        return resultadoFinal;
    }
}