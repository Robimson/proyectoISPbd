package com.soportenet.soportetecnico.repository;

import com.soportenet.soportetecnico.entity.Solicitud;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SolicitudRepository extends JpaRepository<Solicitud, Long> {

    /** Cliente: sus propias solicitudes (caso de uso 4.1.4). */
    Page<Solicitud> findByClienteIdUsuario(Long idCliente, Pageable pageable);

    /** Cliente: sus propias solicitudes, filtradas por estado. */
    Page<Solicitud> findByClienteIdUsuarioAndEstadoNombreEstado(Long idCliente, String nombreEstado, Pageable pageable);

    /** Administrador: todas las solicitudes, filtradas por estado (caso de uso 4.3.3). */
    Page<Solicitud> findByEstadoNombreEstado(String nombreEstado, Pageable pageable);

    /**
     * Tecnico: "Mis tareas" (caso de uso 4.2.2) - solicitudes asignadas a el
     * directamente o a un grupo del que es miembro, usando solo la
     * asignacion vigente. Ordenadas por prioridad real (orden de la tabla
     * prioridad), no por el id de la fila.
     */
    @Query(value = "SELECT s.* FROM solicitud s " +
                   "JOIN asignacion_solicitud a ON a.id_solicitud = s.id_solicitud AND a.vigente = true " +
                   "LEFT JOIN tecnico_grupo tg ON tg.id_grupo = a.id_grupo AND tg.id_usuario = :idTecnico " +
                   "LEFT JOIN prioridad p ON p.id_prioridad = s.id_prioridad " +
                   "WHERE a.id_tecnico = :idTecnico OR tg.id_usuario = :idTecnico " +
                   "ORDER BY COALESCE(p.orden, 0) DESC, s.fecha_creacion ASC",
           countQuery = "SELECT count(*) FROM solicitud s " +
                   "JOIN asignacion_solicitud a ON a.id_solicitud = s.id_solicitud AND a.vigente = true " +
                   "LEFT JOIN tecnico_grupo tg ON tg.id_grupo = a.id_grupo AND tg.id_usuario = :idTecnico " +
                   "WHERE a.id_tecnico = :idTecnico OR tg.id_usuario = :idTecnico",
           nativeQuery = true)
    Page<Solicitud> findMisTareas(@Param("idTecnico") Long idTecnico, Pageable pageable);

    /**
     * Invoca sp_cierre_automatico_por_vencimiento(). Cierra las solicitudes
     * en "Resuelta - Pendiente Confirmacion del Cliente" cuyo plazo vencio;
     * devuelve cuantas se cerraron. Pensada para correr periodicamente
     * (ver CierreAutomaticoScheduler), no para un endpoint HTTP.
     */
    @Query(value = "SELECT sp_cierre_automatico_por_vencimiento()", nativeQuery = true)
    Integer cerrarSolicitudesVencidas();

    /**
     * Invoca sp_crear_solicitud(...) directamente en PostgreSQL.
     * Toda la validacion (cliente existe, cuenta activa, descripcion no vacia,
     * categoria valida) ya vive en la funcion de la base de datos; aqui solo
     * la llamamos y devolvemos el id generado. La prioridad no se recibe: la
     * establece el Administrador al asignar (sp_asignar_solicitud).
     */
    @Query(value = "SELECT sp_crear_solicitud(:idCliente, :descripcion, :idCategoria)",
           nativeQuery = true)
    Long crearSolicitud(
            @Param("idCliente") Long idCliente,
            @Param("descripcion") String descripcion,
            @Param("idCategoria") Integer idCategoria
    );

    /**
     * Invoca sp_asignar_solicitud(...) directamente en PostgreSQL. Valida
     * administrador activo, que se indique exactamente un tecnico o un grupo
     * (no ambos), que la solicitud no este Cerrada, y exige motivo cuando es
     * una reasignacion; tambien notifica al tecnico. Todo eso vive en el
     * procedimiento, aqui solo lo invocamos.
     */
    @Query(value = "SELECT sp_asignar_solicitud(:idSolicitud, :idAdministrador, :idTecnico, :idGrupo, :idPrioridad, :motivoReasignacion)",
           nativeQuery = true)
    void asignarSolicitud(
            @Param("idSolicitud") Long idSolicitud,
            @Param("idAdministrador") Long idAdministrador,
            @Param("idTecnico") Long idTecnico,
            @Param("idGrupo") Long idGrupo,
            @Param("idPrioridad") Integer idPrioridad,
            @Param("motivoReasignacion") String motivoReasignacion
    );

    /**
     * Invoca sp_confirmar_cliente(...) directamente en PostgreSQL. Valida
     * que el cliente sea el dueno de la solicitud, que su cuenta este activa
     * y que el ticket este en "Resuelta - Pendiente Confirmacion del
     * Cliente". Si problemaResuelto es true cierra el ticket; si es false lo
     * regresa a "En Proceso" y notifica al tecnico vigente.
     */
    @Query(value = "SELECT sp_confirmar_cliente(:idSolicitud, :idCliente, :problemaResuelto)",
           nativeQuery = true)
    void confirmarCliente(
            @Param("idSolicitud") Long idSolicitud,
            @Param("idCliente") Long idCliente,
            @Param("problemaResuelto") Boolean problemaResuelto
    );

    /**
     * Invoca sp_reabrir_ticket_cerrado_administrativo(...). Solo un
     * administrador activo puede reabrir un ticket Cerrada; lo regresa a "En
     * Proceso". El procedimiento levanta temporalmente el bypass del trigger
     * fn_pre_update_solicitud() (que normalmente bloquea escribir sobre una
     * solicitud Cerrada) solo para esta operacion.
     */
    @Query(value = "SELECT sp_reabrir_ticket_cerrado_administrativo(:idSolicitud, :idAdministrador)",
           nativeQuery = true)
    void reabrirTicketCerrado(
            @Param("idSolicitud") Long idSolicitud,
            @Param("idAdministrador") Long idAdministrador
    );
}
