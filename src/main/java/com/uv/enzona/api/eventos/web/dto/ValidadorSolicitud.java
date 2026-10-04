package com.uv.enzona.api.eventos.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Designa como personal de acceso a una cuenta ya registrada y activa. */
public record ValidadorSolicitud(@NotBlank @Email String correo) {
}
