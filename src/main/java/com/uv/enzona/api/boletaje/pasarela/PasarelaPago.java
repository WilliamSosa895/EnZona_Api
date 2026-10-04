package com.uv.enzona.api.boletaje.pasarela;

import java.math.BigDecimal;

/**
 * Puerto hacia la pasarela de pago externa (Anexo E, DA-07). La elección del proveedor sigue
 * abierta (riesgo E.13): para integrarlo basta con una nueva implementación de esta interfaz,
 * sin tocar el resto del sistema.
 */
public interface PasarelaPago {

    ResultadoCobro cobrar(SolicitudCobro solicitud);

    /** Consulta el resultado de un cobro anterior por su clave de idempotencia. */
    ResultadoCobro consultar(String claveIdempotencia);

    ResultadoReembolso reembolsar(String referenciaCobro, BigDecimal monto, String claveIdempotencia);
}
