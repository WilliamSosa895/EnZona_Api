package com.uv.enzona.api.comun.despliegue;

import com.uv.enzona.api.comun.config.DespliegueProperties;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Latido para alojamientos gratuitos (Render) que apagan la instancia tras 15 minutos sin
 * tráfico: cada 10 minutos la API se pide a sí misma {@code /actuator/health} por su dirección
 * pública, que es lo que el proveedor cuenta como tráfico. Sin {@code PUBLIC_URL} no hace nada.
 * No despierta una instancia ya dormida (para eso está el flujo programado del repositorio de
 * la app); evita que se duerma mientras está arriba.
 */
@Component
public class MantenerDespierta {

    private static final Logger log = LoggerFactory.getLogger(MantenerDespierta.class);
    private static final Duration TIEMPO_ESPERA = Duration.ofSeconds(20);

    private final DespliegueProperties despliegue;
    private final HttpClient http;

    public MantenerDespierta(DespliegueProperties despliegue) {
        this(despliegue, HttpClient.newBuilder().connectTimeout(TIEMPO_ESPERA).build());
    }

    MantenerDespierta(DespliegueProperties despliegue, HttpClient http) {
        this.despliegue = despliegue;
        this.http = http;
    }

    @Scheduled(fixedDelayString = "PT10M", initialDelayString = "PT5M")
    public void latir() {
        if (!despliegue.tieneUrlPublica()) {
            return;
        }
        URI salud = urlDeSalud(despliegue.urlPublica());
        try {
            HttpRequest peticion = HttpRequest.newBuilder(salud).timeout(TIEMPO_ESPERA).GET().build();
            int estado = http.send(peticion, HttpResponse.BodyHandlers.discarding()).statusCode();
            if (estado == 200) {
                log.debug("Latido a {} respondió {}", salud, estado);
            } else {
                log.warn("Latido a {} respondió {}", salud, estado);
            }
        } catch (IOException e) {
            log.warn("Latido a {} falló: {}", salud, e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** {@code https://host/} y {@code https://host} dan la misma dirección de salud. */
    static URI urlDeSalud(String urlPublica) {
        String base = urlPublica.trim();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return URI.create(base + "/actuator/health");
    }
}
