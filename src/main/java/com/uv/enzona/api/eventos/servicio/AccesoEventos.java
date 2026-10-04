package com.uv.enzona.api.eventos.servicio;

import com.uv.enzona.api.comun.error.ApiException;
import com.uv.enzona.api.eventos.modelo.Evento;
import com.uv.enzona.api.eventos.repositorio.EventoRepository;
import org.springframework.stereotype.Component;

/** Carga eventos comprobando quién puede gestionarlos. */
@Component
public class AccesoEventos {

    private final EventoRepository eventos;

    public AccesoEventos(EventoRepository eventos) {
        this.eventos = eventos;
    }

    public Evento cargar(Long eventoId) {
        return eventos.findById(eventoId).orElseThrow(() -> ApiException.noEncontrado("Evento", eventoId));
    }

    /** El organizador dueño del evento, o un administrador si {@code permitirAdmin} es verdadero. */
    public Evento cargarGestionable(Long eventoId, Long usuarioId, boolean esAdmin, boolean permitirAdmin) {
        Evento evento = cargar(eventoId);
        verificarGestion(evento, usuarioId, esAdmin, permitirAdmin);
        return evento;
    }

    /** Igual que {@link #cargarGestionable} pero bloqueando la fila para modificarla. */
    public Evento bloquearGestionable(Long eventoId, Long usuarioId, boolean esAdmin, boolean permitirAdmin) {
        Evento evento = eventos.bloquearPorId(eventoId).orElseThrow(() -> ApiException.noEncontrado("Evento", eventoId));
        verificarGestion(evento, usuarioId, esAdmin, permitirAdmin);
        return evento;
    }

    private static void verificarGestion(Evento evento, Long usuarioId, boolean esAdmin, boolean permitirAdmin) {
        boolean esDueno = evento.perteneceA(usuarioId);
        if (!esDueno && !(permitirAdmin && esAdmin)) {
            throw ApiException.accesoDenegado();
        }
    }
}
