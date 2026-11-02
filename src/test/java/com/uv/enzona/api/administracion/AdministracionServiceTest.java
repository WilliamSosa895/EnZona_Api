package com.uv.enzona.api.administracion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.uv.enzona.api.comun.error.ApiException;
import com.uv.enzona.api.eventos.repositorio.EventoRepository;
import com.uv.enzona.api.eventos.servicio.EventoMapper;
import com.uv.enzona.api.identidad.modelo.EstadoUsuario;
import com.uv.enzona.api.identidad.modelo.Usuario;
import com.uv.enzona.api.identidad.repositorio.UsuarioRepository;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdministracionServiceTest {

    private static final long ID = 7L;
    private static final long ADMIN_ID = 1L;

    @Mock
    private UsuarioRepository usuarios;
    @Mock
    private EventoRepository eventos;
    @Mock
    private EventoMapper mapper;
    @InjectMocks
    private AdministracionService servicio;

    @Test
    void darPorVerificadaActivaUnaCuentaPendienteYMarcaElCorreoVerificado() {
        Usuario pendiente = usuario("+522721234567");
        when(usuarios.findById(ID)).thenReturn(Optional.of(pendiente));

        UsuarioAdminRespuesta respuesta = servicio.darPorVerificada(ID);

        assertThat(respuesta.estado()).isEqualTo(EstadoUsuario.ACTIVO);
        assertThat(respuesta.correoVerificado()).isTrue();
        assertThat(pendiente.verificacionCompleta()).isTrue();
    }

    @Test
    void darPorVerificadaRechazaUnaCuentaQueYaEstaActiva() {
        Usuario activa = usuario(null);
        activa.activarVerificada();
        when(usuarios.findById(ID)).thenReturn(Optional.of(activa));

        assertThatThrownBy(() -> servicio.darPorVerificada(ID))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("PENDIENTE");
    }

    @Test
    void darPorVerificadaRechazaUnaCuentaBloqueada() {
        Usuario bloqueada = usuario(null);
        bloqueada.bloquear();
        when(usuarios.findById(ID)).thenReturn(Optional.of(bloqueada));

        assertThatThrownBy(() -> servicio.darPorVerificada(ID)).isInstanceOf(ApiException.class);
        assertThat(bloqueada.getEstado()).isEqualTo(EstadoUsuario.BLOQUEADO);
    }

    @Test
    void darPorVerificadaFallaSiElUsuarioNoExiste() {
        when(usuarios.findById(ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.darPorVerificada(ID)).isInstanceOf(ApiException.class);
    }

    @Test
    void reactivarDesdeElEstadoNoActivaUnaCuentaSinVerificar() {
        Usuario pendiente = usuario(null);
        pendiente.bloquear();
        when(usuarios.findById(ID)).thenReturn(Optional.of(pendiente));

        UsuarioAdminRespuesta respuesta = servicio.cambiarEstado(ID, EstadoUsuario.ACTIVO, ADMIN_ID);

        assertThat(respuesta.estado()).isEqualTo(EstadoUsuario.PENDIENTE);
    }

    private static Usuario usuario(String telefono) {
        return Usuario.nuevo("Alex", "alex@ejemplo.mx", telefono, "curp-cifrado", "curp-hash", "hash",
                OffsetDateTime.now());
    }
}
