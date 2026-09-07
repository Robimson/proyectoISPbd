package com.soportenet.soportetecnico.dto;

/** Proyeccion de cada fila que devuelve fn_tecnicos_disponibles_grupo(...). */
public interface TecnicoDisponibleProjection {
    Long getIdUsuario();
    String getNombreUsuario();
    String getCorreo();
    Long getCargaActual();
}
