package com.uv.enzona.api.identidad.modelo;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class UsuarioTest {

    private static final OffsetDateTime AHORA = OffsetDateTime.of(2026, 10, 4, 12, 0, 0, 0, ZoneOffset.UTC);

    private Usuario usuario(String telefono) {
        return Usuario.nuevo("Ana", "ana@correo.com", telefono, "cifrado", "hash", "bcrypt", AHORA);
    }

    @Test
    void seActivaAlVerificarElCorreoSiNoRegistroTelefono() {
        Usuario usuario = usuario(null);

        usuario.verificarCorreo();
        usuario.activarSiCorresponde();

        assertThat(usuario.getEstado()).isEqualTo(EstadoUsuario.ACTIVO);
    }

    @Test
    void conTelefonoRequiereAmbasVerificaciones() {
        Usuario usuario = usuario("2721234567");

        usuario.verificarCorreo();
        usuario.activarSiCorresponde();
        assertThat(usuario.getEstado()).isEqualTo(EstadoUsuario.PENDIENTE);

        usuario.verificarTelefono();
        usuario.activarSiCorresponde();
        assertThat(usuario.getEstado()).isEqualTo(EstadoUsuario.ACTIVO);
    }

    @Test
    void seBloqueaTemporalmenteAlAgotarLosIntentos() {
        Usuario usuario = usuario(null);

        for (int i = 0; i < 5; i++) {
            usuario.registrarIntentoFallido(5, 15, AHORA);
        }

        assertThat(usuario.bloqueadoTemporalmente(AHORA.plusMinutes(14))).isTrue();
        assertThat(usuario.bloqueadoTemporalmente(AHORA.plusMinutes(16))).isFalse();
    }

    @Test
    void cambiarElTelefonoObligaAVerificarloDeNuevo() {
        Usuario usuario = usuario("2721234567");
        usuario.verificarTelefono();

        usuario.actualizarPerfil("Ana", "2729876543");

        assertThat(usuario.isTelefonoVerificado()).isFalse();
    }
}
