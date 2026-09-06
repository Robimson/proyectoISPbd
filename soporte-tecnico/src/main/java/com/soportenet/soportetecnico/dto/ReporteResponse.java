package com.soportenet.soportetecnico.dto;

import com.soportenet.soportetecnico.entity.ReporteSolicitud;

import java.time.OffsetDateTime;

/**
 * DTO de salida para ReporteSolicitud: evita serializar la entidad JPA
 * directamente, igual que SolicitudResponse.
 */
public class ReporteResponse {

    private final Long idReporte;
    private final Long idSolicitud;
    private final Long idTecnico;
    private final String tecnicoNombre;
    private final String detalleReporte;
    private final String estadoAprobacion;
    private final OffsetDateTime fechaEnvio;
    private final OffsetDateTime fechaRevision;
    private final String comentarioRechazo;

    public ReporteResponse(Long idReporte, Long idSolicitud, Long idTecnico, String tecnicoNombre,
                            String detalleReporte, String estadoAprobacion, OffsetDateTime fechaEnvio,
                            OffsetDateTime fechaRevision, String comentarioRechazo) {
        this.idReporte = idReporte;
        this.idSolicitud = idSolicitud;
        this.idTecnico = idTecnico;
        this.tecnicoNombre = tecnicoNombre;
        this.detalleReporte = detalleReporte;
        this.estadoAprobacion = estadoAprobacion;
        this.fechaEnvio = fechaEnvio;
        this.fechaRevision = fechaRevision;
        this.comentarioRechazo = comentarioRechazo;
    }

    public static ReporteResponse fromEntity(ReporteSolicitud r) {
        return new ReporteResponse(
                r.getIdReporte(),
                r.getSolicitud() != null ? r.getSolicitud().getIdSolicitud() : null,
                r.getTecnico() != null ? r.getTecnico().getIdUsuario() : null,
                (r.getTecnico() != null && r.getTecnico().getUsuario() != null)
                        ? r.getTecnico().getUsuario().getNombreUsuario() : null,
                r.getDetalleReporte(),
                r.getEstadoAprobacion() != null ? r.getEstadoAprobacion().name() : null,
                r.getFechaEnvio(),
                r.getFechaRevision(),
                r.getComentarioRechazo()
        );
    }

    public Long getIdReporte() {
        return idReporte;
    }

    public Long getIdSolicitud() {
        return idSolicitud;
    }

    public Long getIdTecnico() {
        return idTecnico;
    }

    public String getTecnicoNombre() {
        return tecnicoNombre;
    }

    public String getDetalleReporte() {
        return detalleReporte;
    }

    public String getEstadoAprobacion() {
        return estadoAprobacion;
    }

    public OffsetDateTime getFechaEnvio() {
        return fechaEnvio;
    }

    public OffsetDateTime getFechaRevision() {
        return fechaRevision;
    }

    public String getComentarioRechazo() {
        return comentarioRechazo;
    }
}
