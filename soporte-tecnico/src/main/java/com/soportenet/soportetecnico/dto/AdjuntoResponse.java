package com.soportenet.soportetecnico.dto;

import com.soportenet.soportetecnico.entity.Adjunto;

import java.time.OffsetDateTime;

/** DTO de salida para Adjunto: evita serializar la entidad JPA directamente. */
public class AdjuntoResponse {

    private final Long idAdjunto;
    private final Long idSolicitud;
    private final Long idUsuarioSube;
    private final String nombreArchivo;
    private final String tipoArchivo;
    private final Long tamanoArchivo;
    private final OffsetDateTime fechaSubida;

    public AdjuntoResponse(Long idAdjunto, Long idSolicitud, Long idUsuarioSube, String nombreArchivo,
                            String tipoArchivo, Long tamanoArchivo, OffsetDateTime fechaSubida) {
        this.idAdjunto = idAdjunto;
        this.idSolicitud = idSolicitud;
        this.idUsuarioSube = idUsuarioSube;
        this.nombreArchivo = nombreArchivo;
        this.tipoArchivo = tipoArchivo;
        this.tamanoArchivo = tamanoArchivo;
        this.fechaSubida = fechaSubida;
    }

    public static AdjuntoResponse fromEntity(Adjunto a) {
        return new AdjuntoResponse(
                a.getIdAdjunto(),
                a.getSolicitud() != null ? a.getSolicitud().getIdSolicitud() : null,
                a.getIdUsuarioSube(),
                a.getNombreArchivo(),
                a.getTipoArchivo(),
                a.getTamanoArchivo(),
                a.getFechaSubida()
        );
    }

    public Long getIdAdjunto() {
        return idAdjunto;
    }

    public Long getIdSolicitud() {
        return idSolicitud;
    }

    public Long getIdUsuarioSube() {
        return idUsuarioSube;
    }

    public String getNombreArchivo() {
        return nombreArchivo;
    }

    public String getTipoArchivo() {
        return tipoArchivo;
    }

    public Long getTamanoArchivo() {
        return tamanoArchivo;
    }

    public OffsetDateTime getFechaSubida() {
        return fechaSubida;
    }
}
