package com.uv.enzona.api.cancelacion;

import com.uv.enzona.api.boletaje.modelo.EstadoBoleto;
import com.uv.enzona.api.boletaje.modelo.EstadoOrden;
import com.uv.enzona.api.boletaje.modelo.Orden;
import com.uv.enzona.api.boletaje.repositorio.BoletoRepository;
import com.uv.enzona.api.boletaje.repositorio.OrdenRepository;
import com.uv.enzona.api.cancelacion.web.dto.CancelacionRespuesta;
import com.uv.enzona.api.cancelacion.web.dto.CancelacionSolicitud;
import com.uv.enzona.api.comun.error.ApiException;
import com.uv.enzona.api.comun.error.ErrorCodigo;
import com.uv.enzona.api.comun.util.Dinero;
import com.uv.enzona.api.comun.util.Fechas;
import com.uv.enzona.api.eventos.modelo.Evento;
import com.uv.enzona.api.eventos.servicio.AccesoEventos;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cancelación de eventos con anulación de boletos y reembolso total (RF-14, CP-16, CP-47). */
@Service
public class CancelacionService {

    private final AccesoEventos acceso;
    private final OrdenRepository ordenes;
    private final BoletoRepository boletos;
    private final ApplicationEventPublisher publicador;
    private final Clock reloj;

    public CancelacionService(AccesoEventos acceso, OrdenRepository ordenes, BoletoRepository boletos,
            ApplicationEventPublisher publicador, Clock reloj) {
        this.acceso = acceso;
        this.ordenes = ordenes;
        this.boletos = boletos;
        this.publicador = publicador;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public PoliticaCancelacion politica(Long eventoId, Long usuarioId, boolean esAdmin) {
        Evento evento = acceso.cargarGestionable(eventoId, usuarioId, esAdmin, true);
        return calcular(evento, ordenes.findByEventoId(eventoId), esAdmin && !evento.perteneceA(usuarioId));
    }

    @Transactional
    public CancelacionRespuesta cancelar(Long eventoId, Long usuarioId, boolean esAdmin, CancelacionSolicitud solicitud) {
        Evento evento = acceso.bloquearGestionable(eventoId, usuarioId, esAdmin, true);
        boolean administrativa = esAdmin && !evento.perteneceA(usuarioId);
        String motivo = solicitud == null || solicitud.motivo() == null || solicitud.motivo().isBlank()
                ? null : solicitud.motivo().trim();
        if (administrativa && motivo == null) {
            throw new ApiException(ErrorCodigo.MOTIVO_REQUERIDO);
        }
        OffsetDateTime ahora = OffsetDateTime.now(reloj);
        List<Orden> delEvento = ordenes.findByEventoId(eventoId);
        PoliticaCancelacion politica = calcular(evento, delEvento, administrativa);

        evento.cancelar(usuarioId, motivo, ahora);
        int asistentes = (int) delEvento.stream()
                .filter(orden -> orden.getEstado() == EstadoOrden.PAGADA)
                .map(Orden::getAsistenteId)
                .distinct()
                .count();
        delEvento.forEach(Orden::anularPorCancelacionDelEvento);
        // Al final: esta actualización masiva vacía el contexto de persistencia tras guardar lo anterior.
        int anulados = boletos.cancelarDelEvento(eventoId, EstadoBoleto.VALIDO, EstadoBoleto.CANCELADO);

        publicador.publishEvent(new EventoCancelado(eventoId, evento.getNombre(), motivo));
        return new CancelacionRespuesta(eventoId, politica.reembolso(), politica.comision(), anulados, asistentes,
                usuarioId, Fechas.enZonaMx(ahora));
    }

    private PoliticaCancelacion calcular(Evento evento, List<Orden> delEvento, boolean administrativa) {
        BigDecimal cobrado = delEvento.stream()
                .filter(orden -> orden.getEstado() == EstadoOrden.PAGADA)
                .map(Orden::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int vigentes = boletos.delEventoEnEstados(evento.getId(), List.of(EstadoBoleto.VALIDO)).size();
        return PoliticaCancelacion.calcular(evento.getEstado(), evento.getFechaHoraInicio(), OffsetDateTime.now(reloj),
                evento.isEsDePago(), Dinero.normalizar(cobrado), vigentes, administrativa);
    }
}
