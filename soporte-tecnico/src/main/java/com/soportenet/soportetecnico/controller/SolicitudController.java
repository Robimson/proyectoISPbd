package com.soportenet.soportetecnico.controller;

import com.soportenet.soportetecnico.dto.ConfirmarClienteRequest;
import com.soportenet.soportetecnico.dto.CrearSolicitudRequest;
import com.soportenet.soportetecnico.dto.EstadisticasClienteResponse;
import com.soportenet.soportetecnico.dto.EstadisticasTecnicoResponse;
import com.soportenet.soportetecnico.dto.ReporteResponse;
import com.soportenet.soportetecnico.dto.ResumenTecnicoProjection;
import com.soportenet.soportetecnico.dto.SolicitudDetalleResponse;
import com.soportenet.soportetecnico.dto.SolicitudResponse;
import com.soportenet.soportetecnico.entity.AsignacionSolicitud;
import com.soportenet.soportetecnico.entity.Solicitud;
import com.soportenet.soportetecnico.repository.AsignacionSolicitudRepository;
import com.soportenet.soportetecnico.repository.ReporteSolicitudRepository;
import com.soportenet.soportetecnico.repository.SolicitudRepository;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/solicitudes")
public class SolicitudController {

    private final SolicitudRepository solicitudRepository;
    private final AsignacionSolicitudRepository asignacionSolicitudRepository;
    private final ReporteSolicitudRepository reporteSolicitudRepository;

    public SolicitudController(SolicitudRepository solicitudRepository,
                                AsignacionSolicitudRepository asignacionSolicitudRepository,
                                ReporteSolicitudRepository reporteSolicitudRepository) {
        this.solicitudRepository = solicitudRepository;
        this.asignacionSolicitudRepository = asignacionSolicitudRepository;
        this.reporteSolicitudRepository = reporteSolicitudRepository;
    }

    /** Cliente crea una solicitud; la logica vive en sp_crear_solicitud. idCliente sale del JWT. */
    @PostMapping
    @Transactional
    public ResponseEntity<SolicitudResponse> crear(
            @Valid @RequestBody CrearSolicitudRequest request,
            Authentication authentication) {

        Long idCliente = Long.valueOf(authentication.getName());

        Long idSolicitud = solicitudRepository.crearSolicitud(
                idCliente,
                request.getDescripcion(),
                request.getIdCategoria(),
                request.getDireccion()
        );

        Solicitud creada = solicitudRepository.findById(idSolicitud)
                .orElseThrow(() -> new IllegalStateException(
                        "La solicitud se creo pero no se pudo recuperar (id="
                                + idSolicitud + ")"));

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(SolicitudResponse.fromEntity(creada));
    }

    /** Detalle de una solicitud: admin/superusuario ven cualquiera, cliente solo la suya, tecnico solo si tiene acceso. */
    @GetMapping("/{id}")
    public ResponseEntity<SolicitudDetalleResponse> obtener(
            @PathVariable Long id,
            Authentication authentication) {

        Solicitud solicitud = solicitudRepository.findById(id)
                .orElse(null);

        if (solicitud == null) {
            return ResponseEntity.notFound().build();
        }

        if (tieneRol(authentication, "ADMINISTRADOR")
                || tieneRol(authentication, "SUPERUSUARIO")) {

            return ResponseEntity.ok(detalleDe(solicitud));
        }

        Long idUsuario = Long.valueOf(authentication.getName());

        if (tieneRol(authentication, "CLIENTE")) {

            if (solicitud.getCliente() != null
                    && solicitud.getCliente().getIdUsuario() != null
                    && solicitud.getCliente()
                    .getIdUsuario()
                    .equals(idUsuario)) {

                return ResponseEntity.ok(detalleDe(solicitud));
            }

            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .build();
        }

        if (tieneRol(authentication, "TECNICO")) {

            boolean tieneAcceso =
                    solicitudRepository.tecnicoTieneAcceso(id, idUsuario);

            if (tieneAcceso) {
                return ResponseEntity.ok(detalleDe(solicitud));
            }

            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .build();
        }

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .build();
    }

    /** Arma el detalle: entidad + asignacion vigente + historial de reportes. Adjuntos van aparte (GET .../adjuntos). */
    private SolicitudDetalleResponse detalleDe(Solicitud solicitud) {
        AsignacionSolicitud asignacionVigente = asignacionSolicitudRepository
                .findBySolicitudIdSolicitudAndVigenteTrue(solicitud.getIdSolicitud())
                .orElse(null);

        List<ReporteResponse> reportes = reporteSolicitudRepository
                .findBySolicitudIdSolicitudOrderByFechaEnvioDesc(solicitud.getIdSolicitud())
                .stream()
                .map(ReporteResponse::fromEntity)
                .collect(Collectors.toList());

        return SolicitudDetalleResponse.construir(solicitud, asignacionVigente, reportes);
    }

