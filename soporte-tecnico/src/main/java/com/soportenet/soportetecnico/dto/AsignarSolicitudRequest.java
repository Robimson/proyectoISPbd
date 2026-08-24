package com.soportenet.soportetecnico.dto;

/**
 * Lo que el Administrador envia para asignar (o reasignar) una solicitud a
 * un tecnico o a un grupo tecnico (caso de uso 4.3.4 del documento).
 * idAdministrador sale del JWT, no del body. idTecnico e idGrupo: se debe
 * indicar exactamente uno de los dos; esa validacion, la de
 * motivoReasignacion obligatorio en reasignaciones, y la de que la
 * solicitud no este Cerrada, ya viven en sp_asignar_solicitud.
 */
public class AsignarSolicitudRequest {

    private Long idTecnico;

    private Long idGrupo;

    private Integer idPrioridad;

    private String motivoReasignacion;

    public AsignarSolicitudRequest() {
    }

    public Long getIdTecnico() {
        return idTecnico;
    }

    public void setIdTecnico(Long idTecnico) {
        this.idTecnico = idTecnico;
    }

    public Long getIdGrupo() {
        return idGrupo;
    }

    public void setIdGrupo(Long idGrupo) {
        this.idGrupo = idGrupo;
    }

    public Integer getIdPrioridad() {
        return idPrioridad;
    }

    public void setIdPrioridad(Integer idPrioridad) {
        this.idPrioridad = idPrioridad;
    }

    public String getMotivoReasignacion() {
        return motivoReasignacion;
    }

    public void setMotivoReasignacion(String motivoReasignacion) {
        this.motivoReasignacion = motivoReasignacion;
    }
}
