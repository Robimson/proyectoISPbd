package com.soportenet.soportetecnico.dto;

import com.soportenet.soportetecnico.entity.Usuario;

/**
 * DTO de salida para Usuario: nunca incluye contrasena_hash, ni siquiera el
 * hash. Evita serializar la entidad JPA directamente, igual que
 * SolicitudResponse y ReporteResponse.
 */
public class UsuarioResponse {

    private final Long idUsuario;
    private final String nombreUsuario;
    private final String correo;
    private final String rol;
    private final String estadoCuenta;

    public UsuarioResponse(Long idUsuario, String nombreUsuario, String correo, String rol, String estadoCuenta) {
        this.idUsuario = idUsuario;
        this.nombreUsuario = nombreUsuario;
        this.correo = correo;
        this.rol = rol;
        this.estadoCuenta = estadoCuenta;
    }

    public static UsuarioResponse fromEntity(Usuario u) {
        return new UsuarioResponse(
                u.getIdUsuario(),
                u.getNombreUsuario(),
                u.getCorreo(),
                u.getRol() != null ? u.getRol().name() : null,
                u.getEstadoCuenta() != null ? u.getEstadoCuenta().name() : null
        );
    }

    public Long getIdUsuario() {
        return idUsuario;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public String getCorreo() {
        return correo;
    }

    public String getRol() {
        return rol;
    }

    public String getEstadoCuenta() {
        return estadoCuenta;
    }
}
