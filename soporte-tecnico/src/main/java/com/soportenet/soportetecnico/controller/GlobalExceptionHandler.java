package com.soportenet.soportetecnico.controller;

import java.util.Map;

import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;


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

   
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", ex.getMessage() != null ? ex.getMessage() : "Ocurrio un error interno."));
    }

    // Nombre de restriccion (constraint) -> mensaje amigable en español.
    // Los nombres son exactamente los que existen hoy en la base (ver
    // pg_constraint); si se agrega una restriccion nueva y no esta aca,
    // el metodo de abajo cae al mensaje generico por tipo (unique/fk/check).
    private static final Map<String, String> MENSAJES_POR_RESTRICCION = Map.ofEntries(
            // UNIQUE
            Map.entry("usuario_correo_key", "Ese correo ya está registrado."),
            Map.entry("grupo_tecnico_nombre_grupo_key", "Ya existe un grupo con ese nombre."),
            Map.entry("categoria_nombre_categoria_key", "Ya existe una categoría con ese nombre."),
            Map.entry("estado_nombre_estado_key", "Ya existe un estado con ese nombre."),
            Map.entry("prioridad_nombre_prioridad_key", "Ya existe una prioridad con ese nombre."),
            Map.entry("prioridad_orden_key", "Ya existe una prioridad con ese orden."),
            Map.entry("token_activacion_token_key", "Ese token ya fue generado, intenta de nuevo."),

            // FOREIGN KEY
            Map.entry("solicitud_id_categoria_fkey", "La categoría seleccionada no existe."),
            Map.entry("solicitud_id_cliente_fkey", "El cliente indicado no existe."),
            Map.entry("solicitud_id_estado_fkey", "El estado indicado no existe."),
            Map.entry("solicitud_id_prioridad_fkey", "La prioridad indicada no existe."),
            Map.entry("asignacion_solicitud_id_tecnico_fkey", "El técnico seleccionado no existe."),
            Map.entry("asignacion_solicitud_id_grupo_fkey", "El grupo técnico seleccionado no existe."),
            Map.entry("asignacion_solicitud_id_solicitud_fkey", "La solicitud indicada no existe."),
            Map.entry("tecnico_grupo_id_grupo_fkey", "El grupo técnico indicado no existe."),
            Map.entry("tecnico_grupo_id_usuario_fkey", "El técnico indicado no existe."),
            Map.entry("adjunto_id_solicitud_fkey", "La solicitud indicada no existe."),
            Map.entry("reporte_solicitud_id_solicitud_fkey", "La solicitud indicada no existe."),
            Map.entry("reporte_solicitud_id_tecnico_fkey", "El técnico indicado no existe."),
            Map.entry("anuncio_id_administrador_fkey", "El administrador indicado no existe."),
            Map.entry("respaldo_bd_id_usuario_solicito_fkey", "El usuario indicado no existe."),

            // CHECK
            Map.entry("adjunto_tamano_archivo_check", "El archivo supera el tamaño máximo permitido (10MB)."),
            Map.entry("chk_motivo_reasignacion", "Debes indicar un motivo para la reasignación."),
            Map.entry("chk_asignacion_destino", "Debes asignar a un técnico o a un grupo, no ambos ni ninguno."),
            Map.entry("configuracion_respaldo_dia_semana_full_check", "El día de la semana debe estar entre 0 y 6."),
            Map.entry("configuracion_respaldo_frecuencia_check", "La frecuencia debe ser DIARIO o SEMANAL.")
    );

    private String extraerMensajePostgres(Throwable ex) {
        Throwable actual = ex;
        while (actual.getCause() != null) {
            actual = actual.getCause();
        }

        // Si viene de una restriccion de Postgres (UNIQUE/FK/CHECK/NOT NULL)
        // se traduce a un mensaje amigable.
        if (actual instanceof PSQLException) {
            PSQLException psqlEx = (PSQLException) actual;
            ServerErrorMessage detalle = psqlEx.getServerErrorMessage();
            String restriccion = detalle != null ? detalle.getConstraint() : null;

            if (restriccion != null && MENSAJES_POR_RESTRICCION.containsKey(restriccion)) {
                return MENSAJES_POR_RESTRICCION.get(restriccion);
            }

            String sqlState = psqlEx.getSQLState();
            if ("23505".equals(sqlState)) {
                return "Ya existe un registro con esos datos.";
            }
            if ("23503".equals(sqlState)) {
                return "La operación hace referencia a un registro que no existe o ya fue eliminado.";
            }
            if ("23502".equals(sqlState)) {
                return "Falta un dato obligatorio.";
            }
            if ("23514".equals(sqlState)) {
                return "Los datos no cumplen con una regla de validación del sistema.";
            }

            if (detalle != null && detalle.getMessage() != null) {
                return detalle.getMessage();
            }
        }

        return actual.getMessage();
    }
}
