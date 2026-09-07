package com.soportenet.soportetecnico.controller;

import com.soportenet.soportetecnico.dto.AgregarMiembroRequest;
import com.soportenet.soportetecnico.dto.CrearGrupoRequest;
import com.soportenet.soportetecnico.dto.GrupoTecnicoConteoProjection;
import com.soportenet.soportetecnico.dto.UsuarioBusquedaProjection;
import com.soportenet.soportetecnico.entity.GrupoTecnico;
import com.soportenet.soportetecnico.repository.GrupoTecnicoRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;


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
    public List<GrupoTecnicoConteoProjection> listar() {
        return grupoTecnicoRepository.listarConConteo();
    }

   
    @GetMapping("/{idGrupo}/miembros")
    public List<UsuarioBusquedaProjection> listarMiembros(@PathVariable Long idGrupo) {
        return grupoTecnicoRepository.listarMiembros(idGrupo);
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
