package com.soportenet.soportetecnico.dto;

/**
 * Proyeccion de la fila unica que devuelve fn_resumen_tecnico(...). Spring
 * Data mapea las columnas de la consulta nativa a estos getters por nombre
 * (en_proceso -> getEnProceso(), etc.), sin necesidad de una entidad.
 */
public interface ResumenTecnicoProjection {
    Long getEnProceso();
    Long getPendienteAprobacion();
    Long getResueltasHoy();
    Long getTotalCerradas();
}
