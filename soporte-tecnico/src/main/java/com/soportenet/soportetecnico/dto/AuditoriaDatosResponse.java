package com.soportenet.soportetecnico.dto;

import com.soportenet.soportetecnico.entity.AuditoriaDatos;

import java.time.OffsetDateTime;

/** DTO de salida para la pantalla de auditoria de datos (Superusuario, cambios en tablas de operacion interna). */
public class AuditoriaDatosResponse {

    private final Long idAuditoria;
    private final String tablaAfectada;
    private final String operacion;
    private final String datosAnteriores;
    private final String datosNuevos;
    private final Long idUsuarioResponsable;
    private final OffsetDateTime fecha;

    public AuditoriaDatosResponse(Long idAuditoria, String tablaAfectada, String operacion, String datosAnteriores,
                                   String datosNuevos, Long idUsuarioResponsable, OffsetDateTime fecha) {
        this.idAuditoria = idAuditoria;
        this.tablaAfectada = tablaAfectada;
        this.operacion = operacion;
        this.datosAnteriores = datosAnteriores;
        this.datosNuevos = datosNuevos;
        this.idUsuarioResponsable = idUsuarioResponsable;
        this.fecha = fecha;
    }

    public static AuditoriaDatosResponse fromEntity(AuditoriaDatos a) {
        return new AuditoriaDatosResponse(
                a.getIdAuditoria(), a.getTablaAfectada(),
                a.getOperacion() != null ? a.getOperacion().name() : null,
                a.getDatosAnteriores(), a.getDatosNuevos(), a.getIdUsuarioResponsable(), a.getFecha()
        );
    }

    public Long getIdAuditoria() {
        return idAuditoria;
    }

    public String getTablaAfectada() {
        return tablaAfectada;
    }

    public String getOperacion() {
        return operacion;
    }

    public String getDatosAnteriores() {
        return datosAnteriores;
    }

    public String getDatosNuevos() {
        return datosNuevos;
    }

    public Long getIdUsuarioResponsable() {
        return idUsuarioResponsable;
    }

    public OffsetDateTime getFecha() {
        return fecha;
    }
}
