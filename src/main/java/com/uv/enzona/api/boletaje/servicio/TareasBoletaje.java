package com.uv.enzona.api.boletaje.servicio;

import com.uv.enzona.api.boletaje.modelo.EstadoBoleto;
import com.uv.enzona.api.boletaje.modelo.EstadoOrden;
import com.uv.enzona.api.boletaje.modelo.Orden;
import com.uv.enzona.api.boletaje.pasarela.PasarelaNoDisponibleException;
import com.uv.enzona.api.boletaje.pasarela.PasarelaPago;
import com.uv.enzona.api.boletaje.pasarela.ResultadoCobro;
import com.uv.enzona.api.boletaje.repositorio.BoletoRepository;
import com.uv.enzona.api.boletaje.repositorio.OrdenRepository;
import com.uv.enzona.api.eventos.modelo.EstadoEvento;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Tareas periódicas del boletaje. Nota: con varias instancias de la API cada una ejecutaría las
 * tareas; las operaciones son idempotentes, pero para escalar conviene un candado distribuido
 * (pendiente documentado).
 */
@Component
public class TareasBoletaje {

    private static final Logger log = LoggerFactory.getLogger(TareasBoletaje.class);

    private final OrdenRepository ordenes;
    private final BoletoRepository boletos;
    private final ConfirmacionService confirmaciones;
    private final ReembolsoService reembolsos;
    private final PasarelaPago pasarela;
    private final TransactionTemplate transaccion;
    private final Clock reloj;

    public TareasBoletaje(OrdenRepository ordenes, BoletoRepository boletos, ConfirmacionService confirmaciones,
            ReembolsoService reembolsos, PasarelaPago pasarela, TransactionTemplate transaccion, Clock reloj) {
        this.ordenes = ordenes;
        this.boletos = boletos;
        this.confirmaciones = confirmaciones;
        this.reembolsos = reembolsos;
        this.pasarela = pasarela;
        this.transaccion = transaccion;
        this.reloj = reloj;
    }

    /** Resuelve las órdenes cuya reserva venció: consulta a la pasarela y confirma o libera (CP-37). */
    @Scheduled(fixedDelayString = "PT1M", initialDelayString = "PT30S")
    public void resolverOrdenesVencidas() {
        List<Orden> vencidas = transaccion.execute(estado -> ordenes.vencidas(EstadoOrden.PENDIENTE,
                OffsetDateTime.now(reloj), PageRequest.of(0, 50)));
        for (Orden orden : vencidas == null ? List.<Orden>of() : vencidas) {
            try {
                resolver(orden);
            } catch (RuntimeException e) {
                log.error("No se pudo resolver la orden vencida {}", orden.getId(), e);
            }
        }
    }

    private void resolver(Orden orden) {
        if (orden.gratuita()) {
            confirmaciones.expirar(orden.getId());
            return;
        }
        ResultadoCobro cobro;
        try {
            cobro = pasarela.consultar(OrdenService.claveCobro(orden.getId()));
        } catch (PasarelaNoDisponibleException e) {
            confirmaciones.expirar(orden.getId());
            return;
        }
        switch (cobro.estado()) {
            case APROBADO -> confirmaciones.confirmarPago(orden.getId(), cobro.referencia());
            case RECHAZADO -> confirmaciones.rechazarPago(orden.getId(), cobro.referencia());
            case PENDIENTE, DESCONOCIDO -> confirmaciones.expirar(orden.getId());
        }
    }

    @Scheduled(fixedDelayString = "PT5M", initialDelayString = "PT2M")
    public void procesarReembolsos() {
        int reembolsados = reembolsos.procesarPendientes();
        if (reembolsados > 0) {
            log.info("{} pagos reembolsados", reembolsados);
        }
    }

    /** Los boletos sin usar de eventos realizados pasan a EXPIRADO. */
    @Scheduled(fixedDelayString = "PT15M", initialDelayString = "PT3M")
    public void expirarBoletos() {
        Integer expirados = transaccion.execute(estado -> boletos.expirarDeEventosRealizados(EstadoBoleto.VALIDO,
                EstadoBoleto.EXPIRADO, EstadoEvento.REALIZADO));
        if (expirados != null && expirados > 0) {
            log.info("{} boletos expirados", expirados);
        }
    }
}
