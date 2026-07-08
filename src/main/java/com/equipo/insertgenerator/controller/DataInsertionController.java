package com.equipo.insertgenerator.controller;

import com.equipo.insertgenerator.dto.ColumnInfo;
import com.equipo.insertgenerator.dto.DataInsertionDTO;
import com.equipo.insertgenerator.dto.GeneratedDataResponse;
import com.equipo.insertgenerator.security.SecuritySanitizer;
import com.equipo.insertgenerator.service.DataGenerationService;
import com.equipo.insertgenerator.service.DataInsertionService;
import com.equipo.insertgenerator.service.DatabaseMetadataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/insert")
@CrossOrigin(origins = "*")
public class DataInsertionController {
    @Autowired
    private DataInsertionService insertionService;

    @Autowired
    private DataGenerationService generationService;

    @Autowired
    private DatabaseMetadataService metadataService;

    // URL: POST http://localhost:8080/api/insert/ejecutar?tabla=alumnos
    @PostMapping("/ejecutar")
    public ResponseEntity<?> ejecutarInsercion(@RequestParam String tabla, @RequestBody DataInsertionDTO solicitudFrontend) {
        List<ColumnInfo> columnas = metadataService.obtenerEstructuraTabla(tabla);

        // 2. SOLUCIÓN AL ERROR: En vez de usar getTotalRequested(), asignamos una cantidad fija
        // o validamos si el frontend ya manda una estructura para medir su tamaño.
        int cantidadAGenerar = 5; // Por seguridad, generamos bloques de 5 registros para las pruebas locales

        if (solicitudFrontend.getRecords() != null && !solicitudFrontend.getRecords().isEmpty()) {
            cantidadAGenerar = solicitudFrontend.getRecords().size();
        }
        // Llamamos al servicio de tu compañero para que la IA/Faker rellene los datos en base a esa cantidad
        GeneratedDataResponse datosSinteticos = generationService.generarDatos(columnas, cantidadAGenerar);

        // 3. Pasar los registros generados automáticamente al objeto de inserción
        solicitudFrontend.setRecords(datosSinteticos.getRecords());

        // 4. Tu motor ejecuta la persistencia dinámica en MySQL y retorna el Contrato 3
        Map<String, Object> resultadoFinal = insertionService.insertarLote(tabla, solicitudFrontend);

        return ResponseEntity.ok(resultadoFinal);
    }
}
