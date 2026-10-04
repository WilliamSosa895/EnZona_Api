package com.uv.enzona.api.boletaje.servicio;

import com.uv.enzona.api.boletaje.modelo.Boleto;
import com.uv.enzona.api.boletaje.modelo.EstadoOrden;
import com.uv.enzona.api.boletaje.modelo.EstadoPago;
import com.uv.enzona.api.boletaje.modelo.Orden;
import com.uv.enzona.api.boletaje.modelo.OrdenDetalle;
import com.uv.enzona.api.boletaje.modelo.Pago;
import com.uv.enzona.api.boletaje.repositorio.BoletoRepository;
import com.uv.enzona.api.boletaje.repositorio.OrdenDetalleRepository;
import com.uv.enzona.api.boletaje.repositorio.OrdenRepository;
import com.uv.enzona.api.boletaje.repositorio.PagoRepository;
import com.uv.enzona.api.comun.error.ApiException;
import com.uv.enzona.api.eventos.modelo.EstadoAsiento;
import com.uv.enzona.api.eventos.repositorio.AsientoRepository;
import com.uv.enzona.api.eventos.repositorio.TipoBoletoRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cierra una orden pendiente: emite los boletos firmados (pago aprobado o confirmación gratuita)
 * o libera lugares y asientos (pago rechazado o reserva vencida). Cada operación bloquea la orden
 * y es idempotente: si la orden ya no está pendiente, no hace nada (CP-MULTI-07).
 */
@Service
public class ConfirmacionService {

    private static final Logger log = LoggerFactory.getLogger(ConfirmacionService.class);

    private final OrdenRepository ordenes;
    private final OrdenDetalleRepository detalles;
    private final PagoRepository pagos;
    private final BoletoRepository boletos;
    private final TipoBoletoRepository tipos;
    private final AsientoRepository asientos;
    private final FirmaBoletos firma;
    private final ApplicationEventPublisher publicador;
    private final Clock reloj;

    public ConfirmacionService(OrdenRepository ordenes, OrdenDetalleRepository detalles, PagoRepository pagos,
            BoletoRepository boletos, TipoBoletoRepository tipos, AsientoRepository asientos, FirmaBoletos firma,
            ApplicationEventPublisher publicador, Clock reloj) {
        this.ordenes = ordenes;
        this.detalles = detalles;
        this.pagos = pagos;
        this.boletos = boletos;
        this.tipos = tipos;
        this.asientos = asientos;
        this.firma = firma;
        this.publicador = publicador;
        this.reloj = reloj;
    }

    /** Confirmación de asistencia en un evento gratuito (RF-10, CP-08). */
    @Transactional
    public void confirmarSinCobro(Long ordenId) {
        Orden orden = bloquear(ordenId);
        if (orden.pendiente() && orden.gratuita()) {
            emitir(orden);
        }
    }

    /**
     * Pago aprobado: emite los N boletos de la orden (RF-11, RF-12, CP-09, CP-MULTI-01). Si la
     * aprobación llega cuando la reserva ya venció, se registra el pago y la tarea de reembolsos
     * lo devuelve (riesgo E.13 del Anexo E).
     */
    @Transactional
    public void confirmarPago(Long ordenId, String referencia) {
        Orden orden = bloquear(ordenId);
        OffsetDateTime ahora = OffsetDateTime.now(reloj);
        boolean yaRegistrado = pagos.findFirstByOrdenIdAndEstado(ordenId, EstadoPago.APROBADO).isPresent();
        if (!yaRegistrado) {
            pagos.save(Pago.registrar(ordenId, orden.getTotal(), EstadoPago.APROBADO, referencia, ahora));
        }
        if (orden.pendiente()) {
            emitir(orden);
        } else if (orden.getEstado() == EstadoOrden.CANCELADA && !yaRegistrado) {
            log.warn("Pago aprobado para la orden {} que ya estaba cancelada; se reembolsará", ordenId);
        }
    }

    /** Pago rechazado: no se emite nada y se devuelven lugares y asientos (CP-10, CP-MULTI-06). */
    @Transactional
    public void rechazarPago(Long ordenId, String referencia) {
        Orden orden = bloquear(ordenId);
        if (!orden.pendiente()) {
            return;
        }
        pagos.save(Pago.registrar(ordenId, orden.getTotal(), EstadoPago.RECHAZADO, referencia, OffsetDateTime.now(reloj)));
        liberar(orden);
        orden.cancelar();
    }

    /** Reserva vencida sin confirmación de pago (CP-37). */
    @Transactional
    public void expirar(Long ordenId) {
        Orden orden = bloquear(ordenId);
        if (orden.pendiente() && orden.getExpiraEn() != null && orden.getExpiraEn().isBefore(OffsetDateTime.now(reloj))) {
            liberar(orden);
            orden.cancelar();
        }
    }

    private void emitir(Orden orden) {
        OffsetDateTime ahora = OffsetDateTime.now(reloj);
        OrdenDetalle detalle = detalle(orden);
        List<Long> asientosOrden = detalle.asientos();
        if (!asientosOrden.isEmpty()) {
            int ocupados = asientos.cambiarEstado(asientosOrden, orden.getEventoId(), EstadoAsiento.RESERVADO,
                    EstadoAsiento.OCUPADO);
            if (ocupados != asientosOrden.size()) {
                throw new IllegalStateException("Los asientos de la orden " + orden.getId() + " ya no estaban reservados");
            }
        }
        List<Boleto> emitidos = new ArrayList<>();
        for (int i = 0; i < detalle.getCantidad(); i++) {
            Long asientoId = asientosOrden.isEmpty() ? null : asientosOrden.get(i);
            String codigo = firma.nuevoCodigo();
            emitidos.add(Boleto.emitir(orden.getId(), detalle.getTipoBoletoId(), asientoId, codigo,
                    firma.firmar(codigo, orden.getEventoId()), ahora));
        }
        boletos.saveAll(emitidos);
        orden.marcarPagada();
        publicador.publishEvent(new BoletosEmitidos(orden.getId()));
    }

    private void liberar(Orden orden) {
        OrdenDetalle detalle = detalle(orden);
        tipos.reponer(detalle.getTipoBoletoId(), detalle.getCantidad());
        if (!detalle.asientos().isEmpty()) {
            asientos.cambiarEstado(detalle.asientos(), orden.getEventoId(), EstadoAsiento.RESERVADO,
                    EstadoAsiento.DISPONIBLE);
        }
    }

    private Orden bloquear(Long ordenId) {
        return ordenes.bloquearPorId(ordenId).orElseThrow(() -> ApiException.noEncontrado("Orden", ordenId));
    }

    private OrdenDetalle detalle(Orden orden) {
        return detalles.findByOrdenId(orden.getId()).stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("La orden " + orden.getId() + " no tiene detalle"));
    }
}
