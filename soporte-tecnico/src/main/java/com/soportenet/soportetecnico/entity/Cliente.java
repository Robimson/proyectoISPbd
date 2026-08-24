package com.soportenet.soportetecnico.entity;

import com.soportenet.soportetecnico.enums.EstadoPago;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Extiende Usuario con datos propios del cliente.
 * La PK (id_usuario) es COMPARTIDA con Usuario -> se usa @MapsId, no herencia Java.
 * estado_pago (seccion 7.4) es solo informativo para el Administrador al
 * priorizar; se actualiza por carga manual de Excel/CSV, nunca bloquea ni
 * cierra solicitudes automaticamente.
 */
@Entity
@Table(name = "cliente")
public class Cliente {

    @Id
    @Column(name = "id_usuario")
    private Long idUsuario;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    @Column(name = "direccion", length = 255)
    private String direccion;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_pago", nullable = false, columnDefinition = "estado_pago_tipo")
    private EstadoPago estadoPago = EstadoPago.al_dia;

    public Cliente() {
    }

    public Cliente(Long idUsuario, Usuario usuario, String direccion) {
        this.idUsuario = idUsuario;
        this.usuario = usuario;
        this.direccion = direccion;
    }

    public Long getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Long idUsuario) {
        this.idUsuario = idUsuario;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public EstadoPago getEstadoPago() {
        return estadoPago;
    }

    public void setEstadoPago(EstadoPago estadoPago) {
        this.estadoPago = estadoPago;
    }
}
