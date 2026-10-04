package com.uv.enzona.api.identidad.web.dto;

import com.uv.enzona.api.identidad.modelo.CanalVerificacion;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ReenvioSolicitud(@NotBlank @Email String correo, CanalVerificacion canal) {

    public CanalVerificacion canalOPorDefecto() {
        return canal == null ? CanalVerificacion.CORREO : canal;
    }
}
