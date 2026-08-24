package com.soportenet.soportetecnico.enums;

/** Refleja operacion_auditoria_tipo en PostgreSQL (que hizo el trigger fn_auditar_cambio()). */
public enum OperacionAuditoria {
    INSERT,
    UPDATE,
    DELETE
}
