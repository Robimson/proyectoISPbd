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

/**
 * Edicion del perfil operativo del tecnico (especialidad/nivel), separado de
 * CatalogoController (que solo expone GET /api/tecnicos de solo lectura para
 * los desplegables). sp_invitar_usuario crea al tecnico con especialidad NULL
 * y nivel 'junior' por defecto - este es el unico lugar que los actualiza
 * despues, y solo el Superusuario puede hacerlo (es quien ya administra esa
 * cuenta y sus grupos, seccion 2.4 del documento).
 */
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

    /**
     * Un solo tecnico, para precargar el modal de "Perfil tecnico" del
     * Superusuario. findById() ya viene de JpaRepository (sin SQL propio,
     * mismo patron que SolicitudController/ReporteController) - a proposito
     * en vez de reusar CatalogoController.listarTecnicos(), que trae TODOS
     * los tecnicos habilitados solo para leer uno.
     */
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
