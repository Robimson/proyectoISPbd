package com.soportenet.soportetecnico.dto;

public class LoginResponse {

    private final String token;
    private final Long idUsuario;
    private final String rol;
    private final String estadoPago;
    private final Long idSesion;

    public LoginResponse(String token, Long idUsuario, String rol, String estadoPago, Long idSesion) {
        this.token = token;
        this.idUsuario = idUsuario;
        this.rol = rol;
        this.estadoPago = estadoPago;
        this.idSesion = idSesion;
    }

    public String getToken() {
        return token;
    }

    public Long getIdUsuario() {
        return idUsuario;
    }

    public String getRol() {
        return rol;
    }

    public String getEstadoPago() {
        return estadoPago;
    }

    public Long getIdSesion() {
        return idSesion;
    }
}
