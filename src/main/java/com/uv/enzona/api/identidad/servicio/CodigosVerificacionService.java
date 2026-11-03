package com.uv.enzona.api.identidad.servicio;

import com.uv.enzona.api.comun.config.EnzonaProperties;
import com.uv.enzona.api.comun.error.ApiException;
import com.uv.enzona.api.comun.error.ErrorCodigo;
import com.uv.enzona.api.identidad.modelo.CanalVerificacion;
import com.uv.enzona.api.identidad.modelo.CodigoVerificacion;
import com.uv.enzona.api.identidad.modelo.PropositoCodigo;
import com.uv.enzona.api.identidad.modelo.Usuario;
import com.uv.enzona.api.identidad.repositorio.CodigoVerificacionRepository;
import com.uv.enzona.api.identidad.repositorio.UsuarioRepository;
import com.uv.enzona.api.identidad.web.dto.ReenvioSolicitud;
import com.uv.enzona.api.identidad.web.dto.VerificacionRespuesta;
import com.uv.enzona.api.identidad.web.dto.VerificacionSolicitud;
import java.time.Clock;
import java.time.OffsetDateTime;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Emisión y comprobación de códigos de un solo uso (RF-02, CP-01, CP-04, CP-22, CP-23). */
@Service
public class CodigosVerificacionService {

    /** Tiempo mínimo entre dos reenvíos, para evitar abuso del canal de correo o SMS. */
    private static final int SEGUNDOS_ENTRE_REENVIOS = 60;

    private final CodigoVerificacionRepository codigos;
    private final UsuarioRepository usuarios;
    private final GeneradorCodigo generador;
    private final PasswordEncoder encoder;
    private final ApplicationEventPublisher eventos;
    private final EnzonaProperties propiedades;
    private final Clock reloj;

    public CodigosVerificacionService(CodigoVerificacionRepository codigos, UsuarioRepository usuarios,
            GeneradorCodigo generador, PasswordEncoder encoder, ApplicationEventPublisher eventos,
            EnzonaProperties propiedades, Clock reloj) {
        this.codigos = codigos;
        this.usuarios = usuarios;
        this.generador = generador;
        this.encoder = encoder;
        this.eventos = eventos;
        this.propiedades = propiedades;
        this.reloj = reloj;
    }

    /** Invalida los códigos de verificación anteriores del canal y emite uno nuevo. Debe correr dentro de una transacción. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void emitir(Usuario usuario, CanalVerificacion canal) {
        emitir(usuario, canal, PropositoCodigo.VERIFICACION);
    }

    /**
     * Invalida los códigos anteriores del mismo canal y propósito y emite uno nuevo. Los códigos
     * de cada propósito viven aparte: pedir uno de recuperación no anula el de verificación.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void emitir(Usuario usuario, CanalVerificacion canal, PropositoCodigo proposito) {
        OffsetDateTime ahora = OffsetDateTime.now(reloj);
        codigos.findByUsuarioIdAndPropositoAndCanalAndUsadoFalse(usuario.getId(), proposito, canal)
                .forEach(CodigoVerificacion::marcarUsado);
        String codigo = generador.generar();
        int vigencia = propiedades.verificacion().minutosVigencia();
        codigos.save(CodigoVerificacion.nuevo(usuario.getId(), canal, proposito, encoder.encode(codigo),
                ahora.plusMinutes(vigencia), ahora));
        String destino = canal == CanalVerificacion.CORREO ? usuario.getCorreo() : usuario.getTelefono();
        eventos.publishEvent(new CodigoVerificacionEmitido(destino, canal, proposito, usuario.getNombre(), codigo,
                vigencia));
    }

    /**
     * Comprueba el último código vigente del usuario para ese canal y propósito y lo consume.
     * Lanza {@code CODIGO_VERIFICACION_INVALIDO}, {@code CODIGO_VERIFICACION_EXPIRADO} o
     * {@code DEMASIADOS_INTENTOS}; los intentos fallidos quedan registrados en el código.
     */
    @Transactional(propagation = Propagation.MANDATORY, noRollbackFor = ApiException.class)
    public void consumir(Usuario usuario, CanalVerificacion canal, PropositoCodigo proposito, String codigoEscrito) {
        CodigoVerificacion codigo = codigos
                .findFirstByUsuarioIdAndPropositoAndCanalAndUsadoFalseOrderByFechaCreacionDesc(usuario.getId(),
                        proposito, canal)
                .orElseThrow(() -> new ApiException(ErrorCodigo.CODIGO_VERIFICACION_INVALIDO));

        if (codigo.expirado(OffsetDateTime.now(reloj))) {
            throw new ApiException(ErrorCodigo.CODIGO_VERIFICACION_EXPIRADO);
        }
        if (codigo.intentosAgotados(propiedades.verificacion().maximoIntentos())) {
            throw new ApiException(ErrorCodigo.DEMASIADOS_INTENTOS,
                    "Se agotaron los intentos para este código; solicita uno nuevo");
        }
        if (!encoder.matches(codigoEscrito, codigo.getCodigoHash())) {
            codigo.registrarIntento();
            throw new ApiException(ErrorCodigo.CODIGO_VERIFICACION_INVALIDO);
        }
        codigo.marcarUsado();
    }

