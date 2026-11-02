package com.uv.enzona.api.eventos.imagenes;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uv.enzona.api.comun.config.EnzonaProperties;
import com.uv.enzona.api.comun.error.ApiException;
import com.uv.enzona.api.comun.error.ErrorCodigo;

/**
 * Moderación con Sightengine (https://sightengine.com): la imagen se envía desde
 * el servidor con {@code SIGHTENGINE_USER} y {@code SIGHTENGINE_SECRET} y se
 * rechaza si la probabilidad de desnudez o contenido sexual, violencia gráfica
 * (gore), violencia o autolesión supera el umbral configurado.
 *
 * Si el servicio no responde, la imagen se rechaza (nunca se publica sin revisar).
 */
@Component
@ConditionalOnProperty(name = "enzona.imagenes.moderacion", havingValue = "sightengine")
public class ModeradorSightengine implements ModeradorImagenes {

    private static final Logger log = LoggerFactory.getLogger(ModeradorSightengine.class);
    private static final String MODELOS = "nudity-2.1,gore-2.0,violence,self-harm";

    private final String usuario;
    private final String secreto;
    private final double umbral;
    private final HttpClient http;
    private final ObjectMapper json;

    @Autowired
    public ModeradorSightengine(EnzonaProperties propiedades, ObjectMapper json) {
        this(propiedades.imagenes().sightengineUsuario(), propiedades.imagenes().sightengineSecreto(),
                propiedades.imagenes().umbralModeracion(), HttpClient.newHttpClient(), json);
        if (usuario == null || usuario.isBlank() || secreto == null || secreto.isBlank()) {
            throw new IllegalStateException("IMAGE_MODERATION=sightengine requiere SIGHTENGINE_USER y SIGHTENGINE_SECRET");
        }
    }

    ModeradorSightengine(String usuario, String secreto, double umbral, HttpClient http, ObjectMapper json) {
        this.usuario = usuario;
        this.secreto = secreto;
        this.umbral = umbral;
        this.http = http;
        this.json = json;
    }

    @Override
    public Veredicto moderar(byte[] datos, String tipoMime) {
        String limite = "----EnZona" + UUID.randomUUID();
        List<byte[]> partes = new ArrayList<>();
        Map.of("models", MODELOS, "api_user", usuario, "api_secret", secreto)
                .forEach((clave, valor) -> partes.add(campo(limite, clave, valor)));
        partes.add(("--" + limite + "\r\nContent-Disposition: form-data; name=\"media\"; filename=\"imagen\"\r\n"
                + "Content-Type: " + tipoMime + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        partes.add(datos);
        partes.add(("\r\n--" + limite + "--\r\n").getBytes(StandardCharsets.UTF_8));

        HttpRequest peticion = HttpRequest.newBuilder()
                .uri(URI.create("https://api.sightengine.com/1.0/check.json"))
                .header("Content-Type", "multipart/form-data; boundary=" + limite)
                .POST(HttpRequest.BodyPublishers.ofByteArray(concatenar(partes)))
                .build();
        try {
            HttpResponse<String> respuesta = http.send(peticion, HttpResponse.BodyHandlers.ofString());
            JsonNode cuerpo = json.readTree(respuesta.body());
            if (respuesta.statusCode() / 100 != 2 || !"success".equals(cuerpo.path("status").asText())) {
                log.warn("Sightengine respondió {}: {}", respuesta.statusCode(), respuesta.body());
                throw new ApiException(ErrorCodigo.SERVICIO_EXTERNO_NO_DISPONIBLE,
                        "No se pudo revisar la imagen; inténtalo de nuevo más tarde");
            }
            return evaluar(cuerpo, umbral);
        } catch (IOException e) {
            throw new ApiException(ErrorCodigo.SERVICIO_EXTERNO_NO_DISPONIBLE, "No se pudo contactar al servicio de moderación");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException(ErrorCodigo.SERVICIO_EXTERNO_NO_DISPONIBLE, "La revisión de la imagen se interrumpió");
        }
    }

    /**
     * Regla de decisión sobre la respuesta de Sightengine. Separada para probarla con
     * respuestas de ejemplo sin llamar al servicio.
     */
    static Veredicto evaluar(JsonNode respuesta, double umbral) {
        JsonNode desnudez = respuesta.path("nudity");
        double sexual = desnudez.path("sexual_activity").asDouble(0) + desnudez.path("sexual_display").asDouble(0)
                + desnudez.path("erotica").asDouble(0);
        if (sexual >= umbral) {
            return Veredicto.rechazo("La imagen contiene desnudez o contenido sexual");
        }
        if (respuesta.path("gore").path("prob").asDouble(0) >= umbral) {
            return Veredicto.rechazo("La imagen contiene violencia gráfica");
        }
        if (respuesta.path("violence").path("prob").asDouble(0) >= umbral) {
            return Veredicto.rechazo("La imagen contiene violencia");
        }
        if (respuesta.path("self-harm").path("prob").asDouble(0) >= umbral) {
            return Veredicto.rechazo("La imagen muestra autolesiones");
        }
        return Veredicto.aprobada();
    }

    private static byte[] campo(String limite, String nombre, String valor) {
        return ("--" + limite + "\r\nContent-Disposition: form-data; name=\"" + nombre + "\"\r\n\r\n" + valor + "\r\n")
                .getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] concatenar(List<byte[]> partes) {
        int total = partes.stream().mapToInt(p -> p.length).sum();
        byte[] todo = new byte[total];
        int posicion = 0;
        for (byte[] parte : partes) {
            System.arraycopy(parte, 0, todo, posicion, parte.length);
            posicion += parte.length;
        }
        return todo;
    }
}
