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
     * actualmente asignado (directo o por su grupo vigente), via
     * fn_puede_acceder_a_solicitud().
     */
    @Query(value = "SELECT fn_puede_acceder_a_solicitud(:idSolicitud, :idUsuario)", nativeQuery = true)
    boolean puedeAccederASolicitud(@Param("idSolicitud") Long idSolicitud, @Param("idUsuario") Long idUsuario);
}
