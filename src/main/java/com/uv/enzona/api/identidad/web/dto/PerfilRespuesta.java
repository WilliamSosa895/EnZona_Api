package com.uv.enzona.api.identidad.web.dto;

import com.uv.enzona.api.comun.util.Fechas;
import com.uv.enzona.api.identidad.modelo.EstadoUsuario;
import com.uv.enzona.api.identidad.modelo.Usuario;
import java.time.OffsetDateTime;
import java.util.List;

/** Perfil propio (RF-04). El CURP nunca se devuelve. */
public record PerfilRespuesta(Long id, String nombre, String correo, String telefono, boolean correoVerificado,
        boolean telefonoVerificado, EstadoUsuario estado, List<String> roles, OffsetDateTime fechaRegistro) {

    public static PerfilRespuesta de(Usuario usuario) {
        return new PerfilRespuesta(usuario.getId(), usuario.getNombre(), usuario.getCorreo(), usuario.getTelefono(),
                usuario.isCorreoVerificado(), usuario.isTelefonoVerificado(), usuario.getEstado(),
                usuario.nombresRoles(), Fechas.enZonaMx(usuario.getFechaRegistro()));
    }
}
