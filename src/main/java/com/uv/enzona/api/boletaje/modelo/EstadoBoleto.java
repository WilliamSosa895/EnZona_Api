package com.uv.enzona.api.boletaje.modelo;

/** Un boleto VALIDO pasa a USADO una sola vez (RNF-02). */
public enum EstadoBoleto {
    VALIDO, USADO, CANCELADO, EXPIRADO
}
