package com.uv.enzona.api.eventos.servicio;

import java.time.OffsetDateTime;

/** Criterios de búsqueda del catálogo. Todos son opcionales. */
public record FiltroEventos(String texto, Integer categoriaId, String ciudad, OffsetDateTime desde,
        OffsetDateTime hasta, Boolean esDePago, Double latitud, Double longitud, Double radioKm,
        int pagina, int tamano) {

    public boolean porUbicacion() {
        return latitud != null && longitud != null;
    }
}
