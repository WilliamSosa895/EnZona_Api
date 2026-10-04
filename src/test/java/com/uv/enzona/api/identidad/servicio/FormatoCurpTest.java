package com.uv.enzona.api.identidad.servicio;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** CP-03: validación del formato del CURP con valores límite. */
class FormatoCurpTest {

    @ParameterizedTest
    @ValueSource(strings = {"GOMA850101HVZNRN09", "pera000229mdfrrsa1", "LOPJ991231HVZPRNA1"})
    void aceptaCurpConEstructuraValida(String curp) {
        assertThat(FormatoCurp.esValido(curp)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "GOMA850101HVZNRN0",    // 17 caracteres
        "GOMA850101HVZNRN099",  // 19 caracteres
        "GOMA851301HVZNRN09",   // mes 13
        "GOMA850132HVZNRN09",   // día 32
        "GOMA850101HXXNRN09",   // entidad inexistente
        "GOMA850101HVZNRN0!",   // carácter no permitido
        ""
    })
    void rechazaCurpConFormatoInvalido(String curp) {
        assertThat(FormatoCurp.esValido(curp)).isFalse();
    }
}
