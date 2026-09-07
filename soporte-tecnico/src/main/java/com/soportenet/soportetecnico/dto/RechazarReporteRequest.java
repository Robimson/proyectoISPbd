package com.soportenet.soportetecnico.dto;

import jakarta.validation.constraints.NotBlank;


public class RechazarReporteRequest {

    @NotBlank(message = "Debe indicar el motivo del rechazo")
    private String comentarioRechazo;

    public RechazarReporteRequest() {
    }

    public String getComentarioRechazo() {
        return comentarioRechazo;
    }

    public void setComentarioRechazo(String comentarioRechazo) {
        this.comentarioRechazo = comentarioRechazo;
    }
}
