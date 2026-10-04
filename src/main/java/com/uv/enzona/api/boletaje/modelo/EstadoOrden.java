package com.uv.enzona.api.boletaje.modelo;

/** Orden de compra o confirmación. Una orden gratuita confirmada queda PAGADA con total 0. */
public enum EstadoOrden {
    PENDIENTE, PAGADA, CANCELADA, REEMBOLSADA
}
