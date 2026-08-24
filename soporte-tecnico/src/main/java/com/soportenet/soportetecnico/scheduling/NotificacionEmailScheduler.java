package com.soportenet.soportetecnico.scheduling;

import com.soportenet.soportetecnico.email.EmailService;
import com.soportenet.soportetecnico.entity.Notificacion;
import com.soportenet.soportetecnico.entity.Usuario;
import com.soportenet.soportetecnico.repository.NotificacionRepository;
import com.soportenet.soportetecnico.repository.UsuarioRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Manda por correo cada notificacion pendiente (seccion 8 del documento:
 * ticket asignado, reasignado, reporte aprobado/rechazado, resuelto
 * pendiente confirmacion, reabierto). Los procedimientos SQL ya insertan la
 * fila en `notificacion`; este scheduler solo la recoge y la envia, para no
 * tener que tocar cada procedimiento con logica de correo.
 */
@Component
public class NotificacionEmailScheduler {

    private static final Map<String, String> ASUNTOS = Map.of(
            "ticket_asignado", "Se te asigno una solicitud",
            "ticket_reasignado", "Se te reasigno una solicitud",
            "reporte_rechazado", "Tu reporte fue rechazado",
            "ticket_resuelto_pendiente_confirmacion", "Tu solicitud fue resuelta - confirma la solucion",
            "ticket_reabierto", "Una solicitud fue reabierta"
    );
    private static final String ASUNTO_DEFAULT = "Notificacion de SoporteNet";

    private final NotificacionRepository notificacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final EmailService emailService;

    public NotificacionEmailScheduler(NotificacionRepository notificacionRepository,
                                       UsuarioRepository usuarioRepository,
                                       EmailService emailService) {
        this.notificacionRepository = notificacionRepository;
        this.usuarioRepository = usuarioRepository;
        this.emailService = emailService;
    }

    @Scheduled(fixedRate = 60, initialDelay = 15, timeUnit = TimeUnit.SECONDS)
    @Transactional
    public void enviarPendientes() {
        List<Notificacion> pendientes = notificacionRepository.findByCorreoEnviadoFalse();

        for (Notificacion notificacion : pendientes) {
            usuarioRepository.findById(notificacion.getIdUsuarioDestino())
                    .ifPresent(usuario -> enviarYMarcar(notificacion, usuario));
        }
    }

    private void enviarYMarcar(Notificacion notificacion, Usuario usuario) {
        String asunto = ASUNTOS.getOrDefault(notificacion.getTipoEvento(), ASUNTO_DEFAULT);
        emailService.enviar(usuario.getCorreo(), asunto, notificacion.getMensaje());

        notificacion.setCorreoEnviado(true);
        notificacionRepository.save(notificacion);
    }
}