    /** true si se emitió un código de ese canal y propósito hace menos de 60 s (antiabuso del correo o SMS). */
    public boolean emitidoHacePoco(Usuario usuario, CanalVerificacion canal, PropositoCodigo proposito) {
        OffsetDateTime limite = OffsetDateTime.now(reloj).minusSeconds(SEGUNDOS_ENTRE_REENVIOS);
        return codigos.findFirstByUsuarioIdAndPropositoAndCanalOrderByFechaCreacionDesc(usuario.getId(), proposito, canal)
                .map(ultimo -> ultimo.getFechaCreacion().isAfter(limite))
                .orElse(false);
    }

    /**
     * Comprueba el código. Los intentos fallidos se guardan aunque la respuesta sea un error,
     * por eso la transacción no se revierte con {@link ApiException}.
     */
    @Transactional(noRollbackFor = ApiException.class)
    public VerificacionRespuesta verificar(VerificacionSolicitud solicitud) {
        CanalVerificacion canal = solicitud.canalOPorDefecto();
        Usuario usuario = usuarios.findByCorreo(Correos.normalizar(solicitud.correo()))
                .orElseThrow(() -> new ApiException(ErrorCodigo.CODIGO_VERIFICACION_INVALIDO));
        consumir(usuario, canal, PropositoCodigo.VERIFICACION, solicitud.codigo());
        if (canal == CanalVerificacion.CORREO) {
            usuario.verificarCorreo();
        } else {
            usuario.verificarTelefono();
        }
        usuario.activarSiCorresponde();
        return VerificacionRespuesta.de(usuario);
    }

    /**
     * Reenvía un código. Responde igual exista o no la cuenta, para no revelar qué correos
     * están registrados (enumeración de usuarios).
     */
    @Transactional
    public void reenviar(ReenvioSolicitud solicitud) {
        CanalVerificacion canal = solicitud.canalOPorDefecto();
        usuarios.findByCorreo(Correos.normalizar(solicitud.correo())).ifPresent(usuario -> {
            boolean yaVerificado = canal == CanalVerificacion.CORREO
                    ? usuario.isCorreoVerificado() : usuario.isTelefonoVerificado();
            boolean sinTelefono = canal == CanalVerificacion.TELEFONO && usuario.getTelefono() == null;
            if (yaVerificado || sinTelefono || emitidoHacePoco(usuario, canal, PropositoCodigo.VERIFICACION)) {
                return;
            }
            emitir(usuario, canal);
        });
    }
}
