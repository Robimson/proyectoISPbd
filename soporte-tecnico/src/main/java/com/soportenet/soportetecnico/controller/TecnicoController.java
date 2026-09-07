package com.soportenet.soportetecnico.controller;

import com.soportenet.soportetecnico.dto.EditarPerfilTecnicoRequest;
import com.soportenet.soportetecnico.dto.TecnicoDisponibleProjection;
import com.soportenet.soportetecnico.dto.TecnicoResponse;
import com.soportenet.soportetecnico.entity.Tecnico;
import com.soportenet.soportetecnico.repository.TecnicoRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/tecnicos")
public class TecnicoController {

    private final TecnicoRepository tecnicoRepository;

    public TecnicoController(TecnicoRepository tecnicoRepository) {
        this.tecnicoRepository = tecnicoRepository;
    }

    /**
     * Tecnicos habilitados/activos con su carga actual (asignaciones
     * vigentes) - usado en "Asignar técnico" a una solicitud. Sin "nombre"
     * (u opcional/vacio) devuelve los 8 mas libres, para que el admin vea
     * de una a quien puede asignar sin tener que escribir nada; con
     * "nombre" filtra por ese termino.
     */
    @GetMapping("/buscar")
    public List<TecnicoDisponibleProjection> buscar(@RequestParam(required = false) String nombre) {
        return tecnicoRepository.buscarConCarga(nombre, 8);
    }

  
    @GetMapping("/{id}")
    public ResponseEntity<TecnicoResponse> obtener(@PathVariable Long id) {
        return tecnicoRepository.findById(id)
                .map(t -> ResponseEntity.ok(TecnicoResponse.fromEntity(t)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/perfil")
    @Transactional
    public ResponseEntity<TecnicoResponse> editarPerfil(@PathVariable Long id,
                                                           @Valid @RequestBody EditarPerfilTecnicoRequest request,
                                                           Authentication authentication) {

        Long idSuperusuario = Long.valueOf(authentication.getName());

        tecnicoRepository.editarPerfilTecnico(
                idSuperusuario,
                id,
                request.getEspecialidad(),
                request.getNivel().name()
        );

        Tecnico actualizado = tecnicoRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException(
                        "El perfil se actualizo pero no se pudo recuperar (id=" + id + ")"));

        return ResponseEntity.ok(TecnicoResponse.fromEntity(actualizado));
    }
}
