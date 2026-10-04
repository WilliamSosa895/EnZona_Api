package com.uv.enzona.api.boletaje.servicio;

import com.uv.enzona.api.boletaje.modelo.Boleto;
import com.uv.enzona.api.boletaje.modelo.Orden;
import com.uv.enzona.api.boletaje.web.dto.BoletoRespuesta;
import com.uv.enzona.api.comun.util.Fechas;
import com.uv.enzona.api.eventos.modelo.Asiento;
import com.uv.enzona.api.eventos.modelo.Evento;
import com.uv.enzona.api.eventos.modelo.TipoBoleto;
import com.uv.enzona.api.eventos.repositorio.AsientoRepository;
import com.uv.enzona.api.eventos.repositorio.EventoRepository;
import com.uv.enzona.api.eventos.repositorio.TipoBoletoRepository;
import com.uv.enzona.api.boletaje.repositorio.OrdenRepository;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Arma las respuestas de boletos cargando órdenes, eventos, tipos y asientos por lote (sin N+1). */
@Component
public class BoletoMapper {

    private final OrdenRepository ordenes;
    private final EventoRepository eventos;
    private final TipoBoletoRepository tipos;
    private final AsientoRepository asientos;

    public BoletoMapper(OrdenRepository ordenes, EventoRepository eventos, TipoBoletoRepository tipos,
            AsientoRepository asientos) {
        this.ordenes = ordenes;
        this.eventos = eventos;
        this.tipos = tipos;
        this.asientos = asientos;
    }

    public List<BoletoRespuesta> respuestas(List<Boleto> boletos) {
        if (boletos.isEmpty()) {
            return List.of();
        }
        Map<Long, Orden> porOrden = indexar(ordenes.findAllById(ids(boletos, Boleto::getOrdenId)), Orden::getId);
        Map<Long, Evento> porEvento = indexar(eventos.findAllById(porOrden.values().stream().map(Orden::getEventoId)
                .distinct().toList()), Evento::getId);
        Map<Long, TipoBoleto> porTipo = indexar(tipos.findAllById(ids(boletos, Boleto::getTipoBoletoId)), TipoBoleto::getId);
        Map<Long, Asiento> porAsiento = indexar(asientos.findAllById(ids(boletos, Boleto::getAsientoId)), Asiento::getId);

        return boletos.stream().map(boleto -> {
            Orden orden = porOrden.get(boleto.getOrdenId());
            Evento evento = porEvento.get(orden.getEventoId());
            TipoBoleto tipo = porTipo.get(boleto.getTipoBoletoId());
            Asiento asiento = boleto.getAsientoId() == null ? null : porAsiento.get(boleto.getAsientoId());
            return new BoletoRespuesta(
                    boleto.getId(),
                    boleto.getOrdenId(),
                    orden.getEventoId(),
                    boleto.getTipoBoletoId(),
                    boleto.getAsientoId(),
                    boleto.getCodigo(),
                    boleto.getQrFirma(),
                    boleto.contenidoQr(),
                    boleto.getEstado(),
                    Fechas.enZonaMx(boleto.getFechaEmision()),
                    Fechas.enZonaMx(boleto.getFechaUso()),
                    evento.getNombre(),
                    Fechas.enZonaMx(evento.getFechaHoraInicio()),
                    evento.getLugar(),
                    tipo == null ? null : tipo.getNombre(),
                    asiento == null ? null : asiento.etiqueta());
        }).toList();
    }

    private static List<Long> ids(List<Boleto> boletos, Function<Boleto, Long> campo) {
        return boletos.stream().map(campo).filter(Objects::nonNull).distinct().toList();
    }

    private static <T> Map<Long, T> indexar(List<T> elementos, Function<T, Long> id) {
        return elementos.stream().collect(Collectors.toMap(id, Function.identity()));
    }
}
