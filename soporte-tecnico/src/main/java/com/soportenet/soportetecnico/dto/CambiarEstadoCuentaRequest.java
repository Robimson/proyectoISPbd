package com.soportenet.soportetecnico.dto;

import com.soportenet.soportetecnico.enums.EstadoCuenta;
import jakarta.validation.constraints.NotNull;

public class CambiarEstadoCuentaRequest {

    @NotNull(message = "estadoCuenta es obligatorio")
    private EstadoCuenta estadoCuenta;

    public CambiarEstadoCuentaRequest() {
    }

    public EstadoCuenta getEstadoCuenta() {
        return estadoCuenta;
    }

    public void setEstadoCuenta(EstadoCuenta estadoCuenta) {
        this.estadoCuenta = estadoCuenta;
    }
}
