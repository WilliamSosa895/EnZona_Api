package com.uv.enzona.api.boletaje.servicio;

import java.util.Optional;
import java.util.regex.Pattern;

/** Contenido leído de un código QR: "codigo.firma". */
public record QrBoleto(String codigo, String firma) {

    private static final Pattern FORMATO = Pattern.compile("^[A-Za-z0-9_-]{4,80}\\.[A-Za-z0-9_-]{16,128}$");

    public static Optional<QrBoleto> leer(String contenido) {
        if (contenido == null) {
            return Optional.empty();
        }
        String limpio = contenido.trim();
        if (!FORMATO.matcher(limpio).matches()) {
            return Optional.empty();
        }
        int punto = limpio.indexOf('.');
        return Optional.of(new QrBoleto(limpio.substring(0, punto), limpio.substring(punto + 1)));
    }
}
