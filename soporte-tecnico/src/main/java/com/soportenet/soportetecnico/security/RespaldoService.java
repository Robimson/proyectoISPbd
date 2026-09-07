package com.soportenet.soportetecnico.security;

import com.soportenet.soportetecnico.dto.ConfiguracionRespaldoRequest;
import com.soportenet.soportetecnico.entity.ConfiguracionRespaldo;
import com.soportenet.soportetecnico.entity.EstadoRespaldo;
import com.soportenet.soportetecnico.entity.RespaldoBd;
import com.soportenet.soportetecnico.entity.TipoRespaldo;
import com.soportenet.soportetecnico.repository.ConfiguracionRespaldoRepository;
import com.soportenet.soportetecnico.repository.RespaldoBdRepository;
import com.soportenet.soportetecnico.repository.UsuarioRepository;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class RespaldoService {

    private final RespaldoBdRepository respaldoRepository;
    private final ConfiguracionRespaldoRepository configuracionRepository;
    private final UsuarioRepository usuarioRepository;
    private final TaskExecutor taskExecutor;

    @Value("${respaldo.ruta-base}")
    private String rutaBase;

    @Value("${respaldo.ruta-wal}")
    private String rutaWal;

    @Value("${respaldo.pg-basebackup-path}")
    private String pgBasebackupPath;

    @Value("${respaldo.host}")
    private String host;

    @Value("${respaldo.puerto}")
    private String puerto;

    @Value("${respaldo.usuario-replicacion}")
    private String usuarioReplicacion;

    @Value("${respaldo.password-replicacion}")
    private String passwordReplicacion;

    public RespaldoService(
            RespaldoBdRepository respaldoRepository,
            ConfiguracionRespaldoRepository configuracionRepository,
            UsuarioRepository usuarioRepository,
            @Qualifier("taskExecutor") TaskExecutor taskExecutor) {

        this.respaldoRepository = respaldoRepository;
        this.configuracionRepository = configuracionRepository;
        this.usuarioRepository = usuarioRepository;
        this.taskExecutor = taskExecutor;
    }

    // ============================================================
    // RESPALDO FULL
    // ============================================================

    public RespaldoBd iniciarRespaldoFullAsync(
            Long idUsuarioSolicito,
            boolean manual) {

        RespaldoBd respaldo = new RespaldoBd();

        respaldo.setTipo(TipoRespaldo.FULL);
        respaldo.setEstado(EstadoRespaldo.EN_PROCESO);
        respaldo.setFechaInicio(OffsetDateTime.now());
        respaldo.setRutaArchivo("(en proceso)");
        respaldo.setGeneradoManualmente(manual);

        if (idUsuarioSolicito != null) {
            usuarioRepository.findById(idUsuarioSolicito)
                    .ifPresent(respaldo::setUsuarioSolicito);
        }

        RespaldoBd guardado = respaldoRepository.save(respaldo);

        taskExecutor.execute(() ->
                ejecutarRespaldoFull(guardado.getIdRespaldo())
        );

        return guardado;
    }

    private void ejecutarRespaldoFull(Long idRespaldo) {

        RespaldoBd respaldo =
                respaldoRepository.findById(idRespaldo).orElseThrow();

        String timestamp =
                DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
                        .format(LocalDateTime.now());

        String nombreCarpeta = "full_" + timestamp;

        Path destino =
                Paths.get(rutaBase, "full", nombreCarpeta);

        try {

            Files.createDirectories(destino);

            ProcessBuilder pb = new ProcessBuilder(
                    pgBasebackupPath,
                    "-h", host,
                    "-p", puerto,
                    "-U", usuarioReplicacion,
                    "-D", destino.toString(),
                    "-F", "tar",
                    "-z",
                    "-X", "stream",
                    "-P"
            );

            pb.environment().put(
                    "PGPASSWORD",
                    passwordReplicacion
            );

            pb.redirectErrorStream(true);

            Process proceso = pb.start();

            String salida;

            try (InputStream is = proceso.getInputStream()) {
                salida = new String(is.readAllBytes());
            }

            int codigoSalida = proceso.waitFor();

            if (codigoSalida != 0) {
                throw new IllegalStateException(
                        "pg_basebackup finalizo con codigo "
                                + codigoSalida
                                + ": "
                                + salida
                );
            }

            Path zipFinal =
                    Paths.get(
                            rutaBase,
                            "full",
                            nombreCarpeta + ".zip"
                    );

            comprimirCarpeta(destino, zipFinal);

          //  eliminarCarpetaRecursivo(destino);

            respaldo.setRutaArchivo(
                    zipFinal.toAbsolutePath().toString()
            );

            respaldo.setTamanoBytes(
                    Files.size(zipFinal)
            );

            respaldo.setEstado(
                    EstadoRespaldo.COMPLETADO
            );

            respaldo.setFechaFin(
                    OffsetDateTime.now()
            );

            respaldo.setMensajeError(null);

        } catch (Exception e) {

            respaldo.setEstado(
                    EstadoRespaldo.ERROR
            );

            respaldo.setMensajeError(
                    e.getMessage()
            );

            respaldo.setFechaFin(
                    OffsetDateTime.now()
            );
        }

        respaldoRepository.save(respaldo);
    }

    // ============================================================
    // COMPRESION DEL FULL
    // ============================================================

    private void comprimirCarpeta(
            Path carpeta,
            Path zipDestino) throws IOException {

        try (ZipOutputStream zos =
                     new ZipOutputStream(
                             Files.newOutputStream(zipDestino))) {

            try (DirectoryStream<Path> archivos =
                         Files.newDirectoryStream(carpeta)) {

                for (Path archivo : archivos) {

                    if (Files.isRegularFile(archivo)) {

                        zos.putNextEntry(
                                new ZipEntry(
                                        archivo.getFileName().toString()
                                )
                        );

                        Files.copy(archivo, zos);

                        zos.closeEntry();
                    }
                }
            }
        }
    }

    // ============================================================
    // ELIMINAR CARPETA TEMPORAL
    // ============================================================

    private void eliminarCarpetaRecursivo(
            Path carpeta) throws IOException {

        try (var stream = Files.walk(carpeta)) {

            stream
                    .sorted(Comparator.reverseOrder())
                    .forEach(p -> {

                        try {
                            Files.deleteIfExists(p);
                        } catch (IOException ignored) {
                        }

                    });
        }
    }

    // ============================================================
    // PROGRAMACION DEL FULL
    // ============================================================

    @Scheduled(cron = "0 * * * * *")
    public void verificarProgramacionFull() {

        ConfiguracionRespaldo config =
                configuracionRepository
                        .findById((short) 1)
                        .orElse(null);

        if (config == null ||
                !Boolean.TRUE.equals(config.getActivo())) {

            return;
        }

        LocalTime ahora =
                LocalTime.now()
                        .withSecond(0)
                        .withNano(0);

        LocalTime horaConfigurada =
                config.getHoraRespaldoFull()
                        .withSecond(0)
                        .withNano(0);

        if (!ahora.equals(horaConfigurada)) {
            return;
        }

        LocalDate hoy = LocalDate.now();

        if ("SEMANAL".equalsIgnoreCase(
                config.getFrecuencia())) {

            int diaHoy =
                    hoy.getDayOfWeek().getValue() % 7;

            if (config.getDiaSemanaFull() == null ||
                    diaHoy != config.getDiaSemanaFull()) {

                return;
            }
        }

        OffsetDateTime desdeMedianoche =
                hoy.atStartOfDay(
                        ZoneId.systemDefault()
                ).toOffsetDateTime();

        boolean yaEjecutadoHoy =
                respaldoRepository
                        .existsByTipoAndEstadoAndFechaInicioAfter(
                                TipoRespaldo.FULL,
                                EstadoRespaldo.COMPLETADO,
                                desdeMedianoche
                        );

        if (yaEjecutadoHoy) {
            return;
        }

        iniciarRespaldoFullAsync(
                null,
                false
        );
    }

    // ============================================================
    // RESPALDO INCREMENTAL / WAL
    // ============================================================

    /**
     * PostgreSQL genera los archivos WAL automáticamente
     * cuando archive_mode está activo.
     *
     * Este método revisa la carpeta configurada y registra
     * solamente los archivos que todavía no están registrados.
     */
    @Scheduled(fixedDelay = 60000)
    public void escanearArchivosWal() {

        File carpeta = new File(rutaWal);

        if (!carpeta.exists() ||
                !carpeta.isDirectory()) {

            return;
        }

        File[] archivos =
                carpeta.listFiles(File::isFile);

        if (archivos == null ||
                archivos.length == 0) {

            return;
        }

        Set<String> yaRegistrados =
                new HashSet<>(
                        respaldoRepository.rutasWalRegistradas()
                );

        for (File archivo : archivos) {

            String ruta =
                    archivo.getAbsolutePath();

            /*
             * Evita registrar nuevamente el mismo
             * archivo WAL.
             */
            if (yaRegistrados.contains(ruta)) {
                continue;
            }

            /*
             * Ignorar archivos vacíos.
             */
            if (archivo.length() <= 0) {
                continue;
            }

            RespaldoBd respaldo =
                    new RespaldoBd();

            respaldo.setTipo(
                    TipoRespaldo.WAL
            );

            respaldo.setEstado(
                    EstadoRespaldo.COMPLETADO
            );

            respaldo.setRutaArchivo(
                    ruta
            );

            respaldo.setTamanoBytes(
                    archivo.length()
            );

            /*
             * Usamos directamente la fecha del archivo
             * convertida a OffsetDateTime.
             *
             * No necesitamos FileTime ni toInstant().
             */
            OffsetDateTime fechaArchivo =
                    OffsetDateTime.ofInstant(
                            java.time.Instant.ofEpochMilli(
                                    archivo.lastModified()
                            ),
                            ZoneId.systemDefault()
                    );

            respaldo.setFechaInicio(
                    fechaArchivo
            );

            respaldo.setFechaFin(
                    fechaArchivo
            );

            respaldo.setGeneradoManualmente(
                    false
            );

            respaldoRepository.save(respaldo);

            /*
             * Lo agregamos inmediatamente al conjunto
             * para evitar duplicados dentro de la misma
             * ejecución.
             */
            yaRegistrados.add(ruta);
        }
    }

    // ============================================================
    // CONFIGURACION
    // ============================================================

    public ConfiguracionRespaldo obtenerConfiguracion() {

        return configuracionRepository
                .findById((short) 1)
                .orElseThrow();
    }

    public ConfiguracionRespaldo actualizarConfiguracion(
            Long idSuperusuario,
            ConfiguracionRespaldoRequest request) {

        ConfiguracionRespaldo config =
                obtenerConfiguracion();

        config.setHoraRespaldoFull(
                request.getHoraRespaldoFull()
        );

        config.setFrecuencia(
                request.getFrecuencia()
        );

        config.setDiaSemanaFull(
                request.getDiaSemanaFull()
        );

        if (request.getActivo() != null) {

            config.setActivo(
                    request.getActivo()
            );
        }

        return configuracionRepository.save(config);
    }
}