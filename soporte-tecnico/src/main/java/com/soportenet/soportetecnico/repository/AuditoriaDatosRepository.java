package com.soportenet.soportetecnico.repository;

import com.soportenet.soportetecnico.dto.ResumenAuditoriaProjection;
import com.soportenet.soportetecnico.entity.AuditoriaDatos;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AuditoriaDatosRepository extends JpaRepository<AuditoriaDatos, Long> {

    /** Superusuario: historial de cambios en tablas de operacion interna, mas reciente primero. */
    Page<AuditoriaDatos> findAllByOrderByFechaDesc(Pageable pageable);

    /** Superusuario: historial de cambios filtrado por tabla afectada. */
    Page<AuditoriaDatos> findByTablaAfectadaOrderByFechaDesc(String tablaAfectada, Pageable pageable);

    /**
     * Invoca fn_resumen_auditoria() - conteos rapidos para la pestana
     * "Auditoria" antes de entrar al detalle de las tablas.
     */
    @Query(value = "SELECT * FROM fn_resumen_auditoria()", nativeQuery = true)
    ResumenAuditoriaProjection resumenAuditoria();
}
