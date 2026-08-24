package com.soportenet.soportetecnico.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Lo que un usuario ya logeado envia para cambiar su propia contrasena
 * (distinto de ActivarCuentaRequest, que es para la primera vez). Exige la
 * contrasena actual para verificar que quien hace el cambio es el dueno de
 * la cuenta, no solo alguien con la sesion abierta en el navegador.
 */
public class CambiarContrasenaRequest {

    @NotBlank(message = "La contrasena actual es obligatoria")
    private String contrasenaActual;

    @NotBlank(message = "La contrasena nueva no puede estar vacia")
    @Size(min = 8, message = "La contrasena nueva debe tener al menos 8 caracteres")
    private String contrasenaNueva;

    public CambiarContrasenaRequest() {
    }

    public String getContrasenaActual() {
        return contrasenaActual;
    }

    public void setContrasenaActual(String contrasenaActual) {
        this.contrasenaActual = contrasenaActual;
    }

    public String getContrasenaNueva() {
        return contrasenaNueva;
    }

    public void setContrasenaNueva(String contrasenaNueva) {
        this.contrasenaNueva = contrasenaNueva;
    }
}
