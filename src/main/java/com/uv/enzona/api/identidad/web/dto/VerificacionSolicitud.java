package com.uv.enzona.api.identidad.web.dto;

import com.uv.enzona.api.identidad.modelo.CanalVerificacion;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerificacionSolicitud(
        @NotBlank @Email String correo,
        @Schema(description = "CORREO (por defecto) o TELEFONO") CanalVerificacion canal,
        @NotBlank @Pattern(regexp = "^[0-9]{6}$", message = "El código tiene 6 dígitos") String codigo) {

    public CanalVerificacion canalOPorDefecto() {
        return canal == null ? CanalVerificacion.CORREO : canal;
    }
}
