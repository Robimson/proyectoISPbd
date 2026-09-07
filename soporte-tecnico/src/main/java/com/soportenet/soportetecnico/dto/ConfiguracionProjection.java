package com.soportenet.soportetecnico.dto;

import java.time.Instant;


public interface ConfiguracionProjection {
    String getNombreNegocio();
    String getCategoria();
    String getEslogan();
    String getLogoUrl();
    String getColorPrimario();
    Instant getFechaModificacion();
}
