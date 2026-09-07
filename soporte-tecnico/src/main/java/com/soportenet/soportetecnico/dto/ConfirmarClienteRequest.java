package com.soportenet.soportetecnico.dto;

import jakarta.validation.constraints.NotNull;


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
