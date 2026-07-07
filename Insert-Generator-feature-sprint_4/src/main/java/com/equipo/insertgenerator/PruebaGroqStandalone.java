package com.equipo.insertgenerator;

import com.equipo.insertgenerator.dto.ColumnInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.util.*;

/**
 * Prueba MANUAL, solo para verificar la integración con Groq,
 * sin depender de Spring Boot completo ni de la base de datos.
 * Se puede borrar antes de la entrega final.
 */
public class PruebaGroqStandalone {

    public static void main(String[] args) throws Exception {

        String apiKey = ""; // pega tu key real solo para esta prueba

        // 1. Simulamos columnas falsas (como si vinieran del Contrato 1)
        List<ColumnInfo> columnasFalsas = new ArrayList<>();

        Map<String, Object> restriccionCheck = new HashMap<>();
        restriccionCheck.put("type", "CHECK");
        restriccionCheck.put("expression", "edad >= 18");

        columnasFalsas.add(new ColumnInfo(
                "edad", "INT", false, false, false, true, null, restriccionCheck
        ));

        columnasFalsas.add(new ColumnInfo(
                "nombre_completo", "VARCHAR", false, false, false, false, 100, null
        ));

        // 2. Simulamos registros generados por Faker, con un error a propósito
        List<Map<String, Object>> registrosFalsos = new ArrayList<>();

        Map<String, Object> registro1 = new LinkedHashMap<>();
        registro1.put("nombre_completo", "Juan Pérez");
        registro1.put("edad", 15); // <-- viola el CHECK a propósito
        registrosFalsos.add(registro1);

        Map<String, Object> registro2 = new LinkedHashMap<>();
        registro2.put("nombre_completo", "María López");
        registro2.put("edad", 34); // <-- este ya es válido
        registrosFalsos.add(registro2);

        // 3. Armamos el prompt (mismo formato que en AiDataGeneratorClient real)
        ObjectMapper mapper = new ObjectMapper();
        String columnasJson = mapper.writeValueAsString(columnasFalsas);
        String registrosJson = mapper.writeValueAsString(registrosFalsos);

        String prompt = "Eres un generador de datos sintéticos para pruebas de bases de datos.\n" +
                "Te doy la estructura de columnas de una tabla y una lista de registros ya generados.\n" +
                "Corrige SOLO los valores que violen restricciones CHECK, manteniendo el resto igual.\n" +
                "Responde ÚNICAMENTE con un arreglo JSON válido de registros, sin texto adicional, sin backticks.\n\n" +
                "Estructura de columnas:\n" + columnasJson + "\n\n" +
                "Registros a revisar:\n" + registrosJson;

        // 4. Llamamos a Groq
        RestTemplate restTemplate = new RestTemplate();
        String url = "https://api.groq.com/openai/v1/chat/completions";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "model", "llama-3.3-70b-versatile",
                "messages", List.of(Map.of("role", "user", "content", prompt))
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        System.out.println("Enviando a Groq...\n");
        System.out.println("Registros ANTES de corregir:");
        System.out.println(registrosJson);
        System.out.println();

        ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        String textoRespuesta = (String) message.get("content");

        System.out.println("Registros DESPUÉS de que Groq los corrigiera:");
        System.out.println(textoRespuesta);
    }
}