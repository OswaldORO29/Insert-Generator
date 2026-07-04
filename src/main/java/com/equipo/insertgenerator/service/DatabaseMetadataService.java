package com.equipo.insertgenerator.service;

import com.equipo.insertgenerator.dto.ColumnInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DatabaseMetadataService {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<String> obtenerTablas() {
        String sql = "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'defaultdb'";
        return jdbcTemplate.queryForList(sql, String.class);
    }

    public List<ColumnInfo> obtenerEstructuraTabla(String tableName) {
        // Traemos más metadatos
        String sql = "SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE, COLUMN_KEY, CHARACTER_MAXIMUM_LENGTH, EXTRA " +
                "FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE TABLE_SCHEMA = 'defaultdb' AND TABLE_NAME = ?";

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            String columnName = rs.getString("COLUMN_NAME");
            String dataType = rs.getString("DATA_TYPE").toUpperCase();

            // Evalua reglas booleanas mapeando las respuestas de MySQL
            boolean isNullable = rs.getString("IS_NULLABLE").equalsIgnoreCase("YES");
            boolean isPrimaryKey = rs.getString("COLUMN_KEY").equalsIgnoreCase("PRI");
            boolean isForeignKey = rs.getString("COLUMN_KEY").equalsIgnoreCase("MUL");
            boolean isAutoIncrement = rs.getString("EXTRA").toLowerCase().contains("auto_increment");

            // Obteniene la longitud máxima
            Integer maxLength = rs.getObject("CHARACTER_MAXIMUM_LENGTH") != null ? rs.getInt("CHARACTER_MAXIMUM_LENGTH") : null;

            // Construye el mapa de restricciones provisional
            Map<String, Object> restrictions = null;
            if (isForeignKey) {
                restrictions = new HashMap<>();
                restrictions.put("type", "FOREIGN_KEY");
                restrictions.put("note", "Falta mapear tabla relacional en el siguiente sprint");
            }

            return new ColumnInfo(
                    columnName,
                    dataType,
                    isPrimaryKey,
                    isForeignKey,
                    isAutoIncrement,
                    isNullable,
                    maxLength,
                    restrictions
            );
        }, tableName);
    }
}
