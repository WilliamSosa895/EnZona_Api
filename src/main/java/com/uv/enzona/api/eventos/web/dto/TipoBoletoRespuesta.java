package com.uv.enzona.api.eventos.web.dto;

import com.uv.enzona.api.comun.util.Dinero;
import com.uv.enzona.api.eventos.modelo.TipoBoleto;

public record TipoBoletoRespuesta(Long id, String nombre, String precio, int cantidadTotal, int disponibles) {

    public static TipoBoletoRespuesta de(TipoBoleto tipo) {
        return new TipoBoletoRespuesta(tipo.getId(), tipo.getNombre(), Dinero.texto(tipo.getPrecio()),
                tipo.getCantidadTotal(), tipo.getCantidadDisponible());
    }
}
