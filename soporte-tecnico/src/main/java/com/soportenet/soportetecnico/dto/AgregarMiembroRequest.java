package com.soportenet.soportetecnico.dto;

import jakarta.validation.constraints.NotNull;

public class AgregarMiembroRequest {

    @NotNull(message = "idTecnico es obligatorio")
    private Long idTecnico;

    public AgregarMiembroRequest() {
    }

    public Long getIdTecnico() {
        return idTecnico;
    }

    public void setIdTecnico(Long idTecnico) {
        this.idTecnico = idTecnico;
    }
}
