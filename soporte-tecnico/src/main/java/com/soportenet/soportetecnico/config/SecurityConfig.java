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

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Configuración CORS para permitir que el frontend HTML/JS
     * se comunique con el backend Spring Boot.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuracion = new CorsConfiguration();

        configuracion.setAllowedOriginPatterns(List.of("*"));

        configuracion.setAllowedMethods(List.of(
                "GET",
                "POST",
                "PUT",
                "DELETE",
                "PATCH",
                "OPTIONS"
        ));

        configuracion.setAllowedHeaders(List.of("*"));

        UrlBasedCorsConfigurationSource fuente =
                new UrlBasedCorsConfigurationSource();

        fuente.registerCorsConfiguration("/**", configuracion);

        return fuente;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            CorsConfigurationSource corsConfigurationSource) throws Exception {

        http

                // =====================================================
                // CORS
                // =====================================================
                .cors(cors -> cors
                        .configurationSource(corsConfigurationSource)
                )

                // =====================================================
                // CSRF
                // =====================================================
                .csrf(csrf -> csrf.disable())

                // =====================================================
                // SESIONES
                // =====================================================
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                // =====================================================
                // AUTORIZACIONES
                // =====================================================
                .authorizeHttpRequests(auth -> auth

                        // =================================================
                        // AUTENTICACIÓN
                        // =================================================
                        .requestMatchers(
                                "/api/auth/**",
                                "/api/usuarios/activacion"
                        ).permitAll()


                        // =================================================
                        // CONFIGURACIÓN DEL SISTEMA
                        // =================================================
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/configuracion",
                                "/api/configuracion/logo/archivo"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/configuracion",
                                "/api/configuracion/logo"
                        ).hasRole("SUPERUSUARIO")


                        // =================================================
                        // USUARIOS
                        // =================================================
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/usuarios/invitaciones"
                        ).hasRole("SUPERUSUARIO")

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/usuarios/*/estado"
                        ).hasRole("SUPERUSUARIO")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/usuarios"
                        ).hasRole("SUPERUSUARIO")


                        // =================================================
                        // GRUPOS TÉCNICOS
                        // =================================================
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/grupos-tecnicos/**"
                        ).hasRole("SUPERUSUARIO")

                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/grupos-tecnicos/**"
                        ).hasRole("SUPERUSUARIO")


                        // =================================================
                        // RESPALDOS
                        // =================================================

                        // Obtener configuración del respaldo
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/respaldos/configuracion"
                        ).hasRole("SUPERUSUARIO")

                        // Guardar configuración del respaldo
                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/respaldos/configuracion"
                        ).hasRole("SUPERUSUARIO")

                        // Listar respaldos
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/respaldos"
                        ).hasRole("SUPERUSUARIO")

                        // Generar respaldo FULL
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/respaldos/full"
                        ).hasRole("SUPERUSUARIO")


                        // =================================================
                        // SOLICITUDES - CLIENTE
                        // =================================================
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/solicitudes"
                        ).hasRole("CLIENTE")

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/solicitudes/*/confirmacion"
                        ).hasRole("CLIENTE")


                        // =================================================
                        // SOLICITUDES - TÉCNICO
                        // =================================================
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/solicitudes/mis-tareas"
                        ).hasRole("TECNICO")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/solicitudes/mis-tareas/resumen"
                        ).hasRole("TECNICO")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/solicitudes/mis-tareas/estadisticas"
                        ).hasRole("TECNICO")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/solicitudes/mis-estadisticas"
                        ).hasRole("CLIENTE")


                        // =================================================
                        // DASHBOARDS
                        // =================================================
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/dashboard/administrador"
                        ).hasRole("ADMINISTRADOR")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/dashboard/superusuario"
                        ).hasRole("SUPERUSUARIO")


                        // =================================================
                        // ASIGNACIONES
                        // =================================================
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/solicitudes/*/asignaciones"
                        ).hasRole("ADMINISTRADOR")

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/solicitudes/*/reapertura"
                        ).hasRole("ADMINISTRADOR")


                        // =================================================
                        // REPORTES
                        // =================================================
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/solicitudes/*/reportes"
                        ).hasRole("TECNICO")

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/reportes/*/aprobacion"
                        ).hasRole("ADMINISTRADOR")

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/reportes/*/rechazo"
                        ).hasRole("ADMINISTRADOR")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/reportes"
                        ).hasRole("ADMINISTRADOR")


                        // =================================================
                        // ADJUNTOS
                        // =================================================
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/solicitudes/*/adjuntos"
                        ).hasAnyRole(
                                "CLIENTE",
                                "TECNICO"
                        )


                        // =================================================
                        // CLIENTES
                        // =================================================
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/clientes"
                        ).hasAnyRole(
                                "ADMINISTRADOR",
                                "SUPERUSUARIO"
                        )

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/clientes/*/estado-pago"
                        ).hasAnyRole(
                                "ADMINISTRADOR",
                                "SUPERUSUARIO"
                        )

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/clientes/mi-perfil"
                        ).hasRole("CLIENTE")


                        // =================================================
                        // TÉCNICOS
                        // =================================================
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/tecnicos/buscar"
                        ).hasAnyRole(
                                "ADMINISTRADOR",
                                "SUPERUSUARIO"
                        )

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/tecnicos/*/perfil"
                        ).hasRole("SUPERUSUARIO")


                        // =================================================
                        // AUDITORÍA
                        // =================================================
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/auditoria/**"
                        ).hasRole("SUPERUSUARIO")


                        // =================================================
                        // ANUNCIOS
                        // =================================================
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/anuncios"
                        ).hasRole("ADMINISTRADOR")

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/anuncios/*/desactivacion"
                        ).hasRole("ADMINISTRADOR")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/anuncios/todos"
                        ).hasRole("ADMINISTRADOR")


                        // =================================================
                        // CUALQUIER OTRA PETICIÓN
                        // =================================================
                        .anyRequest().authenticated()
                )

                // =====================================================
                // FILTRO JWT
                // =====================================================
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}