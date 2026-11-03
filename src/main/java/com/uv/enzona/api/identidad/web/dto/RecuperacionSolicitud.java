package com.uv.enzona.api.identidad.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Correo de la cuenta cuya contraseña se quiere restablecer. */
public record RecuperacionSolicitud(@NotBlank @Email String correo) {
}
