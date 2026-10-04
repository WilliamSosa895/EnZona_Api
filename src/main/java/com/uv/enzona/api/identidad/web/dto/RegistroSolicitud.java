package com.uv.enzona.api.identidad.web.dto;

import com.uv.enzona.api.identidad.servicio.FormatoCurp;
import com.uv.enzona.api.identidad.servicio.PoliticaContrasena;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Alta de cuenta (RF-01, CU-01). */
public record RegistroSolicitud(
        @NotBlank @Size(max = 120)
        String nombre,

        @NotBlank @Email @Size(max = 180)
        String correo,

        @Schema(description = "Opcional. Si se envía, también debe verificarse con un código.", example = "+522721234567")
        @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "El teléfono debe tener de 10 a 15 dígitos")
        String telefono,

        @NotBlank
        @Pattern(regexp = FormatoCurp.REGEX, flags = Pattern.Flag.CASE_INSENSITIVE, message = FormatoCurp.MENSAJE)
        String curp,

        @NotBlank
        @Pattern(regexp = PoliticaContrasena.REGEX, message = PoliticaContrasena.MENSAJE)
        String contrasena,

        @Schema(description = "true para activar también el rol de organizador")
        Boolean organizador,

        @Schema(description = "Aceptación del aviso de privacidad (LFPDPPP). Debe ser true.")
        @NotNull @AssertTrue(message = "Debes aceptar el aviso de privacidad")
        Boolean aceptaAvisoPrivacidad) {

    @Override
    public String toString() {
        return "RegistroSolicitud[correo=" + correo + ", curp=******, contrasena=******]";
    }
}
