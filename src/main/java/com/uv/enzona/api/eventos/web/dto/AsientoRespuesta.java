package com.uv.enzona.api.eventos.web.dto;

import com.uv.enzona.api.eventos.modelo.Asiento;
import com.uv.enzona.api.eventos.modelo.EstadoAsiento;

public record AsientoRespuesta(Long id, String seccion, String fila, String numero, EstadoAsiento estado,
        String etiqueta) {

    public static AsientoRespuesta de(Asiento asiento) {
        return new AsientoRespuesta(asiento.getId(), asiento.getSeccion(), asiento.getFila(), asiento.getNumero(),
                asiento.getEstado(), asiento.etiqueta());
    }
}
