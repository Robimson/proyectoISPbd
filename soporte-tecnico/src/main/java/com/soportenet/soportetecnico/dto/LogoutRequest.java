package com.soportenet.soportetecnico.dto;

/** idSesion que devolvio /api/auth/login, para cerrar el registro de auditoria_sesion. */
public class LogoutRequest {

    private Long idSesion;

    public LogoutRequest() {
    }

    public Long getIdSesion() {
        return idSesion;
    }

    public void setIdSesion(Long idSesion) {
        this.idSesion = idSesion;
    }
}