    /**
     * Cliente:
     * lista sus propias solicitudes.
     *
     * Administrador/Superusuario:
     * lista todas las solicitudes.
     *
     * Se puede filtrar opcionalmente por estado.
     */
    @GetMapping
    public ResponseEntity<Page<SolicitudResponse>> listar(
            @RequestParam(required = false) String estado,
            @PageableDefault(
                    size = 20,
                    sort = "fechaCreacion",
                    direction = Sort.Direction.DESC
            )
            Pageable pageable,
            Authentication authentication) {

        Page<Solicitud> pagina;

        if (tieneRol(authentication, "ADMINISTRADOR")
                || tieneRol(authentication, "SUPERUSUARIO")) {

            // El admin ve una cola de trabajo, no un feed cronologico - ver
            // el comentario en SolicitudRepository.findTodasOrdenadoParaAdmin.
            // El orden ya queda fijo en esa consulta nativa, asi que aca se
            // ignora el sort que trae el Pageable (el de @PageableDefault
            // abajo es para el Cliente) y solo se reusan pagina/tamano.
            Pageable paginaSinOrden = PageRequest.of(
                    pageable.getPageNumber(), pageable.getPageSize());

            pagina = (estado != null)
                    ? solicitudRepository
                    .findPorEstadoOrdenadoParaAdmin(estado, paginaSinOrden)
                    : solicitudRepository.findTodasOrdenadoParaAdmin(paginaSinOrden);

        } else {

            Long idCliente =
                    Long.valueOf(authentication.getName());

            pagina = (estado != null)
                    ? solicitudRepository
                    .findByClienteIdUsuarioAndEstadoNombreEstado(
                            idCliente,
                            estado,
                            pageable
                    )
                    : solicitudRepository
                    .findByClienteIdUsuario(
                            idCliente,
                            pageable
                    );
        }

        return ResponseEntity.ok(
                pagina.map(SolicitudResponse::fromEntity)
        );
    }

    /**
     * Tecnico:
     * lista las solicitudes asignadas directamente al técnico
     * o a alguno de sus grupos. Se puede filtrar opcionalmente por estado.
     */
    @GetMapping("/mis-tareas")
    public ResponseEntity<Page<SolicitudResponse>> misTareas(
            @RequestParam(required = false) String estado,
            @PageableDefault(size = 20) Pageable pageable,
            Authentication authentication) {

        Long idTecnico =
                Long.valueOf(authentication.getName());

        Page<Solicitud> pagina =
                solicitudRepository.findMisTareas(
                        idTecnico,
                        estado,
                        pageable
                );

        return ResponseEntity.ok(
                pagina.map(SolicitudResponse::fromEntity)
        );
    }

    /**
     * Tecnico: conteos para la pestana "Resumen" de su panel (en proceso,
     * pendiente aprobacion, resueltas hoy, total cerradas).
     */
    @GetMapping("/mis-tareas/resumen")
    public ResponseEntity<ResumenTecnicoProjection> resumenMisTareas(
            Authentication authentication) {

        Long idTecnico = Long.valueOf(authentication.getName());

        return ResponseEntity.ok(
                solicitudRepository.resumenTecnico(idTecnico)
        );
    }

    /**
     * Cliente: datos para los graficos de su "Resumen" (sus solicitudes por
     * estado y por categoria).
     */
    @GetMapping("/mis-estadisticas")
    public ResponseEntity<EstadisticasClienteResponse> misEstadisticas(
            Authentication authentication) {

        Long idCliente = Long.valueOf(authentication.getName());

        return ResponseEntity.ok(new EstadisticasClienteResponse(
                solicitudRepository.contarPorEstadoCliente(idCliente),
                solicitudRepository.contarPorCategoriaCliente(idCliente)
        ));
    }

    /**
     * Tecnico: datos para los graficos de su "Resumen" (sus tareas vigentes
     * por prioridad y su propia tasa de aprobacion de reportes).
     */
    @GetMapping("/mis-tareas/estadisticas")
    public ResponseEntity<EstadisticasTecnicoResponse> misTareasEstadisticas(
            Authentication authentication) {

        Long idTecnico = Long.valueOf(authentication.getName());

        return ResponseEntity.ok(new EstadisticasTecnicoResponse(
                solicitudRepository.contarPorPrioridadTecnico(idTecnico),
                reporteSolicitudRepository.contarAprobacionTecnico(idTecnico)
        ));
    }

    /**
     * Cliente confirma o rechaza la solución del ticket.
     *
     * Si confirma:
     * el ticket puede cerrarse.
     *
     * Si indica que el problema persiste:
     * la solicitud vuelve a proceso.
     */
    @PostMapping("/{id}/confirmacion")
    @Transactional
    public ResponseEntity<SolicitudResponse> confirmar(
            @PathVariable Long id,
            @Valid @RequestBody ConfirmarClienteRequest request,
            Authentication authentication) {

        Long idCliente =
                Long.valueOf(authentication.getName());

        solicitudRepository.confirmarCliente(
                id,
                idCliente,
                request.getProblemaResuelto()
        );

        Solicitud actualizada =
                solicitudRepository.findById(id)
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "La solicitud se confirmo "
                                                + "pero no se pudo recuperar "
                                                + "(id=" + id + ")"
                                )
                        );

        return ResponseEntity.ok(
                SolicitudResponse.fromEntity(actualizada)
        );
    }

    /**
     * Administrador reabre administrativamente un ticket Cerrada, que
     * vuelve a "En Proceso". La validacion de que quien pide esto es un
     * administrador activo y de que el ticket realmente este Cerrada vive
     * en sp_reabrir_ticket_cerrado_administrativo.
     */
    @PostMapping("/{id}/reapertura")
    @Transactional
    public ResponseEntity<SolicitudResponse> reabrir(
            @PathVariable Long id,
            Authentication authentication) {

        Long idAdministrador = Long.valueOf(authentication.getName());

        solicitudRepository.reabrirTicketCerrado(id, idAdministrador);

        Solicitud actualizada =
                solicitudRepository.findById(id)
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "El ticket se reabrio "
                                                + "pero no se pudo recuperar "
                                                + "(id=" + id + ")"
                                )
                        );

        return ResponseEntity.ok(
                SolicitudResponse.fromEntity(actualizada)
        );
    }

    /**
     * Comprueba si el usuario autenticado posee un rol determinado.
     */
    private boolean tieneRol(
            Authentication authentication,
            String rol) {

        String authority = "ROLE_" + rol;

        for (GrantedAuthority ga :
                authentication.getAuthorities()) {

            if (ga.getAuthority().equals(authority)) {
                return true;
            }
        }

        return false;
    }
}