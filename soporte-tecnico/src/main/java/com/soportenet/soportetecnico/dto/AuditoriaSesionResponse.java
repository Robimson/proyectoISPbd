package com.soportenet.soportetecnico.dto;

import com.soportenet.soportetecnico.entity.AuditoriaSesion;

import java.time.OffsetDateTime;

/** DTO de salida para la pantalla de auditoria de sesiones (Superusuario, seccion 11). */
public class AuditoriaSesionResponse {

    private final Long idSesion;
    private final Long idUsuario;
    private final OffsetDateTime fechaEntrada;
    private final OffsetDateTime ultimaActividad;
    private final OffsetDateTime fechaSalida;
    private final String ipOrigen;

    public AuditoriaSesionResponse(Long idSesion, Long idUsuario, OffsetDateTime fechaEntrada,
                                    OffsetDateTime ultimaActividad, OffsetDateTime fechaSalida, String ipOrigen) {
        this.idSesion = idSesion;
        this.idUsuario = idUsuario;
        this.fechaEntrada = fechaEntrada;
        this.ultimaActividad = ultimaActividad;
        this.fechaSalida = fechaSalida;
        this.ipOrigen = ipOrigen;
    }

    public static AuditoriaSesionResponse fromEntity(AuditoriaSesion s) {
        return new AuditoriaSesionResponse(
                s.getIdSesion(), s.getIdUsuario(), s.getFechaEntrada(),
                s.getUltimaActividad(), s.getFechaSalida(), s.getIpOrigen()
        );
    }

    public Long getIdSesion() {
        return idSesion;
    }

    public Long getIdUsuario() {
        return idUsuario;
    }

    public OffsetDateTime getFechaEntrada() {
        return fechaEntrada;
    }

    public OffsetDateTime getUltimaActividad() {
        return ultimaActividad;
    }

    public OffsetDateTime getFechaSalida() {
        return fechaSalida;
    }

    public String getIpOrigen() {
        return ipOrigen;
    }
}
