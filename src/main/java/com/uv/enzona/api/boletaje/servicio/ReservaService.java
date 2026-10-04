package com.uv.enzona.api.boletaje.servicio;

import com.uv.enzona.api.boletaje.modelo.EstadoOrden;
import com.uv.enzona.api.boletaje.modelo.MetodoPago;
import com.uv.enzona.api.boletaje.modelo.Orden;
import com.uv.enzona.api.boletaje.modelo.OrdenDetalle;
import com.uv.enzona.api.boletaje.repositorio.OrdenDetalleRepository;
import com.uv.enzona.api.boletaje.repositorio.OrdenRepository;
import com.uv.enzona.api.boletaje.web.dto.CrearOrdenSolicitud;
import com.uv.enzona.api.comun.config.EnzonaProperties;
import com.uv.enzona.api.comun.error.ApiException;
import com.uv.enzona.api.comun.error.ErrorCodigo;
import com.uv.enzona.api.eventos.modelo.EstadoAsiento;
import com.uv.enzona.api.eventos.modelo.EstadoEvento;
import com.uv.enzona.api.eventos.modelo.Evento;
import com.uv.enzona.api.eventos.modelo.TipoBoleto;
import com.uv.enzona.api.eventos.repositorio.AsientoRepository;
import com.uv.enzona.api.eventos.repositorio.EventoRepository;
import com.uv.enzona.api.eventos.repositorio.TipoBoletoRepository;
import com.uv.enzona.api.identidad.repositorio.UsuarioRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reserva de lugares y asientos para una orden, todo o nada (Anexo E, escenario E.7.1).
 * Si falta un solo lugar o un solo asiento, la transacción se revierte y no queda nada reservado.
 */
@Service
public class ReservaService {

    /** Una confirmación gratuita vigente es una orden pendiente o ya confirmada (PAGADA con total 0). */
    private static final Set<EstadoOrden> ORDENES_VIGENTES = Set.of(EstadoOrden.PENDIENTE, EstadoOrden.PAGADA);

    private final EventoRepository eventos;
    private final TipoBoletoRepository tipos;
    private final AsientoRepository asientos;
    private final OrdenRepository ordenes;
    private final OrdenDetalleRepository detalles;
    private final UsuarioRepository usuarios;
    private final EnzonaProperties propiedades;
    private final Clock reloj;

    public ReservaService(EventoRepository eventos, TipoBoletoRepository tipos, AsientoRepository asientos,
            OrdenRepository ordenes, OrdenDetalleRepository detalles, UsuarioRepository usuarios,
            EnzonaProperties propiedades, Clock reloj) {
        this.eventos = eventos;
        this.tipos = tipos;
        this.asientos = asientos;
        this.ordenes = ordenes;
        this.detalles = detalles;
        this.usuarios = usuarios;
        this.propiedades = propiedades;
        this.reloj = reloj;
    }

