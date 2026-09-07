package com.soportenet.soportetecnico.dto;

import com.soportenet.soportetecnico.entity.AsignacionSolicitud;
import com.soportenet.soportetecnico.entity.Solicitud;

import java.time.OffsetDateTime;
import java.util.List;


public class SolicitudDetalleResponse {

    private final Long idSolicitud;
    private final String descripcion;
    private final String direccion;
    private final Double lat;
    private final Double lng;
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

    public SolicitudDetalleResponse(Long idSolicitud, String descripcion, String direccion, Double lat, Double lng,
                                     String categoria,
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
        this.lat = lat;
        this.lng = lng;
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

  
    public static SolicitudDetalleResponse construir(Solicitud s, AsignacionSolicitud asignacionVigente,
                                                       List<ReporteResponse> reportes) {
        boolean tieneTecnico = asignacionVigente != null && asignacionVigente.getTecnico() != null;
        boolean tieneGrupo = asignacionVigente != null && asignacionVigente.getGrupo() != null;

        return new SolicitudDetalleResponse(
                s.getIdSolicitud(),
                s.getDescripcion(),
                s.getDireccion(),
                s.getLat(),
                s.getLng(),
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

    public Double getLat() {
        return lat;
    }

    public Double getLng() {
        return lng;
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
