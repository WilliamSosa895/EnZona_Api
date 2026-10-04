package com.uv.enzona.api.eventos.servicio;

import com.uv.enzona.api.comun.config.EnzonaProperties;
import com.uv.enzona.api.comun.error.ApiException;
import com.uv.enzona.api.eventos.modelo.Evento;
import com.uv.enzona.api.eventos.repositorio.EventoRepository;
import com.uv.enzona.api.eventos.repositorio.TipoBoletoRepository;
import com.uv.enzona.api.eventos.web.dto.EventoRespuesta;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Catálogo de eventos: listado, búsqueda, filtros y descubrimiento por cercanía
 * (RF-05, RF-06, RF-07, RF-19; CU-02).
 */
@Service
public class ConsultaEventosService {

    /** Radio por defecto de la búsqueda por cercanía cuando el cliente no lo indica. */
    private static final double RADIO_POR_DEFECTO_KM = 15.0;
    /** Tope de candidatos dentro del rectángulo de búsqueda; suficiente para el volumen del piloto. */
    private static final int MAXIMO_CANDIDATOS = 500;

    private final EventoRepository eventos;
    private final TipoBoletoRepository tipos;
    private final EventoMapper mapper;
    private final EnzonaProperties propiedades;
    private final Clock reloj;

    public ConsultaEventosService(EventoRepository eventos, TipoBoletoRepository tipos, EventoMapper mapper,
            EnzonaProperties propiedades, Clock reloj) {
        this.eventos = eventos;
        this.tipos = tipos;
        this.mapper = mapper;
        this.propiedades = propiedades;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public Page<EventoRespuesta> listar(FiltroEventos filtro) {
        if ((filtro.latitud() == null) != (filtro.longitud() == null)) {
            throw ApiException.reglaNegocio("Envía lat y lng juntas para buscar por cercanía");
        }
        OffsetDateTime ahora = OffsetDateTime.now(reloj);
        Specification<Evento> criterio = Specification
                .where(EspecificacionesEvento.publicadosVigentes(ahora, propiedades.eventos().horasDuracionPorDefecto()))
                .and(EspecificacionesEvento.texto(filtro.texto()))
                .and(EspecificacionesEvento.categoria(filtro.categoriaId()))
                .and(EspecificacionesEvento.ciudad(filtro.ciudad()))
                .and(EspecificacionesEvento.desde(filtro.desde()))
                .and(EspecificacionesEvento.hasta(filtro.hasta()))
                .and(EspecificacionesEvento.esDePago(filtro.esDePago()));

        return filtro.porUbicacion() ? porCercania(criterio, filtro) : porFecha(criterio, filtro);
    }

    private Page<EventoRespuesta> porFecha(Specification<Evento> criterio, FiltroEventos filtro) {
        PageRequest pagina = PageRequest.of(filtro.pagina(), filtro.tamano(),
                Sort.by("fechaHoraInicio").ascending().and(Sort.by("id")));
        Page<Evento> resultado = eventos.findAll(criterio, pagina);
        return new PageImpl<>(mapper.resumenes(resultado.getContent(), Map.of()), pagina, resultado.getTotalElements());
    }

    /**
     * Filtra primero con un rectángulo (usa el índice geográfico) y después calcula la distancia
     * real, ordena por cercanía y pagina en memoria. Con el volumen del piloto (cientos de eventos
     * al mes) es más simple y suficiente que una extensión geoespacial.
     */
    private Page<EventoRespuesta> porCercania(Specification<Evento> criterio, FiltroEventos filtro) {
        double radio = Math.min(filtro.radioKm() == null ? RADIO_POR_DEFECTO_KM : filtro.radioKm(),
                propiedades.eventos().radioMaximoKm());
        double lat = filtro.latitud();
        double lng = filtro.longitud();
        List<Evento> candidatos = eventos.findAll(criterio.and(EspecificacionesEvento.dentroDe(Distancia.caja(lat, lng, radio))),
                PageRequest.of(0, MAXIMO_CANDIDATOS, Sort.by("fechaHoraInicio"))).getContent();

        Map<Long, Double> distancias = new HashMap<>();
        for (Evento evento : candidatos) {
            distancias.put(evento.getId(), Distancia.km(lat, lng,
                    evento.getLatitud().doubleValue(), evento.getLongitud().doubleValue()));
        }
        List<Evento> cercanos = candidatos.stream()
                .filter(evento -> distancias.get(evento.getId()) <= radio)
                .sorted(Comparator.<Evento>comparingDouble(evento -> distancias.get(evento.getId()))
                        .thenComparing(Evento::getFechaHoraInicio))
                .toList();

        PageRequest pagina = PageRequest.of(filtro.pagina(), filtro.tamano());
        int desde = (int) Math.min(pagina.getOffset(), cercanos.size());
        int hasta = Math.min(desde + pagina.getPageSize(), cercanos.size());
        return new PageImpl<>(mapper.resumenes(cercanos.subList(desde, hasta), distancias), pagina, cercanos.size());
    }

    /** Los borradores solo los ve su organizador (o un administrador); para los demás no existen. */
    @Transactional(readOnly = true)
    public EventoRespuesta detalle(Long eventoId, Optional<Long> usuarioId, boolean esAdmin) {
        Evento evento = eventos.findById(eventoId).orElseThrow(() -> ApiException.noEncontrado("Evento", eventoId));
        boolean esDueno = usuarioId.map(evento::perteneceA).orElse(false);
        if (!evento.visibleAlPublico() && !esDueno && !esAdmin) {
            throw ApiException.noEncontrado("Evento", eventoId);
        }
        return mapper.detalle(evento, tipos.findByEventoIdOrderByPrecioAscIdAsc(eventoId));
    }

    @Transactional(readOnly = true)
    public List<EventoRespuesta> delOrganizador(Long organizadorId) {
        return mapper.resumenes(eventos.findByOrganizadorIdOrderByFechaHoraInicioDesc(organizadorId), Map.of());
    }
}
