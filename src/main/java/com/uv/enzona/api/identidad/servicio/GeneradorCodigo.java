package com.uv.enzona.api.identidad.servicio;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

/** Genera códigos numéricos de 6 dígitos con un generador criptográficamente seguro. */
@Component
public class GeneradorCodigo {

    private final SecureRandom aleatorio = new SecureRandom();

    public String generar() {
        return String.format("%06d", aleatorio.nextInt(1_000_000));
    }
}
