package com.uv.enzona.api.boletaje.servicio;

import com.uv.enzona.api.boletaje.modelo.EstadoOrden;
import com.uv.enzona.api.boletaje.modelo.EstadoPago;
import com.uv.enzona.api.boletaje.modelo.Orden;
import com.uv.enzona.api.boletaje.modelo.Pago;
import com.uv.enzona.api.boletaje.pasarela.PasarelaNoDisponibleException;
import com.uv.enzona.api.boletaje.pasarela.PasarelaPago;
import com.uv.enzona.api.boletaje.pasarela.ResultadoReembolso;
import com.uv.enzona.api.boletaje.repositorio.OrdenRepository;
import com.uv.enzona.api.boletaje.repositorio.PagoRepository;
import com.uv.enzona.api.eventos.modelo.EstadoEvento;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Devuelve los pagos de eventos cancelados y los aprobados tarde (RF-14, CP-16). Es idempotente:
 * la clave de reembolso es fija por pago, y un reembolso fallido se reintenta en la siguiente
 * ejecución. La llamada a la pasarela ocurre fuera de cualquier transacción.
 */
@Service
public class ReembolsoService {

    private static final Logger log = LoggerFactory.getLogger(ReembolsoService.class);
    private static final int LOTE = 50;

    private final PagoRepository pagos;
    private final OrdenRepository ordenes;
    private final PasarelaPago pasarela;
    private final TransactionTemplate transaccion;
    private final Clock reloj;

    public ReembolsoService(PagoRepository pagos, OrdenRepository ordenes, PasarelaPago pasarela,
            TransactionTemplate transaccion, Clock reloj) {
        this.pagos = pagos;
        this.ordenes = ordenes;
        this.pasarela = pasarela;
        this.transaccion = transaccion;
        this.reloj = reloj;
    }

    public int procesarPendientes() {
        List<Pago> pendientes = transaccion.execute(estado -> pagos.pendientesDeReembolso(EstadoPago.APROBADO,
                EstadoOrden.PAGADA, EstadoOrden.CANCELADA, EstadoEvento.CANCELADO, PageRequest.of(0, LOTE)));
        int reembolsados = 0;
        for (Pago pago : pendientes == null ? List.<Pago>of() : pendientes) {
            try {
                ResultadoReembolso resultado = pasarela.reembolsar(pago.getReferenciaPasarela(), pago.getMonto(),
                        "reembolso-pago-" + pago.getId());
                if (!resultado.exitoso()) {
                    log.warn("La pasarela no aplicó el reembolso del pago {}: {}", pago.getId(), resultado.mensaje());
                    continue;
                }
                transaccion.executeWithoutResult(estado -> registrar(pago.getId(), resultado.referencia()));
                reembolsados++;
            } catch (PasarelaNoDisponibleException e) {
                log.warn("Pasarela no disponible; el reembolso del pago {} se reintentará", pago.getId());
            }
        }
        return reembolsados;
    }

    private void registrar(Long pagoId, String referencia) {
        pagos.findById(pagoId)
                .filter(pago -> pago.getEstado() == EstadoPago.APROBADO)
                .ifPresent(pago -> {
                    pago.marcarReembolsado(referencia, OffsetDateTime.now(reloj));
                    ordenes.findById(pago.getOrdenId()).ifPresent(Orden::marcarReembolsada);
                });
    }
}
