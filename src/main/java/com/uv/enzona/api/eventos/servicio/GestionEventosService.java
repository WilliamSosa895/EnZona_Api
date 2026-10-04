package com.uv.enzona.api.eventos.servicio;

import com.uv.enzona.api.comun.error.ApiException;
import com.uv.enzona.api.comun.error.ErrorCodigo;
import com.uv.enzona.api.eventos.modelo.DatosEvento;
import com.uv.enzona.api.eventos.modelo.EstadoEvento;
import com.uv.enzona.api.eventos.modelo.Evento;
import com.uv.enzona.api.eventos.modelo.TipoBoleto;
import com.uv.enzona.api.eventos.repositorio.AsientoRepository;
import com.uv.enzona.api.eventos.repositorio.CategoriaRepository;
import com.uv.enzona.api.eventos.repositorio.EventoRepository;
import com.uv.enzona.api.eventos.repositorio.TipoBoletoRepository;
import com.uv.enzona.api.eventos.web.dto.EventoRespuesta;
import com.uv.enzona.api.eventos.web.dto.EventoSolicitud;
import com.uv.enzona.api.eventos.web.dto.TipoBoletoSolicitud;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Creación, publicación y modificación de eventos bajo políticas (RF-08, RF-13, CU-05). */
@Service
public class GestionEventosService {

    private static final String CIUDAD_POR_DEFECTO = "Orizaba";

    private final EventoRepository eventos;
    private final TipoBoletoRepository tipos;
    private final AsientoRepository asientos;
    private final CategoriaRepository categorias;
    private final AsientosService mapaAsientos;
    private final AccesoEventos acceso;
    private final EventoMapper mapper;
    private final ApplicationEventPublisher publicador;
    private final Clock reloj;

    public GestionEventosService(EventoRepository eventos, TipoBoletoRepository tipos, AsientoRepository asientos,
            CategoriaRepository categorias, AsientosService mapaAsientos, AccesoEventos acceso, EventoMapper mapper,
            ApplicationEventPublisher publicador, Clock reloj) {
        this.eventos = eventos;
        this.tipos = tipos;
        this.asientos = asientos;
        this.categorias = categorias;
        this.mapaAsientos = mapaAsientos;
        this.acceso = acceso;
        this.mapper = mapper;
        this.publicador = publicador;
        this.reloj = reloj;
    }

    // ------------------------------------------------------------------ alta (RF-08)

    @Transactional
    public EventoRespuesta crear(Long organizadorId, EventoSolicitud solicitud) {
        ReglasEvento.validar(solicitud, true);
        validarCategoria(solicitud.categoriaId());
        OffsetDateTime ahora = OffsetDateTime.now(reloj);

        Evento evento = eventos.save(Evento.crear(organizadorId, datos(solicitud), ahora));
        crearInventario(evento, solicitud);
        if (solicitud.requiereAsiento()) {
            mapaAsientos.generar(evento.getId(), solicitud.seccionesONada());
        }
        if (solicitud.publicar()) {
            evento.publicar(ahora);
        }
        return mapper.detalle(evento, tipos.findByEventoIdOrderByPrecioAscIdAsc(evento.getId()));
    }

    @Transactional
    public EventoRespuesta publicar(Long eventoId, Long usuarioId) {
        Evento evento = acceso.bloquearGestionable(eventoId, usuarioId, false, false);
        evento.publicar(OffsetDateTime.now(reloj));
        return mapper.detalle(evento, tipos.findByEventoIdOrderByPrecioAscIdAsc(eventoId));
    }

    // ------------------------------------------------------------------ modificación (RF-13)

