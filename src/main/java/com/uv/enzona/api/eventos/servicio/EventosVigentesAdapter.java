package com.uv.enzona.api.eventos.servicio;

import com.uv.enzona.api.comun.config.EnzonaProperties;
import com.uv.enzona.api.eventos.repositorio.EventoRepository;
import com.uv.enzona.api.identidad.servicio.ConsultaEventosVigentes;
import java.time.Clock;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Component;

/** Implementa el puerto de identidad para saber si un organizador tiene eventos publicados vigentes. */
@Component
public class EventosVigentesAdapter implements ConsultaEventosVigentes {

    private final EventoRepository eventos;
    private final EnzonaProperties propiedades;
    private final Clock reloj;

    public EventosVigentesAdapter(EventoRepository eventos, EnzonaProperties propiedades, Clock reloj) {
        this.eventos = eventos;
        this.propiedades = propiedades;
        this.reloj = reloj;
    }

    @Override
    public boolean tieneEventosVigentes(Long organizadorId) {
        return eventos.count(EspecificacionesEvento.publicadosVigentes(OffsetDateTime.now(reloj),
                        propiedades.eventos().horasDuracionPorDefecto())
                .and(EspecificacionesEvento.delOrganizador(organizadorId))) > 0;
    }
}
