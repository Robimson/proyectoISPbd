package com.soportenet.soportetecnico.controller;

import com.soportenet.soportetecnico.dto.AsignarSolicitudRequest;
import com.soportenet.soportetecnico.dto.SolicitudResponse;
import com.soportenet.soportetecnico.entity.Solicitud;
import com.soportenet.soportetecnico.repository.SolicitudRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Asignacion y reasignacion de una solicitud a un tecnico o a un grupo
 * tecnico (caso de uso 4.3.4 del documento). Toda la logica de negocio
 * (administrador activo, exactamente tecnico o grupo, motivo obligatorio en
 * reasignaciones, bloqueo si esta Cerrada, notificacion al tecnico) vive en
 * sp_asignar_solicitud dentro de PostgreSQL; este endpoint solo la invoca y
 * devuelve la solicitud con su estado y prioridad ya actualizados.
 */
@RestController
@RequestMapping("/api/solicitudes")
public class AsignacionSolicitudController {

    private final SolicitudRepository solicitudRepository;

    public AsignacionSolicitudController(SolicitudRepository solicitudRepository) {
        this.solicitudRepository = solicitudRepository;
    }

    @PostMapping("/{id}/asignaciones")
    @Transactional
    public ResponseEntity<SolicitudResponse> asignar(@PathVariable Long id,
                                                       @Valid @RequestBody AsignarSolicitudRequest request,
                                                       Authentication authentication) {

        Long idAdministrador = Long.valueOf(authentication.getName());

        solicitudRepository.asignarSolicitud(
                id,
                idAdministrador,
                request.getIdTecnico(),
                request.getIdGrupo(),
                request.getIdPrioridad(),
                request.getMotivoReasignacion()
        );

        Solicitud actualizada = solicitudRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException(
                        "La solicitud se asigno pero no se pudo recuperar (id=" + id + ")"));

        return ResponseEntity.ok(SolicitudResponse.fromEntity(actualizada));
    }
}
