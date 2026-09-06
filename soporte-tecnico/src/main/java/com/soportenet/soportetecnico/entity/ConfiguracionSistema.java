package com.soportenet.soportetecnico.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

/**
 * Configuracion global de marca (nombre del negocio, logo, color) - fila
 * unica (id_configuracion siempre 1). Publica sin login porque hasta la
 * pantalla de login la necesita para mostrar el nombre/logo correctos.
 */
@Entity
@Table(name = "configuracion_sistema")
public class ConfiguracionSistema {

    @Id
    @Column(name = "id_configuracion")
    private Short idConfiguracion;

    @Column(name = "nombre_negocio", nullable = false, length = 150)
    private String nombreNegocio;

    @Column(name = "categoria", nullable = false, length = 150)
    private String categoria;

    @Column(name = "eslogan", nullable = false, length = 300)
    private String eslogan;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "color_primario", length = 7)
    private String colorPrimario;

    @Column(name = "fecha_modificacion", insertable = false, updatable = false)
    private OffsetDateTime fechaModificacion;

    public ConfiguracionSistema() {
    }

    public Short getIdConfiguracion() {
        return idConfiguracion;
    }

    public String getNombreNegocio() {
        return nombreNegocio;
    }

    public String getCategoria() {
        return categoria;
    }

    public String getEslogan() {
        return eslogan;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public String getColorPrimario() {
        return colorPrimario;
    }

    public OffsetDateTime getFechaModificacion() {
        return fechaModificacion;
    }
}
