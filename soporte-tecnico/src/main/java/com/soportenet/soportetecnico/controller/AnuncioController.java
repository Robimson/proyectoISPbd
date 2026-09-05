package com.soportenet.soportetecnico.controller;

import com.soportenet.soportetecnico.dto.AnuncioAdminProjection;
import com.soportenet.soportetecnico.dto.AnuncioProjection;
import com.soportenet.soportetecnico.dto.AnuncioResponse;
import com.soportenet.soportetecnico.dto.CrearAnuncioRequest;
import com.soportenet.soportetecnico.entity.Anuncio;
import com.soportenet.soportetecnico.repository.AnuncioRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Anuncios globales del Administrador para incidencias masivas (seccion 2.3
 * del documento) - la tabla ya existia en el esquema original pero nunca se
 * habia conectado a ningun procedimiento ni pantalla. El objetivo es que un
 * corte masivo se comunique una sola vez en vez de que cada cliente cree su
 * propio ticket duplicado.
 */
@RestController
@RequestMapping("/api/anuncios")
public class AnuncioController {

    private final AnuncioRepository anuncioRepository;

    public AnuncioController(AnuncioRepository anuncioRepository) {
        this.anuncioRepository = anuncioRepository;
    }

    /**
     * Cualquier rol logueado: los anuncios activos y vigentes (el banner que
     * se muestra arriba de cada panel).
     */
    @GetMapping
    public List<AnuncioProjection> listarActivos() {
        return anuncioRepository.listarActivos();
    }

    /**
     * Administrador: todos los anuncios, activos e inactivos, para la
     * pantalla de gestion.
     */
    @GetMapping("/todos")
    public List<AnuncioAdminProjection> listarTodos() {
        return anuncioRepository.listarTodos();
    }

    @PostMapping
    @Transactional
    public ResponseEntity<AnuncioResponse> crear(@Valid @RequestBody CrearAnuncioRequest request,
                                                    Authentication authentication) {

        Long idAdministrador = Long.valueOf(authentication.getName());

        Long idAnuncio = anuncioRepository.crearAnuncio(
                idAdministrador,
                request.getTitulo(),
                request.getMensaje(),
                request.getFechaExpiracion()
        );

        Anuncio creado = anuncioRepository.findById(idAnuncio)
                .orElseThrow(() -> new IllegalStateException(
                        "El anuncio se creo pero no se pudo recuperar (id=" + idAnuncio + ")"));

        return ResponseEntity.status(HttpStatus.CREATED).body(AnuncioResponse.fromEntity(creado));
    }

    @PostMapping("/{id}/desactivacion")
    @Transactional
    public ResponseEntity<AnuncioResponse> desactivar(@PathVariable Long id, Authentication authentication) {

        Long idAdministrador = Long.valueOf(authentication.getName());

        anuncioRepository.desactivarAnuncio(idAdministrador, id);

        Anuncio actualizado = anuncioRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException(
                        "El anuncio se desactivo pero no se pudo recuperar (id=" + id + ")"));

        return ResponseEntity.ok(AnuncioResponse.fromEntity(actualizado));
    }
}
