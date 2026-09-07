package com.soportenet.soportetecnico.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "respaldo_bd")
public class RespaldoBd {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_respaldo")
    private Long idRespaldo;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false)
    private TipoRespaldo tipo;

    @Column(name = "fecha_inicio", nullable = false)
    private OffsetDateTime fechaInicio;

    @Column(name = "fecha_fin")
    private OffsetDateTime fechaFin;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoRespaldo estado;

    @Column(name = "ruta_archivo", nullable = false, length = 500)
    private String rutaArchivo;

    @Column(name = "tamano_bytes")
    private Long tamanoBytes;

    @Column(name = "mensaje_error", columnDefinition = "TEXT")
    private String mensajeError;

    @Column(name = "generado_manualmente", nullable = false)
    private Boolean generadoManualmente = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_solicito")
    private Usuario usuarioSolicito;

    public Long getIdRespaldo() { return idRespaldo; }
    public void setIdRespaldo(Long idRespaldo) { this.idRespaldo = idRespaldo; }
    public TipoRespaldo getTipo() { return tipo; }
    public void setTipo(TipoRespaldo tipo) { this.tipo = tipo; }
    public OffsetDateTime getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(OffsetDateTime fechaInicio) { this.fechaInicio = fechaInicio; }
    public OffsetDateTime getFechaFin() { return fechaFin; }
    public void setFechaFin(OffsetDateTime fechaFin) { this.fechaFin = fechaFin; }
    public EstadoRespaldo getEstado() { return estado; }
    public void setEstado(EstadoRespaldo estado) { this.estado = estado; }
    public String getRutaArchivo() { return rutaArchivo; }
    public void setRutaArchivo(String rutaArchivo) { this.rutaArchivo = rutaArchivo; }
    public Long getTamanoBytes() { return tamanoBytes; }
    public void setTamanoBytes(Long tamanoBytes) { this.tamanoBytes = tamanoBytes; }
    public String getMensajeError() { return mensajeError; }
    public void setMensajeError(String mensajeError) { this.mensajeError = mensajeError; }
    public Boolean getGeneradoManualmente() { return generadoManualmente; }
    public void setGeneradoManualmente(Boolean generadoManualmente) { this.generadoManualmente = generadoManualmente; }
    public Usuario getUsuarioSolicito() { return usuarioSolicito; }
    public void setUsuarioSolicito(Usuario usuarioSolicito) { this.usuarioSolicito = usuarioSolicito; }
}