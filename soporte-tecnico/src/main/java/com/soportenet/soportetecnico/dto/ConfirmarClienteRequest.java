package com.soportenet.soportetecnico.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Lo que el Cliente envia al confirmar (o rechazar) la solucion de su
 * ticket, una vez que esta "Resuelta - Pendiente Confirmacion del Cliente"
 * (seccion 3.1 del documento). Si problemaResuelto es false, el ticket se
 * reabre y vuelve a "En Proceso". idCliente no se recibe aqui: se toma del
 * usuario autenticado (JWT); sp_confirmar_cliente igual valida que sea el
 * dueno de la solicitud, como defensa adicional.
 */
public class ConfirmarClienteRequest {

    @NotNull(message = "problemaResuelto es obligatorio")
    private Boolean problemaResuelto;

    public ConfirmarClienteRequest() {
    }

    public Boolean getProblemaResuelto() {
        return problemaResuelto;
    }

    public void setProblemaResuelto(Boolean problemaResuelto) {
        this.problemaResuelto = problemaResuelto;
    }
}
