package com.equipo.insertgenerator.service;

import com.equipo.insertgenerator.dto.DataInsertionDTO;
import com.equipo.insertgenerator.security.SecuritySanitizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Servicio encargado de la persistencia dinámica masiva en la base de datos.
 * Toma los registros limpios provistos por el módulo de Faker/IA y construye
 * consultas preparadas en caliente, mitigando riesgos de inyección SQL.
 */
@Service
public class DataInsertionService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Ejecuta la inserción de un lote de registros dentro de una transacción controlada.
     * Si algunas filas fallan por restricciones de la BD, se reportan individualmente
     * sin abortar el progreso general (Estatus: completed_with_errors).
     */
    @Transactional
    public Map<String, Object> insertarLote(String nombreTabla, DataInsertionDTO solicitud) {
        Map<String, Object> respuesta = new HashMap<>();

        if (!SecuritySanitizer.esNombreIdentificadorSeguro(nombreTabla)) {
            respuesta.put("status", "failed");
            respuesta.put("insertedCount", 0);
            respuesta.put("message", "Nombre de tabla inválido o malicioso.");
            return respuesta;
        }

        List<Map<String, Object>> registros = solicitud.getRecords();
        if (registros == null || registros.isEmpty()) {
            respuesta.put("status", "empty");
            respuesta.put("insertedCount", 0);
            respuesta.put("message", "No se proporcionaron registros para insertar.");
            return respuesta;
        }

        int insertadosConExito = 0;
        List<Map<String, Object>> erroresDetallados = new ArrayList<>();

        for (int i = 0; i < registros.size(); i++) {
            Map<String, Object> registro = registros.get(i);

            try {
                ejecutarInsercionIndividual(nombreTabla, registro);
                insertadosConExito++;
            } catch (Exception e) {
                Map<String, Object> infoError = new HashMap<>();
                infoError.put("fila", i + 1);
                infoError.put("error", e.getMessage());
                erroresDetallados.add(infoError);
            }
        }

        // 3. Estructurar el reporte de salida del Contrato 3
        // Usamos el tamaño de la lista de registros que el backend acaba de generar e insertar
        long totalSolicitado = registros.size();
        respuesta.put("totalRequested", totalSolicitado);
        respuesta.put("insertedCount", insertadosConExito);

        if (insertadosConExito == totalSolicitado) {
            respuesta.put("status", "completed");
        } else if (insertadosConExito > 0) {
            respuesta.put("status", "completed_with_errors");
            respuesta.put("errors", erroresDetallados);
        } else {
            respuesta.put("status", "failed");
            respuesta.put("errors", erroresDetallados);
        }

        return respuesta;
    }

    /**
     * Construye y ejecuta un INSERT INTO dinámico utilizando consultas parametrizadas (?)
     */
    private void ejecutarInsercionIndividual(String tabla, Map<String, Object> registro) {
        StringBuilder sql = new StringBuilder("INSERT INTO " + tabla + " (");
        StringBuilder valoresComodines = new StringBuilder(") VALUES (");
        List<Object> valoresParametros = new ArrayList<>();

        // Construir dinámicamente las columnas y los comodines '?'
        int contador = 0;
        for (Map.Entry<String, Object> columna : registro.entrySet()) {
            // Sanitizar cada nombre de columna antes de meterlo al String SQL
            if (!SecuritySanitizer.esNombreIdentificadorSeguro(columna.getKey())) {
                throw new IllegalArgumentException("Nombre de columna sospechoso detectado: " + columna.getKey());
            }

            if (contador > 0) {
                sql.append(", ");
                valoresComodines.append(", ");
            }

            sql.append(columna.getKey());
            valoresComodines.append("?");
            valoresParametros.add(columna.getValue());
            contador++;
        }

        sql.append(valoresComodines).append(")");

        // Ejecución segura parametrizada con JdbcTemplate (Previene Inyección SQL)
        jdbcTemplate.update(sql.toString(), valoresParametros.toArray());
    }
}