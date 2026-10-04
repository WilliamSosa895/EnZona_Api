package com.uv.enzona.api.identidad.servicio;

import com.uv.enzona.api.comun.error.ApiException;
import com.uv.enzona.api.comun.error.ErrorCodigo;
import com.uv.enzona.api.identidad.modelo.CanalVerificacion;
import com.uv.enzona.api.identidad.modelo.NombreRol;
import com.uv.enzona.api.identidad.modelo.Usuario;
import com.uv.enzona.api.identidad.repositorio.RolRepository;
import com.uv.enzona.api.identidad.repositorio.UsuarioRepository;
import com.uv.enzona.api.identidad.web.dto.RegistroRespuesta;
import com.uv.enzona.api.identidad.web.dto.RegistroSolicitud;
import java.time.Clock;
import java.time.OffsetDateTime;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Registro de cuentas (RF-01, CU-01, CP-01, CP-02, CP-03). */
@Service
public class RegistroService {

    private final UsuarioRepository usuarios;
    private final RolRepository roles;
    private final CifradorCurp cifrador;
    private final PasswordEncoder encoder;
    private final CodigosVerificacionService codigos;
    private final Clock reloj;

    public RegistroService(UsuarioRepository usuarios, RolRepository roles, CifradorCurp cifrador,
            PasswordEncoder encoder, CodigosVerificacionService codigos, Clock reloj) {
        this.usuarios = usuarios;
        this.roles = roles;
        this.cifrador = cifrador;
        this.encoder = encoder;
        this.codigos = codigos;
        this.reloj = reloj;
    }

    @Transactional
    public RegistroRespuesta registrar(RegistroSolicitud solicitud) {
        String correo = Correos.normalizar(solicitud.correo());
        String curp = CifradorCurp.normalizar(solicitud.curp());
        if (usuarios.existsByCorreo(correo)) {
            throw new ApiException(ErrorCodigo.CORREO_REGISTRADO);
        }
        String curpHash = cifrador.indice(curp);
        if (usuarios.existsByCurpHash(curpHash)) {
            throw new ApiException(ErrorCodigo.CURP_REGISTRADO);
        }

        String telefono = solicitud.telefono() == null || solicitud.telefono().isBlank() ? null : solicitud.telefono().trim();
        Usuario usuario = Usuario.nuevo(solicitud.nombre().trim(), correo, telefono, cifrador.cifrar(curp), curpHash,
                encoder.encode(solicitud.contrasena()), OffsetDateTime.now(reloj));
        usuario.agregarRol(roles.obtener(NombreRol.ASISTENTE));
        if (Boolean.TRUE.equals(solicitud.organizador())) {
            usuario.agregarRol(roles.obtener(NombreRol.ORGANIZADOR));
        }
        usuarios.save(usuario);

        codigos.emitir(usuario, CanalVerificacion.CORREO);
        if (telefono != null) {
            codigos.emitir(usuario, CanalVerificacion.TELEFONO);
        }
        return RegistroRespuesta.de(usuario);
    }
}
