package com.soportenet.soportetecnico.dto;

import jakarta.validation.constraints.NotBlank;


public class CrearSolicitudRequest {

    @NotBlank(message = "La descripcion no puede estar vacia")
    private String descripcion;

    private Integer idCategoria;

    @NotBlank(message = "La direccion no puede estar vacia")
    private String direccion;

    public CrearSolicitudRequest() {
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Integer getIdCategoria() {
        return idCategoria;
    }

    public void setIdCategoria(Integer idCategoria) {
        this.idCategoria = idCategoria;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }
}
