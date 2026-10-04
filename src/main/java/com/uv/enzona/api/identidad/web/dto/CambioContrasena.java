package com.uv.enzona.api.identidad.web.dto;

import com.uv.enzona.api.identidad.servicio.PoliticaContrasena;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CambioContrasena(
        @NotBlank @Size(max = 72) String actual,
        @NotBlank @Pattern(regexp = PoliticaContrasena.REGEX, message = PoliticaContrasena.MENSAJE) String nueva) {

    @Override
    public String toString() {
        return "CambioContrasena[******]";
    }
}
