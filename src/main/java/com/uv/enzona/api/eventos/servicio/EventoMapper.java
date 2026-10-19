package com.uv.enzona.api.eventos.servicio;

import com.uv.enzona.api.comun.util.Dinero;
import com.uv.enzona.api.comun.util.Fechas;
import com.uv.enzona.api.eventos.modelo.Categoria;
import com.uv.enzona.api.eventos.modelo.Evento;
import com.uv.enzona.api.eventos.modelo.TipoBoleto;
import com.uv.enzona.api.eventos.repositorio.CategoriaRepository;
import com.uv.enzona.api.eventos.repositorio.TipoBoletoRepository;
import com.uv.enzona.api.eventos.repositorio.TipoBoletoRepository.ResumenInventario;
import com.uv.enzona.api.eventos.web.dto.EventoRespuesta;
import com.uv.enzona.api.eventos.web.dto.TipoBoletoRespuesta;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Arma las respuestas de evento con su inventario y categoría en dos consultas por página (sin N+1). */
@Component
public class EventoMapper {

    private final TipoBoletoRepository tipos;
    private final CategoriaRepository categorias;

    public EventoMapper(TipoBoletoRepository tipos, CategoriaRepository categorias) {
        this.tipos = tipos;
        this.categorias = categorias;
    }

    public List<EventoRespuesta> resumenes(List<Evento> eventos, Map<Long, Double> distancias) {
        if (eventos.isEmpty()) {
            return List.of();
        }
        Map<Long, ResumenInventario> inventario = tipos.resumir(eventos.stream().map(Evento::getId).toList()).stream()
                .collect(Collectors.toMap(ResumenInventario::getEventoId, Function.identity()));
        Map<Integer, String> nombresCategoria = nombresCategoria();
        return eventos.stream()
                .map(evento -> {
                    ResumenInventario resumen = inventario.get(evento.getId());
                    int disponibles = resumen == null || resumen.getDisponibles() == null
                            ? 0 : resumen.getDisponibles().intValue();
                    BigDecimal precioDesde = resumen == null ? null : resumen.getPrecioDesde();
                    return armar(evento, nombresCategoria, disponibles, precioDesde, distancias.get(evento.getId()), null);
                })
                .toList();
    }

    public EventoRespuesta detalle(Evento evento, List<TipoBoleto> tiposDelEvento) {
        int disponibles = tiposDelEvento.stream().mapToInt(TipoBoleto::getCantidadDisponible).sum();
        BigDecimal precioDesde = tiposDelEvento.stream().map(TipoBoleto::getPrecio).min(BigDecimal::compareTo).orElse(null);
        List<TipoBoletoRespuesta> tiposRespuesta = tiposDelEvento.stream().map(TipoBoletoRespuesta::de).toList();
        return armar(evento, nombresCategoria(), disponibles, precioDesde, null, tiposRespuesta);
    }

    private Map<Integer, String> nombresCategoria() {
        return categorias.findAll().stream().collect(Collectors.toMap(Categoria::getId, Categoria::getNombre));
    }

    private static EventoRespuesta armar(Evento e, Map<Integer, String> nombresCategoria, int disponibles,
            BigDecimal precioDesde, Double distanciaKm, List<TipoBoletoRespuesta> tiposRespuesta) {
        return new EventoRespuesta(
                e.getId(),
                e.getOrganizadorId(),
                e.getNombre(),
                e.getDescripcion(),
                e.getLugar(),
                e.getDireccion(),
                e.getCiudad(),
                e.getLatitud() == null ? null : e.getLatitud().doubleValue(),
                e.getLongitud() == null ? null : e.getLongitud().doubleValue(),
                e.getCategoriaId(),
                e.getCategoriaId() == null ? null : nombresCategoria.get(e.getCategoriaId()),
                Fechas.enZonaMx(e.getFechaHoraInicio()),
                Fechas.enZonaMx(e.getFechaHoraFin()),
                e.isEsDePago(),
                e.isRequiereAsiento(),
                e.getAforo(),
                disponibles,
                precioDesde == null ? null : Dinero.texto(precioDesde),
                e.getEstado(),
                distanciaKm == null ? null : Math.round(distanciaKm * 100.0) / 100.0,
                Fechas.enZonaMx(e.getFechaCancelacion()),
                e.getMotivoCancelacion(),
                tiposRespuesta,
                e.getImagenUrl());
    }
}
