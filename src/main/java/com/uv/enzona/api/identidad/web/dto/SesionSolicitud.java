package com.uv.enzona.api.identidad.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Inicio de sesión con correo o CURP (RF-03). */
public record SesionSolicitud(
        @Schema(description = "Correo electrónico o CURP", example = "ana@correo.com")
        @NotBlank @Size(max = 180) String identificador,
        @NotBlank @Size(max = 72) String contrasena) {

    @Override
    public String toString() {
        return "SesionSolicitud[identificador=******, contrasena=******]";
    }
}
