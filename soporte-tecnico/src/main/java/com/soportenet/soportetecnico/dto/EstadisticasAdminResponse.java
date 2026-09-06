package com.soportenet.soportetecnico.dto;

import java.util.List;

/** Datos de los graficos del "Resumen" del Administrador. */
public class EstadisticasAdminResponse {

    private final List<ConteoProjection> porEstado;
    private final List<ConteoProjection> porPrioridad;
    private final List<ConteoProjection> porCategoria;
    private final List<ConteoProjection> tasaAprobacionReportes;
    private final List<ConteoProjection> cargaTecnicos;

    public EstadisticasAdminResponse(List<ConteoProjection> porEstado, List<ConteoProjection> porPrioridad,
                                      List<ConteoProjection> porCategoria, List<ConteoProjection> tasaAprobacionReportes,
                                      List<ConteoProjection> cargaTecnicos) {
        this.porEstado = porEstado;
        this.porPrioridad = porPrioridad;
        this.porCategoria = porCategoria;
        this.tasaAprobacionReportes = tasaAprobacionReportes;
        this.cargaTecnicos = cargaTecnicos;
    }

    public List<ConteoProjection> getPorEstado() {
        return porEstado;
    }

    public List<ConteoProjection> getPorPrioridad() {
        return porPrioridad;
    }

    public List<ConteoProjection> getPorCategoria() {
        return porCategoria;
    }

    public List<ConteoProjection> getTasaAprobacionReportes() {
        return tasaAprobacionReportes;
    }

    public List<ConteoProjection> getCargaTecnicos() {
        return cargaTecnicos;
    }
}
