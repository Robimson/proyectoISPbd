package com.soportenet.soportetecnico.entity;

import com.soportenet.soportetecnico.enums.NivelTecnico;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Extiende Usuario con datos del personal tecnico.
 * 'nivel' se usa para sugerir aptitud al asignar tickets (seccion 5.3 del documento).
 */
@Entity
@Table(name = "tecnico")
public class Tecnico {

    @Id
    @Column(name = "id_usuario")
    private Long idUsuario;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    @Column(name = "especialidad", length = 150)
    private String especialidad;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name = "nivel", nullable = false, columnDefinition = "nivel_tecnico_tipo")
    private NivelTecnico nivel = NivelTecnico.junior;

    @Column(name = "habilitado", nullable = false)
    private Boolean habilitado = true;

    public Tecnico() {
    }

    public Tecnico(Long idUsuario, Usuario usuario, String especialidad,
                    NivelTecnico nivel, Boolean habilitado) {
        this.idUsuario = idUsuario;
        this.usuario = usuario;
        this.especialidad = especialidad;
        this.nivel = nivel;
        this.habilitado = habilitado;
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

    public Boolean getHabilitado() {
        return habilitado;
    }

    public void setHabilitado(Boolean habilitado) {
        this.habilitado = habilitado;
    }
}
