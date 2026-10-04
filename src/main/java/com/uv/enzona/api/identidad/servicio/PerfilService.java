package com.uv.enzona.api.identidad.servicio;

import com.uv.enzona.api.comun.error.ApiException;
import com.uv.enzona.api.comun.error.ErrorCodigo;
import com.uv.enzona.api.identidad.modelo.CanalVerificacion;
import com.uv.enzona.api.identidad.modelo.NombreRol;
import com.uv.enzona.api.identidad.modelo.Usuario;
import com.uv.enzona.api.identidad.repositorio.RolRepository;
import com.uv.enzona.api.identidad.repositorio.UsuarioRepository;
import com.uv.enzona.api.identidad.web.dto.CambioContrasena;
import com.uv.enzona.api.identidad.web.dto.PerfilActualizacion;
import com.uv.enzona.api.identidad.web.dto.PerfilRespuesta;
import com.uv.enzona.api.identidad.web.dto.UsuarioResumen;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Perfil propio (RF-04) y activación del rol de organizador (gestión de roles). */
@Service
public class PerfilService {

    private final UsuarioRepository usuarios;
    private final RolRepository roles;
    private final PasswordEncoder encoder;
    private final CodigosVerificacionService codigos;
    private final ConsultaEventosVigentes eventosVigentes;

    public PerfilService(UsuarioRepository usuarios, RolRepository roles, PasswordEncoder encoder,
            CodigosVerificacionService codigos, ConsultaEventosVigentes eventosVigentes) {
        this.usuarios = usuarios;
        this.roles = roles;
        this.encoder = encoder;
        this.codigos = codigos;
        this.eventosVigentes = eventosVigentes;
    }

    @Transactional(readOnly = true)
    public PerfilRespuesta obtener(Long usuarioId) {
        return PerfilRespuesta.de(cargar(usuarioId));
    }

    @Transactional
    public PerfilRespuesta actualizar(Long usuarioId, PerfilActualizacion cambios) {
        Usuario usuario = cargar(usuarioId);
        String telefono = cambios.telefono() == null || cambios.telefono().isBlank() ? null : cambios.telefono().trim();
        String telefonoAnterior = usuario.getTelefono();
        usuario.actualizarPerfil(cambios.nombre().trim(), telefono);
        if (telefono != null && !telefono.equals(telefonoAnterior)) {
            codigos.emitir(usuario, CanalVerificacion.TELEFONO);
        }
        return PerfilRespuesta.de(usuario);
    }

    @Transactional
    public void cambiarContrasena(Long usuarioId, CambioContrasena cambio) {
        Usuario usuario = cargar(usuarioId);
        if (!encoder.matches(cambio.actual(), usuario.getContrasenaHash())) {
            throw new ApiException(ErrorCodigo.CREDENCIALES_INVALIDAS, "La contraseña actual no es correcta");
        }
        usuario.cambiarContrasena(encoder.encode(cambio.nueva()));
    }

    @Transactional
    public UsuarioResumen activarOrganizador(Long usuarioId) {
        Usuario usuario = cargar(usuarioId);
        usuario.agregarRol(roles.obtener(NombreRol.ORGANIZADOR));
        return UsuarioResumen.de(usuario);
    }

    /** No se permite si tiene eventos publicados vigentes o si el organizador es su único rol. */
    @Transactional
    public UsuarioResumen desactivarOrganizador(Long usuarioId) {
        Usuario usuario = cargar(usuarioId);
        if (!usuario.tieneRol(NombreRol.ORGANIZADOR)) {
            return UsuarioResumen.de(usuario);
        }
        if (usuario.getRoles().size() == 1) {
            throw new ApiException(ErrorCodigo.ROL_EN_USO, "El rol de organizador es el único rol de la cuenta");
        }
        if (eventosVigentes.tieneEventosVigentes(usuarioId)) {
            throw new ApiException(ErrorCodigo.ROL_EN_USO,
                    "Tienes eventos publicados vigentes; cancélalos o espera a que terminen");
        }
        usuario.quitarRol(NombreRol.ORGANIZADOR);
        return UsuarioResumen.de(usuario);
    }

    private Usuario cargar(Long usuarioId) {
        return usuarios.findById(usuarioId).orElseThrow(() -> ApiException.noEncontrado("Usuario", usuarioId));
    }
}
