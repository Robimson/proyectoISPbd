package com.soportenet.soportetecnico.dto;

/**
 * Lo que el Administrador envia al aprobar un reporte de solucion (caso de
 * uso 4.3.7 del documento). idAdministrador sale del JWT, no del body.
 * diasPlazoConfirmacion es opcional: si no se envia, el controlador usa 3
 * (el mismo default documentado en la seccion 3.1 y en sp_aprobar_reporte).
 */
public class AprobarReporteRequest {

    private Integer diasPlazoConfirmacion;

    public AprobarReporteRequest() {
    }

    public Integer getDiasPlazoConfirmacion() {
        return diasPlazoConfirmacion;
    }

    public void setDiasPlazoConfirmacion(Integer diasPlazoConfirmacion) {
        this.diasPlazoConfirmacion = diasPlazoConfirmacion;
    }
}
