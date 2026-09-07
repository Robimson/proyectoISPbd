package com.soportenet.soportetecnico.repository;

import com.soportenet.soportetecnico.dto.ConteoProjection;
import com.soportenet.soportetecnico.entity.Tecnico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TecnicoRepository extends JpaRepository<Tecnico, Long> {

    List<Tecnico> findByHabilitado(Boolean habilitado);

    
    @Query(value = "SELECT * FROM fn_conteo_tecnicos_nivel()", nativeQuery = true)
    List<ConteoProjection> contarPorNivel();

    
    @Query(value = "SELECT * FROM fn_conteo_carga_trabajo_tecnicos()", nativeQuery = true)
    List<ConteoProjection> contarCargaTrabajo();

    
    @Query(value = "SELECT sp_editar_perfil_tecnico(:idSuperusuario, :idTecnico, :especialidad, CAST(:nivel AS nivel_tecnico_tipo))",
           nativeQuery = true)
    void editarPerfilTecnico(
            @Param("idSuperusuario") Long idSuperusuario,
            @Param("idTecnico") Long idTecnico,
            @Param("especialidad") String especialidad,
            @Param("nivel") String nivel
    );
}
