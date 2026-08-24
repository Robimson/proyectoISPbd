package com.soportenet.soportetecnico.enums;

/**
 * Refleja el tipo rol_usuario_tipo definido en PostgreSQL.
 * Los valores deben coincidir EXACTAMENTE (en minusculas) con los del ENUM en la BD.
 */
public enum RolUsuario {
    cliente,
    tecnico,
    administrador,
    superusuario
}
