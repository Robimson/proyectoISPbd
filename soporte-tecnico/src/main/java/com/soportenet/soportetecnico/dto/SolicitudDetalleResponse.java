package com.soportenet.soportetecnico.dto;

import com.soportenet.soportetecnico.entity.AsignacionSolicitud;
import com.soportenet.soportetecnico.entity.Solicitud;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * DTO de salida para la pantalla "Ver detalles de solicitud" (GET
 * /api/solicitudes/{id}). Es mas rico que SolicitudResponse (el que usan las
 * tablas paginadas) a proposito: una vista de detalle es una sola fila, asi
 * que aca si vale la pena resolver cliente, asignacion vigente y reportes -
 * hacerlo tambien en las listas paginadas dispararia N+1 consultas por
 * pagina.
 */
public class SolicitudDetalleResponse {

    private final Long idSolicitud;
    private final String descripcion;
    private final String direccion;
    private final String categoria;
    private final OffsetDateTime fechaCreacion;
    private final String estado;
    private final String prioridad;
    private final OffsetDateTime fechaLimiteConfirmacion;
    private final Integer version;

    private final String clienteNombre;
    private final String clienteCorreo;
    private final String clienteEstadoPago;

    private final String tecnicoAsignadoNombre;
    private final String tecnicoAsignadoCorreo;
    private final String grupoAsignadoNombre;
    private final OffsetDateTime fechaAsignacion;
    private final Boolean esReasignacion;
    private final String motivoReasignacion;

    private final List<ReporteResponse> reportes;

    public SolicitudDetalleResponse(Long idSolicitud, String descripcion, String direccion, String categoria,
                                     OffsetDateTime fechaCreacion, String estado, String prioridad,
                                     OffsetDateTime fechaLimiteConfirmacion, Integer version,
                                     String clienteNombre, String clienteCorreo, String clienteEstadoPago,
                                     String tecnicoAsignadoNombre, String tecnicoAsignadoCorreo,
                                     String grupoAsignadoNombre, OffsetDateTime fechaAsignacion,
                                     Boolean esReasignacion, String motivoReasignacion,
                                     List<ReporteResponse> reportes) {
        this.idSolicitud = idSolicitud;
        this.descripcion = descripcion;
        this.direccion = direccion;
        this.categoria = categoria;
        this.fechaCreacion = fechaCreacion;
        this.estado = estado;
        this.prioridad = prioridad;
        this.fechaLimiteConfirmacion = fechaLimiteConfirmacion;
        this.version = version;
        this.clienteNombre = clienteNombre;
        this.clienteCorreo = clienteCorreo;
        this.clienteEstadoPago = clienteEstadoPago;
        this.tecnicoAsignadoNombre = tecnicoAsignadoNombre;
        this.tecnicoAsignadoCorreo = tecnicoAsignadoCorreo;
        this.grupoAsignadoNombre = grupoAsignadoNombre;
        this.fechaAsignacion = fechaAsignacion;
        this.esReasignacion = esReasignacion;
        this.motivoReasignacion = motivoReasignacion;
        this.reportes = reportes;
    }

    /**
     * Arma el DTO a partir de la entidad y, opcionalmente, la asignacion
     * vigente (null si la solicitud todavia no fue asignada) y sus reportes
     * de solucion (lista vacia si el tecnico todavia no envio ninguno).
     */
    public static SolicitudDetalleResponse construir(Solicitud s, AsignacionSolicitud asignacionVigente,
                                                       List<ReporteResponse> reportes) {
        boolean tieneTecnico = asignacionVigente != null && asignacionVigente.getTecnico() != null;
        boolean tieneGrupo = asignacionVigente != null && asignacionVigente.getGrupo() != null;

        return new SolicitudDetalleResponse(
                s.getIdSolicitud(),
                s.getDescripcion(),
                s.getDireccion(),
                s.getCategoria() != null ? s.getCategoria().getNombreCategoria() : null,
                s.getFechaCreacion(),
                s.getEstado() != null ? s.getEstado().getNombreEstado() : null,
                s.getPrioridad() != null ? s.getPrioridad().getNombrePrioridad() : null,
                s.getFechaLimiteConfirmacion(),
                s.getVersion(),
                (s.getCliente() != null && s.getCliente().getUsuario() != null)
                        ? s.getCliente().getUsuario().getNombreUsuario() : null,
                (s.getCliente() != null && s.getCliente().getUsuario() != null)
                        ? s.getCliente().getUsuario().getCorreo() : null,
                (s.getCliente() != null && s.getCliente().getEstadoPago() != null)
                        ? s.getCliente().getEstadoPago().name() : null,
                (tieneTecnico && asignacionVigente.getTecnico().getUsuario() != null)
                        ? asignacionVigente.getTecnico().getUsuario().getNombreUsuario() : null,
                (tieneTecnico && asignacionVigente.getTecnico().getUsuario() != null)
                        ? asignacionVigente.getTecnico().getUsuario().getCorreo() : null,
                tieneGrupo ? asignacionVigente.getGrupo().getNombreGrupo() : null,
                asignacionVigente != null ? asignacionVigente.getFechaAsignacion() : null,
                asignacionVigente != null ? asignacionVigente.getEsReasignacion() : null,
                asignacionVigente != null ? asignacionVigente.getMotivoReasignacion() : null,
                reportes
        );
    }

    public Long getIdSolicitud() {
        return idSolicitud;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getDireccion() {
        return direccion;
    }

    public String getCategoria() {
        return categoria;
    }

    public OffsetDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public String getEstado() {
        return estado;
    }

    public String getPrioridad() {
        return prioridad;
    }

    public OffsetDateTime getFechaLimiteConfirmacion() {
        return fechaLimiteConfirmacion;
    }

    public Integer getVersion() {
        return version;
    }

    public String getClienteNombre() {
        return clienteNombre;
    }

    public String getClienteCorreo() {
        return clienteCorreo;
    }

    public String getClienteEstadoPago() {
        return clienteEstadoPago;
    }

    public String getTecnicoAsignadoNombre() {
        return tecnicoAsignadoNombre;
    }

    public String getTecnicoAsignadoCorreo() {
        return tecnicoAsignadoCorreo;
    }

    public String getGrupoAsignadoNombre() {
        return grupoAsignadoNombre;
    }

    public OffsetDateTime getFechaAsignacion() {
        return fechaAsignacion;
    }

    public Boolean getEsReasignacion() {
        return esReasignacion;
    }

    public String getMotivoReasignacion() {
        return motivoReasignacion;
    }

    public List<ReporteResponse> getReportes() {
        return reportes;
    }
}
