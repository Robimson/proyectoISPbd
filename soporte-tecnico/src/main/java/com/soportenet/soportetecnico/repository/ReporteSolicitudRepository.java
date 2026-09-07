package com.soportenet.soportetecnico.repository;

import com.soportenet.soportetecnico.dto.ConteoProjection;
import com.soportenet.soportetecnico.entity.ReporteSolicitud;
import com.soportenet.soportetecnico.enums.EstadoAprobacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReporteSolicitudRepository extends JpaRepository<ReporteSolicitud, Long> {

    /** Administrador: reportes filtrados por estado de aprobacion (caso de uso 4.3.6). */
    Page<ReporteSolicitud> findByEstadoAprobacion(EstadoAprobacion estadoAprobacion, Pageable pageable);

    
    List<ReporteSolicitud> findBySolicitudIdSolicitudOrderByFechaEnvioDesc(Long idSolicitud);

    
    @Query(value = "SELECT sp_enviar_reporte(:idSolicitud, :idTecnico, :detalleReporte)",
           nativeQuery = true)
    Long enviarReporte(
            @Param("idSolicitud") Long idSolicitud,
            @Param("idTecnico") Long idTecnico,
            @Param("detalleReporte") String detalleReporte
    );

    
    @Query(value = "SELECT sp_aprobar_reporte(:idReporte, :idAdministrador, :diasPlazoConfirmacion)",
           nativeQuery = true)
    void aprobarReporte(
            @Param("idReporte") Long idReporte,
            @Param("idAdministrador") Long idAdministrador,
            @Param("diasPlazoConfirmacion") Integer diasPlazoConfirmacion
    );

    
    @Query(value = "SELECT sp_rechazar_reporte(:idReporte, :idAdministrador, :comentarioRechazo)",
           nativeQuery = true)
    void rechazarReporte(
            @Param("idReporte") Long idReporte,
            @Param("idAdministrador") Long idAdministrador,
            @Param("comentarioRechazo") String comentarioRechazo
    );

    
    @Query(value = "SELECT * FROM fn_conteo_aprobacion_tecnico(:idTecnico)", nativeQuery = true)
    List<ConteoProjection> contarAprobacionTecnico(@Param("idTecnico") Long idTecnico);

    /** Administrador: tasa de aprobacion de reportes a nivel de todo el negocio. */
    @Query(value = "SELECT * FROM fn_conteo_aprobacion()", nativeQuery = true)
    List<ConteoProjection> contarAprobacionGlobal();
}
