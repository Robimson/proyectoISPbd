package com.soportenet.soportetecnico.dto;

import jakarta.validation.constraints.NotBlank;

public class CrearGrupoRequest {

    @NotBlank(message = "El nombre del grupo no puede estar vacio")
    private String nombreGrupo;

    public CrearGrupoRequest() {
    }

    public String getNombreGrupo() {
        return nombreGrupo;
    }

    public void setNombreGrupo(String nombreGrupo) {
        this.nombreGrupo = nombreGrupo;
    }
}
