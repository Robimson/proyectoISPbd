package com.soportenet.soportetecnico.entity;

import com.soportenet.soportetecnico.enums.OperacionAuditoria;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

/**
 * Fila que dejo el trigger fn_auditar_cambio() al modificarse una tabla de
 * operacion interna (usuario, cliente, solicitud, asignacion_solicitud,
 * reporte_solicitud, grupo_tecnico, tecnico_grupo). Nunca se inserta desde
 * Java - esta entidad es solo de lectura para la pantalla de auditoria.
 */
@Entity
@Table(name = "auditoria_datos")
public class AuditoriaDatos {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_auditoria")
    private Long idAuditoria;

    @Column(name = "tabla_afectada", nullable = false, length = 100)
    private String tablaAfectada;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name = "operacion", nullable = false, columnDefinition = "operacion_auditoria_tipo")
    private OperacionAuditoria operacion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "datos_anteriores", columnDefinition = "jsonb")
    private String datosAnteriores;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "datos_nuevos", columnDefinition = "jsonb")
    private String datosNuevos;

    @Column(name = "id_usuario_responsable")
    private Long idUsuarioResponsable;

    @Column(name = "fecha", insertable = false, updatable = false)
    private OffsetDateTime fecha;

    public AuditoriaDatos() {
    }

    public Long getIdAuditoria() {
        return idAuditoria;
    }

    public void setIdAuditoria(Long idAuditoria) {
        this.idAuditoria = idAuditoria;
    }

    public String getTablaAfectada() {
        return tablaAfectada;
    }

    public void setTablaAfectada(String tablaAfectada) {
        this.tablaAfectada = tablaAfectada;
    }

    public OperacionAuditoria getOperacion() {
        return operacion;
    }

    public void setOperacion(OperacionAuditoria operacion) {
        this.operacion = operacion;
    }

    public String getDatosAnteriores() {
        return datosAnteriores;
    }

    public void setDatosAnteriores(String datosAnteriores) {
        this.datosAnteriores = datosAnteriores;
    }

    public String getDatosNuevos() {
        return datosNuevos;
    }

    public void setDatosNuevos(String datosNuevos) {
        this.datosNuevos = datosNuevos;
    }

    public Long getIdUsuarioResponsable() {
        return idUsuarioResponsable;
    }

    public void setIdUsuarioResponsable(Long idUsuarioResponsable) {
        this.idUsuarioResponsable = idUsuarioResponsable;
    }

    public OffsetDateTime getFecha() {
        return fecha;
    }

    public void setFecha(OffsetDateTime fecha) {
        this.fecha = fecha;
    }
}
