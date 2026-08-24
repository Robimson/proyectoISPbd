package com.soportenet.soportetecnico.controller;

import com.soportenet.soportetecnico.dto.AprobarReporteRequest;
import com.soportenet.soportetecnico.dto.EnviarReporteRequest;
import com.soportenet.soportetecnico.dto.RechazarReporteRequest;
import com.soportenet.soportetecnico.dto.ReporteResponse;
import com.soportenet.soportetecnico.entity.ReporteSolicitud;
import com.soportenet.soportetecnico.enums.EstadoAprobacion;
import com.soportenet.soportetecnico.repository.ReporteSolicitudRepository;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/**
 * Reporte de solucion del tecnico y su revision por el Administrador (casos
 * de uso 4.2.4 y 4.3.7 del documento). Toda la logica de negocio vive en
 * sp_enviar_reporte, sp_aprobar_reporte y sp_rechazar_reporte dentro de
 * PostgreSQL; estos endpoints solo las invocan. idTecnico/idAdministrador
 * salen del JWT, nunca del body.
 */
@RestController
public class ReporteController {

    private static final int DIAS_PLAZO_CONFIRMACION_DEFAULT = 3;

    private final ReporteSolicitudRepository reporteSolicitudRepository;

    public ReporteController(ReporteSolicitudRepository reporteSolicitudRepository) {
        this.reporteSolicitudRepository = reporteSolicitudRepository;
    }

    @PostMapping("/api/solicitudes/{id}/reportes")
    @Transactional
    public ResponseEntity<ReporteResponse> enviar(@PathVariable Long id,
                                                    @Valid @RequestBody EnviarReporteRequest request,
                                                    Authentication authentication) {

        Long idTecnico = Long.valueOf(authentication.getName());

        Long idReporte = reporteSolicitudRepository.enviarReporte(
                id,
                idTecnico,
                request.getDetalleReporte()
        );

        ReporteSolicitud creado = reporteSolicitudRepository.findById(idReporte)
                .orElseThrow(() -> new IllegalStateException(
                        "El reporte se creo pero no se pudo recuperar (id=" + idReporte + ")"));

        return ResponseEntity.status(HttpStatus.CREATED).body(ReporteResponse.fromEntity(creado));
    }

    @GetMapping("/api/reportes/{id}")
    public ResponseEntity<ReporteResponse> obtener(@PathVariable Long id) {
        return reporteSolicitudRepository.findById(id)
                .map(ReporteResponse::fromEntity)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Administrador: lista reportes filtrados por estado de aprobacion
     * (caso de uso 4.3.6, ej. "pendiente" para el dashboard de aprobacion).
     * Sin filtro, devuelve todos.
     */
    @GetMapping("/api/reportes")
    public ResponseEntity<Page<ReporteResponse>> listar(
            @RequestParam(required = false) EstadoAprobacion estado,
            @PageableDefault(size = 20, sort = "fechaEnvio") Pageable pageable) {

        Page<ReporteSolicitud> pagina = (estado != null)
                ? reporteSolicitudRepository.findByEstadoAprobacion(estado, pageable)
                : reporteSolicitudRepository.findAll(pageable);

        return ResponseEntity.ok(pagina.map(ReporteResponse::fromEntity));
    }

    @PostMapping("/api/reportes/{id}/aprobacion")
    @Transactional
    public ResponseEntity<ReporteResponse> aprobar(@PathVariable Long id,
                                                      @Valid @RequestBody AprobarReporteRequest request,
                                                      Authentication authentication) {

        Long idAdministrador = Long.valueOf(authentication.getName());

        Integer diasPlazoConfirmacion = request.getDiasPlazoConfirmacion() != null
                ? request.getDiasPlazoConfirmacion()
                : DIAS_PLAZO_CONFIRMACION_DEFAULT;

        reporteSolicitudRepository.aprobarReporte(id, idAdministrador, diasPlazoConfirmacion);

        ReporteSolicitud actualizado = reporteSolicitudRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException(
                        "El reporte se aprobo pero no se pudo recuperar (id=" + id + ")"));

        return ResponseEntity.ok(ReporteResponse.fromEntity(actualizado));
    }

    @PostMapping("/api/reportes/{id}/rechazo")
    @Transactional
    public ResponseEntity<ReporteResponse> rechazar(@PathVariable Long id,
                                                       @Valid @RequestBody RechazarReporteRequest request,
                                                       Authentication authentication) {

        Long idAdministrador = Long.valueOf(authentication.getName());

        reporteSolicitudRepository.rechazarReporte(id, idAdministrador, request.getComentarioRechazo());

        ReporteSolicitud actualizado = reporteSolicitudRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException(
                        "El reporte se rechazo pero no se pudo recuperar (id=" + id + ")"));

        return ResponseEntity.ok(ReporteResponse.fromEntity(actualizado));
    }
}
