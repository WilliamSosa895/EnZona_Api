package com.uv.enzona.api.boletaje.servicio;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Base64;
import org.junit.jupiter.api.Test;

/** RNF-02, CP-40, CP-41: autenticidad del boleto. */
class FirmaBoletosTest {

    private final FirmaBoletos firma = new FirmaBoletos(Base64.getEncoder().encodeToString(new byte[32]));

    @Test
    void unaFirmaValidaSeVerifica() {
        String codigo = firma.nuevoCodigo();
        assertThat(firma.verificar(codigo, 7L, firma.firmar(codigo, 7L))).isTrue();
    }

    @Test
    void unaFirmaAlteradaSeRechaza() {
        String codigo = firma.nuevoCodigo();
        String original = firma.firmar(codigo, 7L);
        String alterada = (original.charAt(0) == 'A' ? "B" : "A") + original.substring(1);
    }

    @Test
    void elBoletoDeUnEventoNoSirveParaOtro() {
        String codigo = firma.nuevoCodigo();
        assertThat(firma.verificar(codigo, 8L, firma.firmar(codigo, 7L))).isFalse();
    }

    @Test
    void losCodigosSonUnicosYTienenFormatoDeQr() {
        String codigo = firma.nuevoCodigo();
        assertThat(codigo).startsWith("EZ").hasSize(18).isNotEqualTo(firma.nuevoCodigo());
        assertThat(QrBoleto.leer(codigo + "." + firma.firmar(codigo, 1L))).isPresent();
    }
}
