package com.soportenet.soportetecnico.dto;

import com.soportenet.soportetecnico.entity.Cliente;

/** DTO de salida para Cliente, usado en el listado del Administrador (seccion 7.4). */
public class ClienteResponse {

    private final Long idUsuario;
    private final String nombreUsuario;
    private final String correo;
    private final String direccion;
    private final String estadoPago;

    public ClienteResponse(Long idUsuario, String nombreUsuario, String correo, String direccion, String estadoPago) {
        this.idUsuario = idUsuario;
        this.nombreUsuario = nombreUsuario;
        this.correo = correo;
        this.direccion = direccion;
        this.estadoPago = estadoPago;
    }

    public static ClienteResponse fromEntity(Cliente c) {
        return new ClienteResponse(
                c.getIdUsuario(),
                c.getUsuario() != null ? c.getUsuario().getNombreUsuario() : null,
                c.getUsuario() != null ? c.getUsuario().getCorreo() : null,
                c.getDireccion(),
                c.getEstadoPago() != null ? c.getEstadoPago().name() : null
        );
    }

    public Long getIdUsuario() {
        return idUsuario;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public String getCorreo() {
        return correo;
    }

    public String getDireccion() {
        return direccion;
    }

    public String getEstadoPago() {
        return estadoPago;
    }
}
