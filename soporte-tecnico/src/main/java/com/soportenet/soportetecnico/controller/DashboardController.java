package com.soportenet.soportetecnico.controller;

import com.soportenet.soportetecnico.dto.EstadisticasAdminResponse;
import com.soportenet.soportetecnico.dto.EstadisticasSuperusuarioResponse;
import com.soportenet.soportetecnico.repository.ReporteSolicitudRepository;
import com.soportenet.soportetecnico.repository.SolicitudRepository;
import com.soportenet.soportetecnico.repository.TecnicoRepository;
import com.soportenet.soportetecnico.repository.UsuarioRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Datos para los graficos del "Resumen" de Administrador y Superusuario.
 * Vive aparte de SolicitudController/TecnicoController/UsuarioController
 * porque cada uno de estos dos endpoints combina conteos de varias tablas
 * distintas a la vez (solicitud + reporte_solicitud + tecnico, o usuario +
 * tecnico) - no pertenecen naturalmente a un solo controlador de dominio,
 * son vistas agregadas pensadas para esta pantalla en particular. Todo lo
 * que devuelven es de solo lectura (conteos agrupados), sin logica de
 * negocio nueva.
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final SolicitudRepository solicitudRepository;
    private final ReporteSolicitudRepository reporteSolicitudRepository;
    private final TecnicoRepository tecnicoRepository;
    private final UsuarioRepository usuarioRepository;

    public DashboardController(SolicitudRepository solicitudRepository,
                                ReporteSolicitudRepository reporteSolicitudRepository,
                                TecnicoRepository tecnicoRepository,
                                UsuarioRepository usuarioRepository) {
        this.solicitudRepository = solicitudRepository;
        this.reporteSolicitudRepository = reporteSolicitudRepository;
        this.tecnicoRepository = tecnicoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Administrador: solicitudes por estado/prioridad/categoria, tasa de
     * aprobacion de reportes y carga de trabajo de los tecnicos.
     */
    @GetMapping("/administrador")
    public ResponseEntity<EstadisticasAdminResponse> estadisticasAdministrador() {
        return ResponseEntity.ok(new EstadisticasAdminResponse(
                solicitudRepository.contarPorEstadoGlobal(),
                solicitudRepository.contarPorPrioridadGlobal(),
                solicitudRepository.contarPorCategoriaGlobal(),
                reporteSolicitudRepository.contarAprobacionGlobal(),
                tecnicoRepository.contarCargaTrabajo()
        ));
    }

    /**
     * Superusuario: composicion del equipo tecnico por nivel y de la base de
     * usuarios por rol. "Miembros por grupo tecnico" no esta aca porque el
     * frontend ya lo puede sacar de GET /api/grupos-tecnicos, que ya trae el
     * conteo por grupo.
     */
    @GetMapping("/superusuario")
    public ResponseEntity<EstadisticasSuperusuarioResponse> estadisticasSuperusuario() {
        return ResponseEntity.ok(new EstadisticasSuperusuarioResponse(
                tecnicoRepository.contarPorNivel(),
                usuarioRepository.contarPorRol()
        ));
    }
}