    /**
     * Nota: los tipos de boleto se modifican solo con actualizaciones en la base de datos, nunca
     * guardando la entidad leída: guardar una copia en memoria podría sobrescribir el inventario
     * con un valor viejo si alguien compró mientras tanto.
     *
     * <p>Aplica la política de modificación: no se edita un evento iniciado, cancelado o realizado;
     * el aforo y las cantidades no bajan de lo ya vendido o reservado (CP-15); en un evento
     * publicado no cambia la modalidad, y el precio de un tipo con ventas no cambia (RNF-09).
     */
    @Transactional
    public EventoRespuesta actualizar(Long eventoId, Long usuarioId, EventoSolicitud solicitud) {
        Evento evento = acceso.bloquearGestionable(eventoId, usuarioId, false, false);
        OffsetDateTime ahora = OffsetDateTime.now(reloj);
        if (!evento.esModificable()) {
            throw new ApiException(ErrorCodigo.ESTADO_INVALIDO, "Un evento " + evento.getEstado() + " no se modifica");
        }
        if (evento.iniciado(ahora)) {
            throw new ApiException(ErrorCodigo.POLITICA_MODIFICACION, "El evento ya inició; no se puede modificar");
        }
        ReglasEvento.validar(solicitud, false);
        validarCategoria(solicitud.categoriaId());

        List<TipoBoleto> actuales = tipos.findByEventoIdOrderByPrecioAscIdAsc(eventoId);
        int comprometidos = actuales.stream().mapToInt(TipoBoleto::comprometidos).sum();
        if (solicitud.aforo() < comprometidos) {
            throw new ApiException(ErrorCodigo.POLITICA_MODIFICACION, "No puedes reducir el aforo a "
                    + solicitud.aforo() + ": ya hay " + comprometidos + " lugares vendidos o reservados");
        }
        boolean publicado = evento.getEstado() == EstadoEvento.PUBLICADO;
        boolean cambiaModalidad = evento.isEsDePago() != solicitud.esDePago()
                || evento.isRequiereAsiento() != solicitud.requiereAsiento();
        if (cambiaModalidad && (publicado || comprometidos > 0)) {
            throw new ApiException(ErrorCodigo.POLITICA_MODIFICACION,
                    "La modalidad (de pago o con asiento) no cambia una vez publicado el evento");
        }

        String cambiosRelevantes = publicado ? describirCambios(evento, solicitud) : null;

        // 1) Datos propios del evento. Se guardan antes de tocar el inventario porque las
        //    actualizaciones condicionales limpian el contexto de persistencia.
        evento.aplicar(datos(solicitud));
        evento.cambiarModalidad(solicitud.esDePago(), solicitud.requiereAsiento());
        eventos.saveAndFlush(evento);

        // 2) Inventario y mapa de asientos.
        if (!publicado && comprometidos == 0) {
            tipos.deleteAllInBatch(actuales);
            crearInventario(evento, solicitud);
        } else {
            actualizarInventario(evento, solicitud, actuales);
        }
        if (solicitud.requiereAsiento()) {
            if (!solicitud.seccionesONada().isEmpty()) {
                mapaAsientos.regenerar(eventoId, solicitud.seccionesONada());
            } else if (asientos.countByEventoId(eventoId) != solicitud.aforo()) {
                throw ApiException.reglaNegocio("El aforo cambió: envía las secciones para ajustar el mapa de asientos");
            }
        }

        if (cambiosRelevantes != null && !cambiosRelevantes.isBlank()) {
            publicador.publishEvent(new EventoModificado(eventoId, solicitud.nombre(), cambiosRelevantes));
        }
        Evento actualizado = acceso.cargar(eventoId);
        return mapper.detalle(actualizado, tipos.findByEventoIdOrderByPrecioAscIdAsc(eventoId));
    }

    // ------------------------------------------------------------------ inventario

    private void crearInventario(Evento evento, EventoSolicitud solicitud) {
        if (!solicitud.esDePago()) {
            tipos.save(TipoBoleto.nuevo(evento.getId(), TipoBoleto.NOMBRE_GRATUITO, BigDecimal.ZERO, solicitud.aforo()));
            return;
        }
        List<TipoBoleto> nuevos = solicitud.tiposONada().stream()
                .map(tipo -> TipoBoleto.nuevo(evento.getId(), tipo.nombre().trim(), escala(tipo.precio()), tipo.cantidad()))
                .toList();
        tipos.saveAll(nuevos);
    }

