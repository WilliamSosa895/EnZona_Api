package com.uv.enzona.api.eventos.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Distribución de una sección del mapa de asientos: filas A, B, C… y asientos 1..N por fila. */
public record SeccionSolicitud(
        @NotBlank @Size(max = 40) String nombre,
        @Min(1) @Max(26) int filas,
        @Min(1) @Max(200) int asientosPorFila) {

    public int totalAsientos() {
        return filas * asientosPorFila;
    }
}
