package com.soportenet.soportetecnico.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Lo que el Cliente envia al crear un ticket (caso de uso 4.1.3 del documento).
 * categoria es opcional: el cliente puede no saber elegirla, y a futuro la
 * sugiere el clasificador de IA (seccion 9.1). La prioridad NO se recibe aqui
 * a proposito: el ciclo de vida (seccion 3) dice que un ticket Pendiente aun
 * no tiene prioridad asignada, y es el Administrador quien la establece al
 * asignar (sp_asignar_solicitud). idCliente tampoco se recibe: se toma del
 * usuario autenticado (JWT), no del body, para que nadie pueda crear
 * solicitudes a nombre de otro cliente.
 */
public class CrearSolicitudRequest {

    @NotBlank(message = "La descripcion no puede estar vacia")
    private String descripcion;

    private Integer idCategoria;

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
}
