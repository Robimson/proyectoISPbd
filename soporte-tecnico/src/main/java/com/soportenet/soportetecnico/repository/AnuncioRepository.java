package com.soportenet.soportetecnico.repository;

import com.soportenet.soportetecnico.dto.AnuncioAdminProjection;
import com.soportenet.soportetecnico.dto.AnuncioProjection;
import com.soportenet.soportetecnico.entity.Anuncio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface AnuncioRepository extends JpaRepository<Anuncio, Long> {

    /**
     * Invoca sp_crear_anuncio(...). Valida que quien crea sea un
     * administrador activo, titulo/mensaje no vacios, y que la fecha de
     * expiracion (si viene) sea futura.
     */
    @Query(value = "SELECT sp_crear_anuncio(:idAdministrador, :titulo, :mensaje, :fechaExpiracion)",
           nativeQuery = true)
    Long crearAnuncio(
            @Param("idAdministrador") Long idAdministrador,
            @Param("titulo") String titulo,
            @Param("mensaje") String mensaje,
            @Param("fechaExpiracion") OffsetDateTime fechaExpiracion
    );

    /**
     * Invoca sp_desactivar_anuncio(...). No borra el anuncio (nunca se
     * elimina, igual que el resto del sistema) - solo lo saca del banner.
     */
    @Query(value = "SELECT sp_desactivar_anuncio(:idAdministrador, :idAnuncio)", nativeQuery = true)
    void desactivarAnuncio(
            @Param("idAdministrador") Long idAdministrador,
            @Param("idAnuncio") Long idAnuncio
    );

    /**
     * Invoca fn_listar_anuncios_activos() - el banner que ve cualquier rol
     * logueado. Ya filtra esta_activo=true y no vencidos.
     */
    @Query(value = "SELECT * FROM fn_listar_anuncios_activos()", nativeQuery = true)
    List<AnuncioProjection> listarActivos();

    /**
     * Invoca fn_listar_anuncios_admin() - todos los anuncios (activos e
     * inactivos), para la pantalla de gestion del Administrador.
     */
    @Query(value = "SELECT * FROM fn_listar_anuncios_admin()", nativeQuery = true)
    List<AnuncioAdminProjection> listarTodos();
}
