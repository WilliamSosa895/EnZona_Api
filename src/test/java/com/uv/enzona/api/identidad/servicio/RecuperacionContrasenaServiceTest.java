package com.uv.enzona.api.identidad.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.uv.enzona.api.comun.error.ApiException;
import com.uv.enzona.api.comun.error.ErrorCodigo;
import com.uv.enzona.api.identidad.modelo.CanalVerificacion;
import com.uv.enzona.api.identidad.modelo.EstadoUsuario;
import com.uv.enzona.api.identidad.modelo.PropositoCodigo;
import com.uv.enzona.api.identidad.modelo.Usuario;
import com.uv.enzona.api.identidad.repositorio.UsuarioRepository;
import com.uv.enzona.api.identidad.web.dto.RecuperacionSolicitud;
import com.uv.enzona.api.identidad.web.dto.RestablecimientoSolicitud;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class RecuperacionContrasenaServiceTest {

    private static final String CORREO = "ana@example.com";
    private static final String CODIGO = "123456";
    private static final String NUEVA = "Nueva1234";

    @Mock
    private UsuarioRepository usuarios;
    @Mock
    private CodigosVerificacionService codigos;
    @Mock
    private PasswordEncoder encoder;
    @InjectMocks
    private RecuperacionContrasenaService servicio;

    @Test
    void solicitarEmiteUnCodigoDeRecuperacionAlCorreoDeLaCuenta() {
        Usuario usuario = usuario();
        when(usuarios.findByCorreo(CORREO)).thenReturn(Optional.of(usuario));
        when(codigos.emitidoHacePoco(usuario, CanalVerificacion.CORREO, PropositoCodigo.RECUPERACION)).thenReturn(false);

        servicio.solicitar(new RecuperacionSolicitud(" Ana@Example.com "));

        verify(codigos).emitir(usuario, CanalVerificacion.CORREO, PropositoCodigo.RECUPERACION);
    }

    @Test
    void solicitarNoRevelaSiElCorreoExiste() {
        when(usuarios.findByCorreo(CORREO)).thenReturn(Optional.empty());

        servicio.solicitar(new RecuperacionSolicitud(CORREO));

        verify(codigos, never()).emitir(any(), any(), any());
    }

    @Test
    void solicitarIgnoraCuentasBloqueadasYSolicitudesRepetidas() {
        Usuario bloqueada = usuario();
        bloqueada.bloquear();
        when(usuarios.findByCorreo(CORREO)).thenReturn(Optional.of(bloqueada));
        servicio.solicitar(new RecuperacionSolicitud(CORREO));

        Usuario reciente = usuario();
        when(usuarios.findByCorreo(CORREO)).thenReturn(Optional.of(reciente));
        when(codigos.emitidoHacePoco(reciente, CanalVerificacion.CORREO, PropositoCodigo.RECUPERACION)).thenReturn(true);
        servicio.solicitar(new RecuperacionSolicitud(CORREO));

        verify(codigos, never()).emitir(any(), any(), any());
    }

    @Test
    void restablecerGuardaLaContrasenaNuevaYDejaLaCuentaListaParaEntrar() {
        Usuario usuario = usuario();
        usuario.registrarIntentoFallido(1, 30, OffsetDateTime.now());       // bloqueada por intentos fallidos
        when(usuarios.findByCorreo(CORREO)).thenReturn(Optional.of(usuario));
        when(encoder.encode(NUEVA)).thenReturn("hash-nuevo");

        servicio.restablecer(new RestablecimientoSolicitud(CORREO, CODIGO, NUEVA));

        verify(codigos).consumir(usuario, CanalVerificacion.CORREO, PropositoCodigo.RECUPERACION, CODIGO);
        assertThat(usuario.getContrasenaHash()).isEqualTo("hash-nuevo");
        assertThat(usuario.bloqueadoTemporalmente(OffsetDateTime.now())).isFalse();
        assertThat(usuario.isCorreoVerificado()).isTrue();
        assertThat(usuario.getEstado()).isEqualTo(EstadoUsuario.ACTIVO);
    }

    @Test
    void restablecerConCodigoIncorrectoNoCambiaLaContrasena() {
        Usuario usuario = usuario();
        when(usuarios.findByCorreo(CORREO)).thenReturn(Optional.of(usuario));
        doThrow(new ApiException(ErrorCodigo.CODIGO_VERIFICACION_INVALIDO))
                .when(codigos).consumir(eq(usuario), any(), any(), eq("000000"));

        assertThatThrownBy(() -> servicio.restablecer(new RestablecimientoSolicitud(CORREO, "000000", NUEVA)))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getCodigo())
                .isEqualTo(ErrorCodigo.CODIGO_VERIFICACION_INVALIDO);
        assertThat(usuario.getContrasenaHash()).isEqualTo("hash-viejo");
        verify(encoder, never()).encode(any());
    }

    @Test
    void restablecerConCorreoDesconocidoRespondeComoCodigoInvalido() {
        when(usuarios.findByCorreo(CORREO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.restablecer(new RestablecimientoSolicitud(CORREO, CODIGO, NUEVA)))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getCodigo())
                .isEqualTo(ErrorCodigo.CODIGO_VERIFICACION_INVALIDO);
    }

    @Test
    void restablecerNoAplicaACuentasBloqueadasPorAdministracion() {
        Usuario bloqueada = usuario();
        bloqueada.bloquear();
        when(usuarios.findByCorreo(CORREO)).thenReturn(Optional.of(bloqueada));

        assertThatThrownBy(() -> servicio.restablecer(new RestablecimientoSolicitud(CORREO, CODIGO, NUEVA)))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getCodigo())
                .isEqualTo(ErrorCodigo.CUENTA_BLOQUEADA);
        verify(codigos, never()).consumir(any(), any(), any(), any());
    }

    @Test
    void laSolicitudDeRestablecimientoNoMuestraElCodigoNiLaContrasena() {
        String texto = new RestablecimientoSolicitud(CORREO, CODIGO, NUEVA).toString();

        assertThat(texto).contains(CORREO).doesNotContain(CODIGO).doesNotContain(NUEVA);
    }

    private static Usuario usuario() {
        return Usuario.nuevo("Ana", CORREO, null, "curp-cifrado", "curp-hash", "hash-viejo", OffsetDateTime.now());
    }
}
