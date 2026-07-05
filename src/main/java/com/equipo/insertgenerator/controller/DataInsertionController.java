package com.equipo.insertgenerator.controller;

import com.equipo.insertgenerator.dto.DataInsertionDTO;
import com.equipo.insertgenerator.security.SecuritySanitizer;
import com.equipo.insertgenerator.service.DataInsertionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

public class DataInsertionController {
    @Autowired
    private DataInsertionService insertionService;

    // URL: POST http://localhost:8080/api/insert/ejecutar?tabla=alumnos
    @PostMapping("/ejecutar")
    public ResponseEntity<?> ejecutarInsercion(@RequestParam String tabla, @RequestBody DataInsertionDTO datosSinteticos) {
        if (!SecuritySanitizer.esNombreIdentificadorSeguro(tabla)) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("Operación Cancelada: Nombre de tabla inválido por políticas de seguridad.");
        }

        Map<String, Object> resultado = insertionService.insertarRegistrosMasivos(tabla, datosSinteticos);

        return ResponseEntity.ok(resultado);
    }
}
