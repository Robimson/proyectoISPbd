package com.soportenet.soportetecnico.repository;

import com.soportenet.soportetecnico.entity.Adjunto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AdjuntoRepository extends JpaRepository<Adjunto, Long> {

    List<Adjunto> findBySolicitudIdSolicitudOrderByFechaSubidaAsc(Long idSolicitud);

    /**
     * Invoca sp_agregar_adjunto(...). Valida que la solicitud no este
     * Cerrada, que quien sube sea el cliente dueno o el tecnico asignado
     * (directo o por grupo), y que no se pase de 5 adjuntos por solicitud.
     * El archivo ya debe estar guardado en disco antes de llamar esto; aqui
     * solo se registra la referencia.
     */
    @Query(value = "SELECT sp_agregar_adjunto(:idSolicitud, :idUsuarioSube, :nombreArchivo, :tipoArchivo, :tamanoArchivo, :urlAlmacenamiento)",
           nativeQuery = true)
    Long agregarAdjunto(
            @Param("idSolicitud") Long idSolicitud,
            @Param("idUsuarioSube") Long idUsuarioSube,
            @Param("nombreArchivo") String nombreArchivo,
            @Param("tipoArchivo") String tipoArchivo,
            @Param("tamanoArchivo") Long tamanoArchivo,
            @Param("urlAlmacenamiento") String urlAlmacenamiento
    );

    /**
     * true si idUsuario es el cliente dueno de la solicitud, o el tecnico
     * actualmente asignado (directo o por su grupo vigente). Se usa para
     * autorizar listar/descargar adjuntos sin duplicar toda la logica de
     * asignacion en Java.
     */
    @Query(value = "SELECT EXISTS (" +
            "SELECT 1 FROM solicitud WHERE id_solicitud = :idSolicitud AND id_cliente = :idUsuario " +
            "UNION ALL " +
            "SELECT 1 FROM asignacion_solicitud WHERE id_solicitud = :idSolicitud AND vigente = true AND id_tecnico = :idUsuario " +
            "UNION ALL " +
            "SELECT 1 FROM asignacion_solicitud a JOIN tecnico_grupo tg ON tg.id_grupo = a.id_grupo " +
            "WHERE a.id_solicitud = :idSolicitud AND a.vigente = true AND tg.id_usuario = :idUsuario" +
            ")", nativeQuery = true)
    boolean puedeAccederASolicitud(@Param("idSolicitud") Long idSolicitud, @Param("idUsuario") Long idUsuario);
}
