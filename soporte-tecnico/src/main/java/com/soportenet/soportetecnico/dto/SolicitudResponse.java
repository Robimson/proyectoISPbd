package com.soportenet.soportetecnico.dto;

import com.soportenet.soportetecnico.entity.Solicitud;

import java.time.OffsetDateTime;

/**
 * DTO de salida: evita serializar la entidad JPA directamente (con sus
 * relaciones LAZY, que romperian a Jackson si no se manejan con cuidado).
 * Es inmutable a proposito: se arma completo con el constructor y no se
 * modifica despues.
 */
public class SolicitudResponse {

    private final Long idSolicitud;
    private final String descripcion;
    private final OffsetDateTime fechaCreacion;
    private final String estado;
    private final String prioridad;
    private final Integer version;
    private final String direccion;
    private final Double lat;
    private final Double lng;

    public SolicitudResponse(Long idSolicitud, String descripcion, OffsetDateTime fechaCreacion,
                             String estado, String prioridad, Integer version, String direccion,
                             Double lat, Double lng) {
        this.idSolicitud = idSolicitud;
        this.descripcion = descripcion;
        this.fechaCreacion = fechaCreacion;
        this.estado = estado;
        this.prioridad = prioridad;
        this.version = version;
        this.direccion = direccion;
        this.lat = lat;
        this.lng = lng;
    }

    public static SolicitudResponse fromEntity(Solicitud s) {
        return new SolicitudResponse(
                s.getIdSolicitud(),
                s.getDescripcion(),
                s.getFechaCreacion(),
                s.getEstado() != null ? s.getEstado().getNombreEstado() : null,
                s.getPrioridad() != null ? s.getPrioridad().getNombrePrioridad() : null,
                s.getVersion(),
                s.getDireccion(),
                s.getLat(),
                s.getLng()
        );
    }

    public Long getIdSolicitud() {
        return idSolicitud;
    }

    public String getDescripcion() {
        return descripcion;
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

    public Integer getVersion() {
        return version;
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
}