package com.uv.enzona.api.administracion;

import com.uv.enzona.api.comun.util.Fechas;
import com.uv.enzona.api.identidad.modelo.EstadoUsuario;
import com.uv.enzona.api.identidad.modelo.Usuario;
import java.time.OffsetDateTime;
import java.util.List;

/** Vista administrativa de una cuenta. No incluye el CURP. */
public record UsuarioAdminRespuesta(Long id, String nombre, String correo, String telefono, EstadoUsuario estado,
        boolean correoVerificado, List<String> roles, OffsetDateTime fechaRegistro) {

    public static UsuarioAdminRespuesta de(Usuario u) {
        return new UsuarioAdminRespuesta(u.getId(), u.getNombre(), u.getCorreo(), u.getTelefono(), u.getEstado(),
                u.isCorreoVerificado(), u.nombresRoles(), Fechas.enZonaMx(u.getFechaRegistro()));
    }
}
