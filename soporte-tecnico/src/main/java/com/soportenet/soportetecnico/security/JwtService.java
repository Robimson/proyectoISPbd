package com.soportenet.soportetecnico.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * Emite y valida los JWT de sesion. El token lleva el id de usuario como
 * subject y el rol como claim, para que el filtro de seguridad (siguiente
 * fase) pueda autorizar por rol sin volver a consultar la base de datos en
 * cada request.
 */
@Service
public class JwtService {

    private final SecretKey clave;
    private final long expiracionMinutos;

    public JwtService(@Value("${app.jwt.secret}") String secreto,
                       @Value("${app.jwt.expiration-minutes}") long expiracionMinutos) {
        this.clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
        this.expiracionMinutos = expiracionMinutos;
    }

    public String generarToken(Long idUsuario, String rol, String correo) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .subject(idUsuario.toString())
                .claim("rol", rol)
                .claim("correo", correo)
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plusSeconds(expiracionMinutos * 60)))
                .signWith(clave)
                .compact();
    }

    /**
     * Lanza JwtException (token invalido, corrupto o vencido) si no puede
     * validar el token; quien llame decide como traducir eso a una
     * respuesta HTTP.
     */
    public Claims validarYObtenerClaims(String token) throws JwtException {
        return Jwts.parser()
                .verifyWith(clave)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
