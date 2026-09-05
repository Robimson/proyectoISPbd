package com.soportenet.soportetecnico.controller;

import com.soportenet.soportetecnico.entity.Cliente;
import com.soportenet.soportetecnico.entity.Solicitud;
import com.soportenet.soportetecnico.repository.SolicitudRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Prueba la logica de permisos de SolicitudController.obtener() (caso de
 * uso: quien puede consultar el detalle de una solicitud). Cubre en
 * particular la rama de TECNICO, que fue la ultima en completarse
 * (fn_tecnico_tiene_acceso via SolicitudRepository.tecnicoTieneAcceso).
 */
class SolicitudControllerTest {

    private SolicitudRepository solicitudRepository;
    private SolicitudController solicitudController;

    @BeforeEach
    void configurar() {
        solicitudRepository = Mockito.mock(SolicitudRepository.class);
        solicitudController = new SolicitudController(solicitudRepository);
    }

    private Authentication autenticacionComo(Long idUsuario, String rol) {
        return new UsernamePasswordAuthenticationToken(
                idUsuario.toString(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + rol))
        );
    }

    @Test
    void solicitudInexistenteDevuelveNotFound() {
        when(solicitudRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseEntity<?> respuesta = solicitudController.obtener(99L, autenticacionComo(1L, "ADMINISTRADOR"));

        assertEquals(HttpStatus.NOT_FOUND, respuesta.getStatusCode());
    }

    @Test
    void clienteNoPuedeVerSolicitudDeOtroCliente() {
        Long idSolicitud = 10L;
        Long idClienteAutenticado = 1L;
        Long idClientePropietario = 2L;

        Cliente clientePropietario = new Cliente();
        clientePropietario.setIdUsuario(idClientePropietario);

        Solicitud solicitud = new Solicitud();
        solicitud.setIdSolicitud(idSolicitud);
        solicitud.setCliente(clientePropietario);

        when(solicitudRepository.findById(idSolicitud)).thenReturn(Optional.of(solicitud));

        ResponseEntity<?> respuesta = solicitudController.obtener(
                idSolicitud, autenticacionComo(idClienteAutenticado, "CLIENTE"));

        assertEquals(HttpStatus.FORBIDDEN, respuesta.getStatusCode());
    }

    @Test
    void clientePuedeVerSuPropiaSolicitud() {
        Long idSolicitud = 11L;
        Long idCliente = 1L;

        Cliente cliente = new Cliente();
        cliente.setIdUsuario(idCliente);

        Solicitud solicitud = new Solicitud();
        solicitud.setIdSolicitud(idSolicitud);
        solicitud.setCliente(cliente);

        when(solicitudRepository.findById(idSolicitud)).thenReturn(Optional.of(solicitud));

        ResponseEntity<?> respuesta = solicitudController.obtener(
                idSolicitud, autenticacionComo(idCliente, "CLIENTE"));

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
    }

    @Test
    void tecnicoConAccesoPuedeVerLaSolicitud() {
        Long idSolicitud = 20L;
        Long idTecnico = 5L;

        Solicitud solicitud = new Solicitud();
        solicitud.setIdSolicitud(idSolicitud);

        when(solicitudRepository.findById(idSolicitud)).thenReturn(Optional.of(solicitud));
        when(solicitudRepository.tecnicoTieneAcceso(idSolicitud, idTecnico)).thenReturn(true);

        ResponseEntity<?> respuesta = solicitudController.obtener(
                idSolicitud, autenticacionComo(idTecnico, "TECNICO"));

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
    }

    @Test
    void tecnicoSinAccesoNoPuedeVerLaSolicitud() {
        Long idSolicitud = 21L;
        Long idTecnico = 6L;

        Solicitud solicitud = new Solicitud();
        solicitud.setIdSolicitud(idSolicitud);

        when(solicitudRepository.findById(idSolicitud)).thenReturn(Optional.of(solicitud));
        when(solicitudRepository.tecnicoTieneAcceso(idSolicitud, idTecnico)).thenReturn(false);

        ResponseEntity<?> respuesta = solicitudController.obtener(
                idSolicitud, autenticacionComo(idTecnico, "TECNICO"));

        assertEquals(HttpStatus.FORBIDDEN, respuesta.getStatusCode());
    }

    @Test
    void administradorPuedeVerCualquierSolicitud() {
        Long idSolicitud = 30L;
        Solicitud solicitud = new Solicitud();
        solicitud.setIdSolicitud(idSolicitud);

        when(solicitudRepository.findById(idSolicitud)).thenReturn(Optional.of(solicitud));

        ResponseEntity<?> respuesta = solicitudController.obtener(
                idSolicitud, autenticacionComo(100L, "ADMINISTRADOR"));

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
    }

    @Test
    void superusuarioPuedeVerCualquierSolicitud() {
        Long idSolicitud = 40L;
        Solicitud solicitud = new Solicitud();
        solicitud.setIdSolicitud(idSolicitud);

        when(solicitudRepository.findById(idSolicitud)).thenReturn(Optional.of(solicitud));

        ResponseEntity<?> respuesta = solicitudController.obtener(
                idSolicitud, autenticacionComo(200L, "SUPERUSUARIO"));

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
    }
}
