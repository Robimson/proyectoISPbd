package com.soportenet.soportetecnico.dto;

import java.util.List;

/** Datos de los graficos del "Resumen" del Cliente. */
public class EstadisticasClienteResponse {

    private final List<ConteoProjection> porEstado;
    private final List<ConteoProjection> porCategoria;

    public EstadisticasClienteResponse(List<ConteoProjection> porEstado, List<ConteoProjection> porCategoria) {
        this.porEstado = porEstado;
        this.porCategoria = porCategoria;
    }

    public List<ConteoProjection> getPorEstado() {
        return porEstado;
    }

    public List<ConteoProjection> getPorCategoria() {
        return porCategoria;
    }
}
