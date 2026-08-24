package com.soportenet.soportetecnico.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "prioridad")
public class Prioridad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_prioridad")
    private Integer idPrioridad;

    @Column(name = "nombre_prioridad", nullable = false, unique = true, length = 20)
    private String nombrePrioridad;

    @Column(name = "orden", nullable = false, unique = true)
    private Short orden;

    public Prioridad() {
    }

    public Prioridad(Integer idPrioridad, String nombrePrioridad, Short orden) {
        this.idPrioridad = idPrioridad;
        this.nombrePrioridad = nombrePrioridad;
        this.orden = orden;
    }

    public Integer getIdPrioridad() {
        return idPrioridad;
    }

    public void setIdPrioridad(Integer idPrioridad) {
        this.idPrioridad = idPrioridad;
    }

    public String getNombrePrioridad() {
        return nombrePrioridad;
    }

    public void setNombrePrioridad(String nombrePrioridad) {
        this.nombrePrioridad = nombrePrioridad;
    }

    public Short getOrden() {
        return orden;
    }

    public void setOrden(Short orden) {
        this.orden = orden;
    }
}
