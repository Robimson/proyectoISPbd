package com.soportenet.soportetecnico.entity;

import com.soportenet.soportetecnico.enums.EstadoCuenta;
import com.soportenet.soportetecnico.enums.RolUsuario;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

/**
 * Tabla base para los cuatro roles (cliente, tecnico, administrador, superusuario).
 * Nunca se elimina; solo cambia estado_cuenta (ver Nota de diseno, seccion 2.4 del documento).
 */
@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Long idUsuario;

    @Column(name = "nombre_usuario", nullable = false, length = 150)
    private String nombreUsuario;

    @Column(name = "correo", nullable = false, unique = true, length = 255)
    private String correo;

    // NULL hasta que el usuario activa su cuenta via invitacion (ver TokenActivacion)
    @Column(name = "contrasena_hash", length = 255)
    private String contrasenaHash;

    // Mapea directamente al ENUM rol_usuario_tipo de PostgreSQL (Hibernate 6 named enum)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name = "rol", nullable = false, columnDefinition = "rol_usuario_tipo")
    private RolUsuario rol;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_cuenta", nullable = false, columnDefinition = "estado_cuenta_tipo")
    private EstadoCuenta estadoCuenta = EstadoCuenta.inactivo;

    @Column(name = "fecha_creacion", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime fechaCreacion;

    public Usuario() {
    }

    public Usuario(Long idUsuario, String nombreUsuario, String correo, String contrasenaHash,
                   RolUsuario rol, EstadoCuenta estadoCuenta, OffsetDateTime fechaCreacion) {
        this.idUsuario = idUsuario;
        this.nombreUsuario = nombreUsuario;
        this.correo = correo;
        this.contrasenaHash = contrasenaHash;
        this.rol = rol;
        this.estadoCuenta = estadoCuenta;
        this.fechaCreacion = fechaCreacion;
    }

    public Long getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Long idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getContrasenaHash() {
        return contrasenaHash;
    }

    public void setContrasenaHash(String contrasenaHash) {
        this.contrasenaHash = contrasenaHash;
    }

    public RolUsuario getRol() {
        return rol;
    }

    public void setRol(RolUsuario rol) {
        this.rol = rol;
    }

    public EstadoCuenta getEstadoCuenta() {
        return estadoCuenta;
    }

    public void setEstadoCuenta(EstadoCuenta estadoCuenta) {
        this.estadoCuenta = estadoCuenta;
    }

    public OffsetDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(OffsetDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }
}
