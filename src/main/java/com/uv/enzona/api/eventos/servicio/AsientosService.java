package com.uv.enzona.api.eventos.servicio;

import com.uv.enzona.api.comun.error.ApiException;
import com.uv.enzona.api.eventos.modelo.Asiento;
import com.uv.enzona.api.eventos.modelo.EstadoAsiento;
import com.uv.enzona.api.eventos.modelo.Evento;
import com.uv.enzona.api.eventos.repositorio.AsientoRepository;
import com.uv.enzona.api.eventos.web.dto.AsientoRespuesta;
import com.uv.enzona.api.eventos.web.dto.SeccionSolicitud;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Mapa de asientos de los eventos con asiento numerado (RF-09). */
@Service
public class AsientosService {

    private final AsientoRepository asientos;
    private final AccesoEventos acceso;

    public AsientosService(AsientoRepository asientos, AccesoEventos acceso) {
        this.asientos = asientos;
        this.acceso = acceso;
    }

    /** Genera filas A, B, C… con asientos numerados 1..N en cada sección. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void generar(Long eventoId, List<SeccionSolicitud> secciones) {
        List<Asiento> nuevos = new ArrayList<>();
        for (SeccionSolicitud seccion : secciones) {
            for (int fila = 0; fila < seccion.filas(); fila++) {
                String letra = String.valueOf((char) ('A' + fila));
                for (int numero = 1; numero <= seccion.asientosPorFila(); numero++) {
                    nuevos.add(Asiento.nuevo(eventoId, seccion.nombre().trim(), letra, String.valueOf(numero)));
                }
            }
        }
        asientos.saveAll(nuevos);
    }

    /** Sustituye el mapa completo; solo si ningún asiento está reservado u ocupado. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void regenerar(Long eventoId, List<SeccionSolicitud> secciones) {
        if (asientos.countByEventoIdAndEstadoNot(eventoId, EstadoAsiento.DISPONIBLE) > 0) {
            throw ApiException.reglaNegocio("No se puede cambiar el mapa de asientos: ya hay asientos vendidos o reservados");
        }
        asientos.borrarDelEvento(eventoId);
        generar(eventoId, secciones);
    }

    @Transactional(readOnly = true)
    public List<AsientoRespuesta> listar(Long eventoId, Optional<Long> usuarioId, boolean esAdmin) {
        Evento evento = acceso.cargar(eventoId);
        boolean esDueno = usuarioId.map(evento::perteneceA).orElse(false);
        if (!evento.visibleAlPublico() && !esDueno && !esAdmin) {
            throw ApiException.noEncontrado("Evento", eventoId);
        }
        return asientos.findByEventoId(eventoId).stream()
                .sorted(Comparator.comparing(Asiento::getSeccion)
                        .thenComparing(Asiento::getFila)
                        .thenComparingInt(asiento -> Integer.parseInt(asiento.getNumero())))
                .map(AsientoRespuesta::de)
                .toList();
    }
}
