package com.equipo.insertgenerator.controller;

import com.equipo.insertgenerator.dto.ColumnInfo;
import com.equipo.insertgenerator.dto.GeneratedDataResponse;
import com.equipo.insertgenerator.security.SecuritySanitizer;
import com.equipo.insertgenerator.service.DataGenerationService;
import com.equipo.insertgenerator.service.DatabaseMetadataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/generate")
@CrossOrigin(origins = "*")
public class DataGenerationController {

    @Autowired
    private DataGenerationService generationService;

    @Autowired
    private DatabaseMetadataService metadataService;

    private static final int MAX_REGISTROS_PERMITIDOS = 1000;

    // URL: GET http://localhost:8080/api/generate/datos?tabla=usuarios&cantidad=10
    @GetMapping("/datos")
    public ResponseEntity<?> generarDatos(
            @RequestParam String tabla,
            @RequestParam int cantidad) {

        if (!SecuritySanitizer.esNombreIdentificadorSeguro(tabla)) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("Solicitud Rechazada: El nombre de la tabla contiene caracteres no permitidos.");
        }

        if (cantidad <= 0 || cantidad > MAX_REGISTROS_PERMITIDOS) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("La cantidad debe ser entre 1 y " + MAX_REGISTROS_PERMITIDOS + " registros.");
        }

        List<ColumnInfo> columnas = metadataService.obtenerEstructuraTabla(tabla);

        if (columnas.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body("La tabla especificada no existe.");
        }

        GeneratedDataResponse resultado = generationService.generarDatos(columnas, cantidad);
        return ResponseEntity.ok(resultado);
    }
}