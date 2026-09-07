package com.soportenet.soportetecnico.dto;

import java.util.List;


public class EstadisticasSuperusuarioResponse {

    private final List<ConteoProjection> tecnicosPorNivel;
    private final List<ConteoProjection> usuariosPorRol;

    public EstadisticasSuperusuarioResponse(List<ConteoProjection> tecnicosPorNivel, List<ConteoProjection> usuariosPorRol) {
        this.tecnicosPorNivel = tecnicosPorNivel;
        this.usuariosPorRol = usuariosPorRol;
    }

    public List<ConteoProjection> getTecnicosPorNivel() {
        return tecnicosPorNivel;
    }

    public List<ConteoProjection> getUsuariosPorRol() {
        return usuariosPorRol;
    }
}
