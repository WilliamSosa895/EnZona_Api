package com.uv.enzona.api.comun.seguridad;

import com.uv.enzona.api.comun.error.ApiException;
import com.uv.enzona.api.comun.error.ErrorCodigo;
import java.util.Optional;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Acceso al usuario autenticado de la petición en curso. */
@Component
public class UsuarioActual {

    public Long id() {
        return idOpcional().orElseThrow(() -> new ApiException(ErrorCodigo.NO_AUTENTICADO));
    }

    /** Para rutas públicas que muestran más información si hay sesión (p. ej., borradores propios). */
    public Optional<Long> idOpcional() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion == null || !autenticacion.isAuthenticated()
                || autenticacion instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.valueOf(autenticacion.getName()));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    public boolean tieneRol(String rol) {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        return autenticacion != null && autenticacion.getAuthorities().stream()
                .anyMatch(autoridad -> ("ROLE_" + rol).equals(autoridad.getAuthority()));
    }

    public boolean esAdmin() {
        return tieneRol("ADMIN");
    }
}
