package com.soportenet.soportetecnico.dto;

import java.time.Instant;


public interface AnuncioProjection {
    Long getIdAnuncio();
    String getTitulo();
    String getMensaje();
    Instant getFechaCreacion();
    Instant getFechaExpiracion();
}
