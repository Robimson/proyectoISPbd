package com.soportenet.soportetecnico.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

/**
 * Evento notificado a un usuario (seccion 8 del documento). Las
 * inserta cada procedimiento cuando corresponde (asignar, aprobar/rechazar
 * reporte, confirmar, reabrir); NotificacionEmailScheduler las recoge y las
 * manda por correo, marcando correoEnviado para no duplicar el envio.
 */
@Entity
@Table(name = "notificacion")
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_notificacion")
    private Long idNotificacion;

    @Column(name = "id_usuario_destino", nullable = false)
    private Long idUsuarioDestino;

    @Column(name = "tipo_evento", nullable = false, length = 100)
    private String tipoEvento;

    @Column(name = "mensaje", nullable = false, columnDefinition = "TEXT")
    private String mensaje;

    @Column(name = "fecha_envio", insertable = false, updatable = false)
    private OffsetDateTime fechaEnvio;

    @Column(name = "leido", nullable = false)
    private Boolean leido = false;

    @Column(name = "id_solicitud_relacionada")
    private Long idSolicitudRelacionada;

    @Column(name = "correo_enviado", nullable = false)
    private Boolean correoEnviado = false;

    public Notificacion() {
    }

    public Long getIdNotificacion() {
        return idNotificacion;
    }

    public void setIdNotificacion(Long idNotificacion) {
        this.idNotificacion = idNotificacion;
    }

    public Long getIdUsuarioDestino() {
        return idUsuarioDestino;
    }

    public void setIdUsuarioDestino(Long idUsuarioDestino) {
        this.idUsuarioDestino = idUsuarioDestino;
    }

    public String getTipoEvento() {
        return tipoEvento;
    }

    public void setTipoEvento(String tipoEvento) {
        this.tipoEvento = tipoEvento;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public OffsetDateTime getFechaEnvio() {
        return fechaEnvio;
    }

    public void setFechaEnvio(OffsetDateTime fechaEnvio) {
        this.fechaEnvio = fechaEnvio;
    }

    public Boolean getLeido() {
        return leido;
    }

    public void setLeido(Boolean leido) {
        this.leido = leido;
    }

    public Long getIdSolicitudRelacionada() {
        return idSolicitudRelacionada;
    }

    public void setIdSolicitudRelacionada(Long idSolicitudRelacionada) {
        this.idSolicitudRelacionada = idSolicitudRelacionada;
    }

    public Boolean getCorreoEnviado() {
        return correoEnviado;
    }

    public void setCorreoEnviado(Boolean correoEnviado) {
        this.correoEnviado = correoEnviado;
    }
}
