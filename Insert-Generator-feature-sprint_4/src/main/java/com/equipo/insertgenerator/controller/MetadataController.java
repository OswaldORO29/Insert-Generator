package com.equipo.insertgenerator.controller;

import com.equipo.insertgenerator.dto.ColumnInfo;
import com.equipo.insertgenerator.security.SecuritySanitizer;
import com.equipo.insertgenerator.service.DatabaseMetadataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/metadata")
@CrossOrigin(origins = "*")
public class MetadataController {
    @Autowired
    private DatabaseMetadataService metadataService;

    // URL: http://localhost:8080/api/metadata/tablas
    @GetMapping("/tablas")
    public List<String> getTablas() {
        return metadataService.obtenerTablas();
    }

    // URL: http://localhost:8080/api/metadata/estructura?tabla=alumnos
    @GetMapping("/estructura")
    public ResponseEntity<?> getEstructura(@RequestParam String tabla) {

        // 🔒 CAPA DE SEGURIDAD: Validar que el nombre de la tabla sea alfanumérico seguro
        if (!SecuritySanitizer.esNombreIdentificadorSeguro(tabla)) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("Solicitud Rechazada: El nombre de la tabla contiene caracteres no permitidos o sospechosos.");
        }

        List<ColumnInfo> estructura = metadataService.obtenerEstructuraTabla(tabla);

        if (estructura.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("La tabla especificada no existe.");
        }

        return ResponseEntity.ok(estructura);
    }
}
