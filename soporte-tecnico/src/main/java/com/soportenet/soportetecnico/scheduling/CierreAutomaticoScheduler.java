package com.soportenet.soportetecnico.scheduling;

import com.soportenet.soportetecnico.repository.SolicitudRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cierra automaticamente los tickets "Resuelta - Pendiente Confirmacion del
 * Cliente" cuyo plazo vencio sin respuesta (seccion 3.1 del documento).
 * El SQL ya lo documenta pensado para pg_cron o un scheduler externo;
 * @Scheduled de Spring cumple ese rol sin agregar una dependencia externa.
 */
@Component
public class CierreAutomaticoScheduler {

    private static final Logger log = LoggerFactory.getLogger(CierreAutomaticoScheduler.class);

    private final SolicitudRepository solicitudRepository;

    public CierreAutomaticoScheduler(SolicitudRepository solicitudRepository) {
        this.solicitudRepository = solicitudRepository;
    }

    @Scheduled(fixedRate = 15, initialDelay = 1, timeUnit = java.util.concurrent.TimeUnit.MINUTES)
    @Transactional
    public void cerrarSolicitudesVencidas() {
        Integer total = solicitudRepository.cerrarSolicitudesVencidas();
        if (total != null && total > 0) {
            log.info("Cierre automatico: {} solicitud(es) cerradas por vencimiento de confirmacion.", total);
        }
    }
}
