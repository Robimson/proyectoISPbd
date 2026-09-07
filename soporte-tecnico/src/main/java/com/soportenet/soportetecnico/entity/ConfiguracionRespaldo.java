package com.soportenet.soportetecnico.entity;

import jakarta.persistence.*;
import java.time.LocalTime;
import java.time.OffsetDateTime;

@Entity
@Table(name = "configuracion_respaldo")
public class ConfiguracionRespaldo {

    @Id
    @Column(name = "id_configuracion")
    private Short idConfiguracion = 1;

    @Column(name = "hora_respaldo_full", nullable = false)
    private LocalTime horaRespaldoFull;

    @Column(name = "frecuencia", nullable = false)
    private String frecuencia;

    @Column(name = "dia_semana_full")
    private Short diaSemanaFull;

    @Column(name = "ruta_base", nullable = false)
    private String rutaBase;

    @Column(name = "activo", nullable = false)
    private Boolean activo;

    @Column(name = "fecha_modificacion", insertable = false, updatable = false)
    private OffsetDateTime fechaModificacion;

    public Short getIdConfiguracion() { return idConfiguracion; }
    public LocalTime getHoraRespaldoFull() { return horaRespaldoFull; }
    public void setHoraRespaldoFull(LocalTime horaRespaldoFull) { this.horaRespaldoFull = horaRespaldoFull; }
    public String getFrecuencia() { return frecuencia; }
    public void setFrecuencia(String frecuencia) { this.frecuencia = frecuencia; }
    public Short getDiaSemanaFull() { return diaSemanaFull; }
    public void setDiaSemanaFull(Short diaSemanaFull) { this.diaSemanaFull = diaSemanaFull; }
    public String getRutaBase() { return rutaBase; }
    public void setRutaBase(String rutaBase) { this.rutaBase = rutaBase; }
    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
    public OffsetDateTime getFechaModificacion() { return fechaModificacion; }
}