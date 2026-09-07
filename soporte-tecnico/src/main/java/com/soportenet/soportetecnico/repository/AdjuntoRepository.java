package com.soportenet.soportetecnico.repository;

import com.soportenet.soportetecnico.entity.Adjunto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AdjuntoRepository extends JpaRepository<Adjunto, Long> {

    List<Adjunto> findBySolicitudIdSolicitudOrderByFechaSubidaAsc(Long idSolicitud);

   
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

    
    @Query(value = "SELECT fn_puede_acceder_a_solicitud(:idSolicitud, :idUsuario)", nativeQuery = true)
    boolean puedeAccederASolicitud(@Param("idSolicitud") Long idSolicitud, @Param("idUsuario") Long idUsuario);
}
