package com.uv.enzona.api.identidad.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** El correo y el CURP no se modifican desde el perfil. Cambiar el teléfono exige verificarlo de nuevo. */
public record PerfilActualizacion(
        @NotBlank @Size(max = 120) String nombre,
        @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "El teléfono debe tener de 10 a 15 dígitos") String telefono) {
}
