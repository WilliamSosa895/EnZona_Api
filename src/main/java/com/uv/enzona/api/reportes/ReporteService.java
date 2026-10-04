package com.uv.enzona.api.reportes;

import com.uv.enzona.api.boletaje.modelo.EstadoBoleto;
import com.uv.enzona.api.boletaje.modelo.EstadoOrden;
import com.uv.enzona.api.comun.util.Dinero;
import com.uv.enzona.api.eventos.modelo.Evento;
import com.uv.enzona.api.eventos.modelo.TipoBoleto;
import com.uv.enzona.api.eventos.repositorio.TipoBoletoRepository;
import com.uv.enzona.api.eventos.servicio.AccesoEventos;
import com.uv.enzona.api.validacion.modelo.ResultadoAcceso;
import com.uv.enzona.api.validacion.repositorio.ValidacionAccesoRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReporteService {

    private final AccesoEventos acceso;
    private final ReporteRepository reportes;
    private final TipoBoletoRepository tipos;
    private final ValidacionAccesoRepository bitacora;

    public ReporteService(AccesoEventos acceso, ReporteRepository reportes, TipoBoletoRepository tipos,
            ValidacionAccesoRepository bitacora) {
        this.acceso = acceso;
        this.reportes = reportes;
        this.tipos = tipos;
        this.bitacora = bitacora;
    }

    @Transactional(readOnly = true)
    public ReporteRespuesta reporte(Long eventoId, Long usuarioId, boolean esAdmin) {
        Evento evento = acceso.cargarGestionable(eventoId, usuarioId, esAdmin, true);
        Map<EstadoBoleto, Long> porEstado = reportes.boletosPorEstado(eventoId).stream()
                .collect(Collectors.toMap(ReporteRepository.ConteoPorEstado::getEstado,
                        ReporteRepository.ConteoPorEstado::getCantidad));
        long validos = porEstado.getOrDefault(EstadoBoleto.VALIDO, 0L);
        long usados = porEstado.getOrDefault(EstadoBoleto.USADO, 0L);
        Map<Long, Long> emitidosPorTipo = reportes.boletosPorTipo(eventoId, List.of(EstadoBoleto.VALIDO, EstadoBoleto.USADO))
                .stream().collect(Collectors.toMap(ReporteRepository.ConteoPorTipo::getTipoBoletoId,
                        ReporteRepository.ConteoPorTipo::getCantidad));

        List<TipoBoleto> delEvento = tipos.findByEventoIdOrderByPrecioAscIdAsc(eventoId);
        List<ReporteRespuesta.PorTipo> porTipo = delEvento.stream().map(tipo -> {
            long emitidos = emitidosPorTipo.getOrDefault(tipo.getId(), 0L);
            return new ReporteRespuesta.PorTipo(tipo.getId(), tipo.getNombre(), Dinero.texto(tipo.getPrecio()),
                    tipo.getCantidadTotal(), tipo.getCantidadDisponible(), emitidos,
                    Dinero.texto(tipo.getPrecio().multiply(BigDecimal.valueOf(emitidos))));
        }).toList();

        int disponibles = delEvento.stream().mapToInt(TipoBoleto::getCantidadDisponible).sum();
        double ocupacion = evento.getAforo() == 0 ? 0 : Math.round((validos + usados) * 1000.0 / evento.getAforo()) / 10.0;
        return new ReporteRespuesta(eventoId, evento.getNombre(), evento.getEstado(), evento.getAforo(), disponibles,
                validos + usados, usados, bitacora.contarDelEvento(eventoId, ResultadoAcceso.RECHAZADO),
                porEstado.getOrDefault(EstadoBoleto.CANCELADO, 0L),
                Dinero.texto(reportes.importePorEstado(eventoId, EstadoOrden.PAGADA)),
                Dinero.texto(reportes.importePorEstado(eventoId, EstadoOrden.REEMBOLSADA)),
                ocupacion, porTipo);
    }
}
