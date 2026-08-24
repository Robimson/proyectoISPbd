package com.soportenet.soportetecnico.controller;

import com.soportenet.soportetecnico.dto.AdjuntoResponse;
import com.soportenet.soportetecnico.entity.Adjunto;
import com.soportenet.soportetecnico.repository.AdjuntoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Evidencia adjunta a una solicitud (fotos o PDF, secciones 2.1/2.2 y 6.1
 * del documento): la sube el cliente dueno o el tecnico asignado. El
 * archivo se guarda en disco, nunca en la base de datos - Postgres solo
 * guarda la referencia, y sp_agregar_adjunto valida quien puede subir y el
 * limite de 5 archivos por solicitud.
 */
@RestController
public class AdjuntoController {

    private static final Set<String> TIPOS_PERMITIDOS = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp", "application/pdf"
    );
    private static final long TAMANO_MAXIMO_BYTES = 10L * 1024 * 1024; // 10MB, igual al CHECK de la base

    private final AdjuntoRepository adjuntoRepository;
    private final Path directorioAdjuntos;

    public AdjuntoController(AdjuntoRepository adjuntoRepository,
                              @Value("${app.storage.adjuntos-dir}") String directorioAdjuntos) {
        this.adjuntoRepository = adjuntoRepository;
        this.directorioAdjuntos = Paths.get(directorioAdjuntos).toAbsolutePath().normalize();
    }

    @PostMapping("/api/solicitudes/{id}/adjuntos")
    @Transactional
    public ResponseEntity<?> subir(@PathVariable Long id,
                                    @RequestParam("archivo") MultipartFile archivo,
                                    Authentication authentication) {

        if (archivo.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "El archivo esta vacio."));
        }
        if (archivo.getSize() > TAMANO_MAXIMO_BYTES) {
            return ResponseEntity.badRequest().body(Map.of("error", "El archivo supera el limite de 10MB."));
        }
        String tipoContenido = archivo.getContentType();
        if (tipoContenido == null || !TIPOS_PERMITIDOS.contains(tipoContenido)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Solo se permiten imagenes (JPG, PNG, GIF, WEBP) o PDF."));
        }

        Long idUsuario = Long.valueOf(authentication.getName());

        try {
            Path carpetaSolicitud = directorioAdjuntos.resolve(String.valueOf(id));
            Files.createDirectories(carpetaSolicitud);

            String nombreGuardado = UUID.randomUUID() + extensionParaTipo(tipoContenido);
            Path destino = carpetaSolicitud.resolve(nombreGuardado);
            archivo.transferTo(destino);

            String nombreOriginal = archivo.getOriginalFilename() != null
                    ? archivo.getOriginalFilename()
                    : nombreGuardado;

            Long idAdjunto = adjuntoRepository.agregarAdjunto(
                    id,
                    idUsuario,
                    nombreOriginal,
                    tipoContenido,
                    archivo.getSize(),
                    destino.toString()
            );

            Adjunto creado = adjuntoRepository.findById(idAdjunto)
                    .orElseThrow(() -> new IllegalStateException(
                            "El adjunto se guardo pero no se pudo recuperar (id=" + idAdjunto + ")"));

            return ResponseEntity.status(HttpStatus.CREATED).body(AdjuntoResponse.fromEntity(creado));
        } catch (IOException error) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "No se pudo guardar el archivo en el servidor."));
        }
    }

    @GetMapping("/api/solicitudes/{id}/adjuntos")
    public ResponseEntity<?> listar(@PathVariable Long id, Authentication authentication) {
        if (!autorizado(id, authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<AdjuntoResponse> lista = adjuntoRepository.findBySolicitudIdSolicitudOrderByFechaSubidaAsc(id)
                .stream()
                .map(AdjuntoResponse::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(lista);
    }

    @GetMapping("/api/adjuntos/{id}/archivo")
    public ResponseEntity<?> descargar(@PathVariable Long id, Authentication authentication) {
        Adjunto adjunto = adjuntoRepository.findById(id).orElse(null);
        if (adjunto == null) {
            return ResponseEntity.notFound().build();
        }

        Long idSolicitud = adjunto.getSolicitud().getIdSolicitud();
        if (!autorizado(idSolicitud, authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Path ruta = Paths.get(adjunto.getUrlAlmacenamiento());
        if (!Files.exists(ruta)) {
            return ResponseEntity.notFound().build();
        }

        Resource recurso = new FileSystemResource(ruta);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(adjunto.getTipoArchivo()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + adjunto.getNombreArchivo() + "\"")
                .body(recurso);
    }

    private boolean autorizado(Long idSolicitud, Authentication authentication) {
        if (tieneRol(authentication, "ADMINISTRADOR") || tieneRol(authentication, "SUPERUSUARIO")) {
            return true;
        }
        Long idUsuario = Long.valueOf(authentication.getName());
        return adjuntoRepository.puedeAccederASolicitud(idSolicitud, idUsuario);
    }

    private boolean tieneRol(Authentication authentication, String rol) {
        String authority = "ROLE_" + rol;
        for (GrantedAuthority ga : authentication.getAuthorities()) {
            if (ga.getAuthority().equals(authority)) {
                return true;
            }
        }
        return false;
    }

    private String extensionParaTipo(String tipoContenido) {
        return switch (tipoContenido) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            case "application/pdf" -> ".pdf";
            default -> "";
        };
    }
}
