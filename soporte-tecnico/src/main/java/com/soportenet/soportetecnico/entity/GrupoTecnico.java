package com.soportenet.soportetecnico.entity;

import jakarta.persistence.*;

/** Agrupacion de tecnicos administrada por el Superusuario (seccion 2.4). */
@Entity
@Table(name = "grupo_tecnico")
public class GrupoTecnico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_grupo")
    private Long idGrupo;

    @Column(name = "nombre_grupo", nullable = false, unique = true, length = 100)
    private String nombreGrupo;

    public GrupoTecnico() {
    }

    public GrupoTecnico(Long idGrupo, String nombreGrupo) {
        this.idGrupo = idGrupo;
        this.nombreGrupo = nombreGrupo;
    }

    public Long getIdGrupo() {
        return idGrupo;
    }

    public void setIdGrupo(Long idGrupo) {
        this.idGrupo = idGrupo;
    }

    public String getNombreGrupo() {
        return nombreGrupo;
    }

    public void setNombreGrupo(String nombreGrupo) {
        this.nombreGrupo = nombreGrupo;
    }
}
