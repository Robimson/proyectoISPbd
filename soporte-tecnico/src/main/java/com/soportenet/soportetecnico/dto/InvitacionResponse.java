package com.soportenet.soportetecnico.dto;

/**
 * Respuesta al invitar un usuario. El token va aqui de forma temporal:
 * todavia no hay envio de correo real (seccion 8), asi que por ahora se
 * devuelve directo en la respuesta para poder probar el flujo de
 * activacion. Cuando se conecte un proveedor de correo real, el token deja
 * de viajar en la respuesta HTTP y se envia solo por email.
 */
public class InvitacionResponse {

    private final String correo;
    private final String token;

    public InvitacionResponse(String correo, String token) {
        this.correo = correo;
        this.token = token;
    }

    public String getCorreo() {
        return correo;
    }

    public String getToken() {
        return token;
    }
}
