package com.soportenet.soportetecnico.dto;

/** Proyeccion de cada fila que devuelve fn_buscar_usuarios(...) (autocompletar). */
public interface UsuarioBusquedaProjection {
    Long getIdUsuario();
    String getNombreUsuario();
    String getCorreo();
    String getRol();
}
