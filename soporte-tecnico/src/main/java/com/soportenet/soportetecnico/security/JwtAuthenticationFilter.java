package com.soportenet.soportetecnico.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Lee el header "Authorization: Bearer <token>", valida el JWT y deja al
 * usuario autenticado en el SecurityContext. El subject del token es el
 * id_usuario (como String) y el claim "rol" se mapea a un authority
 * "ROLE_<ROL>" para que SecurityConfig pueda usar hasRole(...).
 * Si el token falta o es invalido, simplemente no autentica: es
 * SecurityConfig quien decide si ese endpoint exige login o no.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring("Bearer ".length());

            try {
                Claims claims = jwtService.validarYObtenerClaims(token);
                String idUsuario = claims.getSubject();
                String rol = claims.get("rol", String.class);

                var authority = new SimpleGrantedAuthority("ROLE_" + rol.toUpperCase());
                var authentication = new UsernamePasswordAuthenticationToken(idUsuario, null, List.of(authority));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (JwtException | IllegalArgumentException ex) {
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}
