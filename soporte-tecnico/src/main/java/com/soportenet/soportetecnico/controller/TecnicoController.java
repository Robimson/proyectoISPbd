package com.soportenet.soportetecnico.controller;

import com.soportenet.soportetecnico.dto.EditarPerfilTecnicoRequest;
import com.soportenet.soportetecnico.dto.TecnicoResponse;
import com.soportenet.soportetecnico.dto.UsuarioBusquedaProjection;
import com.soportenet.soportetecnico.entity.Tecnico;
import com.soportenet.soportetecnico.repository.TecnicoRepository;
import com.soportenet.soportetecnico.repository.UsuarioRepository;
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
    private final UsuarioRepository usuarioRepository;

    public TecnicoController(TecnicoRepository tecnicoRepository, UsuarioRepository usuarioRepository) {
        this.tecnicoRepository = tecnicoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Autocompletar por nombre o correo, solo tecnicos - usado en "Agregar
     * técnico a un grupo", que antes era un <select> con los ~3000 tecnicos
     * de prueba sin poder buscar.
     */
    @GetMapping("/buscar")
    public List<UsuarioBusquedaProjection> buscar(@RequestParam String nombre) {
        return usuarioRepository.buscarTecnicos(nombre);
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
