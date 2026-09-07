package com.soportenet.soportetecnico.dto;

import jakarta.validation.constraints.NotBlank;


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
