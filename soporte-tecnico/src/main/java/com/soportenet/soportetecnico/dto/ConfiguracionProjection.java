package com.soportenet.soportetecnico.dto;

import java.time.Instant;

/**
 * Proyeccion de fn_obtener_configuracion_sistema() - publica, sin login.
 * fechaModificacion como Instant (no OffsetDateTime): una proyeccion de
 * interfaz sobre native query no tiene el conversor de Hibernate para
 * timestamptz - ver AnuncioProjection para el mismo caso ya resuelto antes.
 */
public interface ConfiguracionProjection {
    String getNombreNegocio();
    String getCategoria();
    String getEslogan();
    String getLogoUrl();
    String getColorPrimario();
    Instant getFechaModificacion();
}
