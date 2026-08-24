package com.soportenet.soportetecnico.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Lo que el Tecnico envia al reportar la solucion aplicada a una solicitud
 * (caso de uso 4.2.4 del documento). No puede cerrar el ticket por si mismo;
 * queda en Pendiente Aprobacion hasta que el Administrador lo revise.
 * idTecnico sale del JWT, no del body.
 */
public class EnviarReporteRequest {

    @NotBlank(message = "El detalle del reporte no puede estar vacio")
    private String detalleReporte;

    public EnviarReporteRequest() {
    }

    public String getDetalleReporte() {
        return detalleReporte;
    }

    public void setDetalleReporte(String detalleReporte) {
        this.detalleReporte = detalleReporte;
    }
}
