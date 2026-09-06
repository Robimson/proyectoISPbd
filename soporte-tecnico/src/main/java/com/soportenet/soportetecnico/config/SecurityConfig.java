package com.soportenet.soportetecnico.config;

import com.soportenet.soportetecnico.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Fase 2 de autenticacion: cada endpoint exige el rol correspondiente segun
 * la seccion 2 del documento, y el JwtAuthenticationFilter corre antes del
 * filtro de login para dejar al usuario autenticado en el SecurityContext.
 *
 * No hay chequeo de "dueno del recurso" todavia (ej: que un Cliente solo
 * pueda ver SUS solicitudes, o que un Tecnico solo reporte tickets que
 * tiene asignados): eso vive en las validaciones de los procedimientos SQL
 * para las escrituras (sp_enviar_reporte, sp_confirmar_cliente, etc. ya
 * validan pertenencia/autorizacion), pero los GET de lectura por ahora solo
 * exigen estar autenticado, no ser el dueno especifico del recurso.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * El frontend es HTML/JS plano servido desde un origen distinto al del
     * backend (ej. Live Server en localhost:5500), asi que el navegador
     * exige CORS. Se permite cualquier origen porque la autenticacion es
     * por JWT en el header Authorization, no por cookies (no hay
     * credenciales de navegador involucradas, asi que "*" es seguro aqui).
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuracion = new CorsConfiguration();
        configuracion.setAllowedOriginPatterns(List.of("*"));
        configuracion.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuracion.setAllowedHeaders(List.of("*"));

        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/**", configuracion);
        return fuente;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                     JwtAuthenticationFilter jwtAuthenticationFilter,
                                                     CorsConfigurationSource corsConfigurationSource) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**", "/api/usuarios/activacion").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/configuracion", "/api/configuracion/logo/archivo").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/configuracion", "/api/configuracion/logo").hasRole("SUPERUSUARIO")
                        .requestMatchers(HttpMethod.POST, "/api/usuarios/invitaciones").hasRole("SUPERUSUARIO")
                        .requestMatchers(HttpMethod.POST, "/api/usuarios/*/estado").hasRole("SUPERUSUARIO")
                        .requestMatchers(HttpMethod.GET, "/api/usuarios").hasRole("SUPERUSUARIO")
                        .requestMatchers(HttpMethod.POST, "/api/grupos-tecnicos/**").hasRole("SUPERUSUARIO")
                        .requestMatchers(HttpMethod.DELETE, "/api/grupos-tecnicos/**").hasRole("SUPERUSUARIO")
                        .requestMatchers(HttpMethod.POST, "/api/solicitudes").hasRole("CLIENTE")
                        .requestMatchers(HttpMethod.POST, "/api/solicitudes/*/confirmacion").hasRole("CLIENTE")
                        .requestMatchers(HttpMethod.GET, "/api/solicitudes/mis-tareas").hasRole("TECNICO")
                        .requestMatchers(HttpMethod.GET, "/api/solicitudes/mis-tareas/resumen").hasRole("TECNICO")
                        .requestMatchers(HttpMethod.GET, "/api/solicitudes/mis-tareas/estadisticas").hasRole("TECNICO")
                        .requestMatchers(HttpMethod.GET, "/api/solicitudes/mis-estadisticas").hasRole("CLIENTE")
                        .requestMatchers(HttpMethod.GET, "/api/dashboard/administrador").hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.GET, "/api/dashboard/superusuario").hasRole("SUPERUSUARIO")
                        .requestMatchers(HttpMethod.POST, "/api/solicitudes/*/asignaciones").hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.POST, "/api/solicitudes/*/reapertura").hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.POST, "/api/solicitudes/*/reportes").hasRole("TECNICO")
                        .requestMatchers(HttpMethod.POST, "/api/reportes/*/aprobacion").hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.POST, "/api/reportes/*/rechazo").hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.GET, "/api/reportes").hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.POST, "/api/solicitudes/*/adjuntos").hasAnyRole("CLIENTE", "TECNICO")
                        .requestMatchers(HttpMethod.GET, "/api/clientes").hasAnyRole("ADMINISTRADOR", "SUPERUSUARIO")
                        .requestMatchers(HttpMethod.POST, "/api/clientes/*/estado-pago").hasAnyRole("ADMINISTRADOR", "SUPERUSUARIO")
                        .requestMatchers(HttpMethod.GET, "/api/clientes/mi-perfil").hasRole("CLIENTE")
                        .requestMatchers(HttpMethod.GET, "/api/tecnicos/buscar").hasAnyRole("ADMINISTRADOR", "SUPERUSUARIO")
                        .requestMatchers(HttpMethod.POST, "/api/tecnicos/*/perfil").hasRole("SUPERUSUARIO")
                        .requestMatchers(HttpMethod.GET, "/api/auditoria/**").hasRole("SUPERUSUARIO")
                        .requestMatchers(HttpMethod.POST, "/api/anuncios").hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.POST, "/api/anuncios/*/desactivacion").hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.GET, "/api/anuncios/todos").hasRole("ADMINISTRADOR")
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
