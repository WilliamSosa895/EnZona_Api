package com.uv.enzona.api.identidad.seguridad;

import com.uv.enzona.api.identidad.modelo.EstadoUsuario;
import com.uv.enzona.api.identidad.modelo.Usuario;
import com.uv.enzona.api.identidad.repositorio.UsuarioRepository;
import java.util.List;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Convierte un JWT válido en la autenticación de la petición. Los roles se leen de la base de
 * datos y no del token: así, bloquear una cuenta o cambiarle un rol surte efecto de inmediato,
 * sin esperar a que el token expire.
 */
@Component
public class UsuarioActivoJwtConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UsuarioRepository usuarios;

    public UsuarioActivoJwtConverter(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Long id;
        try {
            id = Long.valueOf(jwt.getSubject());
        } catch (NumberFormatException e) {
            throw new InvalidBearerTokenException("Token con sujeto no válido");
        }
        Usuario usuario = usuarios.findById(id)
                .orElseThrow(() -> new InvalidBearerTokenException("La cuenta ya no existe"));
        if (usuario.getEstado() != EstadoUsuario.ACTIVO) {
            throw new InvalidBearerTokenException("La cuenta no está activa");
        }
        List<SimpleGrantedAuthority> autoridades = usuario.nombresRoles().stream()
                .map(rol -> new SimpleGrantedAuthority("ROLE_" + rol))
                .toList();
        return new JwtAuthenticationToken(jwt, autoridades, String.valueOf(id));
    }
}
