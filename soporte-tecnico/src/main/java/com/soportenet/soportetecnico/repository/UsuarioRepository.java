package com.soportenet.soportetecnico.repository;

import com.soportenet.soportetecnico.dto.ConteoProjection;
import com.soportenet.soportetecnico.dto.UsuarioBusquedaProjection;
import com.soportenet.soportetecnico.entity.Usuario;
import com.soportenet.soportetecnico.enums.RolUsuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByCorreo(String correo);

    /** Superusuario: listado de usuarios, filtrable por rol (caso de uso 4.4.3). */
    Page<Usuario> findByRol(RolUsuario rol, Pageable pageable);

    
    @Query(value = "SELECT sp_invitar_usuario(:idSuperusuario, :nombreUsuario, :correo, CAST(:rol AS rol_usuario_tipo), :diasValidez)",
           nativeQuery = true)
    String invitarUsuario(
            @Param("idSuperusuario") Long idSuperusuario,
            @Param("nombreUsuario") String nombreUsuario,
            @Param("correo") String correo,
            @Param("rol") String rol,
            @Param("diasValidez") Integer diasValidez
    );

    
    @Query(value = "SELECT sp_activar_cuenta(:token, :contrasenaHash)",
           nativeQuery = true)
    Long activarCuenta(
            @Param("token") String token,
            @Param("contrasenaHash") String contrasenaHash
    );

    
    @Query(value = "SELECT sp_cambiar_estado_cuenta(:idSuperusuario, :idUsuarioObjetivo, CAST(:nuevoEstado AS estado_cuenta_tipo))",
           nativeQuery = true)
    void cambiarEstadoCuenta(
            @Param("idSuperusuario") Long idSuperusuario,
            @Param("idUsuarioObjetivo") Long idUsuarioObjetivo,
            @Param("nuevoEstado") String nuevoEstado
    );

    
    @Query(value = "SELECT sp_cambiar_contrasena(:idUsuario, :contrasenaHash)",
           nativeQuery = true)
    void cambiarContrasena(
            @Param("idUsuario") Long idUsuario,
            @Param("contrasenaHash") String contrasenaHash
    );

    
    @Query(value = "SELECT sp_solicitar_recuperacion_contrasena(:correo, :horasValidez)",
           nativeQuery = true)
    String solicitarRecuperacion(
            @Param("correo") String correo,
            @Param("horasValidez") Integer horasValidez
    );

    
    @Query(value = "SELECT sp_restablecer_contrasena(:token, :contrasenaHash)",
           nativeQuery = true)
    Long restablecerContrasena(
            @Param("token") String token,
            @Param("contrasenaHash") String contrasenaHash
    );

    
    @Query(value = "SELECT * FROM fn_buscar_usuarios(:termino)", nativeQuery = true)
    List<UsuarioBusquedaProjection> buscarUsuarios(@Param("termino") String termino);

    
    @Query(value = "SELECT * FROM fn_buscar_usuarios(:termino, 'tecnico')", nativeQuery = true)
    List<UsuarioBusquedaProjection> buscarTecnicos(@Param("termino") String termino);

    /** Superusuario: cuantos usuarios hay de cada rol, via fn_conteo_usuarios_rol(). */
    @Query(value = "SELECT * FROM fn_conteo_usuarios_rol()", nativeQuery = true)
    List<ConteoProjection> contarPorRol();
}
