package com.soportenet.soportetecnico.repository;

import com.soportenet.soportetecnico.dto.ConteoProjection;
import com.soportenet.soportetecnico.entity.Tecnico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TecnicoRepository extends JpaRepository<Tecnico, Long> {

    List<Tecnico> findByHabilitado(Boolean habilitado);

    /**
     * Superusuario: composicion del equipo por nivel, via
     * fn_conteo_tecnicos_nivel() (todos los tecnicos, habilitados o no -
     * interesa el total del equipo, no solo quien puede tomar trabajo ahora
     * mismo).
     */
    @Query(value = "SELECT * FROM fn_conteo_tecnicos_nivel()", nativeQuery = true)
    List<ConteoProjection> contarPorNivel();

    /**
     * Administrador: cuantos tecnicos (solo habilitados) tienen encima 0,
     * pocas, varias o demasiadas solicitudes vigentes asignadas DIRECTO a
     * ellos, via fn_conteo_carga_trabajo_tecnicos() - ver esa funcion en
     * Postgres para el detalle de las franjas y por que las asignaciones
     * por grupo no cuentan.
     */
    @Query(value = "SELECT * FROM fn_conteo_carga_trabajo_tecnicos()", nativeQuery = true)
    List<ConteoProjection> contarCargaTrabajo();

    /**
     * Invoca sp_editar_perfil_tecnico(...). Solo el Superusuario puede
     * hacerlo (validado dentro del procedimiento) - especialidad y nivel
     * quedan en NULL / 'junior' desde que se invita al tecnico, y este es el
     * unico lugar que los llena o actualiza despues.
     */
    @Query(value = "SELECT sp_editar_perfil_tecnico(:idSuperusuario, :idTecnico, :especialidad, CAST(:nivel AS nivel_tecnico_tipo))",
           nativeQuery = true)
    void editarPerfilTecnico(
            @Param("idSuperusuario") Long idSuperusuario,
            @Param("idTecnico") Long idTecnico,
            @Param("especialidad") String especialidad,
            @Param("nivel") String nivel
    );
}
