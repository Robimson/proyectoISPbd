package com.soportenet.soportetecnico.controller;

import com.soportenet.soportetecnico.dto.ActivarCuentaRequest;
import com.soportenet.soportetecnico.dto.LoginRequest;
import com.soportenet.soportetecnico.dto.LoginResponse;
import com.soportenet.soportetecnico.dto.LogoutRequest;
import com.soportenet.soportetecnico.dto.SolicitarRecuperacionRequest;
import com.soportenet.soportetecnico.email.EmailService;
import com.soportenet.soportetecnico.entity.Usuario;
import com.soportenet.soportetecnico.enums.EstadoCuenta;
import com.soportenet.soportetecnico.enums.RolUsuario;
import com.soportenet.soportetecnico.repository.AuditoriaSesionRepository;
import com.soportenet.soportetecnico.repository.ClienteRepository;
import com.soportenet.soportetecnico.repository.UsuarioRepository;
import com.soportenet.soportetecnico.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

/**
 * Login (caso de uso 4.1.2 del documento). Como el JWT no tiene estado en el
 * servidor, "cerrar sesion" real (invalidar el token) sigue siendo
 * responsabilidad del cliente (descartarlo) - /logout aqui solo cierra el
 * registro de auditoria_sesion (fecha_salida), no invalida el token.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final int HORAS_VALIDEZ_TOKEN_RECUPERACION = 2;
    private static final String MENSAJE_RECUPERACION_GENERICO =
            "Si el correo esta registrado, te llegara un enlace para restablecer tu contrasena.";

    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final AuditoriaSesionRepository auditoriaSesionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final String urlFrontend;

    public AuthController(UsuarioRepository usuarioRepository, ClienteRepository clienteRepository,
                           AuditoriaSesionRepository auditoriaSesionRepository, PasswordEncoder passwordEncoder,
                           JwtService jwtService, EmailService emailService,
                           @Value("${app.frontend.url}") String urlFrontend) {
        this.usuarioRepository = usuarioRepository;
        this.clienteRepository = clienteRepository;
        this.auditoriaSesionRepository = auditoriaSesionRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.emailService = emailService;
        this.urlFrontend = urlFrontend;
    }

    @PostMapping("/login")
    @Transactional
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {

        Optional<Usuario> usuarioOpt = usuarioRepository.findByCorreo(request.getCorreo());

        boolean credencialesValidas = usuarioOpt.isPresent()
                && usuarioOpt.get().getContrasenaHash() != null
                && passwordEncoder.matches(request.getContrasena(), usuarioOpt.get().getContrasenaHash())
                && usuarioOpt.get().getEstadoCuenta() == EstadoCuenta.activo;

        if (!credencialesValidas) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Correo, contrasena o estado de cuenta invalidos."));
        }

        Usuario usuario = usuarioOpt.get();
        String token = jwtService.generarToken(usuario.getIdUsuario(), usuario.getRol().name(), usuario.getCorreo());

        // Si es cliente, se manda su estado de pago para que el frontend
        // pueda mostrar el aviso apenas entra (seccion 7.4: solo
        // informativo, nunca bloquea el login ni nada mas).
        String estadoPago = null;
        if (usuario.getRol() == RolUsuario.cliente) {
            estadoPago = clienteRepository.findById(usuario.getIdUsuario())
                    .map(cliente -> cliente.getEstadoPago().name())
                    .orElse(null);
        }

        // Auditoria de sesiones (seccion 11): queda registrada la entrada y
        // desde donde, y el id_sesion vuelve al frontend para poder cerrar
        // el registro con /logout mas adelante.
        Long idSesion = auditoriaSesionRepository.abrirSesion(usuario.getIdUsuario(), httpRequest.getRemoteAddr());

        // El enum RolUsuario esta declarado en minusculas (coincide con el
        // ENUM de Postgres), pero el frontend y hasRole(...) del backend
        // trabajan con el rol en mayusculas (ROLE_SUPERUSUARIO, etc.) - se
        // normaliza aqui para que el frontend no tenga que adivinar el caso.
        return ResponseEntity.ok(new LoginResponse(
                token, usuario.getIdUsuario(), usuario.getRol().name().toUpperCase(), estadoPago, idSesion));
    }

    /**
     * Cierra el registro de auditoria_sesion (fecha_salida). No invalida el
     * JWT (sigue siendo valido hasta que expire por su cuenta) - es solo
     * para que la pantalla de auditoria del Superusuario refleje cuando
     * alguien cerro sesion desde la app.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody(required = false) LogoutRequest request) {
        if (request != null && request.getIdSesion() != null) {
            auditoriaSesionRepository.cerrarSesion(request.getIdSesion());
        }
        return ResponseEntity.noContent().build();
    }

    /**
     * "Olvide mi contrasena". La respuesta es siempre el mismo mensaje
     * generico, exista o no ese correo en el sistema (evita que el
     * formulario se use para averiguar que correos estan registrados).
     */
    @PostMapping("/recuperacion")
    @Transactional
    public ResponseEntity<Map<String, String>> solicitarRecuperacion(
            @Valid @RequestBody SolicitarRecuperacionRequest request) {

        String token = usuarioRepository.solicitarRecuperacion(
                request.getCorreo(), HORAS_VALIDEZ_TOKEN_RECUPERACION);

        if (token != null) {
            String enlace = urlFrontend + "/restablecer.html?token=" + token;
            emailService.enviar(
                    request.getCorreo(),
                    "Recupera tu contraseña",
                    "Pediste restablecer tu contraseña. Este enlace vence en "
                            + HORAS_VALIDEZ_TOKEN_RECUPERACION + " hora(s).\n\n" +
                            "Si el botón no funciona, entrá a " + urlFrontend + "/restablecer.html y pegá este token:\n" +
                            token + "\n\n" +
                            "Si no fuiste vos quien lo pidió, ignorá este correo.",
                    enlace,
                    "Restablecer contraseña"
            );
        }

        return ResponseEntity.ok(Map.of("mensaje", MENSAJE_RECUPERACION_GENERICO));
    }

    /**
     * Segunda mitad de "olvide mi contrasena": define la contrasena nueva a
     * partir del token que llego por correo. Reutiliza ActivarCuentaRequest
     * porque la forma es identica (token + contrasena con la misma
     * validacion minima).
     */
    @PostMapping("/restablecer")
    @Transactional
    public ResponseEntity<Map<String, String>> restablecer(@Valid @RequestBody ActivarCuentaRequest request) {

        String hash = passwordEncoder.encode(request.getContrasena());
        usuarioRepository.restablecerContrasena(request.getToken(), hash);

        return ResponseEntity.ok(Map.of("mensaje", "Contrasena restablecida correctamente."));
    }
}
