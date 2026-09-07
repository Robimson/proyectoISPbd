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
