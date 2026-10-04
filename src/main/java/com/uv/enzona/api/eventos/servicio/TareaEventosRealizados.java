package com.uv.enzona.api.eventos.servicio;

import com.uv.enzona.api.comun.config.EnzonaProperties;
import com.uv.enzona.api.eventos.modelo.EstadoEvento;
import com.uv.enzona.api.eventos.modelo.Evento;
import com.uv.enzona.api.eventos.repositorio.EventoRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Pasa a REALIZADO los eventos publicados que ya terminaron (ciclo de vida del evento). */
@Component
public class TareaEventosRealizados {

    private static final Logger log = LoggerFactory.getLogger(TareaEventosRealizados.class);
    private static final int LOTE = 200;

    private final EventoRepository eventos;
    private final EnzonaProperties propiedades;
    private final Clock reloj;

    public TareaEventosRealizados(EventoRepository eventos, EnzonaProperties propiedades, Clock reloj) {
        this.eventos = eventos;
        this.propiedades = propiedades;
        this.reloj = reloj;
    }

    @Scheduled(fixedDelayString = "PT15M", initialDelayString = "PT1M")
    @Transactional
    public void marcarRealizados() {
        OffsetDateTime ahora = OffsetDateTime.now(reloj);
        int horas = propiedades.eventos().horasDuracionPorDefecto();
        List<Evento> iniciados = eventos.iniciadosEnEstado(EstadoEvento.PUBLICADO, ahora, PageRequest.of(0, LOTE));
        int marcados = 0;
        for (Evento evento : iniciados) {
            if (evento.finEfectivo(horas).isBefore(ahora)) {
                evento.marcarRealizado();
                marcados++;
            }
        }
        if (marcados > 0) {
            log.info("{} eventos marcados como realizados", marcados);
        }
    }
}
