package com.uv.enzona.api.administracion;

import com.uv.enzona.api.identidad.modelo.EstadoUsuario;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record CambioEstadoSolicitud(@NotNull @Schema(description = "ACTIVO o BLOQUEADO") EstadoUsuario estado) {
}
