package com.uv.enzona.api.boletaje.servicio;

import com.uv.enzona.api.boletaje.web.dto.OrdenRespuesta;

/** {@code nueva=false} cuando la clave de idempotencia ya existía y se devuelve la orden original. */
public record ResultadoOrden(OrdenRespuesta orden, boolean nueva) {
}
