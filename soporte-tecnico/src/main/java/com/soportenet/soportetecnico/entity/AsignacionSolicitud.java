package com.soportenet.soportetecnico.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

/**
 * Historial de asignaciones de una solicitud (append-only: nunca se
 * sobrescribe una fila anterior, ver comentario en el esquema SQL). Esta
 * entidad es de solo lectura desde Java - la fila nueva y el manejo de
 * "vigente" los hace sp_asignar_solicitud dentro de PostgreSQL, nunca un
 * INSERT/UPDATE desde aqui. Se usa para mostrar, en el detalle de una
 * solicitud, quien la tiene asignada ahora mismo.
 */
@Entity
@Table(name = "asignacion_solicitud")
public class AsignacionSolicitud {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_asignacion")
    private Long idAsignacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_solicitud", nullable = false)
    private Solicitud solicitud;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tecnico")
    private Tecnico tecnico;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_grupo")
    private GrupoTecnico grupo;

    @Column(name = "fecha_asignacion", insertable = false, updatable = false)
    private OffsetDateTime fechaAsignacion;

    @Column(name = "motivo_reasignacion", columnDefinition = "TEXT")
    private String motivoReasignacion;

    @Column(name = "es_reasignacion", nullable = false)
    private Boolean esReasignacion;

    @Column(name = "vigente", nullable = false)
    private Boolean vigente;

    public AsignacionSolicitud() {
    }

    public Long getIdAsignacion() {
        return idAsignacion;
    }

    public void setIdAsignacion(Long idAsignacion) {
        this.idAsignacion = idAsignacion;
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

    public GrupoTecnico getGrupo() {
        return grupo;
    }

    public void setGrupo(GrupoTecnico grupo) {
        this.grupo = grupo;
    }

    public OffsetDateTime getFechaAsignacion() {
        return fechaAsignacion;
    }

    public void setFechaAsignacion(OffsetDateTime fechaAsignacion) {
        this.fechaAsignacion = fechaAsignacion;
    }

    public String getMotivoReasignacion() {
        return motivoReasignacion;
    }

    public void setMotivoReasignacion(String motivoReasignacion) {
        this.motivoReasignacion = motivoReasignacion;
    }

    public Boolean getEsReasignacion() {
        return esReasignacion;
    }

    public void setEsReasignacion(Boolean esReasignacion) {
        this.esReasignacion = esReasignacion;
    }

    public Boolean getVigente() {
        return vigente;
    }

    public void setVigente(Boolean vigente) {
        this.vigente = vigente;
    }
}
