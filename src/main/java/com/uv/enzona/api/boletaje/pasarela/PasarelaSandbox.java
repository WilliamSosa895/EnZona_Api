package com.uv.enzona.api.boletaje.pasarela;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Pasarela simulada para desarrollo y pruebas (PAYMENT_PROVIDER=sandbox). Responde según el
 * token recibido, igual que las tarjetas de prueba de una pasarela real:
 * <ul>
 *   <li>{@code tok_rechazado}: cobro rechazado (CP-10, CP-MULTI-06).</li>
 *   <li>{@code tok_pendiente}: queda pendiente hasta que llegue el webhook.</li>
 *   <li>{@code tok_error}: la pasarela no responde.</li>
 *   <li>cualquier otro: aprobado.</li>
 * </ul>
 * Es idempotente por clave: repetir la misma clave devuelve el mismo resultado sin cobrar de nuevo.
 */
@Component
@ConditionalOnProperty(name = "enzona.pasarela.proveedor", havingValue = "sandbox")
public class PasarelaSandbox implements PasarelaPago {

    private final Map<String, ResultadoCobro> cobros = new ConcurrentHashMap<>();
    private final Map<String, ResultadoReembolso> reembolsos = new ConcurrentHashMap<>();

    @Override
    public ResultadoCobro cobrar(SolicitudCobro solicitud) {
        if ("tok_error".equals(solicitud.tokenPago())) {
            throw new PasarelaNoDisponibleException("Sandbox: pasarela no disponible", null);
        }
        return cobros.computeIfAbsent(solicitud.claveIdempotencia(), clave -> switch (String.valueOf(solicitud.tokenPago())) {
            case "tok_rechazado" -> new ResultadoCobro(EstadoCobro.RECHAZADO, referencia("ch"), "Tarjeta rechazada");
            case "tok_pendiente" -> new ResultadoCobro(EstadoCobro.PENDIENTE, referencia("ch"), "En espera de confirmación");
            default -> new ResultadoCobro(EstadoCobro.APROBADO, referencia("ch"), "Cobro aprobado");
        });
    }

    @Override
    public ResultadoCobro consultar(String claveIdempotencia) {
        return cobros.getOrDefault(claveIdempotencia,
                new ResultadoCobro(EstadoCobro.DESCONOCIDO, null, "Sin registro en la pasarela"));
    }

    @Override
    public ResultadoReembolso reembolsar(String referenciaCobro, BigDecimal monto, String claveIdempotencia) {
        return reembolsos.computeIfAbsent(claveIdempotencia,
                clave -> new ResultadoReembolso(true, referencia("re"), "Reembolso aplicado"));
    }

    /** Usado por las pruebas y por el webhook simulado para cerrar un cobro pendiente. */
    public void resolverPendiente(String claveIdempotencia, EstadoCobro estado) {
        cobros.computeIfPresent(claveIdempotencia, (clave, actual) -> new ResultadoCobro(estado, actual.referencia(), "Resuelto"));
    }

    private static String referencia(String prefijo) {
        return "sbx_" + prefijo + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
    }
}
