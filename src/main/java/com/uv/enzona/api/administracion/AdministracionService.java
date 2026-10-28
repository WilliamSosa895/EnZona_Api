package com.uv.enzona.api.administracion;

import com.uv.enzona.api.comun.error.ApiException;
import com.uv.enzona.api.eventos.modelo.EstadoEvento;
import com.uv.enzona.api.eventos.modelo.Evento;
import com.uv.enzona.api.eventos.repositorio.EventoRepository;
import com.uv.enzona.api.eventos.servicio.EventoMapper;
import com.uv.enzona.api.eventos.web.dto.EventoRespuesta;
import com.uv.enzona.api.identidad.modelo.EstadoUsuario;
import com.uv.enzona.api.identidad.modelo.Usuario;
import com.uv.enzona.api.identidad.repositorio.UsuarioRepository;
import java.util.Locale;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Gestión de usuarios y supervisión de eventos por el administrador (RF-18, CU-07, CP-46). */
@Service
public class AdministracionService {

    private final UsuarioRepository usuarios;
    private final EventoRepository eventos;
    private final EventoMapper mapper;

    public AdministracionService(UsuarioRepository usuarios, EventoRepository eventos, EventoMapper mapper) {
        this.usuarios = usuarios;
        this.eventos = eventos;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public Page<UsuarioAdminRespuesta> usuarios(EstadoUsuario estado, String texto, int pagina, int tamano) {
        Specification<Usuario> criterio = (raiz, consulta, cb) -> cb.conjunction();
        if (estado != null) {
            criterio = criterio.and((raiz, consulta, cb) -> cb.equal(raiz.get("estado"), estado));
        }
        if (texto != null && !texto.isBlank()) {
            String patron = "%" + texto.trim().toLowerCase(Locale.ROOT).replace("%", "").replace("_", "") + "%";
            criterio = criterio.and((raiz, consulta, cb) -> cb.or(
                    cb.like(cb.lower(raiz.<String>get("nombre")), patron),
                    cb.like(cb.lower(raiz.<String>get("correo")), patron)));
        }
        return usuarios.findAll(criterio, PageRequest.of(pagina, tamano, Sort.by("fechaRegistro").descending()))
                .map(UsuarioAdminRespuesta::de);
    }

    @Transactional
    public UsuarioAdminRespuesta cambiarEstado(Long usuarioId, EstadoUsuario nuevo, Long adminId) {
        if (usuarioId.equals(adminId)) {
            throw ApiException.reglaNegocio("No puedes cambiar el estado de tu propia cuenta");
        }
        Usuario usuario = usuarios.findById(usuarioId).orElseThrow(() -> ApiException.noEncontrado("Usuario", usuarioId));
        switch (nuevo) {
            case BLOQUEADO -> usuario.bloquear();
            case ACTIVO -> usuario.desbloquear();
            case PENDIENTE -> throw ApiException.reglaNegocio("El estado solo puede cambiarse a ACTIVO o BLOQUEADO");
        }
        return UsuarioAdminRespuesta.de(usuario);
    }

    /**
     * Da por verificada una cuenta PENDIENTE sin pasar por el código: para cuando el correo
     * no llegó (proveedor sin dominio, buzón lleno) y la persona no puede terminar el registro.
     */
    @Transactional
    public UsuarioAdminRespuesta darPorVerificada(Long usuarioId) {
        Usuario usuario = usuarios.findById(usuarioId).orElseThrow(() -> ApiException.noEncontrado("Usuario", usuarioId));
        if (usuario.getEstado() != EstadoUsuario.PENDIENTE) {
            throw ApiException.reglaNegocio("Solo se puede dar por verificada una cuenta PENDIENTE; esta está "
                    + usuario.getEstado());
        }
        usuario.activarVerificada();
        return UsuarioAdminRespuesta.de(usuario);
    }

    @Transactional(readOnly = true)
    public Page<EventoRespuesta> eventos(EstadoEvento estado, int pagina, int tamano) {
        Specification<Evento> criterio = (raiz, consulta, cb) -> estado == null
                ? cb.conjunction() : cb.equal(raiz.get("estado"), estado);
        PageRequest solicitud = PageRequest.of(pagina, tamano, Sort.by("fechaCreacion").descending());
        Page<Evento> resultado = eventos.findAll(criterio, solicitud);
        return new PageImpl<>(mapper.resumenes(resultado.getContent(), Map.of()), solicitud, resultado.getTotalElements());
    }
}
