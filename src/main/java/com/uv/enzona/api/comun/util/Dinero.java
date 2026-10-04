package com.uv.enzona.api.comun.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Importes en pesos mexicanos. Se envían como texto con dos decimales ("360.00") para que los
 * clientes no pierdan precisión con números de punto flotante (contrato de la app Android).
 */
public final class Dinero {

    public static final String MONEDA = "MXN";

    private Dinero() {
    }

    public static BigDecimal normalizar(BigDecimal importe) {
        return (importe == null ? BigDecimal.ZERO : importe).setScale(2, RoundingMode.HALF_UP);
    }

    public static String texto(BigDecimal importe) {
        return normalizar(importe).toPlainString();
    }
}
