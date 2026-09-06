package com.soportenet.soportetecnico.dto;

import java.util.List;

/**
 * Datos de los graficos del "Resumen" del Superusuario. "Miembros por grupo
 * tecnico" NO va aca - ya existe GET /api/grupos-tecnicos con el conteo
 * incluido (GrupoTecnicoConteoProjection), asi que el frontend lo reusa
 * directo en vez de duplicarlo en este endpoint.
 */
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
