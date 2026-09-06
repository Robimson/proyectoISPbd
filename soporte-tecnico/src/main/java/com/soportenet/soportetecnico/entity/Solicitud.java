package com.soportenet.soportetecnico.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

/**
 * Ticket de soporte: entidad central del sistema.
 */
@Entity
@Table(name = "solicitud")
public class Solicitud {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_solicitud")
    private Long idSolicitud;

    @Column(name = "descripcion", nullable = false, columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "fecha_creacion", insertable = false, updatable = false)
    private OffsetDateTime fechaCreacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cliente", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_categoria")
    private Categoria categoria;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_prioridad")
    private Prioridad prioridad;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estado", nullable = false)
    private Estado estado;

    @Column(name = "fecha_limite_confirmacion")
    private OffsetDateTime fechaLimiteConfirmacion;

    // Direccion del SERVICIO para este ticket puntual, no el perfil del
    // cliente (un cliente puede tener mas de una propiedad). La llena
    // sp_crear_solicitud; nullable porque las solicitudes creadas antes de
    // este cambio no la tienen.
    @Column(name = "direccion", length = 255)
    private String direccion;

    @Column(name = "lat")
    private Double lat;

    @Column(name = "lng")
    private Double lng;

    // Bloqueo optimista: OJO, aqui NO se usa @Version de JPA a proposito.
    // El trigger fn_pre_update_solicitud() ya incrementa esta columna en la BD
    // y rechaza el UPDATE si NEW.version llega distinto de OLD.version.
    // Si usaramos @Version, Hibernate enviaria el UPDATE con version+1 ya
    // calculado en el SET, y el trigger lo interpretaria como un conflicto
    // falso (rechazaria TODAS las actualizaciones). Por eso este campo es de
    // solo lectura desde JPA: se consulta, pero las actualizaciones reales se
    // hacen con queries nativas que dejan la columna intacta en el SET,
    // dejando que el trigger sea la unica fuente de verdad del incremento.
    @Column(name = "version", nullable = false, insertable = false, updatable = false)
    private Integer version;

    public Solicitud() {
    }

    public Long getIdSolicitud() {
        return idSolicitud;
    }

    public void setIdSolicitud(Long idSolicitud) {
        this.idSolicitud = idSolicitud;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public OffsetDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(OffsetDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public void setCategoria(Categoria categoria) {
        this.categoria = categoria;
    }

    public Prioridad getPrioridad() {
        return prioridad;
    }

    public void setPrioridad(Prioridad prioridad) {
        this.prioridad = prioridad;
    }

    public Estado getEstado() {
        return estado;
    }

    public void setEstado(Estado estado) {
        this.estado = estado;
    }

    public OffsetDateTime getFechaLimiteConfirmacion() {
        return fechaLimiteConfirmacion;
    }

    public void setFechaLimiteConfirmacion(OffsetDateTime fechaLimiteConfirmacion) {
        this.fechaLimiteConfirmacion = fechaLimiteConfirmacion;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public Double getLat() {
        return lat;
    }

    public void setLat(Double lat) {
        this.lat = lat;
    }

    public Double getLng() {
        return lng;
    }

    public void setLng(Double lng) {
        this.lng = lng;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }
}
