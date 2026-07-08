package com.equipo.insertgenerator.service;

import com.equipo.insertgenerator.dto.ColumnInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Cliente que envuelve la comunicación con la API de Groq (modelos Llama).
 * Su única responsabilidad es: recibir columnas + registros generados
 * por Faker, y devolver esos registros corregidos/ajustados para que
 * cumplan restricciones que Faker no puede razonar (CHECK, coherencia
 * semántica entre columnas, etc).
 *
 * Si la IA falla por cualquier motivo (timeout, error de red, respuesta
 * inválida), se hace fallback silencioso a los registros originales
 * para no romper el flujo del usuario.
 */
@Service
public class AiDataGeneratorClient {

    private static final String MODEL = "llama-3.3-70b-versatile";
    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";

    @Autowired
    private RestTemplate restTemplate;

    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${groq.api.key}")
    private String apiKey;

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> ajustarRegistrosSegunRestricciones(
            List<ColumnInfo> columnas,
            List<Map<String, Object>> registros) {

        if (apiKey == null || apiKey.isBlank()) {
            return registros;
        }

        try {
            String prompt = construirPrompt(columnas, registros);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + apiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = Map.of(
                    "model", MODEL,
                    "messages", List.of(
                            Map.of("role", "user", "content", prompt)
                    )
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(GROQ_URL, request, Map.class);

            if (response.getBody() == null) {
                return registros;
            }

            List<Map<String, Object>> choices =
                    (List<Map<String, Object>>) response.getBody().get("choices");
            if (choices == null || choices.isEmpty()) {
                return registros;
            }

            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            String textoRespuesta = (String) message.get("content");

            String jsonLimpio = limpiarRespuestaJson(textoRespuesta);
            List<Map<String, Object>> registrosAjustados =
                    mapper.readValue(jsonLimpio, List.class);

            if (registrosAjustados.size() != registros.size()) {
                return registros;
            }

            return registrosAjustados;

        } catch (Exception e) {
            System.err.println("AiDataGeneratorClient: fallo al llamar a Groq, usando datos de Faker. Motivo: "
                    + e.getMessage());
            return registros;
        }
    }

    private String construirPrompt(List<ColumnInfo> columnas, List<Map<String, Object>> registros) throws Exception {
        String columnasJson = mapper.writeValueAsString(columnas);
        String registrosJson = mapper.writeValueAsString(registros);

        return "Eres un generador de datos sintéticos para pruebas de bases de datos.\n" +
                "Te doy la estructura de columnas de una tabla y una lista de registros ya generados.\n" +
                "Tu tarea:\n" +
                "1. Revisa cada registro contra las restricciones de las columnas (CHECK, ENUM_VALUES, NOT NULL).\n" +
                "2. Si un valor viola una restricción, corrígelo con un valor válido y coherente.\n" +
                "3. Si un valor ya es válido, déjalo exactamente igual.\n" +
                "4. No agregues ni quites columnas. No agregues ni quites registros.\n" +
                "5. Responde ÚNICAMENTE con un arreglo JSON válido de registros. " +
                "Sin texto adicional, sin explicaciones, sin backticks de markdown.\n\n" +
                "Estructura de columnas:\n" + columnasJson + "\n\n" +
                "Registros a revisar:\n" + registrosJson;
    }

    private String limpiarRespuestaJson(String texto) {
        if (texto == null) return "[]";
        return texto.replaceAll("```json", "")
                .replaceAll("```", "")
                .trim();
    }
}