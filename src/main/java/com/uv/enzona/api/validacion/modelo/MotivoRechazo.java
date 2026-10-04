package com.uv.enzona.api.validacion.modelo;

/** Causa por la que el validador niega el acceso; el mensaje es el que ve el personal en la puerta. */
public enum MotivoRechazo {
    FORMATO_INVALIDO("El código no es un boleto de EnZona"),
    BOLETO_INEXISTENTE("El boleto no existe"),
    FIRMA_INVALIDA("Boleto falso: la firma no coincide"),
    OTRO_EVENTO("El boleto es de otro evento"),
    EVENTO_CANCELADO("Evento cancelado"),
    YA_UTILIZADO("El boleto ya fue utilizado"),
    CANCELADO("El boleto está cancelado"),
    EXPIRADO("El boleto expiró");

    private final String mensaje;

    MotivoRechazo(String mensaje) {
        this.mensaje = mensaje;
    }

    public String mensaje() {
        return mensaje;
    }
}
