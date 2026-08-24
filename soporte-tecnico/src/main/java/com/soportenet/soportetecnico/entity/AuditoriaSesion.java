package com.soportenet.soportetecnico.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

/**
 * Registro de entrada/salida de una sesion (seccion 11 del documento).
 * Solo lectura desde Java: lo escribe el flujo de login/logout del backend.
 */
@Entity
@Table(name = "auditoria_sesion")
public class AuditoriaSesion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_sesion")
    private Long idSesion;

    @Column(name = "id_usuario", nullable = false)
    private Long idUsuario;

    @Column(name = "fecha_entrada", insertable = false, updatable = false)
    private OffsetDateTime fechaEntrada;

    @Column(name = "ultima_actividad")
    private OffsetDateTime ultimaActividad;

    @Column(name = "fecha_salida")
    private OffsetDateTime fechaSalida;

    @JdbcTypeCode(SqlTypes.INET)
    @Column(name = "ip_origen")
    private String ipOrigen;

    public AuditoriaSesion() {
    }

    public Long getIdSesion() {
        return idSesion;
    }

    public void setIdSesion(Long idSesion) {
        this.idSesion = idSesion;
    }

    public Long getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Long idUsuario) {
        this.idUsuario = idUsuario;
    }

    public OffsetDateTime getFechaEntrada() {
        return fechaEntrada;
    }

    public void setFechaEntrada(OffsetDateTime fechaEntrada) {
        this.fechaEntrada = fechaEntrada;
    }

    public OffsetDateTime getUltimaActividad() {
        return ultimaActividad;
    }

    public void setUltimaActividad(OffsetDateTime ultimaActividad) {
        this.ultimaActividad = ultimaActividad;
    }

    public OffsetDateTime getFechaSalida() {
        return fechaSalida;
    }

    public void setFechaSalida(OffsetDateTime fechaSalida) {
        this.fechaSalida = fechaSalida;
    }

    public String getIpOrigen() {
        return ipOrigen;
    }

    public void setIpOrigen(String ipOrigen) {
        this.ipOrigen = ipOrigen;
    }
}
