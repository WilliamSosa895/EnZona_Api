package com.uv.enzona.api.eventos.web.dto;

import com.uv.enzona.api.eventos.modelo.EstadoEvento;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Evento tal como lo consumen la web y la app Android ({@code EventoDto}). Los importes son
 * texto con dos decimales y las fechas, ISO-8601 con el desfase de la Ciudad de México.
 */
public record EventoRespuesta(
        Long id,
        Long organizadorId,
        String nombre,
        String descripcion,
        String lugar,
        String direccion,
        String ciudad,
        Double latitud,
        Double longitud,
        Integer categoriaId,
        String categoria,
        OffsetDateTime fechaHoraInicio,
        OffsetDateTime fechaHoraFin,
        boolean esDePago,
        boolean requiereAsiento,
        int aforo,
        @Schema(description = "Lugares que aún pueden venderse o confirmarse") int disponibles,
        @Schema(description = "Precio más bajo; \"0.00\" en eventos gratuitos") String precioDesde,
        EstadoEvento estado,
        @Schema(description = "Solo si la búsqueda incluyó lat y lng") Double distanciaKm,
        OffsetDateTime fechaCancelacion,
        String motivoCancelacion,
        @Schema(description = "Solo en el detalle del evento") List<TipoBoletoRespuesta> tiposBoleto,
        @Schema(description = "URL pública de la portada; null si no tiene. Relativa (/api/imagenes/…) con el almacén local")
        String imagenUrl) {
}
