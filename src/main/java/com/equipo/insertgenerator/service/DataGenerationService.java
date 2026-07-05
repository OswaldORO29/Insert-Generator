package com.equipo.insertgenerator.service;

import com.equipo.insertgenerator.dto.ColumnInfo;
import com.equipo.insertgenerator.dto.GeneratedDataResponse;
import net.datafaker.Faker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Orquesta la generación de datos sintéticos:
 * 1. Usa Faker para generar valores rápidos por columna.
 * 2. Resuelve llaves foráneas consultando IDs reales existentes.
 * 3. Si hay restricciones complejas (CHECK), delega en AiDataGeneratorClient
 *    la revisión/corrección final de esos registros.
 */
@Service
public class DataGenerationService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AiDataGeneratorClient aiClient;

    private final Faker faker = new Faker(new Locale("es"));

    public GeneratedDataResponse generarDatos(List<ColumnInfo> columnas, int cantidad) {

        Map<String, List<Object>> fksValidas = cargarForeignKeysValidas(columnas);

        List<Map<String, Object>> registros = new ArrayList<>();
        for (int i = 0; i < cantidad; i++) {
            registros.add(generarRegistro(columnas, fksValidas));
        }

        int generadosPorIA = 0;
        boolean requiereRevisionIA = columnas.stream().anyMatch(this::tieneRestriccionCheck);

        if (requiereRevisionIA) {
            registros = aiClient.ajustarRegistrosSegunRestricciones(columnas, registros);
            generadosPorIA = registros.size();
        }

        int generadosPorFaker = cantidad - generadosPorIA;

        GeneratedDataResponse.Summary summary = new GeneratedDataResponse.Summary(
                cantidad, generadosPorFaker, generadosPorIA
        );

        return new GeneratedDataResponse(summary, registros);
    }

    private Map<String, Object> generarRegistro(List<ColumnInfo> columnas, Map<String, List<Object>> fksValidas) {
        Map<String, Object> registro = new LinkedHashMap<>();
        for (ColumnInfo col : columnas) {
            if (col.getIsAutoIncrement()) continue;
            registro.put(col.getName(), generarValorColumna(col, fksValidas));
        }
        return registro;
    }

    @SuppressWarnings("unchecked")
    private Object generarValorColumna(ColumnInfo col, Map<String, List<Object>> fksValidas) {

        if (col.getIsNullable() && faker.random().nextInt(10) == 0) {
            return null;
        }

        if (col.getIsForeignKey()) {
            List<Object> ids = fksValidas.get(col.getName());
            if (ids != null && !ids.isEmpty()) {
                return ids.get(faker.random().nextInt(ids.size()));
            }
            return null;
        }

        if (col.getRestrictions() != null
                && "ENUM_VALUES".equals(col.getRestrictions().get("type"))) {
            List<Object> valores = (List<Object>) col.getRestrictions().get("values");
            return valores.get(faker.random().nextInt(valores.size()));
        }

        String tipo = col.getType() != null ? col.getType().toUpperCase() : "";
        String nombreCol = col.getName().toLowerCase();

        switch (tipo) {
            case "VARCHAR":
            case "TEXT":
            case "CHAR":
                return generarTextoSegunNombre(nombreCol, col.getMaxLength());
            case "INT":
            case "INTEGER":
            case "BIGINT":
            case "SMALLINT":
                return generarEnteroSegunRestriccion(col);
            case "DECIMAL":
            case "FLOAT":
            case "DOUBLE":
                return faker.number().randomDouble(2, 1, 10000);
            case "BOOLEAN":
            case "BOOL":
            case "TINYINT":
                return faker.bool().bool();
            case "DATE":
                return faker.date().birthday().toString();
            case "DATETIME":
            case "TIMESTAMP":
                return new java.sql.Timestamp(
                        faker.date().past(365, java.util.concurrent.TimeUnit.DAYS).getTime()
                ).toString();
            default:
                return faker.lorem().word();
        }
    }

    private String generarTextoSegunNombre(String nombreCol, Integer maxLength) {
        String valor;
        if (nombreCol.contains("nombre") || nombreCol.contains("name")) {
            valor = faker.name().fullName();
        } else if (nombreCol.contains("email") || nombreCol.contains("correo")) {
            valor = faker.internet().emailAddress();
        } else if (nombreCol.contains("telefono") || nombreCol.contains("phone")) {
            valor = faker.phoneNumber().phoneNumber();
        } else if (nombreCol.contains("direccion") || nombreCol.contains("address")) {
            valor = faker.address().fullAddress();
        } else if (nombreCol.contains("empresa") || nombreCol.contains("company")) {
            valor = faker.company().name();
        } else if (nombreCol.contains("ciudad") || nombreCol.contains("city")) {
            valor = faker.address().city();
        } else if (nombreCol.contains("pais") || nombreCol.contains("country")) {
            valor = faker.address().country();
        } else if (nombreCol.contains("usuario") || nombreCol.contains("username")) {
            valor = faker.name().username();
        } else if (nombreCol.contains("password") || nombreCol.contains("contrasena")) {
            valor = faker.internet().password();
        } else if (nombreCol.contains("descripcion") || nombreCol.contains("description")) {
            valor = faker.lorem().sentence();
        } else {
            valor = faker.lorem().word();
        }
        if (maxLength != null && valor.length() > maxLength) {
            valor = valor.substring(0, maxLength);
        }
        return valor;
    }

    private int generarEnteroSegunRestriccion(ColumnInfo col) {
        if (col.getRestrictions() != null && "CHECK".equals(col.getRestrictions().get("type"))) {
            String expresion = String.valueOf(col.getRestrictions().get("expression"));
            Integer minimo = extraerMinimoDeCheck(expresion);
            if (minimo != null) {
                return faker.number().numberBetween(minimo, minimo + 80);
            }
        }
        return faker.number().numberBetween(1, 100);
    }

    private Integer extraerMinimoDeCheck(String expresion) {
        try {
            if (expresion.contains(">=")) {
                return Integer.parseInt(expresion.split(">=")[1].trim());
            }
            if (expresion.contains(">")) {
                return Integer.parseInt(expresion.split(">")[1].trim()) + 1;
            }
        } catch (Exception e) {
            // se ignora, se usa rango por defecto
        }
        return null;
    }

    private boolean tieneRestriccionCheck(ColumnInfo col) {
        return col.getRestrictions() != null
                && "CHECK".equals(col.getRestrictions().get("type"));
    }

    private Map<String, List<Object>> cargarForeignKeysValidas(List<ColumnInfo> columnas) {
        Map<String, List<Object>> resultado = new HashMap<>();
        for (ColumnInfo col : columnas) {
            if (!col.getIsForeignKey() || col.getRestrictions() == null) continue;

            Object refTablaObj = col.getRestrictions().get("referenceTable");
            Object refColObj = col.getRestrictions().get("referenceColumn");
            if (refTablaObj == null || refColObj == null) continue;

            String refTabla = String.valueOf(refTablaObj);
            String refCol = String.valueOf(refColObj);
            try {
                List<Object> ids = jdbcTemplate.queryForList(
                        "SELECT " + refCol + " FROM " + refTabla, Object.class);
                resultado.put(col.getName(), ids);
            } catch (Exception e) {
                resultado.put(col.getName(), Collections.emptyList());
            }
        }
        return resultado;
    }
}