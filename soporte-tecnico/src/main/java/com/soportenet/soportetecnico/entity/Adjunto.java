package com.soportenet.soportetecnico.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

/**
 * Referencia a un archivo de evidencia (seccion 6.1): el binario nunca vive
 * en Postgres, solo la ruta donde el backend lo guardo en disco. Lo puede
 * subir el cliente dueno de la solicitud o el tecnico asignado.
 */
@Entity
@Table(name = "adjunto")
public class Adjunto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_adjunto")
    private Long idAdjunto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_solicitud", nullable = false)
    private Solicitud solicitud;

    @Column(name = "id_usuario_sube", nullable = false)
    private Long idUsuarioSube;

    @Column(name = "nombre_archivo", nullable = false, length = 255)
    private String nombreArchivo;

    @Column(name = "tipo_archivo", nullable = false, length = 50)
    private String tipoArchivo;

    @Column(name = "tamano_archivo", nullable = false)
    private Long tamanoArchivo;

    @Column(name = "url_almacenamiento", nullable = false, length = 500)
    private String urlAlmacenamiento;

    @Column(name = "fecha_subida", insertable = false, updatable = false)
    private OffsetDateTime fechaSubida;

    public Adjunto() {
    }

    public Long getIdAdjunto() {
        return idAdjunto;
    }

    public void setIdAdjunto(Long idAdjunto) {
        this.idAdjunto = idAdjunto;
    }

    public Solicitud getSolicitud() {
        return solicitud;
    }

    public void setSolicitud(Solicitud solicitud) {
        this.solicitud = solicitud;
    }

    public Long getIdUsuarioSube() {
        return idUsuarioSube;
    }

    public void setIdUsuarioSube(Long idUsuarioSube) {
        this.idUsuarioSube = idUsuarioSube;
    }

    public String getNombreArchivo() {
        return nombreArchivo;
    }

    public void setNombreArchivo(String nombreArchivo) {
        this.nombreArchivo = nombreArchivo;
    }

    public String getTipoArchivo() {
        return tipoArchivo;
    }

    public void setTipoArchivo(String tipoArchivo) {
        this.tipoArchivo = tipoArchivo;
    }

    public Long getTamanoArchivo() {
        return tamanoArchivo;
    }

    public void setTamanoArchivo(Long tamanoArchivo) {
        this.tamanoArchivo = tamanoArchivo;
    }

    public String getUrlAlmacenamiento() {
        return urlAlmacenamiento;
    }

    public void setUrlAlmacenamiento(String urlAlmacenamiento) {
        this.urlAlmacenamiento = urlAlmacenamiento;
    }

    public OffsetDateTime getFechaSubida() {
        return fechaSubida;
    }

    public void setFechaSubida(OffsetDateTime fechaSubida) {
        this.fechaSubida = fechaSubida;
    }
}
