package com.uv.enzona.api.identidad.servicio;

import com.uv.enzona.api.comun.config.EnzonaProperties;
import com.uv.enzona.api.comun.error.ApiException;
import com.uv.enzona.api.comun.error.ErrorCodigo;
import com.uv.enzona.api.identidad.modelo.EstadoUsuario;
import com.uv.enzona.api.identidad.modelo.Usuario;
import com.uv.enzona.api.identidad.repositorio.UsuarioRepository;
import com.uv.enzona.api.identidad.web.dto.SesionRespuesta;
import com.uv.enzona.api.identidad.web.dto.SesionSolicitud;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Inicio de sesión con correo o CURP (RF-03, CP-05, CP-06, CP-21, CP-23). */
@Service
public class SesionService {

    private final UsuarioRepository usuarios;
    private final CifradorCurp cifrador;
    private final PasswordEncoder encoder;
    private final TokenService tokens;
    private final EnzonaProperties propiedades;
    private final Clock reloj;
    /** Hash ficticio: igualar el tiempo de respuesta cuando la cuenta no existe evita enumerar usuarios. */
    private final String hashSimulado;

    public SesionService(UsuarioRepository usuarios, CifradorCurp cifrador, PasswordEncoder encoder,
            TokenService tokens, EnzonaProperties propiedades, Clock reloj) {
        this.usuarios = usuarios;
        this.cifrador = cifrador;
        this.encoder = encoder;
        this.tokens = tokens;
        this.propiedades = propiedades;
        this.reloj = reloj;
        this.hashSimulado = encoder.encode("contrasena-simulada-para-tiempo-constante");
    }

    @Transactional(noRollbackFor = ApiException.class)
    public SesionRespuesta iniciar(SesionSolicitud solicitud) {
        Optional<Usuario> encontrado = buscar(solicitud.identificador());
        if (encontrado.isEmpty()) {
            encoder.matches(solicitud.contrasena(), hashSimulado);
            throw new ApiException(ErrorCodigo.CREDENCIALES_INVALIDAS);
        }
        Usuario usuario = encontrado.get();
        OffsetDateTime ahora = OffsetDateTime.now(reloj);

        if (usuario.bloqueadoTemporalmente(ahora)) {
            throw new ApiException(ErrorCodigo.DEMASIADOS_INTENTOS,
                    "El inicio de sesión está bloqueado temporalmente por intentos fallidos");
        }
        if (!encoder.matches(solicitud.contrasena(), usuario.getContrasenaHash())) {
            usuario.registrarIntentoFallido(propiedades.sesion().maximoIntentos(),
                    propiedades.sesion().minutosBloqueo(), ahora);
            throw new ApiException(ErrorCodigo.CREDENCIALES_INVALIDAS);
        }
        usuario.reiniciarIntentos();

        // El estado de la cuenta se revela solo después de comprobar la contraseña.
        if (usuario.getEstado() == EstadoUsuario.PENDIENTE) {
            throw new ApiException(ErrorCodigo.CUENTA_PENDIENTE);
        }
        if (usuario.getEstado() == EstadoUsuario.BLOQUEADO) {
            throw new ApiException(ErrorCodigo.CUENTA_BLOQUEADA);
        }
        return tokens.emitir(usuario);
    }

    private Optional<Usuario> buscar(String identificador) {
        String valor = identificador.trim();
        if (valor.contains("@")) {
            return usuarios.findByCorreo(Correos.normalizar(valor));
        }
        if (!FormatoCurp.esValido(valor)) {
            return Optional.empty();
        }
        return usuarios.findByCurpHash(cifrador.indice(valor));
    }
}
