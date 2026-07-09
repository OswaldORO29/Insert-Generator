package com.equipo.insertgenerator.service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.equipo.insertgenerator.client.AiDataGeneratorClient;
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
 * la revisión/corrección final de esos registros.
 */
@Service
public class DataGenerationService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AiDataGeneratorClient aiClient;

    private final Faker faker = new Faker(new Locale("es"));
    private final java.text.SimpleDateFormat dateFormater = new java.text.SimpleDateFormat("yyyy-MM-dd");

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
                cantidad, generadosPorFaker, generadosPorIA);

        return new GeneratedDataResponse(summary, registros);
    }

    private Map<String, Object> generarRegistro(List<ColumnInfo> columnas, Map<String, List<Object>> fksValidas) {
        Map<String, Object> registro = new LinkedHashMap<>();
        for (ColumnInfo col : columnas) {
            if (col.getIsAutoIncrement())
                continue;
            registro.put(col.getName(), generarValorColumna(col, fksValidas));
        }
        return registro;
    }

    @SuppressWarnings("unchecked")
    private Object generarValorColumna(ColumnInfo col, Map<String, List<Object>> fksValidas) {

        // Si es una Llave Foránea (Como id_carrera)
        if (col.getIsForeignKey() || col.getName().toLowerCase().contains("id_carrera")) {
            List<Object> ids = fksValidas.get(col.getName());

            // Si la consulta a Aiven trajo IDs reales, tomamos uno al azar
            if (ids != null && !ids.isEmpty()) {
                return ids.get(faker.random().nextInt(ids.size()));
            }

            // FALLBACK INTELIGENTE: Si la tabla carreras está vacía en la nube,
            // inventamos un ID numérico lógico (1 al 4) para que MySQL no lo rechace
            return faker.number().numberBetween(1, 4);
        }

        if (col.getIsNullable() && faker.random().nextInt(20) == 0) {
            return null; // Reducimos la probabilidad de nulos para no ensuciar la prueba
        }

        if (col.getRestrictions() != null && "ENUM_VALUES".equals(col.getRestrictions().get("type"))) {
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
                // Si la columna es explícitamente un ID de carrera en la tabla carreras
                if (nombreCol.equals("id_carrera")) {
                    return faker.number().numberBetween(1, 100); // Genera IDs secuenciales/lógicos
                }
                return generarEnteroSegunRestriccion(col);
            case "DECIMAL":
            case "FLOAT":
            case "DOUBLE":
                return faker.number().randomDouble(2, 10, 1000);
            case "BOOLEAN":
            case "BOOL":
            case "TINYINT":
                return faker.bool().bool();
            case "DATE":
                return dateFormater.format(faker.date().birthday());
            case "DATETIME":
            case "TIMESTAMP":
                return new java.sql.Timestamp(faker.date().past(30, java.util.concurrent.TimeUnit.DAYS).getTime())
                        .toString();
            default:
                return faker.lorem().word();
        }
    }

    private String generarTextoSegunNombre(String nombreCol, Integer maxLength) {
        String valor;
        nombreCol = nombreCol.toLowerCase();

        // EXCLUSIVO FRONTEND/ALUMNOS: Separar nombre de pila de los apellidos
        if (nombreCol.equals("nombre") || nombreCol.equals("first_name")) {
            valor = faker.name().firstName(); // Solo el nombre (Ej: María)
        } else if (nombreCol.contains("apellido") || nombreCol.contains("last_name")) {
            valor = faker.name().lastName(); // Solo el apellido (Ej: López)
        }
        // EXCLUSIVO CARRERAS: Evitar que ponga nombres de personas en las carreras
        else if (nombreCol.contains("carrera") || nombreCol.contains("career")) {
            String[] carrerasUTR = {
                    "Ingeniería en Desarrollo de Software",
                    "TSU en Tecnologías de la Información",
                    "Ingeniería en Redes y Ciberseguridad",
                    "Licenciatura en Innovación de Negocios",
                    "Ingeniería en Mecatrónica",
                    "Licenciatura en Lenguas Extranjeras"
            };
            valor = carrerasUTR[faker.random().nextInt(carrerasUTR.length)];
        }
        // Validaciones estándar para el resto de los campos
        else if (nombreCol.contains("email") || nombreCol.contains("correo")) {
            valor = faker.internet().emailAddress();
        } else if (nombreCol.contains("telefono") || nombreCol.contains("phone")) {
            valor = faker.phoneNumber().phoneNumber();
        } else if (nombreCol.contains("direccion") || nombreCol.contains("address")) {
            valor = faker.address().fullAddress();
        } else if (nombreCol.contains("usuario") || nombreCol.contains("username")) {
            valor = faker.name().username();
        } else if (nombreCol.contains("codigo") || nombreCol.contains("code")) {
            // Códigos cortos tipo TI-7B, IDGS en vez de textos largos
            valor = "REG-" + faker.number().numberBetween(100, 999);
        } else {
            valor = faker.lorem().word();
        }

        // Respetar la longitud máxima de VARCHAR de la BD
        if (maxLength != null && valor.length() > maxLength) {
            valor = valor.substring(0, maxLength);
        }
        return valor;
    }

    private int generarEnteroSegunRestriccion(ColumnInfo col) {
        Integer minimo = null;

        if (col.getRestrictions() != null && "CHECK".equals(col.getRestrictions().get("type"))) {
            String expresion = String.valueOf(col.getRestrictions().get("expression"));
            minimo = extraerMinimoDeCheck(expresion);
        }

        // Salvaguarda: aunque la metadata no reporte el CHECK todavía,
        // sabemos por inspección directa en MySQL que edad >= 18.
        if (minimo == null && col.getName().toLowerCase().contains("edad")) {
            minimo = 18;
        }

        if (minimo != null) {
            return faker.number().numberBetween(minimo, minimo + 80);
        }
        return faker.number().numberBetween(1, 100);
    }

    private Integer extraerMinimoDeCheck(String expresion) {
        try {
            Matcher m = Pattern.compile("(>=|>)\\s*(\\d+)").matcher(expresion);
            if (m.find()) {
                int valor = Integer.parseInt(m.group(2));
                return m.group(1).equals(">") ? valor + 1 : valor;
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
            if (!col.getIsForeignKey() || col.getRestrictions() == null)
                continue;

            Object refTablaObj = col.getRestrictions().get("referenceTable");
            Object refColObj = col.getRestrictions().get("referenceColumn");
            if (refTablaObj == null || refColObj == null)
                continue;

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