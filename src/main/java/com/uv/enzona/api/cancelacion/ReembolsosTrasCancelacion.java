package com.uv.enzona.api.cancelacion;

import com.uv.enzona.api.boletaje.servicio.ReembolsoService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Inicia los reembolsos en cuanto se confirma la cancelación; la tarea programada reintenta los fallidos. */
@Component
public class ReembolsosTrasCancelacion {

    private final ReembolsoService reembolsos;

    public ReembolsosTrasCancelacion(ReembolsoService reembolsos) {
        this.reembolsos = reembolsos;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alCancelar(EventoCancelado evento) {
        reembolsos.procesarPendientes();
    }
}
