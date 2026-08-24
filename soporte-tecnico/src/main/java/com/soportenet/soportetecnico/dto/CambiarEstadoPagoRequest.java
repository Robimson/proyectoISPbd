package com.soportenet.soportetecnico.dto;

import com.soportenet.soportetecnico.enums.EstadoPago;
import jakarta.validation.constraints.NotNull;

public class CambiarEstadoPagoRequest {

    @NotNull(message = "estadoPago es obligatorio")
    private EstadoPago estadoPago;

    public CambiarEstadoPagoRequest() {
    }

    public EstadoPago getEstadoPago() {
        return estadoPago;
    }

    public void setEstadoPago(EstadoPago estadoPago) {
        this.estadoPago = estadoPago;
    }
}
