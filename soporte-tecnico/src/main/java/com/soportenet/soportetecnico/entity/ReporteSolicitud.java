package com.soportenet.soportetecnico.entity;

import com.soportenet.soportetecnico.enums.EstadoAprobacion;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

/**
 * Reporte de solucion enviado por el tecnico. El tecnico no puede cerrar el
 * ticket por si mismo; requiere aprobacion del administrador (sp_aprobar_reporte
 * / sp_rechazar_reporte).
 */
@Entity
@Table(name = "reporte_solicitud")
public class ReporteSolicitud {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_reporte")
    private Long idReporte;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_solicitud", nullable = false)
    private Solicitud solicitud;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tecnico", nullable = false)
    private Tecnico tecnico;

    @Column(name = "detalle_reporte", nullable = false, columnDefinition = "TEXT")
    private String detalleReporte;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_aprobacion", nullable = false, columnDefinition = "estado_aprobacion_tipo")
    private EstadoAprobacion estadoAprobacion = EstadoAprobacion.pendiente;

    @Column(name = "fecha_envio", insertable = false, updatable = false)
    private OffsetDateTime fechaEnvio;

    @Column(name = "fecha_revision")
    private OffsetDateTime fechaRevision;

    @Column(name = "id_administrador_revisa")
    private Long idAdministradorRevisa;

    @Column(name = "comentario_rechazo", columnDefinition = "TEXT")
    private String comentarioRechazo;

    public ReporteSolicitud() {
    }

    public Long getIdReporte() {
        return idReporte;
    }

    public void setIdReporte(Long idReporte) {
        this.idReporte = idReporte;
    }

    public Solicitud getSolicitud() {
        return solicitud;
    }

    public void setSolicitud(Solicitud solicitud) {
        this.solicitud = solicitud;
    }

    public Tecnico getTecnico() {
        return tecnico;
    }

    public void setTecnico(Tecnico tecnico) {
        this.tecnico = tecnico;
    }

    public String getDetalleReporte() {
        return detalleReporte;
    }

    public void setDetalleReporte(String detalleReporte) {
        this.detalleReporte = detalleReporte;
    }

    public EstadoAprobacion getEstadoAprobacion() {
        return estadoAprobacion;
    }

    public void setEstadoAprobacion(EstadoAprobacion estadoAprobacion) {
        this.estadoAprobacion = estadoAprobacion;
    }

    public OffsetDateTime getFechaEnvio() {
        return fechaEnvio;
    }

    public void setFechaEnvio(OffsetDateTime fechaEnvio) {
        this.fechaEnvio = fechaEnvio;
    }

    public OffsetDateTime getFechaRevision() {
        return fechaRevision;
    }

    public void setFechaRevision(OffsetDateTime fechaRevision) {
        this.fechaRevision = fechaRevision;
    }

    public Long getIdAdministradorRevisa() {
        return idAdministradorRevisa;
    }

    public void setIdAdministradorRevisa(Long idAdministradorRevisa) {
        this.idAdministradorRevisa = idAdministradorRevisa;
    }

    public String getComentarioRechazo() {
        return comentarioRechazo;
    }

    public void setComentarioRechazo(String comentarioRechazo) {
        this.comentarioRechazo = comentarioRechazo;
    }
}
