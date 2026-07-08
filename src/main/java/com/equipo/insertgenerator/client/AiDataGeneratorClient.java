package com.equipo.insertgenerator.client;

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

        return "Eres un validador experto de datos sintéticos para bases de datos.\n" +
                "Te doy la estructura de una tabla y una lista de registros base.\n\n" +
                "REGLAS ESTRICTAS:\n" +
                "1. Usa EXCLUSIVAMENTE valores realistas y lógicos (ej. 'Ingeniería en Software' para carreras, 'María López' para nombres, 'maria@email.com' para correos).\n" +
                "2. Respeta estrictamente los tipos de datos y la longitud máxima indicada en la estructura.\n" +
                "3. Asegúrate de que los valores generados sean variados y únicos para evitar errores de duplicidad en la base de datos.\n" +
                "4. Si un valor ya es realista y cumple las reglas, déjalo intacto.\n" +
                "5. Responde ÚNICAMENTE con el arreglo JSON válido. Cero texto adicional, cero formato markdown.\n\n" +
                "Estructura de columnas:\n" + columnasJson + "\n\n" +
                "Registros:\n" + registrosJson;
    }
    private String limpiarRespuestaJson(String texto) {
        if (texto == null) return "[]";
        return texto.replaceAll("```json", "")
                .replaceAll("```", "")
                .trim();
    }
}