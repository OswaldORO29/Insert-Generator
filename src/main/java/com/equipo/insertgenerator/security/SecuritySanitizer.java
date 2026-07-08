package com.equipo.insertgenerator.security;

import java.util.regex.Pattern;

public class SecuritySanitizer {
    // Expresión regular que solo permite letras, números y guiones bajos (Alfánumerico seguro)
    private static final Pattern STRICT_SQL_PATTERN = Pattern.compile("^[a-zA-Z0-9_]+$");

    // Palabras clave sospechosas de SQL Injection
    private static final String[] SQL_BLACKLIST = {
            "DROP", "DELETE", "UPDATE", "INSERT", "SELECT", "ALTER", "TRUNCATE",
            "UNION", "GRANT", "REVOKE", "--", "/*", "*/", ";", "OR 1=1"
    };

    /**
     * Valida de forma estricta nombres de tablas o columnas.
     * ¡Solo permite letras, números y guiones bajos! Si lleva un espacio o un ';', se bloquea.
     */
    public static boolean esNombreIdentificadorSeguro(String identificador) {
        if (identificador == null || identificador.trim().isEmpty()) {
            return false;
        }
        return STRICT_SQL_PATTERN.matcher(identificador).matches();
    }

    /**
     * Revisa si el valor de un campo de texto contiene palabras clave peligrosas.
     */
    public static boolean contieneSQLMalicioso(String valor) {
        if (valor == null) return false;

        String valorMayusculas = valor.toUpperCase();
        for (String palabraPeligrosa : SQL_BLACKLIST) {
            if (valorMayusculas.contains(palabraPeligrosa)) {
                return true; // Contiene una palabra prohibida
            }
        }
        return false;
    }
}
