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

    
    @Query(value = "SELECT * FROM fn_todas_ordenado_admin()",
           countQuery = "SELECT fn_todas_ordenado_admin_conteo()",
           nativeQuery = true)
    Page<Solicitud> findTodasOrdenadoParaAdmin(Pageable pageable);

    /** Igual que findTodasOrdenadoParaAdmin(), filtrado por estado (caso de uso 4.3.3). */
    @Query(value = "SELECT * FROM fn_por_estado_ordenado_admin(:estado)",
           countQuery = "SELECT fn_por_estado_ordenado_admin_conteo(:estado)",
           nativeQuery = true)
    Page<Solicitud> findPorEstadoOrdenadoParaAdmin(@Param("estado") String estado, Pageable pageable);

    
    @Query(value = "SELECT * FROM fn_mis_tareas(:idTecnico, :estado)",
           countQuery = "SELECT fn_mis_tareas_conteo(:idTecnico, :estado)",
           nativeQuery = true)
    Page<Solicitud> findMisTareas(@Param("idTecnico") Long idTecnico, @Param("estado") String estado, Pageable pageable);

    
    @Query(value = "SELECT sp_cierre_automatico_por_vencimiento()", nativeQuery = true)
    Integer cerrarSolicitudesVencidas();

    
    @Query(value = "SELECT sp_crear_solicitud(:idCliente, :descripcion, :idCategoria, :direccion)",
           nativeQuery = true)
    Long crearSolicitud(
            @Param("idCliente") Long idCliente,
            @Param("descripcion") String descripcion,
            @Param("idCategoria") Integer idCategoria,
            @Param("direccion") String direccion
    );

    
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

    
    @Query(value = "SELECT sp_confirmar_cliente(:idSolicitud, :idCliente, :problemaResuelto)",
           nativeQuery = true)
    void confirmarCliente(
            @Param("idSolicitud") Long idSolicitud,
            @Param("idCliente") Long idCliente,
            @Param("problemaResuelto") Boolean problemaResuelto
    );

    
    @Query(value = "SELECT sp_reabrir_ticket_cerrado_administrativo(:idSolicitud, :idAdministrador)",
           nativeQuery = true)
    void reabrirTicketCerrado(
            @Param("idSolicitud") Long idSolicitud,
            @Param("idAdministrador") Long idAdministrador
    );

    
    @Query(value = "SELECT fn_tecnico_tiene_acceso(:idSolicitud, :idTecnico)", nativeQuery = true)
    boolean tecnicoTieneAcceso(
            @Param("idSolicitud") Long idSolicitud,
            @Param("idTecnico") Long idTecnico
    );

    
    @Query(value = "SELECT * FROM fn_resumen_tecnico(:idTecnico)", nativeQuery = true)
    ResumenTecnicoProjection resumenTecnico(@Param("idTecnico") Long idTecnico);

    
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
