package com.uv.enzona.api.cancelacion;

import static org.assertj.core.api.Assertions.assertThat;

import com.uv.enzona.api.eventos.modelo.EstadoEvento;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

/** RF-14: regla de cancelación. */
class PoliticaCancelacionTest {

    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-04T12:00:00-06:00");
    private static final OffsetDateTime MANANA = AHORA.plusDays(1);

    @Test
    void unEventoDePagoConVentasReembolsaElTotalSinComision() {
        PoliticaCancelacion p = PoliticaCancelacion.calcular(EstadoEvento.PUBLICADO, MANANA, AHORA, true,
                new BigDecimal("1500.00"), 10, false);
        assertThat(p.permitida()).isTrue();
        assertThat(p.tipo()).isEqualTo("DE_PAGO_CON_VENTAS");
        assertThat(p.reembolso()).isEqualTo("1500.00");
        assertThat(p.comision()).isEqualTo("0.00");
    }

    @Test
    void noSeCancelaUnEventoQueYaInicio() {
        PoliticaCancelacion p = PoliticaCancelacion.calcular(EstadoEvento.PUBLICADO, AHORA.minusMinutes(1), AHORA,
                false, BigDecimal.ZERO, 0, false);
        assertThat(p.permitida()).isFalse();
        assertThat(p.motivoBloqueo()).isNotBlank();
    }

    @Test
    void identificaLaCancelacionAdministrativaYLaGratuita() {
        assertThat(PoliticaCancelacion.calcular(EstadoEvento.PUBLICADO, MANANA, AHORA, false, BigDecimal.ZERO, 3, true)
                .tipo()).isEqualTo("ADMINISTRATIVA");
        assertThat(PoliticaCancelacion.calcular(EstadoEvento.PUBLICADO, MANANA, AHORA, false, BigDecimal.ZERO, 3, false)
                .tipo()).isEqualTo("GRATUITO");
    }
}
