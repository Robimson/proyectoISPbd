package com.soportenet.soportetecnico.dto;

import com.soportenet.soportetecnico.enums.NivelTecnico;
import jakarta.validation.constraints.NotNull;

/**
 * Lo que el Superusuario envia para editar especialidad/nivel de un tecnico
 * despues de invitarlo (sp_invitar_usuario los deja en NULL / 'junior').
 * especialidad es opcional (texto libre); nivel es obligatorio porque el
 * campo del formulario siempre trae un valor seleccionado.
 */
public class EditarPerfilTecnicoRequest {

    private String especialidad;

    @NotNull(message = "El nivel es obligatorio")
    private NivelTecnico nivel;

    public EditarPerfilTecnicoRequest() {
    }

    public String getEspecialidad() {
        return especialidad;
    }

    public void setEspecialidad(String especialidad) {
        this.especialidad = especialidad;
    }

    public NivelTecnico getNivel() {
        return nivel;
    }

    public void setNivel(NivelTecnico nivel) {
        this.nivel = nivel;
    }
}
