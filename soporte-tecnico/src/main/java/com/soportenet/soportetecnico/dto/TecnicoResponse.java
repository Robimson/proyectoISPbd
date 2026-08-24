package com.soportenet.soportetecnico.dto;

import com.soportenet.soportetecnico.entity.Tecnico;

/** DTO de salida para Tecnico, usado en el desplegable de asignacion del Administrador. */
public class TecnicoResponse {

    private final Long idUsuario;
    private final String nombreUsuario;
    private final String especialidad;
    private final String nivel;
    private final Boolean habilitado;

    public TecnicoResponse(Long idUsuario, String nombreUsuario, String especialidad, String nivel, Boolean habilitado) {
        this.idUsuario = idUsuario;
        this.nombreUsuario = nombreUsuario;
        this.especialidad = especialidad;
        this.nivel = nivel;
        this.habilitado = habilitado;
    }

    public static TecnicoResponse fromEntity(Tecnico t) {
        return new TecnicoResponse(
                t.getIdUsuario(),
                t.getUsuario() != null ? t.getUsuario().getNombreUsuario() : null,
                t.getEspecialidad(),
                t.getNivel() != null ? t.getNivel().name() : null,
                t.getHabilitado()
        );
    }

    public Long getIdUsuario() {
        return idUsuario;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public String getEspecialidad() {
        return especialidad;
    }

    public String getNivel() {
        return nivel;
    }

    public Boolean getHabilitado() {
        return habilitado;
    }
}
