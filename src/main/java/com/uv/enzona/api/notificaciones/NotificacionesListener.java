package com.uv.enzona.api.notificaciones;

import com.uv.enzona.api.boletaje.modelo.EstadoBoleto;
import com.uv.enzona.api.boletaje.repositorio.BoletoRepository;
import com.uv.enzona.api.boletaje.repositorio.OrdenRepository;
import com.uv.enzona.api.boletaje.servicio.BoletoMapper;
import com.uv.enzona.api.boletaje.servicio.BoletosEmitidos;
import com.uv.enzona.api.boletaje.web.dto.BoletoRespuesta;
import com.uv.enzona.api.cancelacion.EventoCancelado;
import com.uv.enzona.api.eventos.servicio.EventoModificado;
import com.uv.enzona.api.identidad.modelo.Usuario;
import com.uv.enzona.api.identidad.repositorio.UsuarioRepository;
import com.uv.enzona.api.identidad.servicio.CodigoVerificacionEmitido;
import java.util.List;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Envía los avisos después de confirmar cada transacción y en segundo plano: si la operación
 * se revierte no se envía nada, y un correo lento no retrasa la respuesta de la API.
 */
@Component
public class NotificacionesListener {

    private final Notificador notificador;
    private final OrdenRepository ordenes;
    private final BoletoRepository boletos;
    private final UsuarioRepository usuarios;
    private final BoletoMapper mapper;
    private final DestinatariosRepository destinatarios;
    private final TransactionTemplate transaccion;

    public NotificacionesListener(Notificador notificador, OrdenRepository ordenes, BoletoRepository boletos,
            UsuarioRepository usuarios, BoletoMapper mapper, DestinatariosRepository destinatarios,
            TransactionTemplate transaccion) {
        this.notificador = notificador;
        this.ordenes = ordenes;
        this.boletos = boletos;
        this.usuarios = usuarios;
        this.mapper = mapper;
        this.destinatarios = destinatarios;
        this.transaccion = transaccion;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void codigoEmitido(CodigoVerificacionEmitido evento) {
        notificador.enviarCodigo(evento.destino(), evento.canal(), evento.nombre(), evento.codigo(),
                evento.minutosVigencia());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void boletosEmitidos(BoletosEmitidos evento) {
        MensajeBoletos mensaje = transaccion.execute(estado -> ordenes.findById(evento.ordenId()).flatMap(orden ->
                usuarios.findById(orden.getAsistenteId()).map(usuario -> {
                    List<BoletoRespuesta> emitidos = mapper.respuestas(boletos.findByOrdenIdOrderByIdAsc(orden.getId()));
                    BoletoRespuesta primero = emitidos.get(0);
                    return new MensajeBoletos(usuario.getCorreo(), usuario.getNombre(), primero.eventoNombre(),
                            primero.fechaHoraInicio(), primero.lugar(), emitidos.stream()
                                    .map(b -> new MensajeBoletos.Boleto(b.codigo(), b.contenidoQr(), b.tipoNombre(),
                                            b.asientoEtiqueta()))
                                    .toList());
                })).orElse(null));
        if (mensaje != null) {
            notificador.enviarBoletos(mensaje);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void eventoCancelado(EventoCancelado evento) {
        List<Usuario> afectados = transaccion.execute(estado ->
                destinatarios.conBoletos(evento.eventoId(), List.of(EstadoBoleto.CANCELADO)));
        if (afectados != null) {
            afectados.forEach(u -> notificador.avisarCancelacion(u.getCorreo(), u.getNombre(), evento.nombreEvento(),
                    evento.motivo()));
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void eventoModificado(EventoModificado evento) {
        List<Usuario> afectados = transaccion.execute(estado ->
                destinatarios.conBoletos(evento.eventoId(), List.of(EstadoBoleto.VALIDO)));
        if (afectados != null) {
            afectados.forEach(u -> notificador.avisarCambio(u.getCorreo(), u.getNombre(), evento.nombreEvento(),
                    evento.resumenCambios()));
        }
    }
}
