package com.soportenet.soportetecnico.repository;

import com.soportenet.soportetecnico.entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

  
    @Query(value = "SELECT sp_cambiar_estado_pago(:idAdministrador, :idCliente, CAST(:nuevoEstadoPago AS estado_pago_tipo))",
           nativeQuery = true)
    void cambiarEstadoPago(
            @Param("idAdministrador") Long idAdministrador,
            @Param("idCliente") Long idCliente,
            @Param("nuevoEstadoPago") String nuevoEstadoPago
    );
}
