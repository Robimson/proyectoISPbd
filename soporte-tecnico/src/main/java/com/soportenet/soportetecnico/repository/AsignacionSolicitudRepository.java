package com.soportenet.soportetecnico.repository;

import com.soportenet.soportetecnico.entity.AsignacionSolicitud;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AsignacionSolicitudRepository extends JpaRepository<AsignacionSolicitud, Long> {

    /**
     * La asignacion vigente de una solicitud (quien la tiene ahora mismo),
     * si tiene alguna. Query derivada de Spring Data - sin SQL escrito a
     * mano, igual que AdjuntoRepository.findBySolicitudIdSolicitudOrderByFechaSubidaAsc.
     */
    Optional<AsignacionSolicitud> findBySolicitudIdSolicitudAndVigenteTrue(Long idSolicitud);
}
