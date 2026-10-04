package com.uv.enzona.api.identidad.web.dto;

import com.uv.enzona.api.identidad.modelo.Usuario;
import java.util.List;

/** Mismo contrato que {@code UsuarioDto} de la app Android (id, nombre, roles) más el correo. */
public record UsuarioResumen(Long id, String nombre, String correo, List<String> roles) {

    public static UsuarioResumen de(Usuario usuario) {
        return new UsuarioResumen(usuario.getId(), usuario.getNombre(), usuario.getCorreo(), usuario.nombresRoles());
    }
}