    private void actualizarInventario(Evento evento, EventoSolicitud solicitud, List<TipoBoleto> actuales) {
        Long eventoId = evento.getId();
        if (!solicitud.esDePago()) {
            TipoBoleto general = actuales.get(0);
            ajustarTotal(general, solicitud.aforo());
            return;
        }
        Map<Long, TipoBoleto> porId = actuales.stream().collect(Collectors.toMap(TipoBoleto::getId, Function.identity()));
        List<Long> conservados = new ArrayList<>();
        List<TipoBoleto> nuevos = new ArrayList<>();
        for (TipoBoletoSolicitud pedido : solicitud.tiposONada()) {
            if (pedido.id() == null) {
                nuevos.add(TipoBoleto.nuevo(eventoId, pedido.nombre().trim(), escala(pedido.precio()), pedido.cantidad()));
                continue;
            }
            TipoBoleto actual = porId.get(pedido.id());
            if (actual == null) {
                throw ApiException.reglaNegocio("El tipo de boleto " + pedido.id() + " no pertenece al evento");
            }
            conservados.add(actual.getId());
            if (!actual.getNombre().equals(pedido.nombre().trim())) {
                tipos.renombrar(actual.getId(), pedido.nombre().trim());
            }
            if (escala(pedido.precio()).compareTo(actual.getPrecio()) != 0
                    && tipos.cambiarPrecioSinVentas(actual.getId(), escala(pedido.precio())) == 0) {
                throw new ApiException(ErrorCodigo.POLITICA_MODIFICACION,
                        "El precio de \"" + actual.getNombre() + "\" no cambia porque ya tiene ventas");
            }
            if (pedido.cantidad() != actual.getCantidadTotal()) {
                ajustarTotal(actual, pedido.cantidad());
            }
        }
        for (TipoBoleto actual : actuales) {
            if (!conservados.contains(actual.getId())) {
                if (actual.tieneVentas()) {
                    throw new ApiException(ErrorCodigo.POLITICA_MODIFICACION,
                            "No se elimina \"" + actual.getNombre() + "\" porque ya tiene ventas");
                }
                tipos.deleteById(actual.getId());
            }
        }
        tipos.saveAll(nuevos);
    }

    private void ajustarTotal(TipoBoleto tipo, int nuevoTotal) {
        if (tipos.ajustarTotal(tipo.getId(), nuevoTotal) == 0) {
            throw new ApiException(ErrorCodigo.POLITICA_MODIFICACION, "La cantidad de \"" + tipo.getNombre()
                    + "\" no puede ser menor que lo ya vendido o reservado");
        }
    }

    // ------------------------------------------------------------------ apoyo

    private void validarCategoria(Integer categoriaId) {
        if (categoriaId != null && !categorias.existsById(categoriaId)) {
            throw ApiException.reglaNegocio("La categoría " + categoriaId + " no existe");
        }
    }

    private static DatosEvento datos(EventoSolicitud s) {
        String ciudad = s.ciudad() == null || s.ciudad().isBlank() ? CIUDAD_POR_DEFECTO : s.ciudad().trim();
        return new DatosEvento(s.nombre().trim(), s.descripcion(), s.lugar().trim(), s.direccion(), ciudad,
                coordenada(s.latitud()), coordenada(s.longitud()), s.categoriaId(), s.fechaHoraInicio(),
                s.fechaHoraFin(), s.aforo(), s.esDePago(), s.requiereAsiento());
    }

    private static BigDecimal coordenada(BigDecimal valor) {
        return valor == null ? null : valor.setScale(6, RoundingMode.HALF_UP);
    }

    private static BigDecimal escala(BigDecimal precio) {
        return precio.setScale(2, RoundingMode.HALF_UP);
    }

    private static String describirCambios(Evento evento, EventoSolicitud s) {
        List<String> cambios = new ArrayList<>();
        if (!evento.getFechaHoraInicio().isEqual(s.fechaHoraInicio())) {
            cambios.add("fecha y hora de inicio");
        }
        boolean finCambio = evento.getFechaHoraFin() == null ? s.fechaHoraFin() != null
                : s.fechaHoraFin() == null || !evento.getFechaHoraFin().isEqual(s.fechaHoraFin());
        if (finCambio) {
            cambios.add("hora de término");
        }
        if (!evento.getLugar().equals(s.lugar().trim()) || !Objects.equals(evento.getDireccion(), s.direccion())) {
            cambios.add("lugar");
        }
        return String.join(", ", cambios);
    }
}
