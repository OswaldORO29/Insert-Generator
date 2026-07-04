package com.equipo.insertgenerator.controller;

import com.equipo.insertgenerator.dto.ColumnInfo;
import com.equipo.insertgenerator.service.DatabaseMetadataService;
import org.springframework.beans.factory.annotation.Autowired;
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
    public List<ColumnInfo> getEstructura(@RequestParam String tabla) {
        return metadataService.obtenerEstructuraTabla(tabla);
    }
}
