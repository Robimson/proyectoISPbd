package com.soportenet.soportetecnico.repository;

import com.soportenet.soportetecnico.dto.ConteoProjection;
import com.soportenet.soportetecnico.dto.ResumenTecnicoProjection;
import com.soportenet.soportetecnico.entity.Solicitud;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SolicitudRepository extends JpaRepository<Solicitud, Long> {

    /** Cliente: sus propias solicitudes (caso de uso 4.1.4). */
    Page<Solicitud> findByClienteIdUsuario(Long idCliente, Pageable pageable);

    /** Cliente: sus propias solicitudes, filtradas por estado. */
    Page<Solicitud> findByClienteIdUsuarioAndEstadoNombreEstado(Long idCliente, String nombreEstado, Pageable pageable);

    /** Administrador: todas las solicitudes, filtradas por estado (caso de uso 4.3.3). */
    Page<Solicitud> findByEstadoNombreEstado(String nombreEstado, Pageable pageable);

    /**
     * Administrador/Superusuario: todas las solicitudes en orden de "cola de
     * trabajo" (fn_todas_ordenado_admin - ver esa funcion en Postgres para
     * el detalle del criterio de orden). Se verifico con EXPLAIN que
     * Postgres INLINEA esta funcion (LANGUAGE sql, sin efectos secundarios)
     * y sigue usando idx_solicitud_prioridad_fecha exactamente igual que
     * antes de moverla a la base - no se pierde el rendimiento.
     */
    @Query(value = "SELECT * FROM fn_todas_ordenado_admin()",
            countQuery = "SELECT fn_todas_ordenado_admin_conteo()",
            nativeQuery = true)
    Page<Solicitud> findTodasOrdenadoParaAdmin(Pageable pageable);

    /** Igual que findTodasOrdenadoParaAdmin(), filtrado por estado (caso de uso 4.3.3). */
    @Query(value = "SELECT * FROM fn_por_estado_ordenado_admin(:estado)",
            countQuery = "SELECT fn_por_estado_ordenado_admin_conteo(:estado)",
            nativeQuery = true)
    Page<Solicitud> findPorEstadoOrdenadoParaAdmin(@Param("estado") String estado, Pageable pageable);

    /**
     * Tecnico: "Mis tareas" (caso de uso 4.2.2), via fn_mis_tareas() - ver
     * esa funcion en Postgres para el detalle (UNION directo/por grupo,
     * orden por prioridad real). El plan de EXPLAIN sigue usando
     * idx_asignacion_tecnico_vigente / idx_asignacion_grupo_vigente igual
     * que antes de moverla a la base.
     */
    @Query(value = "SELECT * FROM fn_mis_tareas(:idTecnico, :estado)",
            countQuery = "SELECT fn_mis_tareas_conteo(:idTecnico, :estado)",
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
     * categoria valida, direccion no vacia, rango de lat/lng) ya vive en la
     * funcion de la base de datos; aqui solo la llamamos y devolvemos el id
     * generado. La prioridad no se recibe: la establece el Administrador al
     * asignar (sp_asignar_solicitud). Si direccion viene null, el
     * procedimiento cae de vuelta a la ultima direccion conocida del
     * cliente. lat/lng son opcionales (pueden venir null si el cliente
     * escribio la direccion a mano en vez de usar el mapa).
     */
    @Query(value = "SELECT sp_crear_solicitud(:idCliente, :descripcion, :idCategoria, :direccion, :lat, :lng)",
            nativeQuery = true)
    Long crearSolicitud(
            @Param("idCliente") Long idCliente,
            @Param("descripcion") String descripcion,
            @Param("idCategoria") Integer idCategoria,
            @Param("direccion") String direccion,
            @Param("lat") Double lat,
            @Param("lng") Double lng
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
     * y que el ticket este en "Resuelta - Pendiente Confirmación del
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

    // ---------- Graficos del dashboard (solo lectura, GROUP BY simples) ----------
    // Todas devuelven ConteoProjection (etiqueta + valor), via funciones
    // fn_conteo_* - ver esas funciones en Postgres para el detalle de cada
    // consulta. Las que reciben un id (cliente/tecnico) estan acotadas a lo
    // suyo; las "global" son para Administrador/Superusuario.

    /** Cliente: sus propias solicitudes por estado, para el grafico de dona de "Mis solicitudes". */
    @Query(value = "SELECT * FROM fn_conteo_solicitudes_estado_cliente(:idCliente)", nativeQuery = true)
    List<ConteoProjection> contarPorEstadoCliente(@Param("idCliente") Long idCliente);

    /** Cliente: sus propias solicitudes por categoria. */
    @Query(value = "SELECT * FROM fn_conteo_solicitudes_categoria_cliente(:idCliente)", nativeQuery = true)
    List<ConteoProjection> contarPorCategoriaCliente(@Param("idCliente") Long idCliente);

    /** Tecnico: sus tareas vigentes (directas o por grupo) agrupadas por prioridad. */
    @Query(value = "SELECT * FROM fn_conteo_solicitudes_prioridad_tecnico(:idTecnico)", nativeQuery = true)
    List<ConteoProjection> contarPorPrioridadTecnico(@Param("idTecnico") Long idTecnico);

    /** Administrador/Superusuario: todas las solicitudes por estado. */
    @Query(value = "SELECT * FROM fn_conteo_solicitudes_estado()", nativeQuery = true)
    List<ConteoProjection> contarPorEstadoGlobal();

    /** Administrador/Superusuario: todas las solicitudes por prioridad (incluye "Sin asignar"). */
    @Query(value = "SELECT * FROM fn_conteo_solicitudes_prioridad()", nativeQuery = true)
    List<ConteoProjection> contarPorPrioridadGlobal();

    /** Administrador/Superusuario: todas las solicitudes por categoria (incluye "Sin categoría"). */
    @Query(value = "SELECT * FROM fn_conteo_solicitudes_categoria()", nativeQuery = true)
    List<ConteoProjection> contarPorCategoriaGlobal();
}