package com.uv.enzona.api.validacion.web.dto;

import java.util.List;

public record SincronizacionRespuesta(int procesados, int permitidos, int rechazados, int conflictos,
        List<ResultadoSincronizado> resultados) {
}
