package com.soportenet.soportetecnico.dto;


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
