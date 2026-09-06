package com.soportenet.soportetecnico.email;

import com.soportenet.soportetecnico.dto.ConfiguracionProjection;
import com.soportenet.soportetecnico.repository.ConfiguracionSistemaRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Envio de correos reales (invitaciones y notificaciones, seccion 8 del
 * documento). Nunca lanza excepcion hacia quien la llama: un correo que
 * falla se registra en el log, pero no debe tumbar la operacion principal
 * (invitar un usuario, asignar un ticket, etc. ya se hicieron en la base de
 * datos antes de intentar el envio).
 *
 * Manda HTML con la marca del negocio (nombre y color de
 * configuracion_sistema - la misma que ya personaliza el resto del sitio),
 * no texto plano. El cuerpo que pasa quien llama sigue siendo texto plano
 * con saltos de linea normales ("\n\n" entre parrafos) - esta clase se
 * encarga de escaparlo y convertirlo a HTML, para no tener que tocar cada
 * lugar que arma un mensaje.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final String COLOR_DEFAULT = "#0d9488";
    private static final String NOMBRE_NEGOCIO_DEFAULT = "SoporteNet";

    private final JavaMailSender mailSender;
    private final String remitente;
    private final ConfiguracionSistemaRepository configuracionSistemaRepository;

    public EmailService(JavaMailSender mailSender,
                         @Value("${spring.mail.username}") String remitente,
                         ConfiguracionSistemaRepository configuracionSistemaRepository) {
        this.mailSender = mailSender;
        this.remitente = remitente;
        this.configuracionSistemaRepository = configuracionSistemaRepository;
    }

    /** Correo simple, sin boton de accion - avisos y notificaciones. */
    public void enviar(String destinatario, String asunto, String cuerpo) {
        enviar(destinatario, asunto, cuerpo, null, null);
    }

    /**
     * Correo con un boton de accion (enlace + texto del boton) - para
     * invitacion y recuperacion de contrasena, que necesitan que el usuario
     * haga clic para continuar.
     */
    public void enviar(String destinatario, String asunto, String cuerpo, String enlace, String textoBoton) {
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, false, "UTF-8");
            helper.setFrom(remitente);
            helper.setTo(destinatario);
            helper.setSubject(asunto);
            helper.setText(armarHtml(asunto, cuerpo, enlace, textoBoton), true);
            mailSender.send(mensaje);
        } catch (MessagingException | MailException error) {
            log.error("No se pudo enviar el correo a {}: {}", destinatario, error.getMessage());
        }
    }

    /**
     * Arma el HTML final envolviendo el cuerpo en la plantilla con marca. Si
     * no se puede leer la configuracion del negocio (la base no responde,
     * todavia no hay fila, etc.) cae a los valores por defecto - un correo
     * con branding generico es preferible a que el correo no salga.
     */
    private String armarHtml(String asunto, String cuerpo, String enlace, String textoBoton) {
        String nombreNegocio = NOMBRE_NEGOCIO_DEFAULT;
        String eslogan = "";
        String color = COLOR_DEFAULT;
        try {
            ConfiguracionProjection config = configuracionSistemaRepository.obtener();
            if (config != null) {
                if (config.getNombreNegocio() != null) nombreNegocio = config.getNombreNegocio();
                if (config.getEslogan() != null) eslogan = config.getEslogan();
                if (config.getColorPrimario() != null) color = config.getColorPrimario();
            }
        } catch (Exception error) {
            log.warn("No se pudo leer la configuracion del sistema para el correo, se usa la marca por defecto: {}", error.getMessage());
        }

        String cuerpoHtml = escaparYConSaltos(cuerpo);

        String botonHtml = "";
        if (enlace != null && textoBoton != null) {
            botonHtml =
                "<tr><td style=\"padding: 8px 32px 0;\">" +
                "<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\"><tr><td " +
                "style=\"background-color:" + color + "; border-radius:8px;\">" +
                "<a href=\"" + enlace + "\" target=\"_blank\" " +
                "style=\"display:inline-block; padding:12px 26px; color:#ffffff; text-decoration:none; " +
                "font-family:Arial,Helvetica,sans-serif; font-size:14px; font-weight:bold;\">" +
                escaparHtml(textoBoton) + "</a></td></tr></table></td></tr>";
        }

        return
            "<!DOCTYPE html><html><body style=\"margin:0; padding:0; background-color:#f1f5f9;\">" +
            "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" " +
            "style=\"background-color:#f1f5f9; padding: 32px 16px;\"><tr><td align=\"center\">" +
            "<table role=\"presentation\" width=\"480\" cellpadding=\"0\" cellspacing=\"0\" " +
            "style=\"background-color:#ffffff; border-radius:12px; overflow:hidden; font-family:Arial,Helvetica,sans-serif;\">" +
            "<tr><td style=\"background-color:" + color + "; padding: 22px 32px;\">" +
            "<span style=\"color:#ffffff; font-size:19px; font-weight:bold;\">" + escaparHtml(nombreNegocio) + "</span>" +
            "</td></tr>" +
            "<tr><td style=\"padding: 32px 32px 8px;\">" +
            "<h1 style=\"margin:0 0 16px; font-size:17px; color:#0f172a;\">" + escaparHtml(asunto) + "</h1>" +
            "<div style=\"font-size:14px; line-height:1.6; color:#334155;\">" + cuerpoHtml + "</div>" +
            "</td></tr>" +
            botonHtml +
            "<tr><td style=\"padding: 24px 32px 20px;\"><div style=\"border-top:1px solid #e2e8f0; padding-top:16px;\">" +
            "<span style=\"font-size:12px; color:#94a3b8;\">" + escaparHtml(eslogan) + "</span><br>" +
            "<span style=\"font-size:11px; color:#cbd5e1;\">Este es un mensaje automatico, no respondas a este correo.</span>" +
            "</div></td></tr>" +
            "</table></td></tr></table></body></html>";
    }

    private String escaparHtml(String texto) {
        if (texto == null) return "";
        return texto
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    /** Escapa el texto y convierte los saltos de linea en <br> - el cuerpo sigue siendo texto plano para quien llama. */
    private String escaparYConSaltos(String texto) {
        return escaparHtml(texto).replace("\n", "<br>");
    }
}
