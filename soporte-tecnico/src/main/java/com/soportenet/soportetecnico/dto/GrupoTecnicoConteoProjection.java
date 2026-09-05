package com.soportenet.soportetecnico.dto;

/**
 * Proyeccion de fn_grupos_tecnicos_con_conteo(): cada grupo con cuantos
 * tecnicos tiene. Usada en "Grupos técnicos existentes" - antes solo se veia
 * el nombre, sin saber si un grupo tenia 2 o 15 tecnicos.
 */
public interface GrupoTecnicoConteoProjection {
    Long getIdGrupo();
    String getNombreGrupo();
    Long getTotalTecnicos();
}
