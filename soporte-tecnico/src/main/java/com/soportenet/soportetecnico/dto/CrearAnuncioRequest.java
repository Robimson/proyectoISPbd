package com.soportenet.soportetecnico.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.OffsetDateTime;

/**
 * Lo que el Administrador envia para crear un anuncio global (seccion 2.3).
 * fechaExpiracion es opcional - si no viene, el anuncio queda activo hasta
 * que un administrador lo desactive a mano.
 */
public class CrearAnuncioRequest {

    @NotBlank(message = "El titulo no puede estar vacio")
    private String titulo;

    @NotBlank(message = "El mensaje no puede estar vacio")
    private String mensaje;

    private OffsetDateTime fechaExpiracion;

    public CrearAnuncioRequest() {
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public OffsetDateTime getFechaExpiracion() {
        return fechaExpiracion;
    }

    public void setFechaExpiracion(OffsetDateTime fechaExpiracion) {
        this.fechaExpiracion = fechaExpiracion;
    }
}
