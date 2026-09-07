package com.soportenet.soportetecnico.controller;

import com.soportenet.soportetecnico.dto.ConfiguracionRespaldoRequest;
import com.soportenet.soportetecnico.dto.RespaldoResponse;
import com.soportenet.soportetecnico.entity.ConfiguracionRespaldo;
import com.soportenet.soportetecnico.entity.RespaldoBd;
import com.soportenet.soportetecnico.entity.TipoRespaldo;
import com.soportenet.soportetecnico.repository.RespaldoBdRepository;
import com.soportenet.soportetecnico.security.RespaldoService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("/api/respaldos")
public class RespaldoController {

    private final RespaldoService respaldoService;
    private final RespaldoBdRepository respaldoRepository;

    public RespaldoController(RespaldoService respaldoService, RespaldoBdRepository respaldoRepository) {
        this.respaldoService = respaldoService;
        this.respaldoRepository = respaldoRepository;
    }

    @GetMapping
    public ResponseEntity<Page<RespaldoResponse>> listar(
            @RequestParam(required = false) String tipo,
            @PageableDefault(size = 20) Pageable pageable,
            Authentication authentication) {

        if (!tieneRol(authentication, "SUPERUSUARIO")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Page<RespaldoBd> pagina = (tipo != null)
                ? respaldoRepository.findByTipoOrderByFechaInicioDesc(TipoRespaldo.valueOf(tipo), pageable)
                : respaldoRepository.findAllByOrderByFechaInicioDesc(pageable);

        return ResponseEntity.ok(pagina.map(RespaldoResponse::fromEntity));
    }

    @PostMapping("/full")
    public ResponseEntity<RespaldoResponse> generarFull(Authentication authentication) {
        if (!tieneRol(authentication, "SUPERUSUARIO")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Long idUsuario = Long.valueOf(authentication.getName());
        RespaldoBd creado = respaldoService.iniciarRespaldoFullAsync(idUsuario, true);
        return ResponseEntity.accepted().body(RespaldoResponse.fromEntity(creado));
    }

    /**
     * Descarga un respaldo.
     * - FULL: el archivo en disco YA es un .zip valido armado por
     *   RespaldoService.comprimirCarpeta() - se sirve tal cual, sin tocarlo.
     * - WAL: el archivo en disco NO es un zip - es un segmento binario
     *   interno de Postgres que el propio motor sigue necesitando intacto en
     *   wal_archive para una eventual recuperacion (nunca se mueve, copia ni
     *   comprime ahi). Se envuelve en un zip al vuelo SOLO para esta
     *   descarga puntual.
     *
     * BLOQUE DE DEBUG TEMPORAL incluido mas abajo: ademas de devolver el
     * zip al navegador, guarda una copia en la misma carpeta del WAL
     * (prefijo "debug_") para poder abrirla directamente en el servidor y
     * descartar si el problema esta en el armado del zip o en la descarga.
     */
    @GetMapping("/{id}/descargar")
    public ResponseEntity<StreamingResponseBody> descargar(@PathVariable Long id, Authentication authentication) {
        if (!tieneRol(authentication, "SUPERUSUARIO")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        RespaldoBd respaldo = respaldoRepository.findById(id).orElse(null);
        if (respaldo == null || respaldo.getRutaArchivo() == null) {
            return ResponseEntity.notFound().build();
        }

        File archivo = new File(respaldo.getRutaArchivo());
        if (!archivo.exists()) {
            return ResponseEntity.notFound().build();
        }

        if (respaldo.getTipo() == TipoRespaldo.FULL) {
            StreamingResponseBody cuerpo = salida -> {
                try (InputStream entrada = new FileInputStream(archivo)) {
                    entrada.transferTo(salida);
                }
            };
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + archivo.getName() + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(archivo.length())
                    .body(cuerpo);
        }

        // WAL: armamos el zip completo EN MEMORIA (los segmentos WAL son 16MB
        // fijos, entra sin problema) en vez de escribir directo al stream de
        // salida - asi conocemos el tamano final exacto y podemos guardar una
        // copia de depuracion en disco para abrirla directamente en el
        // servidor, sin que el navegador sea parte de la ecuacion.
        byte[] bytesZip;
        try (java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream()) {
            try (ZipOutputStream zos = new ZipOutputStream(buffer)) {
                zos.putNextEntry(new ZipEntry(archivo.getName()));
                try (InputStream entrada = new FileInputStream(archivo)) {
                    entrada.transferTo(zos);
                }
                zos.closeEntry();
            }
            bytesZip = buffer.toByteArray();
        } catch (java.io.IOException e) {
            throw new RuntimeException("No se pudo armar el zip del WAL: " + e.getMessage(), e);
        }

        // ---- DEBUG TEMPORAL: comentar este bloque completo para desactivarlo ----
        try {
            File debugZip = new File(archivo.getParentFile(), "debug_" + archivo.getName() + ".zip");
            java.nio.file.Files.write(debugZip.toPath(), bytesZip);
            System.out.println("DEBUG: zip WAL guardado en " + debugZip.getAbsolutePath() + " (" + bytesZip.length + " bytes)");
        } catch (java.io.IOException e) {
            System.err.println("DEBUG: no se pudo escribir el zip de depuracion: " + e.getMessage());
        }
        // ---- FIN DEBUG TEMPORAL ----

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + archivo.getName() + ".zip\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(bytesZip.length)
                .body(salida -> salida.write(bytesZip));
    }

    @GetMapping("/configuracion")
    public ResponseEntity<ConfiguracionRespaldo> obtenerConfiguracion(Authentication authentication) {
        if (!tieneRol(authentication, "SUPERUSUARIO")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(respaldoService.obtenerConfiguracion());
    }

    @PutMapping("/configuracion")
    public ResponseEntity<ConfiguracionRespaldo> actualizarConfiguracion(
            @Valid @RequestBody ConfiguracionRespaldoRequest request,
            Authentication authentication) {

        if (!tieneRol(authentication, "SUPERUSUARIO")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Long idSuperusuario = Long.valueOf(authentication.getName());
        return ResponseEntity.ok(respaldoService.actualizarConfiguracion(idSuperusuario, request));
    }

    /**
     * Comprueba si el usuario autenticado posee un rol determinado.
     * Mismo patron que SolicitudController.tieneRol(...).
     */
    private boolean tieneRol(Authentication authentication, String rol) {
        String authority = "ROLE_" + rol;
        for (GrantedAuthority ga : authentication.getAuthorities()) {
            if (ga.getAuthority().equals(authority)) {
                return true;
            }
        }
        return false;
    }
}