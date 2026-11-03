package com.uv.enzona.api.identidad.modelo;

/** Para qué se emitió un código de un solo uso. Cada propósito se comprueba por separado. */
public enum PropositoCodigo {
    /** Activar la cuenta tras el registro (RF-02). */
    VERIFICACION,
    /** Restablecer la contraseña sin iniciar sesión («¿Olvidaste tu contraseña?»). */
    RECUPERACION
}
