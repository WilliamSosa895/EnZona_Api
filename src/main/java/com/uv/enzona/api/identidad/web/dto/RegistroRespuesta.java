package com.uv.enzona.api.identidad.web.dto;

import com.uv.enzona.api.identidad.modelo.CanalVerificacion;
import com.uv.enzona.api.identidad.modelo.EstadoUsuario;
import com.uv.enzona.api.identidad.modelo.Usuario;
import java.util.List;

public record RegistroRespuesta(Long usuarioId, String correo, EstadoUsuario estado,
        List<CanalVerificacion> verificacionesPendientes) {

    public static RegistroRespuesta de(Usuario usuario) {
        return new RegistroRespuesta(usuario.getId(), usuario.getCorreo(), usuario.getEstado(),
                VerificacionRespuesta.pendientes(usuario));
    }
}
