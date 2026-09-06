package com.soportenet.soportetecnico.repository;

import com.soportenet.soportetecnico.dto.GrupoTecnicoConteoProjection;
import com.soportenet.soportetecnico.dto.UsuarioBusquedaProjection;
import com.soportenet.soportetecnico.entity.GrupoTecnico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GrupoTecnicoRepository extends JpaRepository<GrupoTecnico, Long> {

    /**
     * Invoca fn_grupos_tecnicos_con_conteo() - cada grupo con cuantos
     * tecnicos tiene, para "Grupos técnicos existentes".
     */
    @Query(value = "SELECT * FROM fn_grupos_tecnicos_con_conteo()", nativeQuery = true)
    List<GrupoTecnicoConteoProjection> listarConConteo();

    /**
     * Invoca fn_miembros_grupo(...) - los tecnicos de un grupo puntual, para
     * el modal "Editar grupo" (ver quien esta y poder quitarlo).
     */
    @Query(value = "SELECT * FROM fn_miembros_grupo(:idGrupo)", nativeQuery = true)
    List<UsuarioBusquedaProjection> listarMiembros(@Param("idGrupo") Long idGrupo);

    /**
     * Agrega un tecnico a un grupo, via sp_agregar_miembro_grupo(). Si el id
     * no pertenece a un tecnico habilitado o el grupo no existe, la FK de
     * tecnico_grupo lo rechaza como DataIntegrityViolationException (ya
     * traducida a 400 por GlobalExceptionHandler); si ya era miembro, la PK
     * compuesta lo rechaza igual.
     */
    @Query(value = "SELECT sp_agregar_miembro_grupo(:idTecnico, :idGrupo)", nativeQuery = true)
    void agregarMiembro(@Param("idTecnico") Long idTecnico, @Param("idGrupo") Long idGrupo);

    @Query(value = "SELECT sp_quitar_miembro_grupo(:idTecnico, :idGrupo)", nativeQuery = true)
    void retirarMiembro(@Param("idTecnico") Long idTecnico, @Param("idGrupo") Long idGrupo);
}
