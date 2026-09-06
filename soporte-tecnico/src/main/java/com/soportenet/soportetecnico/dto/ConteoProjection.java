package com.soportenet.soportetecnico.dto;

/**
 * Proyeccion generica "etiqueta + cantidad" - la usan todos los graficos del
 * dashboard (solicitudes por estado/prioridad/categoria, tecnicos por nivel,
 * usuarios por rol, carga de trabajo, aprobacion de reportes...). Todos son
 * el mismo shape (GROUP BY algo, COUNT(*)), asi que no hace falta una
 * proyeccion distinta por cada grafico.
 */
public interface ConteoProjection {
    String getEtiqueta();
    Long getValor();
}
