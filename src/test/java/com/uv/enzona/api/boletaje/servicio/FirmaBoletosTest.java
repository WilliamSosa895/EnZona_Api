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
        String alterada = firma.firmar(codigo, 7L).replaceFirst(".$", "A").equals(firma.firmar(codigo, 7L))
                ? firma.firmar(codigo, 7L).replaceFirst(".$", "B") : firma.firmar(codigo, 7L).replaceFirst(".$", "A");
        assertThat(firma.verificar(codigo, 7L, alterada)).isFalse();
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
