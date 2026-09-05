package com.soportenet.soportetecnico.repository;

import com.soportenet.soportetecnico.entity.Tecnico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TecnicoRepository extends JpaRepository<Tecnico, Long> {

    List<Tecnico> findByHabilitado(Boolean habilitado);

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
