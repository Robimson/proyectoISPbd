package com.soportenet.soportetecnico.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Envio de correos reales (invitaciones y notificaciones, seccion 8 del
 * documento). Nunca lanza excepcion hacia quien la llama: un correo que
 * falla se registra en el log, pero no debe tumbar la operacion principal
 * (invitar un usuario, asignar un ticket, etc. ya se hicieron en la base de
 * datos antes de intentar el envio).
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final String remitente;

    public EmailService(JavaMailSender mailSender, @Value("${spring.mail.username}") String remitente) {
        this.mailSender = mailSender;
        this.remitente = remitente;
    }

    public void enviar(String destinatario, String asunto, String cuerpo) {
        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setFrom(remitente);
            mensaje.setTo(destinatario);
            mensaje.setSubject(asunto);
            mensaje.setText(cuerpo);
            mailSender.send(mensaje);
        } catch (MailException error) {
            log.error("No se pudo enviar el correo a {}: {}", destinatario, error.getMessage());
        }
    }
}
