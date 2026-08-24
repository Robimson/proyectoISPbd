package com.soportenet.soportetecnico.repository;

import com.soportenet.soportetecnico.entity.ReporteSolicitud;
import com.soportenet.soportetecnico.enums.EstadoAprobacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReporteSolicitudRepository extends JpaRepository<ReporteSolicitud, Long> {

    /** Administrador: reportes filtrados por estado de aprobacion (caso de uso 4.3.6). */
    Page<ReporteSolicitud> findByEstadoAprobacion(EstadoAprobacion estadoAprobacion, Pageable pageable);

    /**
     * Invoca sp_enviar_reporte(...). Valida que la solicitud este En Proceso
     * y que el tecnico este autorizado (asignado directo o miembro del grupo
     * vigente); pasa la solicitud a Pendiente Aprobacion.
     */
    @Query(value = "SELECT sp_enviar_reporte(:idSolicitud, :idTecnico, :detalleReporte)",
           nativeQuery = true)
    Long enviarReporte(
            @Param("idSolicitud") Long idSolicitud,
            @Param("idTecnico") Long idTecnico,
            @Param("detalleReporte") String detalleReporte
    );

    /**
     * Invoca sp_aprobar_reporte(...). Pasa la solicitud a
     * "Resuelta - Pendiente Confirmacion del Cliente" y fija el plazo de
     * confirmacion. p_dias_plazo_confirmacion siempre debe llegar con un
     * valor: al ser una llamada nativa posicional, un NULL explicito NO
     * activa el DEFAULT 3 del procedimiento.
     */
    @Query(value = "SELECT sp_aprobar_reporte(:idReporte, :idAdministrador, :diasPlazoConfirmacion)",
           nativeQuery = true)
    void aprobarReporte(
            @Param("idReporte") Long idReporte,
            @Param("idAdministrador") Long idAdministrador,
            @Param("diasPlazoConfirmacion") Integer diasPlazoConfirmacion
    );

    /**
     * Invoca sp_rechazar_reporte(...). Regresa la solicitud a "En Proceso"
     * y notifica al tecnico.
     */
    @Query(value = "SELECT sp_rechazar_reporte(:idReporte, :idAdministrador, :comentarioRechazo)",
           nativeQuery = true)
    void rechazarReporte(
            @Param("idReporte") Long idReporte,
            @Param("idAdministrador") Long idAdministrador,
            @Param("comentarioRechazo") String comentarioRechazo
    );
}
