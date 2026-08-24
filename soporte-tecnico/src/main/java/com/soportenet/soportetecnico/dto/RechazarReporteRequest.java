package com.soportenet.soportetecnico.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Lo que el Administrador envia al rechazar un reporte de solucion (caso de
 * uso 4.3.7 del documento). idAdministrador sale del JWT, no del body. El
 * comentario es obligatorio: sp_rechazar_reporte lo exige para que el
 * tecnico sepa que corregir.
 */
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
