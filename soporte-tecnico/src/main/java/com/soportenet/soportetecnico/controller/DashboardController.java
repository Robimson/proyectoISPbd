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

    
    @GetMapping("/superusuario")
    public ResponseEntity<EstadisticasSuperusuarioResponse> estadisticasSuperusuario() {
        return ResponseEntity.ok(new EstadisticasSuperusuarioResponse(
                tecnicoRepository.contarPorNivel(),
                usuarioRepository.contarPorRol()
        ));
    }
}
