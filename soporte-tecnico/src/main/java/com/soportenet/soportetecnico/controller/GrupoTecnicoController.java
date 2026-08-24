package com.soportenet.soportetecnico.controller;

import com.soportenet.soportetecnico.dto.AgregarMiembroRequest;
import com.soportenet.soportetecnico.dto.CrearGrupoRequest;
import com.soportenet.soportetecnico.entity.GrupoTecnico;
import com.soportenet.soportetecnico.repository.GrupoTecnicoRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Creacion y administracion de grupos tecnicos (caso de uso 4.4.5 del
 * documento). GrupoTecnico no tiene relaciones ni datos sensibles, asi que
 * se serializa la entidad directamente (a diferencia de Solicitud/Usuario,
 * que siempre usan un DTO de salida).
 */
@RestController
@RequestMapping("/api/grupos-tecnicos")
public class GrupoTecnicoController {

    private final GrupoTecnicoRepository grupoTecnicoRepository;

    public GrupoTecnicoController(GrupoTecnicoRepository grupoTecnicoRepository) {
        this.grupoTecnicoRepository = grupoTecnicoRepository;
    }

    @PostMapping
    public ResponseEntity<GrupoTecnico> crear(@Valid @RequestBody CrearGrupoRequest request) {
        GrupoTecnico guardado = grupoTecnicoRepository.save(new GrupoTecnico(null, request.getNombreGrupo()));
        return ResponseEntity.status(HttpStatus.CREATED).body(guardado);
    }

    @GetMapping
    public List<GrupoTecnico> listar() {
        return grupoTecnicoRepository.findAll();
    }

    @PostMapping("/{idGrupo}/miembros")
    @Transactional
    public ResponseEntity<Void> agregarMiembro(@PathVariable Long idGrupo,
                                                @Valid @RequestBody AgregarMiembroRequest request) {
        grupoTecnicoRepository.agregarMiembro(request.getIdTecnico(), idGrupo);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{idGrupo}/miembros/{idTecnico}")
    @Transactional
    public ResponseEntity<Void> retirarMiembro(@PathVariable Long idGrupo, @PathVariable Long idTecnico) {
        grupoTecnicoRepository.retirarMiembro(idTecnico, idGrupo);
        return ResponseEntity.noContent().build();
    }
}
