package com.soportenet.soportetecnico.controller;

import com.soportenet.soportetecnico.dto.CambiarEstadoPagoRequest;
import com.soportenet.soportetecnico.dto.ClienteResponse;
import com.soportenet.soportetecnico.email.EmailService;
import com.soportenet.soportetecnico.entity.Cliente;
import com.soportenet.soportetecnico.enums.EstadoPago;
import com.soportenet.soportetecnico.repository.ClienteRepository;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/**
 * Estado de pago del cliente (seccion 7.4 del documento): uso unicamente
 * informativo para que el Administrador lo tenga en cuenta al priorizar -
 * nunca bloquea ni cierra solicitudes automaticamente. Si se marca como
 * moroso, se le avisa por correo (no se suspende nada por si solo).
 */
@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    private final ClienteRepository clienteRepository;
    private final EmailService emailService;

    public ClienteController(ClienteRepository clienteRepository, EmailService emailService) {
        this.clienteRepository = clienteRepository;
        this.emailService = emailService;
    }

    @GetMapping
    public ResponseEntity<Page<ClienteResponse>> listar(@PageableDefault(size = 20) Pageable pageable) {
        Page<Cliente> pagina = clienteRepository.findAll(pageable);
        return ResponseEntity.ok(pagina.map(ClienteResponse::fromEntity));
    }

    /**
     * Cliente: sus propios datos, usados para precargar la direccion en el
     * formulario de "Nueva solicitud" con la ultima que uso (sp_crear_solicitud
     * la va actualizando en cada ticket). idCliente sale del JWT, nunca de un
     * path variable, asi que un cliente nunca puede pedir el perfil de otro.
     */
    @GetMapping("/mi-perfil")
    public ResponseEntity<ClienteResponse> miPerfil(Authentication authentication) {
        Long idCliente = Long.valueOf(authentication.getName());

        Cliente cliente = clienteRepository.findById(idCliente)
                .orElseThrow(() -> new IllegalStateException(
                        "Cliente autenticado no encontrado (id=" + idCliente + ")"));

        return ResponseEntity.ok(ClienteResponse.fromEntity(cliente));
    }

    @PostMapping("/{id}/estado-pago")
    @Transactional
    public ResponseEntity<ClienteResponse> cambiarEstadoPago(@PathVariable Long id,
                                                               @Valid @RequestBody CambiarEstadoPagoRequest request,
                                                               Authentication authentication) {

        Long idAdministrador = Long.valueOf(authentication.getName());

        clienteRepository.cambiarEstadoPago(idAdministrador, id, request.getEstadoPago().name());

        Cliente actualizado = clienteRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException(
                        "El cliente cambio de estado de pago pero no se pudo recuperar (id=" + id + ")"));

        if (request.getEstadoPago() == EstadoPago.moroso && actualizado.getUsuario() != null) {
            emailService.enviar(
                    actualizado.getUsuario().getCorreo(),
                    "Aviso de pago pendiente",
                    "Hola " + actualizado.getUsuario().getNombreUsuario() + ",\n\n" +
                            "Tu cuenta en SoporteNet quedo marcada como pendiente de pago. " +
                            "Por favor regulariza tu pago para evitar inconvenientes con tu servicio."
            );
        }

        return ResponseEntity.ok(ClienteResponse.fromEntity(actualizado));
    }
}
