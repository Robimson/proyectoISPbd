package com.soportenet.soportetecnico.dto;

/** Proyeccion de la fila unica que devuelve fn_resumen_auditoria(). */
public interface ResumenAuditoriaProjection {
    Long getSesionesActivas();
    Long getCambiosHoy();
    Long getInserts();
    Long getUpdates();
    Long getEliminaciones();
    Long getAccionesSistema();
}
