package com.uv.enzona.api.identidad.servicio;

import java.util.Locale;

/** Los correos se comparan sin distinguir mayúsculas y sin espacios. */
public final class Correos {

    private Correos() {
    }

    public static String normalizar(String correo) {
        return correo == null ? null : correo.trim().toLowerCase(Locale.ROOT);
    }
}
