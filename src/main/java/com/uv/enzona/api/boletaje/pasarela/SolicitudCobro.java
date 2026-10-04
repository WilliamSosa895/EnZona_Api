package com.uv.enzona.api.boletaje.pasarela;

import java.math.BigDecimal;

/**
 * Cobro a la pasarela. {@code tokenPago} es el token que genera el SDK de la pasarela en el
 * cliente: la API nunca recibe el número de tarjeta. {@code claveIdempotencia} evita cobrar dos veces.
 */
public record SolicitudCobro(String claveIdempotencia, BigDecimal monto, String moneda, String tokenPago,
        String descripcion) {

    @Override
    public String toString() {
        return "SolicitudCobro[clave=" + claveIdempotencia + ", monto=" + monto + ", token=******]";
    }
}
