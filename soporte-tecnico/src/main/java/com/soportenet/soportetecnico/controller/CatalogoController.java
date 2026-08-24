package com.soportenet.soportetecnico.controller;

import com.soportenet.soportetecnico.dto.TecnicoResponse;
import com.soportenet.soportetecnico.entity.Categoria;
import com.soportenet.soportetecnico.entity.Estado;
import com.soportenet.soportetecnico.entity.Prioridad;
import com.soportenet.soportetecnico.repository.CategoriaRepository;
import com.soportenet.soportetecnico.repository.EstadoRepository;
import com.soportenet.soportetecnico.repository.PrioridadRepository;
import com.soportenet.soportetecnico.repository.TecnicoRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Catalogos de solo lectura para poblar los desplegables del frontend
 * (categoria/prioridad al crear un ticket, estado para filtrar, tecnicos
 * habilitados al asignar). Categoria/Prioridad/Estado no tienen relaciones
 * ni datos sensibles, asi que se sirven como entidad directa.
 */
@RestController
public class CatalogoController {

    private final CategoriaRepository categoriaRepository;
    private final PrioridadRepository prioridadRepository;
    private final EstadoRepository estadoRepository;
    private final TecnicoRepository tecnicoRepository;

    public CatalogoController(CategoriaRepository categoriaRepository,
                               PrioridadRepository prioridadRepository,
                               EstadoRepository estadoRepository,
                               TecnicoRepository tecnicoRepository) {
        this.categoriaRepository = categoriaRepository;
        this.prioridadRepository = prioridadRepository;
        this.estadoRepository = estadoRepository;
        this.tecnicoRepository = tecnicoRepository;
    }

    @GetMapping("/api/categorias")
    public List<Categoria> listarCategorias() {
        return categoriaRepository.findAll();
    }

    @GetMapping("/api/prioridades")
    public List<Prioridad> listarPrioridades() {
        return prioridadRepository.findAll();
    }

    @GetMapping("/api/estados")
    public List<Estado> listarEstados() {
        return estadoRepository.findAll();
    }

    /** Por defecto solo tecnicos habilitados (los que se pueden asignar). */
    @GetMapping("/api/tecnicos")
    public List<TecnicoResponse> listarTecnicos(@RequestParam(required = false, defaultValue = "true") Boolean habilitado) {
        return tecnicoRepository.findByHabilitado(habilitado)
                .stream()
                .map(TecnicoResponse::fromEntity)
                .toList();
    }
}
