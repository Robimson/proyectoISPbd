package com.soportenet.soportetecnico.repository;

import com.soportenet.soportetecnico.dto.ResumenTecnicoProjection;
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
     * asignacion vigente, con filtro opcional por estado. Ordenadas por
     * prioridad real (orden de la tabla prioridad), no por el id de la fila.
     *
     * El "directo o por grupo" se resuelve con un UNION en vez de un OR entre
     * dos tablas distintas (asignacion_solicitud.id_tecnico / tecnico_grupo)
     * - un OR asi le impide a Postgres usar el indice de ninguna de las dos
     * ramas. Con el UNION cada rama usa su propio indice por separado
     * (idx_asignacion_tecnico_vigente / idx_asignacion_grupo_vigente):
     * verificado con EXPLAIN ANALYZE contra 1M+ solicitudes, de ~390ms a
     * ~14ms.
     */
    @Query(value = "SELECT s.* FROM (" +
                   "    SELECT a.id_solicitud FROM asignacion_solicitud a " +
                   "    WHERE a.id_tecnico = :idTecnico AND a.vigente = true " +
                   "    UNION " +
                   "    SELECT a.id_solicitud FROM asignacion_solicitud a " +
                   "    JOIN tecnico_grupo tg ON tg.id_grupo = a.id_grupo AND tg.id_usuario = :idTecnico " +
                   "    WHERE a.vigente = true " +
                   ") mis_asignaciones " +
                   "JOIN solicitud s ON s.id_solicitud = mis_asignaciones.id_solicitud " +
                   "LEFT JOIN prioridad p ON p.id_prioridad = s.id_prioridad " +
                   "LEFT JOIN estado e ON e.id_estado = s.id_estado " +
                   "WHERE (:estado IS NULL OR e.nombre_estado = :estado) " +
                   "ORDER BY COALESCE(p.orden, 0) DESC, s.fecha_creacion ASC",
           countQuery = "SELECT count(*) FROM (" +
                   "    SELECT a.id_solicitud FROM asignacion_solicitud a " +
                   "    WHERE a.id_tecnico = :idTecnico AND a.vigente = true " +
                   "    UNION " +
                   "    SELECT a.id_solicitud FROM asignacion_solicitud a " +
                   "    JOIN tecnico_grupo tg ON tg.id_grupo = a.id_grupo AND tg.id_usuario = :idTecnico " +
                   "    WHERE a.vigente = true " +
                   ") mis_asignaciones " +
                   "JOIN solicitud s ON s.id_solicitud = mis_asignaciones.id_solicitud " +
                   "LEFT JOIN estado e ON e.id_estado = s.id_estado " +
                   "WHERE (:estado IS NULL OR e.nombre_estado = :estado)",
           nativeQuery = true)
    Page<Solicitud> findMisTareas(@Param("idTecnico") Long idTecnico, @Param("estado") String estado, Pageable pageable);

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
     * categoria valida, direccion no vacia) ya vive en la funcion de la base
     * de datos; aqui solo la llamamos y devolvemos el id generado. La
     * prioridad no se recibe: la establece el Administrador al asignar
     * (sp_asignar_solicitud). Si direccion viene null (no deberia, el DTO ya
     * la exige), el procedimiento cae de vuelta a la ultima direccion
     * conocida del cliente.
     */
    @Query(value = "SELECT sp_crear_solicitud(:idCliente, :descripcion, :idCategoria, :direccion)",
           nativeQuery = true)
    Long crearSolicitud(
            @Param("idCliente") Long idCliente,
            @Param("descripcion") String descripcion,
            @Param("idCategoria") Integer idCategoria,
            @Param("direccion") String direccion
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

    /**
     * Invoca fn_tecnico_tiene_acceso(...) - true si la solicitud esta
     * asignada al tecnico directo o a un grupo del que es miembro (solo
     * asignacion vigente). La usa SolicitudController.obtener() para decidir
     * si un tecnico puede ver el detalle de una solicitud que no es suya.
     */
    @Query(value = "SELECT fn_tecnico_tiene_acceso(:idSolicitud, :idTecnico)", nativeQuery = true)
    boolean tecnicoTieneAcceso(
            @Param("idSolicitud") Long idSolicitud,
            @Param("idTecnico") Long idTecnico
    );

    /**
     * Invoca fn_resumen_tecnico(...) - conteos para la pestana "Resumen" del
     * panel de tecnico (en proceso, pendiente aprobacion, resueltas hoy,
     * total cerradas), todo en una sola llamada.
     */
    @Query(value = "SELECT * FROM fn_resumen_tecnico(:idTecnico)", nativeQuery = true)
    ResumenTecnicoProjection resumenTecnico(@Param("idTecnico") Long idTecnico);
}
