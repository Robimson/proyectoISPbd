package com.soportenet.soportetecnico.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Lo que el Superusuario envia para cambiar el nombre, categoria, eslogan y color de marca. */
public class ActualizarConfiguracionRequest {

    @NotBlank(message = "El nombre del negocio no puede estar vacio")
    private String nombreNegocio;

    @NotBlank(message = "La categoria no puede estar vacia")
    private String categoria;

    @NotBlank(message = "El eslogan no puede estar vacio")
    private String eslogan;

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "El color debe ser un codigo hexadecimal valido (ej: #0d9488)")
    private String colorPrimario;

    public ActualizarConfiguracionRequest() {
    }

    public String getNombreNegocio() {
        return nombreNegocio;
    }

    public void setNombreNegocio(String nombreNegocio) {
        this.nombreNegocio = nombreNegocio;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public String getEslogan() {
        return eslogan;
    }

    public void setEslogan(String eslogan) {
        this.eslogan = eslogan;
    }

    public String getColorPrimario() {
        return colorPrimario;
    }

    public void setColorPrimario(String colorPrimario) {
        this.colorPrimario = colorPrimario;
    }
}
