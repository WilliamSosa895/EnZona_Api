package com.uv.enzona.api.validacion.web.dto;

import com.uv.enzona.api.validacion.modelo.MotivoRechazo;
import com.uv.enzona.api.validacion.modelo.ResultadoAcceso;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

public record ResultadoSincronizado(String qr, OffsetDateTime fechaHora, ResultadoAcceso resultado,
        MotivoRechazo motivo,
        @Schema(description = "true si otro dispositivo ya había admitido este boleto (prevalece el escaneo más antiguo)")
        boolean conflicto,
        @Schema(description = "true si este escaneo ya se había sincronizado antes") boolean duplicado) {
}
