package com.soportenet.soportetecnico.repository;

import com.soportenet.soportetecnico.entity.EstadoRespaldo;
import com.soportenet.soportetecnico.entity.RespaldoBd;
import com.soportenet.soportetecnico.entity.TipoRespaldo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.OffsetDateTime;
import java.util.List;

public interface RespaldoBdRepository
        extends JpaRepository<RespaldoBd, Long> {

    Page<RespaldoBd> findAllByOrderByFechaInicioDesc(
            Pageable pageable
    );

    Page<RespaldoBd> findByTipoOrderByFechaInicioDesc(
            TipoRespaldo tipo,
            Pageable pageable
    );

    boolean existsByTipoAndEstadoAndFechaInicioAfter(
            TipoRespaldo tipo,
            EstadoRespaldo estado,
            OffsetDateTime desde
    );

    /**
     * Obtiene las rutas de los WAL que ya fueron registrados.
     *
     * Esto permite que el escáner no vuelva a insertar
     * el mismo archivo WAL en cada ejecución.
     */
    @Query("""
        SELECT r.rutaArchivo
        FROM RespaldoBd r
        WHERE r.tipo = com.soportenet.soportetecnico.entity.TipoRespaldo.WAL
    """)
    List<String> rutasWalRegistradas();
}