package com.soportenet.soportetecnico.dto;

import jakarta.validation.constraints.NotBlank;

public class LoginRequest {

    @NotBlank(message = "El correo no puede estar vacio")
    private String correo;

    @NotBlank(message = "La contrasena no puede estar vacia")
    private String contrasena;

    public LoginRequest() {
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getContrasena() {
        return contrasena;
    }

    public void setContrasena(String contrasena) {
        this.contrasena = contrasena;
    }
}
