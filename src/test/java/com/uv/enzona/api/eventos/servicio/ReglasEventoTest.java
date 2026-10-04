package com.uv.enzona.api.eventos.servicio;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.uv.enzona.api.comun.error.ApiException;
import com.uv.enzona.api.eventos.web.dto.EventoSolicitud;
import com.uv.enzona.api.eventos.web.dto.SeccionSolicitud;
import com.uv.enzona.api.eventos.web.dto.TipoBoletoSolicitud;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

/** CP-31: reglas de consistencia de un evento. */
class ReglasEventoTest {

    private static final OffsetDateTime INICIO = OffsetDateTime.parse("2026-11-20T19:00:00-06:00");

    private static EventoSolicitud evento(boolean dePago, boolean conAsiento, int aforo, OffsetDateTime fin,
            BigDecimal lat, BigDecimal lng, List<TipoBoletoSolicitud> tipos, List<SeccionSolicitud> secciones) {
        return new EventoSolicitud("Concierto", null, "Foro", null, "Orizaba", lat, lng, null, INICIO, fin, aforo,
                dePago, conAsiento, tipos, secciones, false);
    }

    private static TipoBoletoSolicitud tipo(String nombre, String precio, int cantidad) {
        return new TipoBoletoSolicitud(null, nombre, new BigDecimal(precio), cantidad);
    }

    @Test
    void aceptaUnEventoGratuitoSinTipos() {
        assertThatCode(() -> ReglasEvento.validar(evento(false, false, 100, null, null, null, null, null), true))
                .doesNotThrowAnyException();
    }

    @Test
    void rechazaFinAnteriorAlInicio() {
        assertThatThrownBy(() -> ReglasEvento.validar(
                evento(false, false, 100, INICIO.minusMinutes(1), null, null, null, null), true))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void rechazaCoordenadasIncompletas() {
        assertThatThrownBy(() -> ReglasEvento.validar(
                evento(false, false, 100, null, new BigDecimal("18.85"), null, null, null), true))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void unEventoDePagoNecesitaTiposConPrecio() {
        assertThatThrownBy(() -> ReglasEvento.validar(evento(true, false, 100, null, null, null, List.of(), null), true))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> ReglasEvento.validar(
                evento(true, false, 100, null, null, null, List.of(tipo("General", "0", 10)), null), true))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void laSumaDeBoletosNoSuperaElAforo() {
        List<TipoBoletoSolicitud> tipos = List.of(tipo("General", "150", 80), tipo("VIP", "300", 21));
        assertThatThrownBy(() -> ReglasEvento.validar(evento(true, false, 100, null, null, null, tipos, null), true))
                .isInstanceOf(ApiException.class);
        List<TipoBoletoSolicitud> justos = List.of(tipo("General", "150", 80), tipo("VIP", "300", 20));
        assertThatCode(() -> ReglasEvento.validar(evento(true, false, 100, null, null, null, justos, null), true))
                .doesNotThrowAnyException();
    }

    @Test
    void elTotalDeAsientosDebeIgualarAlAforo() {
        List<TipoBoletoSolicitud> tipos = List.of(tipo("Platea", "200", 50));
        List<SeccionSolicitud> secciones = List.of(new SeccionSolicitud("Platea", 5, 10));
        assertThatCode(() -> ReglasEvento.validar(evento(true, true, 50, null, null, null, tipos, secciones), true))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> ReglasEvento.validar(evento(true, true, 60, null, null, null, tipos, secciones), true))
                .isInstanceOf(ApiException.class);
    }
}
