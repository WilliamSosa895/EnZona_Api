package com.uv.enzona.api.validacion.web.dto;

import com.uv.enzona.api.validacion.modelo.MotivoRechazo;
import com.uv.enzona.api.validacion.modelo.ResultadoAcceso;
import java.time.OffsetDateTime;

/** Lo que muestra la pantalla del validador. Un rechazo es una respuesta 200, no un error HTTP. */
public record ValidacionRespuesta(ResultadoAcceso resultado, MotivoRechazo motivo, String mensaje, String codigo,
        String tipoBoleto, String asiento, OffsetDateTime fechaUso) {
}
