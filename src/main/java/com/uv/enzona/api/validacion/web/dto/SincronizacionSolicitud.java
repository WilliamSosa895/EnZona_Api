package com.uv.enzona.api.validacion.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record SincronizacionSolicitud(@NotNull Long eventoId,
        @NotEmpty @Size(max = 2000) List<@Valid RegistroOffline> registros) {
}
