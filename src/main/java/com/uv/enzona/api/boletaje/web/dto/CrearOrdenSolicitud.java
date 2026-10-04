package com.uv.enzona.api.boletaje.web.dto;

import com.uv.enzona.api.boletaje.modelo.MetodoPago;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Compra o confirmación de N boletos en una sola orden. Mismo contrato que {@code CrearOrdenDto}
 * de la app Android, más {@code tokenPago}: el token que genera el SDK de la pasarela en el cliente.
 */
public record CrearOrdenSolicitud(
        @NotNull Long eventoId,
        @Schema(description = "Opcional si el evento tiene un solo tipo de boleto (p. ej., gratuito)") Long tipoBoletoId,
        @NotNull @Min(1) @Max(50) Integer cantidad,
        @Schema(description = "Un asiento por boleto si el evento lo requiere; vacío si no") List<Long> asientosIds,
        @Schema(description = "Métodos de pago guardados: pendiente (no implementado)") Long metodoPagoId,
        @NotNull @Schema(description = "GRATIS en eventos gratuitos; TARJETA en eventos de pago") MetodoPago metodo,
        @Schema(description = "Token de la pasarela. En sandbox: tok_aprobado, tok_rechazado, tok_pendiente")
        @Size(max = 200) String tokenPago) {

    public List<Long> asientosONada() {
        return asientosIds == null ? List.of() : asientosIds;
    }

    @Override
    public String toString() {
        return "CrearOrdenSolicitud[eventoId=" + eventoId + ", cantidad=" + cantidad + ", metodo=" + metodo
                + ", tokenPago=******]";
    }
}
