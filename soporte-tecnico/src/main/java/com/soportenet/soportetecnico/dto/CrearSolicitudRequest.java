package com.soportenet.soportetecnico.dto;

import jakarta.validation.constraints.NotBlank;

public class CrearSolicitudRequest {

    @NotBlank(message = "La descripcion no puede estar vacia")
    private String descripcion;

    private Integer idCategoria;

    @NotBlank(message = "La direccion no puede estar vacia")
    private String direccion;

    // Coordenadas del punto marcado en el mapa (opcional - complementan direccion)
    private Double lat;
    private Double lng;

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

    public Double getLat() {
        return lat;
    }

    public void setLat(Double lat) {
        this.lat = lat;
    }

    public Double getLng() {
        return lng;
    }

    public void setLng(Double lng) {
        this.lng = lng;
    }
}