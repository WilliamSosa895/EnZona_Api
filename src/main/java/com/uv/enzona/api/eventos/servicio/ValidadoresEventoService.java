package com.uv.enzona.api.eventos.servicio;

import com.uv.enzona.api.comun.error.ApiException;
import com.uv.enzona.api.comun.error.ErrorCodigo;
import com.uv.enzona.api.eventos.modelo.EventoValidador;
import com.uv.enzona.api.eventos.repositorio.EventoValidadorRepository;
import com.uv.enzona.api.eventos.web.dto.ValidadorRespuesta;
import com.uv.enzona.api.identidad.modelo.EstadoUsuario;
import com.uv.enzona.api.identidad.modelo.NombreRol;
import com.uv.enzona.api.identidad.modelo.Usuario;
import com.uv.enzona.api.identidad.repositorio.RolRepository;
import com.uv.enzona.api.identidad.repositorio.UsuarioRepository;
import com.uv.enzona.api.identidad.servicio.Correos;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** El organizador designa al personal de acceso de su evento (CU-06, Anexo E §E.3). */
@Service
public class ValidadoresEventoService {

    private final EventoValidadorRepository designaciones;
    private final UsuarioRepository usuarios;
    private final RolRepository roles;
    private final AccesoEventos acceso;

    public ValidadoresEventoService(EventoValidadorRepository designaciones, UsuarioRepository usuarios,
            RolRepository roles, AccesoEventos acceso) {
        this.designaciones = designaciones;
        this.usuarios = usuarios;
        this.roles = roles;
        this.acceso = acceso;
    }

    @Transactional
    public ValidadorRespuesta designar(Long eventoId, Long gestorId, boolean esAdmin, String correo) {
        acceso.cargarGestionable(eventoId, gestorId, esAdmin, true);
        Usuario usuario = usuarios.findByCorreo(Correos.normalizar(correo))
                .orElseThrow(() -> new ApiException(ErrorCodigo.NO_ENCONTRADO,
                        "No existe una cuenta con ese correo"));
        if (usuario.getEstado() != EstadoUsuario.ACTIVO) {
            throw ApiException.reglaNegocio("La cuenta del validador debe estar activa");
        }
        usuario.agregarRol(roles.obtener(NombreRol.VALIDADOR));
        if (!designaciones.estaDesignado(eventoId, usuario.getId())) {
            designaciones.save(new EventoValidador(eventoId, usuario.getId()));
        }
        return new ValidadorRespuesta(usuario.getId(), usuario.getNombre(), usuario.getCorreo());
    }

    @Transactional(readOnly = true)
    public List<ValidadorRespuesta> listar(Long eventoId, Long gestorId, boolean esAdmin) {
        acceso.cargarGestionable(eventoId, gestorId, esAdmin, true);
        List<Long> ids = designaciones.delEvento(eventoId).stream().map(EventoValidador::getUsuarioId).toList();
        return usuarios.findAllById(ids).stream()
                .map(u -> new ValidadorRespuesta(u.getId(), u.getNombre(), u.getCorreo()))
                .toList();
    }

    @Transactional
    public void quitar(Long eventoId, Long gestorId, boolean esAdmin, Long usuarioId) {
        acceso.cargarGestionable(eventoId, gestorId, esAdmin, true);
        designaciones.deleteById(new EventoValidador.Id(eventoId, usuarioId));
    }
}
