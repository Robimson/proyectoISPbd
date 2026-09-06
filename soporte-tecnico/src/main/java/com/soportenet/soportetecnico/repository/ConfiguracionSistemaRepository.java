package com.soportenet.soportetecnico.repository;

import com.soportenet.soportetecnico.dto.ConfiguracionProjection;
import com.soportenet.soportetecnico.entity.ConfiguracionSistema;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConfiguracionSistemaRepository extends JpaRepository<ConfiguracionSistema, Short> {

    /** Invoca fn_obtener_configuracion_sistema() - publica, sin login. */
    @Query(value = "SELECT * FROM fn_obtener_configuracion_sistema()", nativeQuery = true)
    ConfiguracionProjection obtener();

    /** Invoca sp_actualizar_configuracion_sistema(...) - nombre, categoria, eslogan y color, solo Superusuario. */
    @Query(value = "SELECT sp_actualizar_configuracion_sistema(:idSuperusuario, :nombreNegocio, :categoria, :eslogan, :colorPrimario)",
           nativeQuery = true)
    void actualizar(
            @Param("idSuperusuario") Long idSuperusuario,
            @Param("nombreNegocio") String nombreNegocio,
            @Param("categoria") String categoria,
            @Param("eslogan") String eslogan,
            @Param("colorPrimario") String colorPrimario
    );

    /** Invoca sp_actualizar_logo_sistema(...) - solo Superusuario. */
    @Query(value = "SELECT sp_actualizar_logo_sistema(:idSuperusuario, :logoUrl)", nativeQuery = true)
    void actualizarLogo(
            @Param("idSuperusuario") Long idSuperusuario,
            @Param("logoUrl") String logoUrl
    );
}
