package com.soportenet.soportetecnico.repository;

import com.soportenet.soportetecnico.entity.GrupoTecnico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GrupoTecnicoRepository extends JpaRepository<GrupoTecnico, Long> {

    /**
     * Agrega un tecnico a un grupo. Si el id no pertenece a un tecnico
     * habilitado o el grupo no existe, la FK de tecnico_grupo lo rechaza
     * como DataIntegrityViolationException (ya traducida a 400 por
     * GlobalExceptionHandler); si ya era miembro, la PK compuesta lo
     * rechaza igual.
     */
    @Modifying
    @Query(value = "INSERT INTO tecnico_grupo (id_usuario, id_grupo) VALUES (:idTecnico, :idGrupo)",
           nativeQuery = true)
    void agregarMiembro(@Param("idTecnico") Long idTecnico, @Param("idGrupo") Long idGrupo);

    @Modifying
    @Query(value = "DELETE FROM tecnico_grupo WHERE id_usuario = :idTecnico AND id_grupo = :idGrupo",
           nativeQuery = true)
    void retirarMiembro(@Param("idTecnico") Long idTecnico, @Param("idGrupo") Long idGrupo);
}
