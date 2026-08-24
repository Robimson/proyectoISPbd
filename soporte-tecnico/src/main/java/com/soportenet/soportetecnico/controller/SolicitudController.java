package com.soportenet.soportetecnico.controller;

import com.soportenet.soportetecnico.dto.ConfirmarClienteRequest;
import com.soportenet.soportetecnico.dto.CrearSolicitudRequest;
import com.soportenet.soportetecnico.dto.SolicitudResponse;
import com.soportenet.soportetecnico.entity.Solicitud;
import com.soportenet.soportetecnico.repository.SolicitudRepository;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/solicitudes")
public class SolicitudController {

    private final SolicitudRepository solicitudRepository;

    public SolicitudController(SolicitudRepository solicitudRepository) {
        this.solicitudRepository = solicitudRepository;
    }

    /**
     * Cliente crea una nueva solicitud de soporte.
     * Toda la logica de negocio vive en PostgreSQL mediante
     * sp_crear_solicitud.
     *
     * El idCliente se obtiene del JWT y no del body.
     */
    @PostMapping
    @Transactional
    public ResponseEntity<SolicitudResponse> crear(
            @Valid @RequestBody CrearSolicitudRequest request,
            Authentication authentication) {

        Long idCliente = Long.valueOf(authentication.getName());

        Long idSolicitud = solicitudRepository.crearSolicitud(
                idCliente,
                request.getDescripcion(),
                request.getIdCategoria()
        );

        Solicitud creada = solicitudRepository.findById(idSolicitud)
                .orElseThrow(() -> new IllegalStateException(
                        "La solicitud se creo pero no se pudo recuperar (id="
                                + idSolicitud + ")"));

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(SolicitudResponse.fromEntity(creada));
    }

    /**
     * Consulta el detalle de una solicitud.
     *
     * ADMINISTRADOR y SUPERUSUARIO:
     * pueden consultar cualquier solicitud.
     *
     * CLIENTE:
     * solamente puede consultar sus propias solicitudes.
     *
     * TECNICO:
     * temporalmente se valida en un siguiente paso según
     * asignación directa o grupo técnico.
     */
    @GetMapping("/{id}")
    public ResponseEntity<SolicitudResponse> obtener(
            @PathVariable Long id,
            Authentication authentication) {

        Solicitud solicitud = solicitudRepository.findById(id)
                .orElse(null);

        if (solicitud == null) {
            return ResponseEntity.notFound().build();
        }

        /*
         * Administrador y Superusuario pueden consultar
         * cualquier solicitud.
         */
        if (tieneRol(authentication, "ADMINISTRADOR")
                || tieneRol(authentication, "SUPERUSUARIO")) {

            return ResponseEntity.ok(
                    SolicitudResponse.fromEntity(solicitud)
            );
        }

        Long idUsuario = Long.valueOf(authentication.getName());

        /*
         * Cliente:
         * solamente puede consultar solicitudes que le pertenecen.
         */
        if (tieneRol(authentication, "CLIENTE")) {

            if (solicitud.getCliente() != null
                    && solicitud.getCliente().getIdUsuario() != null
                    && solicitud.getCliente()
                    .getIdUsuario()
                    .equals(idUsuario)) {

                return ResponseEntity.ok(
                        SolicitudResponse.fromEntity(solicitud)
                );
            }

            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .build();
        }

        /*
         * Tecnico:
         * en el siguiente paso comprobaremos si la solicitud
         * esta asignada directamente al tecnico o a uno de
         * los grupos a los que pertenece.
         */
        if (tieneRol(authentication, "TECNICO")) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .build();
        }

        /*
         * Cualquier otro rol no autorizado.
         */
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .build();
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

            pagina = (estado != null)
                    ? solicitudRepository
                    .findByEstadoNombreEstado(estado, pageable)
                    : solicitudRepository.findAll(pageable);

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
     * o a alguno de sus grupos.
     */
    @GetMapping("/mis-tareas")
    public ResponseEntity<Page<SolicitudResponse>> misTareas(
            @PageableDefault(size = 20) Pageable pageable,
            Authentication authentication) {

        Long idTecnico =
                Long.valueOf(authentication.getName());

        Page<Solicitud> pagina =
                solicitudRepository.findMisTareas(
                        idTecnico,
                        pageable
                );

        return ResponseEntity.ok(
                pagina.map(SolicitudResponse::fromEntity)
        );
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