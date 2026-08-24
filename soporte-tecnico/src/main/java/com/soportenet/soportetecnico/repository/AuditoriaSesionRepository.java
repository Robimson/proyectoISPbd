package com.soportenet.soportetecnico.repository;

import com.soportenet.soportetecnico.entity.AuditoriaSesion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditoriaSesionRepository extends JpaRepository<AuditoriaSesion, Long> {

    /** Superusuario: historial de sesiones, mas reciente primero (seccion 11). */
    Page<AuditoriaSesion> findAllByOrderByFechaEntradaDesc(Pageable pageable);

    /** Superusuario: historial de sesiones de un usuario puntual. */
    Page<AuditoriaSesion> findByIdUsuarioOrderByFechaEntradaDesc(Long idUsuario, Pageable pageable);

    /** Invoca sp_abrir_sesion(...) al iniciar sesion; devuelve el id_sesion para cerrarla despues. */
    @Query(value = "SELECT sp_abrir_sesion(:idUsuario, CAST(:ipOrigen AS inet))", nativeQuery = true)
    Long abrirSesion(@Param("idUsuario") Long idUsuario, @Param("ipOrigen") String ipOrigen);

    /** Invoca sp_cerrar_sesion(...) al cerrar sesion desde el frontend. */
    @Query(value = "SELECT sp_cerrar_sesion(:idSesion)", nativeQuery = true)
    void cerrarSesion(@Param("idSesion") Long idSesion);
}
