package com.soportenet.soportetecnico.controller;

import com.soportenet.soportetecnico.dto.AuditoriaDatosResponse;
import com.soportenet.soportetecnico.dto.AuditoriaSesionResponse;
import com.soportenet.soportetecnico.dto.ResumenAuditoriaProjection;
import com.soportenet.soportetecnico.dto.UsuarioBusquedaProjection;
import com.soportenet.soportetecnico.entity.AuditoriaDatos;
import com.soportenet.soportetecnico.entity.AuditoriaSesion;
import com.soportenet.soportetecnico.repository.AuditoriaDatosRepository;
import com.soportenet.soportetecnico.repository.AuditoriaSesionRepository;
import com.soportenet.soportetecnico.repository.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    private final UsuarioRepository usuarioRepository;

    public AuditoriaController(AuditoriaSesionRepository auditoriaSesionRepository,
                                AuditoriaDatosRepository auditoriaDatosRepository,
                                UsuarioRepository usuarioRepository) {
        this.auditoriaSesionRepository = auditoriaSesionRepository;
        this.auditoriaDatosRepository = auditoriaDatosRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Conteos rapidos (sesiones activas, cambios de hoy por tipo) para
     * mostrar antes de entrar al detalle de las tablas de auditoria.
     */
    @GetMapping("/resumen")
    public ResponseEntity<ResumenAuditoriaProjection> resumen() {
        return ResponseEntity.ok(auditoriaDatosRepository.resumenAuditoria());
    }

    /**
     * Autocompletar por nombre o correo para el filtro de "Auditoria de
     * sesiones" (que hasta ahora solo filtraba por ID exacto de usuario).
     */
    @GetMapping("/usuarios/buscar")
    public ResponseEntity<List<UsuarioBusquedaProjection>> buscarUsuarios(
            @RequestParam String nombre) {

        if (nombre == null || nombre.trim().length() < 2) {
            return ResponseEntity.ok(List.of());
        }

        return ResponseEntity.ok(usuarioRepository.buscarUsuarios(nombre.trim()));
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
