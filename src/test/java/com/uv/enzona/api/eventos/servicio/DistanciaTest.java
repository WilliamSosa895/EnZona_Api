package com.uv.enzona.api.eventos.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

/** RF-19: cálculo de distancias para el descubrimiento por cercanía. */
class DistanciaTest {

    // Parque Castillo (Orizaba) y Parque 21 de Mayo (Córdoba), coordenadas aproximadas.
    private static final double ORIZABA_LAT = 18.8506;
    private static final double ORIZABA_LNG = -97.1003;
    private static final double CORDOBA_LAT = 18.8942;
    private static final double CORDOBA_LNG = -96.9346;

    @Test
    void laDistanciaAUnMismoPuntoEsCero() {
        assertThat(Distancia.km(ORIZABA_LAT, ORIZABA_LNG, ORIZABA_LAT, ORIZABA_LNG)).isEqualTo(0.0);
    }

    @Test
    void calculaUnaDistanciaRealista() {
        assertThat(Distancia.km(ORIZABA_LAT, ORIZABA_LNG, CORDOBA_LAT, CORDOBA_LNG)).isCloseTo(18.0, within(2.0));
    }

    @Test
    void laCajaContieneElCirculoDeBusqueda() {
        Distancia.Caja caja = Distancia.caja(ORIZABA_LAT, ORIZABA_LNG, 20);
        assertThat(CORDOBA_LAT).isBetween(caja.latMin(), caja.latMax());
        assertThat(CORDOBA_LNG).isBetween(caja.lonMin(), caja.lonMax());
    }
}
