package com.uv.enzona.api.eventos.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record TipoBoletoSolicitud(
        @Schema(description = "Solo al modificar: id del tipo existente; null para agregar uno nuevo")
        Long id,
        @NotBlank @Size(max = 80) String nombre,
        @NotNull @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal precio,
        @NotNull @Min(1) @Max(100000) Integer cantidad) {
}
