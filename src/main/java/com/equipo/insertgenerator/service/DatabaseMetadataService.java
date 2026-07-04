package com.equipo.insertgenerator.service;

import com.equipo.insertgenerator.dto.ColumnInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DatabaseMetadataService {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    //obtiene todas las tablas disponibles
    public List<String> obtenerTablas() {
        String sql = "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'defaultdb'";
        return jdbcTemplate.queryForList(sql, String.class);
    }

    //obtiene los detalles de las columnas de una tabla específica
    public List<ColumnInfo> obtenerEstructuraTabla(String tableName) {
        String sql = "SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE, COLUMN_KEY " +
                "FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE TABLE_SCHEMA = 'defaultdb' AND TABLE_NAME = ?";

        return jdbcTemplate.query(sql, (rs, rowNum) -> new ColumnInfo(
                rs.getString("COLUMN_NAME"),
                rs.getString("DATA_TYPE"),
                rs.getString("IS_NULLABLE"),
                rs.getString("COLUMN_KEY")
        ), tableName);
    }
}
