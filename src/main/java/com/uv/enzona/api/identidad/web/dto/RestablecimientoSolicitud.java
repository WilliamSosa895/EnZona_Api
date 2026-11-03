package com.uv.enzona.api.identidad.web.dto;

import com.uv.enzona.api.identidad.servicio.PoliticaContrasena;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Código recibido por correo y contraseña nueva. {@link #toString()} nunca muestra la contraseña. */
public record RestablecimientoSolicitud(
        @NotBlank @Email String correo,
        @NotBlank @Pattern(regexp = "^[0-9]{6}$", message = "El código tiene 6 dígitos") String codigo,
        @NotBlank @Pattern(regexp = PoliticaContrasena.REGEX, message = PoliticaContrasena.MENSAJE) String nuevaContrasena) {

    @Override
    public String toString() {
        return "RestablecimientoSolicitud[correo=" + correo + ", codigo=******, nuevaContrasena=******]";
    }
}
