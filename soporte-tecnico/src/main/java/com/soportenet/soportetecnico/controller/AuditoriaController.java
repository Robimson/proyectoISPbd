package com.soportenet.soportetecnico.controller;

import com.soportenet.soportetecnico.dto.AuditoriaDatosResponse;
import com.soportenet.soportetecnico.dto.AuditoriaSesionResponse;
import com.soportenet.soportetecnico.entity.AuditoriaDatos;
import com.soportenet.soportetecnico.entity.AuditoriaSesion;
import com.soportenet.soportetecnico.repository.AuditoriaDatosRepository;
import com.soportenet.soportetecnico.repository.AuditoriaSesionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Pantallas de auditoria del Superusuario (seccion 11 del documento, mas la
 * extension de auditoria por tablas). Ambas son de solo lectura: las filas
 * las genera el backend/los triggers, no se crean desde aqui.
 */
@RestController
@RequestMapping("/api/auditoria")
public class AuditoriaController {

    private final AuditoriaSesionRepository auditoriaSesionRepository;
    private final AuditoriaDatosRepository auditoriaDatosRepository;

    public AuditoriaController(AuditoriaSesionRepository auditoriaSesionRepository,
                                AuditoriaDatosRepository auditoriaDatosRepository) {
        this.auditoriaSesionRepository = auditoriaSesionRepository;
        this.auditoriaDatosRepository = auditoriaDatosRepository;
    }

    @GetMapping("/sesiones")
    public ResponseEntity<Page<AuditoriaSesionResponse>> listarSesiones(
            @RequestParam(required = false) Long idUsuario,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<AuditoriaSesion> pagina = (idUsuario != null)
                ? auditoriaSesionRepository.findByIdUsuarioOrderByFechaEntradaDesc(idUsuario, pageable)
                : auditoriaSesionRepository.findAllByOrderByFechaEntradaDesc(pageable);

        return ResponseEntity.ok(pagina.map(AuditoriaSesionResponse::fromEntity));
    }

    @GetMapping("/datos")
    public ResponseEntity<Page<AuditoriaDatosResponse>> listarDatos(
            @RequestParam(required = false) String tabla,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<AuditoriaDatos> pagina = (tabla != null && !tabla.isBlank())
                ? auditoriaDatosRepository.findByTablaAfectadaOrderByFechaDesc(tabla, pageable)
                : auditoriaDatosRepository.findAllByOrderByFechaDesc(pageable);

        return ResponseEntity.ok(pagina.map(AuditoriaDatosResponse::fromEntity));
    }
}
