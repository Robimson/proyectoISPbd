package com.soportenet.soportetecnico.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Lo que el usuario invitado envia para activar su cuenta y definir su
 * contrasena por primera vez (caso de uso 4.1.1 del documento). El token
 * viene del correo de invitacion; la longitud minima de la contrasena
 * refleja la seccion 7.3 (requisitos minimos, validados antes de hashear).
 */
public class ActivarCuentaRequest {

    @NotBlank(message = "El token es obligatorio")
    private String token;

    @NotBlank(message = "La contrasena no puede estar vacia")
    @Size(min = 8, message = "La contrasena debe tener al menos 8 caracteres")
    private String contrasena;

    public ActivarCuentaRequest() {
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getContrasena() {
        return contrasena;
    }

    public void setContrasena(String contrasena) {
        this.contrasena = contrasena;
    }
}
