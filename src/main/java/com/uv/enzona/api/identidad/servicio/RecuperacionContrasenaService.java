package com.uv.enzona.api.identidad.servicio;

import com.uv.enzona.api.comun.error.ApiException;
import com.uv.enzona.api.comun.error.ErrorCodigo;
import com.uv.enzona.api.identidad.modelo.CanalVerificacion;
import com.uv.enzona.api.identidad.modelo.EstadoUsuario;
import com.uv.enzona.api.identidad.modelo.PropositoCodigo;
import com.uv.enzona.api.identidad.modelo.Usuario;
import com.uv.enzona.api.identidad.repositorio.UsuarioRepository;
import com.uv.enzona.api.identidad.web.dto.RecuperacionSolicitud;
import com.uv.enzona.api.identidad.web.dto.RestablecimientoSolicitud;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * «¿Olvidaste tu contraseña?»: un código de un solo uso al correo de la cuenta y, con él, una
 * contraseña nueva sin iniciar sesión. Reutiliza los códigos de {@link CodigosVerificacionService}
 * con propósito {@link PropositoCodigo#RECUPERACION}, separados de los de activación.
 */
@Service
public class RecuperacionContrasenaService {

    private final UsuarioRepository usuarios;
    private final CodigosVerificacionService codigos;
    private final PasswordEncoder encoder;

    public RecuperacionContrasenaService(UsuarioRepository usuarios, CodigosVerificacionService codigos,
            PasswordEncoder encoder) {
        this.usuarios = usuarios;
        this.codigos = codigos;
        this.encoder = encoder;
    }

    /**
     * Emite y envía el código. Responde igual exista o no la cuenta (no revela correos registrados),
     * ignora cuentas bloqueadas y descarta en silencio las solicitudes repetidas en menos de 60 s.
     */
    @Transactional
    public void solicitar(RecuperacionSolicitud solicitud) {
        usuarios.findByCorreo(Correos.normalizar(solicitud.correo())).ifPresent(usuario -> {
            if (usuario.getEstado() == EstadoUsuario.BLOQUEADO
                    || codigos.emitidoHacePoco(usuario, CanalVerificacion.CORREO, PropositoCodigo.RECUPERACION)) {
                return;
            }
            codigos.emitir(usuario, CanalVerificacion.CORREO, PropositoCodigo.RECUPERACION);
        });
    }

    /**
     * Comprueba el código y guarda la contraseña nueva. Como el código llegó al correo de la
     * cuenta, también deja el correo verificado (y activa la cuenta si solo faltaba eso) y
     * borra el bloqueo temporal por intentos fallidos de inicio de sesión.
     * Los intentos fallidos del código se conservan aunque la respuesta sea un error.
     */
    @Transactional(noRollbackFor = ApiException.class)
    public void restablecer(RestablecimientoSolicitud solicitud) {
        Usuario usuario = usuarios.findByCorreo(Correos.normalizar(solicitud.correo()))
                .orElseThrow(() -> new ApiException(ErrorCodigo.CODIGO_VERIFICACION_INVALIDO));
        if (usuario.getEstado() == EstadoUsuario.BLOQUEADO) {
            throw new ApiException(ErrorCodigo.CUENTA_BLOQUEADA);
        }
        codigos.consumir(usuario, CanalVerificacion.CORREO, PropositoCodigo.RECUPERACION, solicitud.codigo());
        usuario.cambiarContrasena(encoder.encode(solicitud.nuevaContrasena()));
        usuario.reiniciarIntentos();
        usuario.verificarCorreo();
        usuario.activarSiCorresponde();
    }
}
