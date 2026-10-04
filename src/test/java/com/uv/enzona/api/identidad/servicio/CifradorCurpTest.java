package com.uv.enzona.api.identidad.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Base64;
import org.junit.jupiter.api.Test;

class CifradorCurpTest {

    private static final String CLAVE_32 = Base64.getEncoder().encodeToString(new byte[32]);
    private static final String OTRA_CLAVE_32 = Base64.getEncoder().encodeToString("0123456789abcdef0123456789abcdef".getBytes());
    private static final String CURP = "GOMA850101HVZNRN09";

    private final CifradorCurp cifrador = new CifradorCurp(CLAVE_32, OTRA_CLAVE_32);

    @Test
    void cifraYDescifraElCurpSinGuardarloEnClaro() {
        String cifrado = cifrador.cifrar(CURP);

        assertThat(cifrado).doesNotContain(CURP);
        assertThat(cifrador.descifrar(cifrado)).isEqualTo(CURP);
    }

    @Test
    void cadaCifradoUsaUnVectorDistinto() {
        assertThat(cifrador.cifrar(CURP)).isNotEqualTo(cifrador.cifrar(CURP));
    }

    @Test
    void elIndiceEsDeterministaYNoDependeDeMayusculasNiEspacios() {
        assertThat(cifrador.indice(" goma850101hvznrn09 ")).isEqualTo(cifrador.indice(CURP)).hasSize(64);
    }

    @Test
    void rechazaUnaClaveDeCifradoQueNoEsDe32Bytes() {
        String claveCorta = Base64.getEncoder().encodeToString(new byte[16]);
        assertThatThrownBy(() -> new CifradorCurp(claveCorta, OTRA_CLAVE_32)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void detectaUnTextoCifradoAlterado() {
        byte[] datos = Base64.getDecoder().decode(cifrador.cifrar(CURP));
        datos[datos.length - 1] ^= 1;
        String alterado = Base64.getEncoder().encodeToString(datos);

        assertThatThrownBy(() -> cifrador.descifrar(alterado)).isInstanceOf(IllegalStateException.class);
    }
}
