package com.uv.enzona.api.boletaje.pasarela;

/** La pasarela no respondió. La orden queda pendiente y la tarea programada la resuelve después. */
public class PasarelaNoDisponibleException extends RuntimeException {

    public PasarelaNoDisponibleException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
