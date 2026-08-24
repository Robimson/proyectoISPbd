package com.soportenet.soportetecnico.dto;

import com.soportenet.soportetecnico.enums.RolUsuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Lo que el Superusuario envia para invitar un nuevo usuario (caso de uso
 * 4.4.2 del documento). No incluye contrasena: el Superusuario nunca la crea
 * (seccion 2.4); el usuario invitado la define al activar su cuenta con el
 * token que devuelve este endpoint. idSuperusuario sale del JWT, no del body.
 */
public class InvitarUsuarioRequest {

    @NotBlank(message = "El nombre de usuario no puede estar vacio")
    private String nombreUsuario;

    @NotBlank(message = "El correo no puede estar vacio")
    @Email(message = "El correo no tiene un formato valido")
    private String correo;

    @NotNull(message = "El rol es obligatorio")
    private RolUsuario rol;

    private Integer diasValidezToken;

    public InvitarUsuarioRequest() {
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

    public RolUsuario getRol() {
        return rol;
    }

    public void setRol(RolUsuario rol) {
        this.rol = rol;
    }

    public Integer getDiasValidezToken() {
        return diasValidezToken;
    }

    public void setDiasValidezToken(Integer diasValidezToken) {
        this.diasValidezToken = diasValidezToken;
    }
}
