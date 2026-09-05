package com.soportenet.soportetecnico.dto;

import java.time.Instant;

/**
 * Proyeccion de fn_listar_anuncios_activos() - el banner que ve cualquier
 * rol logueado. Las fechas van como Instant (no OffsetDateTime): a
 * diferencia de una entidad JPA completa, una proyeccion de interfaz sobre
 * native query no tiene el conversor de Hibernate para timestamptz - el
 * driver ya entrega Instant, y pedirle OffsetDateTime rompia la
 * serializacion (HttpMessageNotWritableException, se veia como un 403
 * generico del lado del cliente).
 */
public interface AnuncioProjection {
    Long getIdAnuncio();
    String getTitulo();
    String getMensaje();
    Instant getFechaCreacion();
    Instant getFechaExpiracion();
}
