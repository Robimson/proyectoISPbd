package com.soportenet.soportetecnico.dto;

import com.soportenet.soportetecnico.entity.Anuncio;

import java.time.OffsetDateTime;

/** DTO de salida para Anuncio, usado al crear uno nuevo. */
public class AnuncioResponse {

    private final Long idAnuncio;
    private final String titulo;
    private final String mensaje;
    private final OffsetDateTime fechaCreacion;
    private final OffsetDateTime fechaExpiracion;
    private final Boolean estaActivo;

    public AnuncioResponse(Long idAnuncio, String titulo, String mensaje, OffsetDateTime fechaCreacion,
                            OffsetDateTime fechaExpiracion, Boolean estaActivo) {
        this.idAnuncio = idAnuncio;
        this.titulo = titulo;
        this.mensaje = mensaje;
        this.fechaCreacion = fechaCreacion;
        this.fechaExpiracion = fechaExpiracion;
        this.estaActivo = estaActivo;
    }

    public static AnuncioResponse fromEntity(Anuncio a) {
        return new AnuncioResponse(
                a.getIdAnuncio(),
                a.getTitulo(),
                a.getMensaje(),
                a.getFechaCreacion(),
                a.getFechaExpiracion(),
                a.getEstaActivo()
        );
    }

    public Long getIdAnuncio() {
        return idAnuncio;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getMensaje() {
        return mensaje;
    }

    public OffsetDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public OffsetDateTime getFechaExpiracion() {
        return fechaExpiracion;
    }

    public Boolean getEstaActivo() {
        return estaActivo;
    }
}
