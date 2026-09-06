package com.soportenet.soportetecnico.dto;

import java.util.List;

/** Datos de los graficos del "Resumen" del Tecnico. */
public class EstadisticasTecnicoResponse {

    private final List<ConteoProjection> porPrioridad;
    private final List<ConteoProjection> misReportes;

    public EstadisticasTecnicoResponse(List<ConteoProjection> porPrioridad, List<ConteoProjection> misReportes) {
        this.porPrioridad = porPrioridad;
        this.misReportes = misReportes;
    }

    public List<ConteoProjection> getPorPrioridad() {
        return porPrioridad;
    }

    public List<ConteoProjection> getMisReportes() {
        return misReportes;
    }
}
