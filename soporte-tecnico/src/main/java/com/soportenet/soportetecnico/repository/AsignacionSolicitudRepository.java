package com.soportenet.soportetecnico.repository;

import com.soportenet.soportetecnico.entity.AsignacionSolicitud;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AsignacionSolicitudRepository extends JpaRepository<AsignacionSolicitud, Long> {

    
    Optional<AsignacionSolicitud> findBySolicitudIdSolicitudAndVigenteTrue(Long idSolicitud);
}
