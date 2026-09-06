package com.soportenet.soportetecnico.controller;

import com.soportenet.soportetecnico.dto.ActualizarConfiguracionRequest;
import com.soportenet.soportetecnico.dto.ConfiguracionProjection;
import com.soportenet.soportetecnico.repository.ConfiguracionSistemaRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * Configuracion global de marca (nombre del negocio, logo, color) - la pide
 * el Superusuario para poder personalizar la app sin depender de que un
 * programador cambie codigo. GET es publico (sin login): hasta la pantalla
 * de login necesita mostrar el nombre/logo correctos antes de que exista
 * una sesion - es la unica excepcion real a "todo pide JWT" en el sistema.
 */
@RestController
@RequestMapping("/api/configuracion")
public class ConfiguracionController {

    private static final String NOMBRE_ARCHIVO_LOGO = "logo.png";
    private static final long TAMANO_MAXIMO_BYTES = 2L * 1024 * 1024; // 2MB, un logo no necesita mas

    private final ConfiguracionSistemaRepository configuracionRepository;
    private final Path directorioLogo;

    public ConfiguracionController(ConfiguracionSistemaRepository configuracionRepository,
                                    @Value("${app.storage.logo-dir}") String directorioLogo) {
        this.configuracionRepository = configuracionRepository;
        this.directorioLogo = Paths.get(directorioLogo).toAbsolutePath().normalize();
    }

    @GetMapping
    public ConfiguracionProjection obtener() {
        return configuracionRepository.obtener();
    }

    @PostMapping
    @Transactional
    public ResponseEntity<ConfiguracionProjection> actualizar(@Valid @RequestBody ActualizarConfiguracionRequest request,
                                                                Authentication authentication) {

        Long idSuperusuario = Long.valueOf(authentication.getName());

        configuracionRepository.actualizar(idSuperusuario, request.getNombreNegocio(), request.getCategoria(),
                request.getEslogan(), request.getColorPrimario());

        return ResponseEntity.ok(configuracionRepository.obtener());
    }

    @PostMapping("/logo")
    @Transactional
    public ResponseEntity<?> actualizarLogo(@RequestParam("archivo") MultipartFile archivo,
                                             Authentication authentication) {

        if (archivo.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "El archivo esta vacio."));
        }
        if (archivo.getSize() > TAMANO_MAXIMO_BYTES) {
            return ResponseEntity.badRequest().body(Map.of("error", "El logo no puede superar 2MB."));
        }
        if (!"image/png".equals(archivo.getContentType())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Solo se permiten imagenes PNG."));
        }

        Long idSuperusuario = Long.valueOf(authentication.getName());

        try {
            Files.createDirectories(directorioLogo);

            Path destino = directorioLogo.resolve(NOMBRE_ARCHIVO_LOGO);
            archivo.transferTo(destino);

            // URL fija (siempre el mismo archivo, se sobreescribe) - el
            // frontend le agrega un parametro de version para que el
            // navegador no se quede con una copia vieja en cache.
            configuracionRepository.actualizarLogo(idSuperusuario, "/api/configuracion/logo/archivo");

            return ResponseEntity.ok(configuracionRepository.obtener());
        } catch (IOException error) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "No se pudo guardar el logo en el servidor."));
        }
    }

    /** Publico: sirve el logo actual. Si nunca se subio ninguno, 404 (el frontend usa el logo por defecto). */
    @GetMapping("/logo/archivo")
    public ResponseEntity<Resource> archivoLogo() {
        Path ruta = directorioLogo.resolve(NOMBRE_ARCHIVO_LOGO);
        if (!Files.exists(ruta)) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(new FileSystemResource(ruta));
    }
}
