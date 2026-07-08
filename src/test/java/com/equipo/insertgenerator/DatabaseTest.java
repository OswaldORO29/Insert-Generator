package com.equipo.insertgenerator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseTest implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("PROBANDO CONEXIÓN");
        try {
            String sql = "SELECT NOW()";
            String horaServidor = jdbcTemplate.queryForObject(sql, String.class);
            System.out.println("¡Conexión Exitosa en la nube!");
        } catch (Exception e) {
            System.out.println("Error: No se pudo conectar.");
            System.out.println("Detalle: " + e.getMessage());
        }
    }
}