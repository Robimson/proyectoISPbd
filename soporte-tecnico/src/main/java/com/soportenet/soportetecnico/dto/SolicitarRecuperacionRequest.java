package com.soportenet.soportetecnico.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * "Olvide mi contrasena" (fuera de sesion). Solo pide el correo; la
 * respuesta del backend es siempre el mismo mensaje generico exista o no
 * ese correo, para no revelar que cuentas estan registradas.
 */
public class SolicitarRecuperacionRequest {

    @NotBlank(message = "El correo no puede estar vacio")
    private String correo;

    public SolicitarRecuperacionRequest() {
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }
}
