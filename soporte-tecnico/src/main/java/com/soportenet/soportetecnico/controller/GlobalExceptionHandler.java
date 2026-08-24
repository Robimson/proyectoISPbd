package com.soportenet.soportetecnico.controller;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Los procedimientos en PostgreSQL usan RAISE EXCEPTION para reglas de negocio
 * (ej: "El cliente no tiene una cuenta activa"). Sin este manejador, esos
 * errores llegarian al cliente HTTP como un 500 crudo con stacktrace.
 * Aqui los convertimos en respuestas 400 con el mensaje real de negocio.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleDbError(DataIntegrityViolationException ex) {
        String mensaje = extraerMensajePostgres(ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", mensaje));
    }

    @ExceptionHandler(org.springframework.dao.InvalidDataAccessResourceUsageException.class)
    public ResponseEntity<Map<String, String>> handleSqlError(Exception ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", extraerMensajePostgres(ex)));
    }

    /**
     * Red de seguridad: la mayoria de los RAISE EXCEPTION de los procedimientos
     * no llevan USING ERRCODE, asi que Postgres les asigna el codigo generico
     * P0001. Ese codigo no cae en DataIntegrityViolationException ni en
     * InvalidDataAccessResourceUsageException (los dos handlers de arriba),
     * asi que sin este catch-all esos errores de negocio llegaban como 500.
     * DataAccessException es la superclase de ambos, por eso Spring solo entra
     * aqui cuando ninguno de los handlers mas especificos aplico.
     */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, String>> handleDataAccessError(DataAccessException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", extraerMensajePostgres(ex)));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        String mensaje = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .orElse("Datos invalidos");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", mensaje));
    }

    private String extraerMensajePostgres(Throwable ex) {
        Throwable actual = ex;
        // El mensaje real del RAISE EXCEPTION suele estar en la causa raiz (PSQLException)
        while (actual.getCause() != null) {
            actual = actual.getCause();
        }
        return actual.getMessage();
    }
}
