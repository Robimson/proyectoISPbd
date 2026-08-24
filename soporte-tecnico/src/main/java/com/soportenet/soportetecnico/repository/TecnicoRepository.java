package com.soportenet.soportetecnico.repository;

import com.soportenet.soportetecnico.entity.Tecnico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TecnicoRepository extends JpaRepository<Tecnico, Long> {

    List<Tecnico> findByHabilitado(Boolean habilitado);
}
