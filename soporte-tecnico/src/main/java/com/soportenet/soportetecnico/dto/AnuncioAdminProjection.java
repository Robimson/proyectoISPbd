package com.soportenet.soportetecnico.dto;

import java.time.Instant;

/**
 * Proyeccion de fn_listar_anuncios_admin() - todos los anuncios (activos e
 * inactivos) para gestionarlos. Fechas como Instant, ver el comentario en
 * AnuncioProjection.
 */
public interface AnuncioAdminProjection {
    Long getIdAnuncio();
    String getTitulo();
    String getMensaje();
    Instant getFechaCreacion();
    Instant getFechaExpiracion();
    Boolean getEstaActivo();
}
