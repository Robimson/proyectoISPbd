package com.soportenet.soportetecnico.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;

public class ConfiguracionRespaldoRequest {
    @NotNull
    private LocalTime horaRespaldoFull;
    @NotNull
    private String frecuencia; // "DIARIO" o "SEMANAL"
    private Short diaSemanaFull; // solo si frecuencia = SEMANAL
    private Boolean activo;

    public LocalTime getHoraRespaldoFull() { return horaRespaldoFull; }
    public void setHoraRespaldoFull(LocalTime horaRespaldoFull) { this.horaRespaldoFull = horaRespaldoFull; }
    public String getFrecuencia() { return frecuencia; }
    public void setFrecuencia(String frecuencia) { this.frecuencia = frecuencia; }
    public Short getDiaSemanaFull() { return diaSemanaFull; }
    public void setDiaSemanaFull(Short diaSemanaFull) { this.diaSemanaFull = diaSemanaFull; }
    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
}