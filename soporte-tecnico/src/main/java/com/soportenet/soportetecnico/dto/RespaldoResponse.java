package com.soportenet.soportetecnico.dto;

import com.soportenet.soportetecnico.entity.RespaldoBd;
import java.time.OffsetDateTime;

public class RespaldoResponse {
    private final Long idRespaldo;
    private final String tipo;
    private final OffsetDateTime fechaInicio;
    private final OffsetDateTime fechaFin;
    private final String estado;
    private final Long tamanoBytes;
    private final String mensajeError;
    private final Boolean generadoManualmente;

    public RespaldoResponse(Long idRespaldo, String tipo, OffsetDateTime fechaInicio, OffsetDateTime fechaFin,
                            String estado, Long tamanoBytes, String mensajeError, Boolean generadoManualmente) {
        this.idRespaldo = idRespaldo;
        this.tipo = tipo;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.estado = estado;
        this.tamanoBytes = tamanoBytes;
        this.mensajeError = mensajeError;
        this.generadoManualmente = generadoManualmente;
    }

    public static RespaldoResponse fromEntity(RespaldoBd r) {
        return new RespaldoResponse(
                r.getIdRespaldo(), r.getTipo().name(), r.getFechaInicio(), r.getFechaFin(),
                r.getEstado().name(), r.getTamanoBytes(), r.getMensajeError(), r.getGeneradoManualmente()
        );
    }

    public Long getIdRespaldo() { return idRespaldo; }
    public String getTipo() { return tipo; }
    public OffsetDateTime getFechaInicio() { return fechaInicio; }
    public OffsetDateTime getFechaFin() { return fechaFin; }
    public String getEstado() { return estado; }
    public Long getTamanoBytes() { return tamanoBytes; }
    public String getMensajeError() { return mensajeError; }
    public Boolean getGeneradoManualmente() { return generadoManualmente; }
}