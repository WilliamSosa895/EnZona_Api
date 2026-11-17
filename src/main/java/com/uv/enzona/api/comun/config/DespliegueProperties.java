package com.uv.enzona.api.comun.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Datos del despliegue que la API necesita saber de sí misma.
 *
 * @param urlPublica dirección pública con la que llegan los clientes (por ejemplo
 *                   {@code https://enzona-api.onrender.com}). Vacía en desarrollo. Cuando está
 *                   definida, {@link com.uv.enzona.api.comun.despliegue.MantenerDespierta} se
 *                   pide a sí misma {@code /actuator/health} cada pocos minutos para que el plan
 *                   gratuito del proveedor no apague la instancia por falta de tráfico.
 */
@ConfigurationProperties(prefix = "enzona.despliegue")
public record DespliegueProperties(String urlPublica) {

    public boolean tieneUrlPublica() {
        return urlPublica != null && !urlPublica.isBlank();
    }
}