    @Transactional
    public Orden reservar(Long asistenteId, String claveOperacion, CrearOrdenSolicitud solicitud) {
        OffsetDateTime ahora = OffsetDateTime.now(reloj);
        Evento evento = eventos.findById(solicitud.eventoId())
                .orElseThrow(() -> ApiException.noEncontrado("Evento", solicitud.eventoId()));
        if (evento.getEstado() != EstadoEvento.PUBLICADO || evento.iniciado(ahora)) {
            throw new ApiException(ErrorCodigo.ESTADO_INVALIDO, "El evento no está disponible para compra o confirmación");
        }
        int cantidad = solicitud.cantidad();
        if (cantidad > propiedades.ordenes().maximoBoletosPorOrden()) {
            throw ApiException.reglaNegocio("El máximo por orden es " + propiedades.ordenes().maximoBoletosPorOrden()
                    + " boletos");
        }
        TipoBoleto tipo = resolverTipo(evento, solicitud.tipoBoletoId());
        List<Long> asientosPedidos = asientosDistintos(solicitud.asientosONada());

        validarModalidad(evento, solicitud, cantidad, asientosPedidos);
        if (!evento.isEsDePago()) {
            // Una confirmación por persona (CP-34, CP-MULTI-09). El bloqueo de la fila del usuario
            // evita que dos solicitudes simultáneas de la misma persona pasen la comprobación.
            usuarios.bloquearPorId(asistenteId);
            if (ordenes.existsByAsistenteIdAndEventoIdAndEstadoIn(asistenteId, evento.getId(), ORDENES_VIGENTES)) {
                throw new ApiException(ErrorCodigo.CONFIRMACION_DUPLICADA);
            }
        }

        // Control de aforo: actualización condicional en la base de datos (CP-11, CP-33, CP-MULTI-05).
        if (tipos.descontar(tipo.getId(), cantidad) == 0) {
            throw new ApiException(ErrorCodigo.AFORO_AGOTADO);
        }
        // Asientos: todos o ninguno (CP-36, CP-MULTI-08). Si falla, la transacción revierte el descuento.
        if (!asientosPedidos.isEmpty()
                && asientos.cambiarEstado(asientosPedidos, evento.getId(), EstadoAsiento.DISPONIBLE,
                        EstadoAsiento.RESERVADO) != asientosPedidos.size()) {
            throw new ApiException(ErrorCodigo.ASIENTO_NO_DISPONIBLE);
        }

        // El precio sale del tipo de boleto, nunca del cliente (RNF-09).
        BigDecimal total = tipo.getPrecio().multiply(BigDecimal.valueOf(cantidad));
        OffsetDateTime expira = ahora.plusMinutes(propiedades.ordenes().minutosReserva());
        Orden orden = ordenes.saveAndFlush(Orden.nueva(asistenteId, evento.getId(), total, claveOperacion, expira, ahora));
        detalles.save(OrdenDetalle.nuevo(orden.getId(), tipo.getId(), cantidad, tipo.getPrecio(), asientosPedidos));
        return orden;
    }

    private TipoBoleto resolverTipo(Evento evento, Long tipoBoletoId) {
        List<TipoBoleto> delEvento = tipos.findByEventoIdOrderByPrecioAscIdAsc(evento.getId());
        if (tipoBoletoId == null) {
            if (delEvento.size() == 1) {
                return delEvento.get(0);
            }
            throw ApiException.reglaNegocio("Indica el tipo de boleto (tipoBoletoId)");
        }
        return delEvento.stream()
                .filter(tipo -> tipo.getId().equals(tipoBoletoId))
                .findFirst()
                .orElseThrow(() -> ApiException.reglaNegocio("El tipo de boleto no pertenece al evento"));
    }

    private static void validarModalidad(Evento evento, CrearOrdenSolicitud solicitud, int cantidad,
            List<Long> asientosPedidos) {
        if (!evento.isEsDePago()) {
            if (solicitud.metodo() != MetodoPago.GRATIS) {
                throw ApiException.reglaNegocio("Un evento gratuito se confirma con el método GRATIS");
            }
            if (cantidad != 1) {
                throw ApiException.reglaNegocio("En eventos gratuitos se confirma un lugar por persona");
            }
        } else if (solicitud.metodo() == MetodoPago.GRATIS) {
            throw ApiException.reglaNegocio("El evento es de pago: elige un método de pago");
        }

        if (evento.isRequiereAsiento()) {
            if (asientosPedidos.size() != cantidad) {
                throw ApiException.reglaNegocio("Selecciona un asiento por boleto: faltan "
                        + Math.max(0, cantidad - asientosPedidos.size()) + " de " + cantidad);
            }
        } else if (!asientosPedidos.isEmpty()) {
            throw ApiException.reglaNegocio("Este evento no tiene asientos numerados");
        }
    }

    private static List<Long> asientosDistintos(List<Long> pedidos) {
        Set<Long> distintos = new LinkedHashSet<>(pedidos);
        if (distintos.size() != pedidos.size()) {
            throw ApiException.reglaNegocio("Hay asientos repetidos en la selección");
        }
        return List.copyOf(distintos);
    }
}
