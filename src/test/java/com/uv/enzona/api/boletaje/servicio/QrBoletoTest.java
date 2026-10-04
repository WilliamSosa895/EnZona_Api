package com.uv.enzona.api.boletaje.servicio;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

class QrBoletoTest {

    @Test
    void separaCodigoYFirma() {
        QrBoleto qr = QrBoleto.leer(" EZabcdefghijklmnop.abcdefghijklmnopqrstuvwxyz0123456789-_ABCDEF ").orElseThrow();
        assertThat(qr.codigo()).isEqualTo("EZabcdefghijklmnop");
        assertThat(qr.firma()).isEqualTo("abcdefghijklmnopqrstuvwxyz0123456789-_ABCDEF");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"sinpunto", "EZabc.corta", "EZ abc.firmafirmafirmafirma", "a.b.c", "<script>.alert(1)"})
    void rechazaContenidoConFormatoInvalido(String contenido) {
        assertThat(QrBoleto.leer(contenido)).isEmpty();
    }
}
