package com.uv.enzona.api.identidad.web.dto;

import com.uv.enzona.api.identidad.modelo.CanalVerificacion;
import com.uv.enzona.api.identidad.modelo.EstadoUsuario;
import com.uv.enzona.api.identidad.modelo.Usuario;
import java.util.ArrayList;
import java.util.List;

public record VerificacionRespuesta(EstadoUsuario estado, boolean correoVerificado, boolean telefonoVerificado,
        List<CanalVerificacion> verificacionesPendientes) {

    public static VerificacionRespuesta de(Usuario usuario) {
        return new VerificacionRespuesta(usuario.getEstado(), usuario.isCorreoVerificado(),
                usuario.isTelefonoVerificado(), pendientes(usuario));
    }

    static List<CanalVerificacion> pendientes(Usuario usuario) {
        List<CanalVerificacion> pendientes = new ArrayList<>();
        if (!usuario.isCorreoVerificado()) {
            pendientes.add(CanalVerificacion.CORREO);
        }
        if (usuario.getTelefono() != null && !usuario.isTelefonoVerificado()) {
            pendientes.add(CanalVerificacion.TELEFONO);
        }
        return pendientes;
    }
}
