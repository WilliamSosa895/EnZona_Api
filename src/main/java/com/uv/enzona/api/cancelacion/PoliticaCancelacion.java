package com.uv.enzona.api.cancelacion;

import com.uv.enzona.api.comun.util.Dinero;
import com.uv.enzona.api.eventos.modelo.EstadoEvento;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Regla de cancelación calculada en el servidor, para que la web y la app muestren lo mismo
 * (RNF-10). Los asistentes reciben siempre el 100 % de lo pagado. La comisión al organizador
 * que propone la app Android no está en los requisitos y queda pendiente: aquí vale 0.
 */
public record PoliticaCancelacion(boolean permitida, String tipo, String importeCobrado, String comision,
        String reembolso, int boletosAfectados, String motivoBloqueo) {

    public static PoliticaCancelacion calcular(EstadoEvento estado, OffsetDateTime inicio, OffsetDateTime ahora,
            boolean esDePago, BigDecimal importeCobrado, int boletosVigentes, boolean cancelacionAdministrativa) {
        String importe = Dinero.texto(importeCobrado);
        String cero = Dinero.texto(BigDecimal.ZERO);
        if (estado == EstadoEvento.CANCELADO || estado == EstadoEvento.REALIZADO) {
            return new PoliticaCancelacion(false, null, importe, cero, cero, 0, "El evento ya está " + estado);
        }
        if (!inicio.isAfter(ahora)) {
            return new PoliticaCancelacion(false, null, importe, cero, cero, 0,
                    "El evento ya inició; solo puede marcarse como finalizado");
        }
        String tipo;
        if (cancelacionAdministrativa) {
            tipo = "ADMINISTRATIVA";
        } else if (estado == EstadoEvento.BORRADOR) {
            tipo = "BORRADOR";
        } else if (!esDePago) {
            tipo = "GRATUITO";
        } else if (importeCobrado.signum() == 0) {
            tipo = "DE_PAGO_SIN_VENTAS";
        } else {
            tipo = "DE_PAGO_CON_VENTAS";
        }
        return new PoliticaCancelacion(true, tipo, importe, cero, importe, boletosVigentes, null);
    }
}
