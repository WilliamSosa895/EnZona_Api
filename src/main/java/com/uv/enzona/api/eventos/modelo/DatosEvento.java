package com.uv.enzona.api.eventos.modelo;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** Datos editables de un evento, ya normalizados por la capa de servicio. */
public record DatosEvento(String nombre, String descripcion, String lugar, String direccion, String ciudad,
        BigDecimal latitud, BigDecimal longitud, Integer categoriaId, OffsetDateTime fechaHoraInicio,
        OffsetDateTime fechaHoraFin, int aforo, boolean esDePago, boolean requiereAsiento) {
}
